import { useEffect, useState } from 'react'
import MiniAreaCard from './MiniAreaCard'
import StatusDot from '../ui/StatusDot'
import {
  getInstanceRealtimeMetrics,
  subscribeInstanceMetricStream,
  type InstanceListItem,
  type InstanceRealtimeMetricPoint,
  type ServerItem,
} from '../../lib/instances'

const MAX_SERIES_POINTS = 30

function formatBytes(bytes: number | null) {
  if (bytes === null) return '—'
  return `${(bytes / 1024 / 1024 / 1024).toFixed(1)}GB`
}

function formatBytesPerSec(bytesPerSec: number | null) {
  if (bytesPerSec === null) return '—'
  return `${(bytesPerSec / 1024 / 1024).toFixed(2)} MB/s`
}

function shortTime(iso: string) {
  return new Date(iso).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
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
  const [connected, setConnected] = useState(false)

  useEffect(() => {
    setMetrics([])
    setConnected(false)

    getInstanceRealtimeMetrics(instance.instanceId).then(setMetrics)

    const unsubscribe = subscribeInstanceMetricStream(
      instance.instanceId,
      (event) => {
        setConnected(true)
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
      () => setConnected(false),
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
    <div>
      <div className="mb-3 flex items-start justify-between">
        <h1 className="flex items-baseline gap-3 text-xl font-bold text-slate-900">
          {instance.name}
          <span className="text-sm font-normal text-slate-400">
            {instance.ip} · 앱 {instance.serverCount}개
          </span>
        </h1>
        <StatusDot tone={connected ? 'normal' : 'idle'} label={connected ? 'SSE 연결됨' : 'SSE 연결 대기'} />
      </div>

      <div className="mb-6 grid grid-cols-2 gap-4">
        <MiniAreaCard
          title="CPU"
          stats={[{ label: '사용률', value: latest?.cpuUsagePct !== null && latest ? `${latest.cpuUsagePct?.toFixed(1)}%` : '—' }]}
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
              tone: 'warn',
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
              tone: 'danger',
            },
          ]}
          data={netTimeline}
          dataKey="rxMBps"
          color="#9b8ac1"
        />
      </div>

      <div className="mb-3 text-sm font-semibold text-slate-800">
        이 인스턴스에서 실행 중인 앱
      </div>
      {apps.length === 0 ? (
        <p className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-400">등록된 앱이 없습니다.</p>
      ) : (
        <div className="grid grid-cols-3 gap-4">
          {apps.map((app) => (
            <button
              key={app.serverId}
              type="button"
              onClick={() => onSelectApp(app.serverId)}
              className="rounded-lg border border-slate-200 bg-white p-4 text-left hover:border-brand-300 hover:shadow-sm"
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="font-semibold text-slate-900">{app.name}</span>
                <StatusDot tone={app.status === 'CONNECTED' ? 'normal' : 'idle'} label={app.status === 'CONNECTED' ? '연결됨' : '대기'} />
              </div>
              <div className="text-xs text-slate-500">포트 {app.port ?? '—'}</div>
            </button>
          ))}
        </div>
      )}

      <p className="mt-6 text-xs leading-relaxed text-slate-400">
        인스턴스는 호스트 지표(CPU · 메모리 · 디스크 · 네트워크)만, 앱은 애플리케이션 지표(JVM · HikariCP · HTTP · API)만
        가집니다. 앱을 선택하면 JVM · HikariCP · HTTP · API 지표가 열립니다.
      </p>
    </div>
  )
}