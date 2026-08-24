import { useEffect, useState } from 'react'
import MiniAreaCard from './MiniAreaCard'
import StatusDot, { STREAM_STATUS_CONFIG, type StreamStatus } from '../ui/StatusDot'
import { getTone, DEFAULT_THRESHOLDS } from '../../lib/thresholdUtils'
import {
  getInstanceRealtimeMetrics,
  subscribeInstanceMetricStream,
  type InstanceListItem,
  type InstanceRealtimeMetricPoint,
  type ServerItem,
} from '../../lib/instances'

const MAX_SERIES_POINTS = 90 // 최근 15분치 (최대 90개) 유지

function formatBytes(bytes: number | null) {
  if (bytes === null) return '—'
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)}GB`
}

function formatBytesPerSec(bytesPerSec: number | null) {
  if (bytesPerSec === null) return '—'
  return `${(bytesPerSec / 1024 / 1024).toFixed(2)} MB/s`
}

function shortTime(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
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
  const [metrics, setMetrics] = useState<InstanceRealtimeMetricPoint[]>([])
  const [streamStatus, setStreamStatus] = useState<StreamStatus>('syncing')

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

  const latest = metrics.length > 0 ? metrics[metrics.length - 1] : null

  const cpuTimeline = metrics.map((m) => ({ time: shortTime(m.collectedAt), cpuPct: m.cpuUsagePct ?? 0 }))
  const memTimeline = metrics.map((m) => ({
    time: shortTime(m.collectedAt),
    memAvailGb: m.memAvailableBytes !== null ? Number((m.memAvailableBytes / 1024 / 1024 / 1024).toFixed(2)) : 0,
  }))
  const diskTimeline = metrics.map((m) => ({
    time: shortTime(m.collectedAt),
    utilPct: m.diskUtilizationPct ?? 0,
  }))
  const netTimeline = metrics.map((m) => ({
    time: shortTime(m.collectedAt),
    rxMBps: m.netRxBytesPerSec !== null ? Number((m.netRxBytesPerSec / 1024 / 1024).toFixed(2)) : 0,
  }))

  return (
    <div className="w-full flex-1 flex flex-col min-h-0">
      {/* 헤더 */}
      <div className="mb-4 flex items-start justify-between shrink-0">
        <h1 className="flex items-baseline gap-3 text-xl font-bold text-slate-900">
          {instance.name}
          <span className="text-sm font-normal text-slate-400">
            {instance.ip}
          </span>
        </h1>
        <div className="flex items-center gap-2">
          <span className="inline-flex items-center rounded-md bg-slate-100 px-2 py-0.5 text-xs font-medium text-slate-600 border border-slate-200/80">
            최근 15분
          </span>
          <StatusDot
            tone={STREAM_STATUS_CONFIG[streamStatus].tone}
            label={STREAM_STATUS_CONFIG[streamStatus].label}
          />
        </div>
      </div>

      {/* 메인 2열 레이아웃: 좌측 앱 목록 + 우측 차트 2x2 */}
      <div className="flex-1 flex gap-4 min-h-0">
        {/* 좌측: 실행 중인 앱 목록 */}
        <div className="w-52 shrink-0 flex flex-col gap-3">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wide">
            실행 중인 앱 <span className="text-slate-400 font-normal">· {apps.length}개</span>
          </div>
          {apps.length === 0 ? (
            <p className="rounded-lg border border-slate-200 bg-white p-4 text-xs text-slate-400 text-center">
              등록된 앱이 없습니다.
            </p>
          ) : (
            <div className="flex flex-col gap-2 overflow-y-auto">
              {apps.map((app) => (
                <button
                  key={app.serverId}
                  type="button"
                  onClick={() => onSelectApp(app.serverId)}
                  className="rounded-lg border border-slate-200 bg-white p-3 text-left hover:border-brand-300 hover:shadow-sm transition-all"
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-xs font-bold text-slate-900 truncate">{app.name}</span>
                    <StatusDot
                      tone={app.status === 'CONNECTED' ? 'normal' : 'idle'}
                      label={app.status === 'CONNECTED' ? '연결됨' : '대기'}
                    />
                  </div>
                  <div className="text-[11px] text-slate-400">포트 {app.port ?? '—'}</div>
                  <div className="mt-2 text-[11px] font-medium text-brand-600 flex items-center gap-1">
                    <span>상세 모니터링</span>
                    <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                </button>
              ))}
            </div>
          )}

          <p className="mt-auto text-[10px] leading-relaxed text-slate-400">
            앱 카드를 선택하면 JVM · HTTP · HikariCP · ThreadPool 상세 지표가 열립니다.
          </p>
        </div>

        {/* 우측: 차트 2x2 (서버 모니터링과 동일하게 세로 꽉차게) */}
        <div className="flex-1 grid grid-cols-2 grid-rows-2 gap-4 min-h-0">
          <MiniAreaCard
            title="CPU"
            stats={[
              {
                label: '사용률',
                value: latest?.cpuUsagePct !== null && latest ? `${latest.cpuUsagePct?.toFixed(1)}%` : '—',
                tone: getTone(latest?.cpuUsagePct, DEFAULT_THRESHOLDS.CPU_USAGE.warn, DEFAULT_THRESHOLDS.CPU_USAGE.crit),
              },
            ]}
            data={cpuTimeline}
            dataKey="cpuPct"
            color="#5b7fa6"
          />
          <MiniAreaCard
            title="가용 메모리"
            stats={[{ label: '가용', value: latest ? formatBytes(latest.memAvailableBytes) : '—' }]}
            data={memTimeline}
            dataKey="memAvailGb"
            color="#4a8c6f"
          />
          <MiniAreaCard
            title="디스크 I/O"
            stats={[
              { label: '읽기', value: latest ? formatBytesPerSec(latest.diskReadBytesPerSec) : '—' },
              { label: '쓰기', value: latest ? formatBytesPerSec(latest.diskWriteBytesPerSec) : '—' },
              {
                label: 'Utilization',
                value: latest?.diskUtilizationPct !== null && latest ? `${latest.diskUtilizationPct?.toFixed(1)}%` : '—',
                tone: getTone(latest?.diskUtilizationPct, DEFAULT_THRESHOLDS.DISK_USAGE.warn, DEFAULT_THRESHOLDS.DISK_USAGE.crit),
              },
            ]}
            data={diskTimeline}
            dataKey="utilPct"
            color="#c8922f"
          />
          <MiniAreaCard
            title="네트워크"
            stats={[
              { label: 'RX', value: latest ? formatBytesPerSec(latest.netRxBytesPerSec) : '—' },
              { label: 'TX', value: latest ? formatBytesPerSec(latest.netTxBytesPerSec) : '—' },
              {
                label: '에러율',
                value: latest?.netErrorsPerSec !== null && latest ? `${latest.netErrorsPerSec?.toFixed(2)}/s` : '—',
              },
            ]}
            data={netTimeline}
            dataKey="rxMBps"
            color="#9b8ac1"
          />
        </div>
      </div>
    </div>
  )
}