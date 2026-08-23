import { useEffect, useMemo, useState } from 'react'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import Card from '../components/ui/Card'
import { ApiError } from '../lib/api'
import {
  getInstanceHistoryMetrics,
  getInstances,
  type InstanceHistoryMetrics,
  type InstanceListItem,
} from '../lib/instances'

type MetricOption = {
  key: 'cpu' | 'memory' | 'disk' | 'network'
  label: string
  unit: string
  seriesKey: keyof NonNullable<InstanceHistoryMetrics['series'][number]>
}

const METRIC_OPTIONS: MetricOption[] = [
  { key: 'cpu', label: 'CPU 사용률', unit: '%', seriesKey: 'cpuUsageAvg' },
  { key: 'memory', label: '가용 메모리', unit: 'GB', seriesKey: 'memAvailableAvg' },
  { key: 'disk', label: '디스크 사용률', unit: '%', seriesKey: 'diskUsedPctMax' },
  { key: 'network', label: '네트워크 수신', unit: 'Mbps', seriesKey: 'rxMbpsAvg' },
]

function formatDate(d: Date) {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function yesterday() {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  return formatDate(d)
}

function shortTime(iso: string) {
  return new Date(iso).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })
}

export default function HistoryPage() {
  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [instanceId, setInstanceId] = useState<number | null>(null)
  const [date, setDate] = useState(yesterday())
  const [metric, setMetric] = useState<MetricOption>(METRIC_OPTIONS[0])
  const [data, setData] = useState<InstanceHistoryMetrics | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    getInstances()
      .then((res) => {
        setInstances(res)
        if (res.length > 0) setInstanceId(res[0].instanceId)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : '인스턴스 목록을 불러오지 못했습니다.'))
  }, [])

  const fetchHistory = () => {
    if (instanceId === null) return
    setError(null)
    setLoading(true)
    getInstanceHistoryMetrics(instanceId, date)
      .then(setData)
      .catch((err) => {
        setData(null)
        setError(err instanceof ApiError ? err.message : '과거 데이터를 불러오지 못했습니다.')
      })
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (instanceId !== null) fetchHistory()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instanceId])

  const selectedInstance = instances.find((i) => i.instanceId === instanceId)

  const chartData = useMemo(() => {
    if (!data) return []
    return data.series.map((p) => {
      const raw = p[metric.seriesKey]
      let value = raw === null || raw === undefined ? null : Number(raw)
      if (value !== null && metric.key === 'memory') {
        value = Number((value / 1024 / 1024 / 1024).toFixed(2))
      }
      return { time: shortTime(p.statTime), value }
    })
  }, [data, metric])

  const stats = useMemo(() => {
    if (!data) return null
    const { cpu, memory, disk, network } = data.summary
    switch (metric.key) {
      case 'cpu':
        return cpu ? { avg: cpu.cpuUsageAvg, max: cpu.cpuUsageMax, min: null } : null
      case 'memory':
        return memory
          ? {
              avg: Number((memory.memAvailableAvg / 1024 / 1024 / 1024).toFixed(2)),
              max: null,
              min: Number((memory.memAvailableMin / 1024 / 1024 / 1024).toFixed(2)),
            }
          : null
      case 'disk':
        return disk ? { avg: disk.readIopsAvg, max: disk.diskUsedPctMax, min: null } : null
      case 'network':
        return network ? { avg: network.rxMbpsAvg, max: network.txMbpsAvg, min: null } : null
      default:
        return null
    }
  }, [data, metric])

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900">과거 데이터 조회</h1>

      <Card className="mt-5 mb-2 flex flex-wrap items-center gap-3 p-4">
        <select
          value={instanceId ?? ''}
          onChange={(e) => setInstanceId(Number(e.target.value))}
          className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800"
        >
          {instances.map((inst) => (
            <option key={inst.instanceId} value={inst.instanceId}>
              {inst.name}
            </option>
          ))}
        </select>
        <select
          disabled
          className="rounded-md border border-slate-300 bg-slate-50 px-3 py-2 text-sm text-slate-400"
        >
          <option>앱: 준비 중</option>
        </select>
        <div className="flex items-center gap-2 text-sm text-slate-600">
          <input
            type="date"
            value={date}
            max={yesterday()}
            onChange={(e) => setDate(e.target.value)}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
        <select
          value={metric.key}
          onChange={(e) => setMetric(METRIC_OPTIONS.find((m) => m.key === e.target.value) ?? METRIC_OPTIONS[0])}
          className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600"
        >
          {METRIC_OPTIONS.map((m) => (
            <option key={m.key} value={m.key}>
              {m.label}
            </option>
          ))}
        </select>
        <button
          type="button"
          onClick={fetchHistory}
          disabled={instanceId === null || loading}
          className="ml-auto rounded-md bg-brand-500 px-5 py-2 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
        >
          {loading ? '조회 중...' : '조회'}
        </button>
      </Card>
      <p className="mb-6 text-xs leading-relaxed text-slate-400">
        인스턴스의 호스트 지표(CPU · 메모리 · 디스크 · 네트워크)를 1시간 단위로 집계된 값으로 조회합니다. 앱(서버) 단위
        과거 데이터 조회는 아직 제공되지 않습니다.
      </p>

      {error && (
        <p className="mb-4 rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
          {error}
        </p>
      )}

      <div className="flex gap-6">
        <Card className="flex-1 p-6">
          <h2 className="mb-6 font-semibold text-slate-900">
            {selectedInstance?.name ?? '—'} · {metric.label} · {data?.date ?? date}
          </h2>
          {data && chartData.every((p) => p.value === null) ? (
            <p className="py-16 text-center text-sm text-slate-400">해당 날짜에 집계된 데이터가 없습니다.</p>
          ) : (
            <div className="h-72">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                  <defs>
                    <linearGradient id="grad-history" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="#5b7fa6" stopOpacity={0.25} />
                      <stop offset="100%" stopColor="#5b7fa6" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                  <XAxis dataKey="time" tick={{ fontSize: 11, fill: '#94a3b8' }} tickLine={false} />
                  <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={34} />
                  <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#e2e8f0' }} />
                  <Area
                    type="monotone"
                    dataKey="value"
                    stroke="#5b7fa6"
                    strokeWidth={2}
                    fill="url(#grad-history)"
                    dot={{ r: 3, fill: '#5b7fa6' }}
                    connectNulls
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <div className="flex w-64 shrink-0 flex-col gap-4">
          <Card className="p-4">
            <div className="text-xs text-slate-500">기간 평균</div>
            <div className="mt-1 text-2xl font-bold text-slate-900">
              {stats?.avg ?? '—'} {stats?.avg !== undefined && stats?.avg !== null ? metric.unit : ''}
            </div>
          </Card>
          <Card className="p-4">
            <div className="text-xs text-slate-500">{metric.key === 'network' ? '송신 평균' : '최댓값'}</div>
            <div className="mt-1 text-2xl font-bold text-warn-600">
              {stats?.max ?? '—'} {stats?.max !== undefined && stats?.max !== null ? metric.unit : ''}
            </div>
          </Card>
          {stats?.min !== null && stats?.min !== undefined && (
            <Card className="p-4">
              <div className="text-xs text-slate-500">최솟값</div>
              <div className="mt-1 text-2xl font-bold text-slate-900">
                {stats.min} {metric.unit}
              </div>
            </Card>
          )}
          <p className="text-xs leading-relaxed text-slate-400">
            1시간 단위로 롤업 집계된 통계이며, 5분 원시 집계를 기반으로 산출됩니다.
          </p>
        </div>
      </div>
    </div>
  )
}
