import { useEffect, useRef, useState } from 'react'
import type { ServerItem } from '../../lib/instances'
import {
  getServerHistoryMetrics,
  getServerRealtimeMetrics,
  subscribeServerMetricStream,
  type ServerHistoryMetrics,
  type ServerRealtimeCurrent,
  type ServerRealtimeMetrics,
  type ServerRealtimeSeriesPoint,
  type ServerSseStreamEvent,
} from '../../lib/servers'
import StatusDot, { STREAM_STATUS_CONFIG, type StreamStatus } from '../ui/StatusDot'
import DatePicker from '../ui/DatePicker'
import OverviewTab from './OverviewTab'
import HttpApiTab from './HttpApiTab'
import JvmTab from './JvmTab'
import HikariTab from './HikariTab'
import ThreadPoolTab from './ThreadPoolTab'

const INITIAL_FETCH_POINTS = 30 // 초기 5분치 (30개) 조회
const MAX_SERIES_POINTS = 90 // 최근 15분치 (최대 90개) 누적 및 유지

function formatDate(d: Date) {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

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

function formatMb(bytes: number | undefined | null) {
  return bytes ? Math.round(bytes / 1024 / 1024) : 0
}

function shortTime(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}

function yesterdayDate() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  return formatDate(d)
}

function shortHourMin(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

export default function AppDetailView({ server }: { server: ServerItem }) {
  const [tab, setTab] = useState<TabKey>('overview')
  const [mode, setMode] = useState<'realtime' | 'history'>('realtime')
  const [historyDate, setHistoryDate] = useState(yesterdayDate())
  const [historyData, setHistoryData] = useState<ServerHistoryMetrics | null>(null)

  const [metrics, setMetrics] = useState<ServerRealtimeMetrics | null>(null)
  const [streamStatus, setStreamStatus] = useState<StreamStatus>('syncing')
  const [threads, setThreads] = useState<{ live: number; blocked: number }>({ live: 0, blocked: 0 })
  const uptimeRef = useRef(0)

  // 1. 실시간 스트림 (과거 지표를 보는 동안에도 백그라운드에서 유지)
  useEffect(() => {
    setMetrics(null)
    setStreamStatus('syncing')
    uptimeRef.current = 0

    getServerRealtimeMetrics(server.serverId, INITIAL_FETCH_POINTS)
      .then((res) => {
        uptimeRef.current = res.current?.processUptimeSeconds ?? 0
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

  // 2. 과거 지표 조회
  useEffect(() => {
    if (mode === 'history') {
      getServerHistoryMetrics(server.serverId, historyDate)
        .then(setHistoryData)
        .catch(() => setHistoryData(null))
    }
  }, [mode, server.serverId, historyDate])

  const isRealtime = mode === 'realtime'

  // 실시간 시리즈 vs 과거 시리즈 통일 매핑
  const series: ServerRealtimeSeriesPoint[] = isRealtime
    ? (metrics?.series ?? [])
    : (historyData?.series ?? []).map((s) => ({
        collectedAt: s.statTime,
        jvmHeapUsedBytes: s.jvmHeapUsedBytes ?? 0,
        jvmHeapMaxBytes: s.jvmHeapMaxBytes ?? 0,
        jvmOldGenUsedBytes: s.jvmOldGenUsedBytes ?? 0,
        gcPauseSecondsSum: s.gcPauseSecondsSum ?? 0,
        totalRps: s.totalRpsAvg ?? 0,
        avgLatencyMs: s.avgLatencyMs ?? 0,
        hikaricpActiveTotal: s.hikaricpActiveAvg ?? 0,
        executorActiveTotal: s.executorActiveAvg ?? 0,
      }))

  const latestSeries = series.length > 0 ? series[series.length - 1] : null

  // 타임라인 차트 데이터 구성
  const rpsLatencyTimeline = series.map((s) => ({
    time: isRealtime ? shortTime(s.collectedAt) : shortHourMin(s.collectedAt),
    rps: s.totalRps ?? 0,
    latencyMs: s.avgLatencyMs ?? 0,
  }))

  const heapTimeline = series.map((s) => ({
    time: isRealtime ? shortTime(s.collectedAt) : shortHourMin(s.collectedAt),
    heapMb: formatMb(s.jvmHeapUsedBytes),
  }))

  const hikariTimeline = series.map((s) => ({
    time: isRealtime ? shortTime(s.collectedAt) : shortHourMin(s.collectedAt),
    active: s.hikaricpActiveTotal ?? 0,
  }))

  const executorTimeline = series.map((s) => ({
    time: isRealtime ? shortTime(s.collectedAt) : shortHourMin(s.collectedAt),
    active: s.executorActiveTotal ?? 0,
  }))

  // 현재 또는 과거 요약 수치
  const current = metrics?.current
  const historySummary = historyData?.summary

  // JVM Heap 계산
  const heapUsagePct = isRealtime
    ? latestSeries?.jvmHeapMaxBytes
      ? ((latestSeries.jvmHeapUsedBytes / latestSeries.jvmHeapMaxBytes) * 100).toFixed(1)
      : '0'
    : historySummary?.jvm?.heapUsedMaxBytes
      ? (((historySummary.jvm.heapUsedAvgBytes ?? 0) / historySummary.jvm.heapUsedMaxBytes) * 100).toFixed(1)
      : '0'

  // HikariCP 계산
  const totalHikariActive = isRealtime
    ? (current?.hikaricpPools.reduce((sum, p) => sum + p.active, 0) ?? 0)
    : (historySummary?.hikaricpPools.reduce((sum, p) => sum + (p.activePoolAvg ?? 0), 0) ?? 0)

  const totalHikariMax = isRealtime
    ? (current?.hikaricpPools.reduce((sum, p) => sum + p.max, 0) ?? 0)
    : (historySummary?.hikaricpPools.reduce((sum, p) => sum + (p.activePoolMax ?? 0), 0) ?? 0)

  const totalHikariIdle = isRealtime
    ? (current?.hikaricpPools.reduce((sum, p) => sum + p.idle, 0) ?? 0)
    : 0

  const totalHikariPending = isRealtime
    ? (current?.hikaricpPools.reduce((sum, p) => sum + p.pending, 0) ?? 0)
    : (historySummary?.hikaricpPools.reduce((sum, p) => sum + (p.pendingThreadsMax ?? 0), 0) ?? 0)

  // ThreadPool 계산
  const UNBOUNDED_THRESHOLD = 100_000_000
  const boundedExecutors = current?.executors.filter((e) => e.max < UNBOUNDED_THRESHOLD) ?? []
  const hasUnboundedExecutor = (current?.executors.length ?? 0) > boundedExecutors.length

  const totalExecActive = isRealtime
    ? (current?.executors.reduce((sum, e) => sum + e.active, 0) ?? 0)
    : (historySummary?.executors.reduce((sum, e) => sum + (e.activeThreadsAvg ?? 0), 0) ?? 0)

  const totalExecMax = boundedExecutors.reduce((sum, e) => sum + e.max, 0)
  const totalExecQueued = isRealtime
    ? (current?.executors.reduce((sum, e) => sum + e.queuedTasks, 0) ?? 0)
    : (historySummary?.executors.reduce((sum, e) => sum + (e.queuedTasksAvg ?? 0), 0) ?? 0)

  const totalExecRemaining = boundedExecutors.reduce((sum, e) => sum + e.queueRemaining, 0)
  const totalExecMaxLabel = hasUnboundedExecutor ? `${totalExecMax}+∞` : `${totalExecMax}`
  const totalExecRemainingLabel = hasUnboundedExecutor ? `${totalExecRemaining}+∞` : `${totalExecRemaining}`

  // HTTP 성능 지표
  const totalRps = isRealtime
    ? (current?.httpEndpoints.reduce((sum, e) => sum + e.rps, 0) ?? 0)
    : (historySummary?.httpEndpoints.reduce((sum, e) => sum + (e.rpsAvg ?? 0), 0) ?? 0)

  const avgLatency = isRealtime
    ? current?.httpEndpoints.length
      ? current.httpEndpoints.reduce((sum, e) => sum + e.avgLatencyMs, 0) / current.httpEndpoints.length
      : 0
    : historySummary?.httpEndpoints.length
      ? historySummary.httpEndpoints.reduce((sum, e) => sum + (e.avgResTimeMs ?? 0), 0) / historySummary.httpEndpoints.length
      : 0

  const maxLatency = isRealtime
    ? current?.httpEndpoints.length
      ? Math.max(...current.httpEndpoints.map((e) => e.maxLatencyMs))
      : 0
    : historySummary?.httpEndpoints.length
      ? Math.max(...historySummary.httpEndpoints.map((e) => e.maxResTimeMs ?? 0))
      : 0

  const avgErrorRate = isRealtime
    ? current?.httpEndpoints.length
      ? current.httpEndpoints.reduce((sum, e) => sum + e.errorRatePct, 0) / current.httpEndpoints.length
      : 0
    : historySummary?.httpEndpoints.length
      ? historySummary.httpEndpoints.reduce((sum, e) => sum + (e.errorRateAvg ?? 0), 0) / historySummary.httpEndpoints.length
      : 0

  return (
    <div className="w-full flex-1 flex flex-col min-h-0">
      {/* 헤더 */}
      <div className="mb-3 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 shrink-0">
        <div>
          <h1 className="flex items-baseline gap-3 text-xl font-bold text-slate-900">
            {server.name}
            <span className="text-sm font-normal text-slate-400">
              포트 {server.port ?? '—'}
            </span>
          </h1>
        </div>

        {/* 우측 컨트롤 바 */}
        <div className="flex flex-wrap items-center gap-2.5">
          {/* 모드 전환 세그먼트 버튼 */}
          <div className="flex rounded-lg border border-slate-200 bg-slate-100 p-0.5 text-xs font-semibold">
            <button
              type="button"
              onClick={() => setMode('realtime')}
              className={`rounded-md px-3 py-1 transition-all ${
                isRealtime
                  ? 'bg-white text-slate-900 shadow-2xs font-bold'
                  : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              실시간
            </button>
            <button
              type="button"
              onClick={() => setMode('history')}
              className={`rounded-md px-3 py-1 transition-all ${
                !isRealtime
                  ? 'bg-white text-slate-900 shadow-2xs font-bold'
                  : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              과거
            </button>
          </div>

          {/* 실시간 모드일 때: 최근 15분 뱃지 */}
          {isRealtime ? (
            <span className="inline-flex items-center rounded-md border border-slate-300 bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-700 shadow-2xs">
              최근 15분
            </span>
          ) : (
            /* 과거 지표 모드일 때: 커스텀 캘린더 */
            <DatePicker
              value={historyDate}
              maxDate={formatDate(new Date())}
              onChange={setHistoryDate}
            />
          )}

          <StatusDot
            tone={STREAM_STATUS_CONFIG[streamStatus].tone}
            label={isRealtime ? STREAM_STATUS_CONFIG[streamStatus].label : '실시간 수신중'}
          />
        </div>
      </div>

      {/* 탭 내비게이션 */}
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

      {/* 탭 콘텐츠 영역 */}
      <div className="flex-1 flex flex-col min-h-0">
        {tab === 'overview' && (
          <OverviewTab
            onNavigateTab={(k) => setTab(k as TabKey)}
            rpsLatencyTimeline={rpsLatencyTimeline}
            heapTimeline={heapTimeline}
            hikariTimeline={hikariTimeline}
            executorTimeline={executorTimeline}
            http={{ rps: totalRps, avgMs: avgLatency, errorRate: avgErrorRate }}
            jvm={{
              heapUsage: heapUsagePct,
              oldGenMb: isRealtime
                ? formatMb(latestSeries?.jvmOldGenUsedBytes)
                : formatMb(historySummary?.jvm?.oldGenUsedAvgBytes),
              gcPauseS: isRealtime
                ? (latestSeries?.gcPauseSecondsSum ?? 0)
                : (historySummary?.jvm?.gcPauseSecondsSum ?? 0),
            }}
            hikari={{
              active: totalHikariActive,
              max: totalHikariMax,
              idle: totalHikariIdle,
              pending: totalHikariPending,
            }}
            threadPool={{
              active: totalExecActive,
              max: totalExecMaxLabel,
              queued: totalExecQueued,
              remaining: totalExecRemainingLabel,
            }}
          />
        )}
        {tab === 'http' && (
          <HttpApiTab
            endpoints={
              isRealtime
                ? (current?.httpEndpoints ?? [])
                : (historySummary?.httpEndpoints.map((ep) => ({
                    uri: ep.uri,
                    method: ep.method,
                    status: '',
                    requestsCount: ep.totalRequestsCount,
                    rps: ep.rpsAvg,
                    avgLatencyMs: ep.avgResTimeMs,
                    maxLatencyMs: ep.maxResTimeMs,
                    errorRatePct: ep.errorRateAvg,
                  })) ?? [])
            }
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
            pools={
              isRealtime
                ? (current?.hikaricpPools ?? [])
                : (historySummary?.hikaricpPools.map((p) => ({
                    poolName: p.poolName,
                    active: p.activePoolAvg,
                    max: p.activePoolMax,
                    idle: 0,
                    pending: p.pendingThreadsMax,
                    timeoutsTotal: p.timeoutCountSum,
                  })) ?? [])
            }
            series={series}
            totalActive={totalHikariActive}
            totalMax={totalHikariMax}
            totalIdle={totalHikariIdle}
            totalPending={totalHikariPending}
          />
        )}
        {tab === 'threadpool' && (
          <ThreadPoolTab
            executors={
              isRealtime
                ? (current?.executors ?? [])
                : (historySummary?.executors.map((e) => ({
                    name: e.name,
                    active: e.activeThreadsAvg,
                    max: e.maxThreadsAvg,
                    queuedTasks: e.queuedTasksAvg,
                    queueRemaining: 0,
                  })) ?? [])
            }
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