import { useEffect, useRef, useState } from 'react'
import type { ServerItem } from '../../lib/instances'
import {
  getServerRealtimeMetrics,
  subscribeServerMetricStream,
  type ServerRealtimeCurrent,
  type ServerRealtimeMetrics,
  type ServerRealtimeSeriesPoint,
  type ServerSseStreamEvent,
} from '../../lib/servers'
import StatusDot from '../ui/StatusDot'
import OverviewTab from './OverviewTab'
import HttpApiTab from './HttpApiTab'
import SimpleDetailTab from './SimpleDetailTab'

const MAX_SERIES_POINTS = 30

function toCurrent(event: ServerSseStreamEvent): ServerRealtimeCurrent {
  return {
    collectedAt: event.collectedAt,
    processUptimeSeconds: 0,
    httpEndpoints: event.httpEndpoints.map((ep) => ({
      uri: ep.uri,
      method: ep.method,
      status: '',
      requestsCount: ep.requestsCount,
      rps: ep.rps,
      avgLatencyMs: ep.avgLatencyMs,
      maxLatencyMs: ep.maxLatencyMs,
      errorRatePct: ep.errorRatePct,
    })),
    hikaricpPools: event.hikaricpPools,
    executors: event.executors,
  }
}

function toSeriesPoint(event: ServerSseStreamEvent): ServerRealtimeSeriesPoint {
  return {
    collectedAt: event.collectedAt,
    jvmHeapUsedBytes: event.jvm.heapUsedBytes,
    jvmHeapMaxBytes: event.jvm.heapMaxBytes,
    jvmOldGenUsedBytes: 0,
    gcPauseSecondsSum: event.jvm.gcPauseSecondsSum,
    totalRps: event.summary.totalRps,
    avgLatencyMs: event.summary.avgLatencyMs,
    hikaricpActiveTotal: event.summary.hikaricpActiveTotal,
    executorActiveTotal: event.summary.executorActiveTotal,
  }
}

const tabs = [
  { key: 'overview', label: 'Overview (종합 개요)', icon: '📊' },
  { key: 'http', label: 'HTTP · API', icon: '🌐' },
  { key: 'jvm', label: 'JVM', icon: '🩷' },
  { key: 'hikari', label: 'HikariCP', icon: '🗄️' },
  { key: 'threadpool', label: 'ThreadPool', icon: '⚡' },
] as const

type TabKey = (typeof tabs)[number]['key']

function formatMb(bytes: number | undefined) {
  return bytes ? Math.round(bytes / 1024 / 1024) : 0
}

function shortTime(iso: string) {
  return new Date(iso).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

export default function AppDetailView({ server }: { server: ServerItem }) {
  const [tab, setTab] = useState<TabKey>('overview')
  const [metrics, setMetrics] = useState<ServerRealtimeMetrics | null>(null)
  const [connected, setConnected] = useState(false)
  const uptimeRef = useRef(0)

  useEffect(() => {
    setMetrics(null)
    setConnected(false)
    uptimeRef.current = 0

    getServerRealtimeMetrics(server.serverId, MAX_SERIES_POINTS).then((res) => {
      uptimeRef.current = res.current?.processUptimeSeconds ?? 0
      setMetrics(res)
    })

    const unsubscribe = subscribeServerMetricStream(
      server.serverId,
      (event) => {
        setConnected(true)
        const current = { ...toCurrent(event), processUptimeSeconds: uptimeRef.current }
        setMetrics((prev) => ({
          current,
          series: [...(prev?.series ?? []), toSeriesPoint(event)].slice(-MAX_SERIES_POINTS),
        }))
      },
      () => setConnected(false),
    )

    return unsubscribe
  }, [server.serverId])

  const current = metrics?.current
  const series = metrics?.series ?? []
  const latestSeries = series.length > 0 ? series[series.length - 1] : null

  const heapTimeline = series.map((p) => ({ time: shortTime(p.collectedAt), heapMb: formatMb(p.jvmHeapUsedBytes) }))
  const hikariTimeline = series.map((p) => ({ time: shortTime(p.collectedAt), active: p.hikaricpActiveTotal }))
  const executorTimeline = series.map((p) => ({ time: shortTime(p.collectedAt), active: p.executorActiveTotal }))
  const rpsLatencyTimeline = series.map((p) => ({ time: shortTime(p.collectedAt), rps: p.totalRps, latencyMs: p.avgLatencyMs }))

  const heapUsagePct =
    latestSeries && latestSeries.jvmHeapMaxBytes > 0
      ? ((latestSeries.jvmHeapUsedBytes / latestSeries.jvmHeapMaxBytes) * 100).toFixed(1)
      : '—'

  const totalHikariActive = current?.hikaricpPools.reduce((sum, p) => sum + p.active, 0) ?? 0
  const totalHikariMax = current?.hikaricpPools.reduce((sum, p) => sum + p.max, 0) ?? 0
  const totalHikariIdle = current?.hikaricpPools.reduce((sum, p) => sum + p.idle, 0) ?? 0
  const totalHikariPending = current?.hikaricpPools.reduce((sum, p) => sum + p.pending, 0) ?? 0

  // 스케줄러 등 무제한 큐를 쓰는 executor는 max/queueRemaining이 Integer.MAX_VALUE에 가까운 관례값으로 오므로
  // 유한한 executor들과 합산하면 의미 없는 값이 됨 — 무제한 executor는 합산에서 빼고 별도로 표시한다.
  const UNBOUNDED_THRESHOLD = 100_000_000
  const boundedExecutors = current?.executors.filter((e) => e.max < UNBOUNDED_THRESHOLD) ?? []
  const hasUnboundedExecutor = (current?.executors.length ?? 0) > boundedExecutors.length

  const totalExecActive = current?.executors.reduce((sum, e) => sum + e.active, 0) ?? 0
  const totalExecMax = boundedExecutors.reduce((sum, e) => sum + e.max, 0)
  const totalExecQueued = current?.executors.reduce((sum, e) => sum + e.queuedTasks, 0) ?? 0
  const totalExecRemaining = boundedExecutors.reduce((sum, e) => sum + e.queueRemaining, 0)
  const totalExecMaxLabel = hasUnboundedExecutor ? `${totalExecMax}+∞` : `${totalExecMax}`
  const totalExecRemainingLabel = hasUnboundedExecutor ? `${totalExecRemaining}+∞` : `${totalExecRemaining}`

  const totalRps = current?.httpEndpoints.reduce((sum, e) => sum + e.rps, 0) ?? 0
  const avgLatency = current?.httpEndpoints.length
    ? current.httpEndpoints.reduce((sum, e) => sum + e.avgLatencyMs, 0) / current.httpEndpoints.length
    : 0
  const avgErrorRate = current?.httpEndpoints.length
    ? current.httpEndpoints.reduce((sum, e) => sum + e.errorRatePct, 0) / current.httpEndpoints.length
    : 0

  return (
    <div>
      <div className="mb-3 flex items-start justify-between">
        <div>
          <h1 className="flex items-baseline gap-3 text-xl font-bold text-slate-900">
            {server.name}
            <span className="text-sm font-normal text-slate-400">
              포트 {server.port ?? '—'}
            </span>
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <StatusDot tone={connected ? 'normal' : 'idle'} label={connected ? 'SSE 연결됨' : 'SSE 연결 대기'} />
        </div>
      </div>

      <div className="mb-5 flex gap-1 border-b border-slate-200 text-sm font-medium">
        {tabs.map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setTab(t.key)}
            className={`-mb-px flex items-center gap-1.5 border-b-2 px-3 pb-3 ${
              tab === t.key ? 'border-brand-500 text-brand-600' : 'border-transparent text-slate-400 hover:text-slate-600'
            }`}
          >
            <span>{t.icon}</span>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <OverviewTab
          onNavigateTab={(k) => setTab(k as TabKey)}
          rpsLatencyTimeline={rpsLatencyTimeline}
          heapTimeline={heapTimeline}
          hikariTimeline={hikariTimeline}
          executorTimeline={executorTimeline}
          http={{ rps: totalRps, avgMs: avgLatency, errorRate: avgErrorRate }}
          jvm={{ heapUsage: heapUsagePct, oldGenMb: formatMb(latestSeries?.jvmOldGenUsedBytes), gcPauseS: latestSeries?.gcPauseSecondsSum ?? 0 }}
          hikari={{ active: totalHikariActive, max: totalHikariMax, idle: totalHikariIdle, pending: totalHikariPending }}
          threadPool={{ active: totalExecActive, max: totalExecMaxLabel, queued: totalExecQueued, remaining: totalExecRemainingLabel }}
        />
      )}
      {tab === 'http' && (
        <HttpApiTab
          endpoints={current?.httpEndpoints ?? []}
          rpsLatencyTimeline={rpsLatencyTimeline}
          totalRps={totalRps}
          avgLatency={avgLatency}
          avgErrorRate={avgErrorRate}
        />
      )}
      {tab === 'jvm' && (
        <SimpleDetailTab
          title="🩷 JVM 메모리 & GC 상세"
          stats={[
            { label: 'Heap 사용률', value: `${heapUsagePct}%` },
            { label: 'Old Gen', value: `${formatMb(latestSeries?.jvmOldGenUsedBytes)} MB`, tone: 'warn' },
            { label: 'GC 정지 시간(누적)', value: `${(latestSeries?.gcPauseSecondsSum ?? 0).toFixed(3)}s` },
            { label: '가동 시간', value: current ? `${Math.round(current.processUptimeSeconds / 60)}분` : '—' },
          ]}
          data={heapTimeline}
          dataKey="heapMb"
          color="#9b8ac1"
        />
      )}
      {tab === 'hikari' && (
        <SimpleDetailTab
          title="🗄️ HikariCP DB 커넥션 풀 상세"
          stats={[
            { label: 'Active', value: `${totalHikariActive}` },
            { label: 'Max', value: `${totalHikariMax}` },
            { label: 'Idle 유휴', value: `${totalHikariIdle} 개` },
            { label: 'Pending 대기', value: `${totalHikariPending} 개` },
          ]}
          data={hikariTimeline}
          dataKey="active"
          color="#4a8c6f"
        />
      )}
      {tab === 'threadpool' && (
        <SimpleDetailTab
          title="⚡ ThreadPool 비동기 스레드 상세"
          stats={[
            { label: 'Active', value: `${totalExecActive} / ${totalExecMaxLabel}`, tone: 'warn' },
            { label: 'Queued 대기', value: `${totalExecQueued} 건` },
            { label: '남은 용량', value: `${totalExecRemainingLabel} 건` },
            { label: 'Max', value: totalExecMaxLabel },
          ]}
          data={executorTimeline}
          dataKey="active"
          color="#c8922f"
        />
      )}
    </div>
  )
}