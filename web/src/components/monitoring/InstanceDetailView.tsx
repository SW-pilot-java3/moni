import { useEffect, useState } from 'react'
import MiniAreaCard from './MiniAreaCard'
import StatusDot, { STREAM_STATUS_CONFIG, type StreamStatus } from '../ui/StatusDot'
import DatePicker from '../ui/DatePicker'
import { getTone, DEFAULT_THRESHOLDS } from '../../lib/thresholdUtils'
import {
  getInstanceHistoryMetrics,
  getInstanceRealtimeMetrics,
  getServerSummaryList,
  subscribeInstanceMetricStream,
  type InstanceHistoryMetrics,
  type InstanceListItem,
  type InstanceRealtimeMetricPoint,
  type ServerItem,
  type ServerSummaryItem,
} from '../../lib/instances'

const MAX_SERIES_POINTS = 90 // 최근 15분치 (최대 90개) 유지

function formatDate(d: Date) {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function yesterdayDate() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  return formatDate(d)
}

function formatBytes(bytes: number | null | undefined) {
  if (bytes === null || bytes === undefined) return '—'
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)}GB`
}

function formatBytesPerSec(bytesPerSec: number | null | undefined) {
  if (bytesPerSec === null || bytesPerSec === undefined) return '—'
  return `${(bytesPerSec / 1024 / 1024).toFixed(2)} MB/s`
}

function shortTime(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}

function shortDateHour(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

export default function InstanceDetailView({
  instance,
  apps,
  onSelectApp,
}: {
  instance: InstanceListItem
  apps: ServerItem[]
  onSelectApp: (serverId: number) => void
}) {
  const [mode, setMode] = useState<'realtime' | 'history'>('realtime')
  const [historyDate, setHistoryDate] = useState(yesterdayDate())
  const [historyData, setInstanceHistory] = useState<InstanceHistoryMetrics | null>(null)
  const [appSummaries, setAppSummaries] = useState<Record<number, ServerSummaryItem>>({})

  const [metrics, setMetrics] = useState<InstanceRealtimeMetricPoint[]>([])
  const [streamStatus, setStreamStatus] = useState<StreamStatus>('syncing')

  // 1. 인스턴스 소속 앱 요약 지표 조회 (5분 배치 집계 스냅샷 + 60초 주기 자동 폴링)
  useEffect(() => {
    const fetchSummaries = () => {
      getServerSummaryList(instance.instanceId)
        .then((list) => {
          const map: Record<number, ServerSummaryItem> = {}
          list.forEach((item) => {
            map[item.serverId] = item
          })
          setAppSummaries(map)
        })
        .catch(() => {})
    }

    fetchSummaries()
    const timer = setInterval(fetchSummaries, 60_000)
    return () => clearInterval(timer)
  }, [instance.instanceId])

  // 2. 실시간 스트림 (과거 지표를 보는 동안에도 백그라운드에서 계속 수신)
  useEffect(() => {
    setMetrics([])
    setStreamStatus('syncing')

    getInstanceRealtimeMetrics(instance.instanceId)
      .then((res) => {
        setMetrics(res)
        setStreamStatus((prev) => (prev === 'connected' ? 'connected' : 'connecting'))
      })
      .catch(() => {
        setStreamStatus('disconnected')
      })

    const unsubscribe = subscribeInstanceMetricStream(
      instance.instanceId,
      (event) => {
        setStreamStatus('connected')
        setMetrics((prev) =>
          [
            ...prev,
            {
              collectedAt: event.collectedAt,
              cpuUsagePct: event.cpuUsagePct,
              memAvailableBytes: event.memAvailableBytes,
              diskReadBytesPerSec: event.diskReadBytesPerSec,
              diskWriteBytesPerSec: event.diskWriteBytesPerSec,
              diskUtilizationPct: event.diskUtilizationPct,
              netRxBytesPerSec: event.netRxBytesPerSec,
              netTxBytesPerSec: event.netTxBytesPerSec,
              netErrorsPerSec: event.netErrorsPerSec,
            },
          ].slice(-MAX_SERIES_POINTS),
        )
      },
      undefined,
      () => {
        setStreamStatus('disconnected')
      },
    )

    return unsubscribe
  }, [instance.instanceId])

  // 3. 과거 지표 조회 (모드가 history이거나 날짜 변경 시 호출)
  useEffect(() => {
    if (mode === 'history') {
      getInstanceHistoryMetrics(instance.instanceId, historyDate)
        .then(setInstanceHistory)
        .catch(() => setInstanceHistory(null))
    }
  }, [mode, instance.instanceId, historyDate])

  const isRealtime = mode === 'realtime'
  const latest = metrics.length > 0 ? metrics[metrics.length - 1] : null
  const summary = historyData?.summary

  // 차트 타임라인 데이터 구성 (실시간 vs 과거)
  const cpuTimeline = isRealtime
    ? metrics.map((m) => ({ time: shortTime(m.collectedAt), cpuPct: m.cpuUsagePct ?? 0 }))
    : (historyData?.series ?? []).map((s) => ({ time: shortDateHour(s.statTime), cpuPct: s.cpuUsageAvg ?? 0 }))

  const memTimeline = isRealtime
    ? metrics.map((m) => ({
        time: shortTime(m.collectedAt),
        memAvailGb: m.memAvailableBytes !== null ? Number((m.memAvailableBytes / 1024 / 1024 / 1024).toFixed(2)) : 0,
      }))
    : (historyData?.series ?? []).map((s) => ({
        time: shortDateHour(s.statTime),
        memAvailGb: s.memAvailableAvg !== null ? Number((s.memAvailableAvg / 1024 / 1024 / 1024).toFixed(2)) : 0,
      }))

  const diskTimeline = isRealtime
    ? metrics.map((m) => ({
        time: shortTime(m.collectedAt),
        utilPct: m.diskUtilizationPct ?? 0,
      }))
    : (historyData?.series ?? []).map((s) => ({
        time: shortDateHour(s.statTime),
        utilPct: s.diskUsedPctMax ?? 0,
      }))

  const netTimeline = isRealtime
    ? metrics.map((m) => ({
        time: shortTime(m.collectedAt),
        rxMBps: m.netRxBytesPerSec !== null ? Number((m.netRxBytesPerSec / 1024 / 1024).toFixed(2)) : 0,
      }))
    : (historyData?.series ?? []).map((s) => ({
        time: shortDateHour(s.statTime),
        rxMBps: s.rxMbpsAvg !== null ? Number(s.rxMbpsAvg.toFixed(2)) : 0,
      }))

  return (
    <div className="w-full flex-1 flex flex-col min-h-0">
      {/* 헤더: 대상 인스턴스 정보 + 우측 모드 스위처/날짜/상태 */}
      <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 shrink-0">
        <h1 className="flex items-baseline gap-3 text-xl font-bold text-slate-900">
          {instance.name}
          <span className="text-sm font-normal text-slate-400">
            {instance.ip}
          </span>
        </h1>

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

      {/* 메인 2열 레이아웃: 좌측 앱 목록 + 우측 차트 2x2 */}
      <div className="flex-1 flex gap-4 min-h-0">
        {/* 좌측: 실행 중인 앱 목록 (지표 포함) */}
        <div className="w-64 shrink-0 flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <div className="text-xs font-semibold text-slate-700 uppercase tracking-wide">
              실행 중인 앱 <span className="text-slate-400 font-normal">· {apps.length}개</span>
            </div>
            <span className="text-[10px] text-slate-400 font-medium bg-slate-100 px-1.5 py-0.5 rounded">
              최근 5분 집계
            </span>
          </div>
          {apps.length === 0 ? (
            <p className="rounded-lg border border-slate-200 bg-white p-4 text-xs text-slate-400 text-center">
              등록된 앱이 없습니다.
            </p>
          ) : (
            <div className="flex flex-col gap-2.5 overflow-y-auto">
              {apps.map((app) => {
                const summary = appSummaries[app.serverId]
                const httpRps = summary?.http?.rps
                const httpAvgMs = summary?.http?.avgResTimeMs
                const heapUsed = summary?.jvm?.heapUsedMB
                const heapMax = summary?.jvm?.heapUsedMaxMB
                const dbActive = summary?.hikaricp?.active
                const dbMax = summary?.hikaricp?.activeMax
                const dbPending = summary?.hikaricp?.pending ?? 0

                return (
                  <button
                    key={app.serverId}
                    type="button"
                    onClick={() => onSelectApp(app.serverId)}
                    className="rounded-lg border border-slate-200 bg-white p-3 text-left hover:border-brand-300 hover:shadow-sm transition-all flex flex-col gap-2"
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-900 truncate">{app.name}</span>
                      <StatusDot
                        tone={app.status === 'CONNECTED' ? 'normal' : 'idle'}
                        label={app.status === 'CONNECTED' ? '연결됨' : '대기'}
                      />
                    </div>
                    <div className="text-[11px] text-slate-400">포트 {app.port ?? '—'}</div>

                    {/* 핵심 3대 지표 (HTTP / Heap / DB Pool) */}
                    <div className="flex flex-col gap-1 py-2 px-2.5 rounded-md bg-slate-50 border border-slate-100 text-[11px]">
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500 font-medium">HTTP</span>
                        <span className="font-semibold text-slate-800 font-mono">
                          {httpRps !== undefined && httpRps !== null ? `${httpRps.toFixed(1)} req/s` : '—'}
                          {httpAvgMs !== undefined && httpAvgMs !== null ? ` · ${httpAvgMs.toFixed(0)}ms` : ''}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500 font-medium">Heap</span>
                        <span className="font-semibold text-slate-800 font-mono">
                          {heapUsed !== undefined && heapUsed !== null ? `${heapUsed} MB` : '—'}
                          {heapMax ? ` / ${heapMax} MB` : ''}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500 font-medium">DB Pool</span>
                        <span className="font-semibold text-slate-800 font-mono">
                          {dbActive !== undefined && dbActive !== null ? `${dbActive}` : '—'}
                          {dbMax ? ` / ${dbMax}` : ''}
                          {dbPending > 0 && <span className="text-amber-600 font-bold ml-1">(대기 {dbPending})</span>}
                        </span>
                      </div>
                    </div>

                    <div className="text-[11px] font-medium text-brand-600 flex items-center justify-between mt-0.5">
                      <span>상세 모니터링</span>
                      <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                      </svg>
                    </div>
                  </button>
                )
              })}
            </div>
          )}

          <p className="mt-auto text-[10px] leading-relaxed text-slate-400">
            앱 카드를 선택하면 JVM · HTTP · HikariCP · ThreadPool 상세 지표가 열립니다.
          </p>
        </div>

        {/* 우측: 차트 2x2 */}
        <div className="flex-1 grid grid-cols-2 grid-rows-2 gap-4 min-h-0">
          {/* 1. CPU 차트 */}
          <MiniAreaCard
            title={isRealtime ? 'CPU' : `CPU 사용률 (${historyDate})`}
            stats={
              isRealtime
                ? [
                    {
                      label: '사용률',
                      value: latest?.cpuUsagePct !== null && latest ? `${latest.cpuUsagePct?.toFixed(1)}%` : '—',
                      tone: getTone(latest?.cpuUsagePct, DEFAULT_THRESHOLDS.CPU_USAGE.warn, DEFAULT_THRESHOLDS.CPU_USAGE.crit),
                    },
                  ]
                : [
                    {
                      label: '일간 평균',
                      value: summary?.cpu?.cpuUsageAvg !== undefined ? `${summary.cpu.cpuUsageAvg.toFixed(1)}%` : '—',
                      tone: getTone(summary?.cpu?.cpuUsageAvg, DEFAULT_THRESHOLDS.CPU_USAGE.warn, DEFAULT_THRESHOLDS.CPU_USAGE.crit),
                    },
                    {
                      label: '일간 최대',
                      value: summary?.cpu?.cpuUsageMax !== undefined ? `${summary.cpu.cpuUsageMax.toFixed(1)}%` : '—',
                    },
                    {
                      label: 'I/O Wait',
                      value: summary?.cpu?.cpuIowaitAvg !== undefined ? `${summary.cpu.cpuIowaitAvg.toFixed(1)}%` : '—',
                    },
                  ]
            }
            data={cpuTimeline}
            dataKey="cpuPct"
            color="#5b7fa6"
          />

          {/* 2. 가용 메모리 차트 */}
          <MiniAreaCard
            title={isRealtime ? '가용 메모리' : `가용 메모리 (${historyDate})`}
            stats={
              isRealtime
                ? [{ label: '가용', value: latest ? formatBytes(latest.memAvailableBytes) : '—' }]
                : [
                    { label: '가용 평균', value: formatBytes(summary?.memory?.memAvailableAvg) },
                    { label: '가용 최소', value: formatBytes(summary?.memory?.memAvailableMin) },
                    { label: 'Swap 최대', value: formatBytes(summary?.memory?.swapUsedMax) },
                  ]
            }
            data={memTimeline}
            dataKey="memAvailGb"
            color="#4a8c6f"
          />

          {/* 3. 디스크 I/O 차트 */}
          <MiniAreaCard
            title={isRealtime ? '디스크 I/O' : `디스크 현황 (${historyDate})`}
            stats={
              isRealtime
                ? [
                    { label: '읽기', value: latest ? formatBytesPerSec(latest.diskReadBytesPerSec) : '—' },
                    { label: '쓰기', value: latest ? formatBytesPerSec(latest.diskWriteBytesPerSec) : '—' },
                    {
                      label: 'Utilization',
                      value: latest?.diskUtilizationPct !== null && latest ? `${latest.diskUtilizationPct?.toFixed(1)}%` : '—',
                      tone: getTone(latest?.diskUtilizationPct, DEFAULT_THRESHOLDS.DISK_USAGE.warn, DEFAULT_THRESHOLDS.DISK_USAGE.crit),
                    },
                  ]
                : [
                    {
                      label: '최대 점유율',
                      value: summary?.disk?.diskUsedPctMax !== undefined ? `${summary.disk.diskUsedPctMax.toFixed(1)}%` : '—',
                      tone: getTone(summary?.disk?.diskUsedPctMax, DEFAULT_THRESHOLDS.DISK_USAGE.warn, DEFAULT_THRESHOLDS.DISK_USAGE.crit),
                    },
                    {
                      label: 'Read IOPS',
                      value: summary?.disk?.readIopsAvg !== undefined ? `${summary.disk.readIopsAvg.toFixed(0)}` : '—',
                    },
                    {
                      label: 'Write IOPS',
                      value: summary?.disk?.writeIopsAvg !== undefined ? `${summary.disk.writeIopsAvg.toFixed(0)}` : '—',
                    },
                  ]
            }
            data={diskTimeline}
            dataKey="utilPct"
            color="#c8922f"
          />

          {/* 4. 네트워크 차트 */}
          <MiniAreaCard
            title={isRealtime ? '네트워크' : `네트워크 전송량 (${historyDate})`}
            stats={
              isRealtime
                ? [
                    { label: 'RX', value: latest ? formatBytesPerSec(latest.netRxBytesPerSec) : '—' },
                    { label: 'TX', value: latest ? formatBytesPerSec(latest.netTxBytesPerSec) : '—' },
                    {
                      label: '에러율',
                      value: latest?.netErrorsPerSec !== null && latest ? `${latest.netErrorsPerSec?.toFixed(2)}/s` : '—',
                    },
                  ]
                : [
                    {
                      label: 'RX 평균',
                      value: summary?.network?.rxMbpsAvg !== undefined ? `${summary.network.rxMbpsAvg.toFixed(2)} MB/s` : '—',
                    },
                    {
                      label: 'TX 평균',
                      value: summary?.network?.txMbpsAvg !== undefined ? `${summary.network.txMbpsAvg.toFixed(2)} MB/s` : '—',
                    },
                    {
                      label: '에러 합계',
                      value: summary?.network?.errorsSum !== undefined ? `${summary.network.errorsSum}건` : '—',
                    },
                  ]
            }
            data={netTimeline}
            dataKey="rxMBps"
            color="#9b8ac1"
          />
        </div>
      </div>
    </div>
  )
}