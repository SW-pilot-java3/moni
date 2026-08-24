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
import StatusDot, { STREAM_STATUS_CONFIG, type StreamStatus } from '../ui/StatusDot'
import OverviewTab from './OverviewTab'
import HttpApiTab from './HttpApiTab'
import JvmTab from './JvmTab'
import HikariTab from './HikariTab'
import ThreadPoolTab from './ThreadPoolTab'

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
    hikaricpPools: event.hikaricpPools.map((p) => ({ ...p, timeoutsTotal: 0 })),
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
    threadsLive: event.jvm.threadsLive,
    threadsBlocked: event.jvm.threadsBlocked,
    totalRps: event.summary.totalRps,
    avgLatencyMs: event.summary.avgLatencyMs,
    hikaricpActiveTotal: event.summary.hikaricpActiveTotal,
    executorActiveTotal: event.summary.executorActiveTotal,
  }
}

const tabs = [
  { key: 'overview', label: 'Overview' },
  { key: 'http', label: 'HTTP · API' },
  { key: 'jvm', label: 'JVM' },
  { key: 'hikari', label: 'HikariCP' },
  { key: 'threadpool', label: 'ThreadPool' },
] as const

type TabKey = (typeof tabs)[number]['key']

function formatMb(bytes: number | undefined) {
  return bytes ? Math.round(bytes / 1024 / 1024) : 0
}

function shortTime(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}

export default function AppDetailView({ server }: { server: ServerItem }) {
  const [tab, setTab] = useState<TabKey>('overview')
  const [metrics, setMetrics] = useState<ServerRealtimeMetrics | null>(null)
  const [streamStatus, setStreamStatus] = useState<StreamStatus>('syncing')
  const [threads, setThreads] = useState<{ live: number; blocked: number }>({ live: 0, blocked: 0 })
  const uptimeRef = useRef(0)

  useEffect(() => {
    setMetrics(null)
    setStreamStatus('syncing')
    uptimeRef.current = 0

    getServerRealtimeMetrics(server.serverId, MAX_SERIES_POINTS)
      .then((res) => {
        uptimeRef.current = res.current?.processUptimeSeconds ?? 0
        // SSE가 REST 응답보다 먼저 도착해 쌓아둔 포인트가 있으면 히스토리 뒤에 이어붙이고,
        // REST 응답으로 통째로 덮어써서 유실되지 않도록 한다.
        setMetrics((prev) => {
          if (!prev) return res
          const existingTimes = new Set(res.series.map((p) => p.collectedAt))
          const extra = prev.series.filter((p) => !existingTimes.has(p.collectedAt))
          return {
            current: prev.current ?? res.current,
            series: [...res.series, ...extra].slice(-MAX_SERIES_POINTS),
          }
        })
      })
      .catch(() => {
        setStreamStatus('disconnected')
      })

    const unsubscribe = subscribeServerMetricStream(
      server.serverId,
      (event) => {
        setStreamStatus('connected')
        setThreads({ live: event.jvm.threadsLive ?? 0, blocked: event.jvm.threadsBlocked ?? 0 })
        const current = { ...toCurrent(event), processUptimeSeconds: uptimeRef.current }
        setMetrics((prev) => ({
          current,
          series: [...(prev?.series ?? []), toSeriesPoint(event)].slice(-MAX_SERIES_POINTS),
        }))
      },
      undefined,
      () => {
        setStreamStatus('disconnected')
      },
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
  const maxLatency = current?.httpEndpoints.length
    ? Math.max(...current.httpEndpoints.map((e) => e.maxLatencyMs))
    : 0
  const avgErrorRate = current?.httpEndpoints.length
    ? current.httpEndpoints.reduce((sum, e) => sum + e.errorRatePct, 0) / current.httpEndpoints.length
    : 0

  return (
    <div className="w-full flex-1 flex flex-col min-h-0">
      <div className="mb-3 flex items-start justify-between shrink-0">
        <div>
          <h1 className="flex items-baseline gap-3 text-xl font-bold text-slate-900">
            {server.name}
            <span className="text-sm font-normal text-slate-400">
              포트 {server.port ?? '—'}
            </span>
          </h1>
        </div>
        <div className="flex items-center gap-2">
          <StatusDot
            tone={STREAM_STATUS_CONFIG[streamStatus].tone}
            label={STREAM_STATUS_CONFIG[streamStatus].label}
          />
        </div>
      </div>

      <div className="mb-4 flex gap-1 border-b border-slate-200 text-sm font-medium shrink-0">
        {tabs.map((t) => (
          <button
            key={t.key}
            type="button"
            onClick={() => setTab(t.key)}
            className={`-mb-px flex items-center gap-1.5 border-b-2 px-3 pb-3 ${
              tab === t.key ? 'border-brand-500 text-brand-600' : 'border-transparent text-slate-400 hover:text-slate-600'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="flex-1 flex flex-col min-h-0">
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
            maxLatency={maxLatency}
            avgErrorRate={avgErrorRate}
          />
        )}
        {tab === 'jvm' && (
          <JvmTab
            current={current}
            series={series}
            latest={latestSeries}
            threadsLive={threads.live}
            threadsBlocked={threads.blocked}
          />
        )}
        {tab === 'hikari' && (
          <HikariTab
            pools={current?.hikaricpPools ?? []}
            series={series}
            totalActive={totalHikariActive}
            totalMax={totalHikariMax}
            totalIdle={totalHikariIdle}
            totalPending={totalHikariPending}
          />
        )}
        {tab === 'threadpool' && (
          <ThreadPoolTab
            executors={current?.executors ?? []}
            series={series}
            totalActive={totalExecActive}
            totalMaxLabel={totalExecMaxLabel}
            totalQueued={totalExecQueued}
            totalRemainingLabel={totalExecRemainingLabel}
          />
        )}
      </div>
    </div>
  )
}