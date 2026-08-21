import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import Card from '../components/ui/Card'
import { ApiError } from '../lib/api'
import {
  getInstanceThresholds,
  getInstances,
  getServers,
  updateInstanceThresholds,
  type InstanceListItem,
  type InstanceMetricKey,
  type InstanceThresholdItem,
  type ServerItem,
} from '../lib/instances'
import {
  getServerThresholds,
  updateServerThresholds,
  type ServerMetricKey,
  type ServerThresholdItem,
} from '../lib/servers'

const instanceMetricMeta: Record<InstanceMetricKey, { label: string; unit: string }> = {
  CPU_USAGE: { label: 'CPU 사용률', unit: '%' },
  MEM_USAGE: { label: '메모리 사용률', unit: '%' },
  DISK_USAGE: { label: '디스크 사용률', unit: '%' },
  DISK_LATENCY: { label: '디스크 지연시간', unit: 'ms' },
  NET_ERROR_RATE: { label: '네트워크 오류율', unit: '%' },
}

const serverMetricMeta: Record<ServerMetricKey, { label: string; unit: string }> = {
  JVM_HEAP_USAGE: { label: 'JVM Heap 사용률', unit: '%' },
  JVM_OLD_GEN_USAGE: { label: 'JVM Old Gen 사용률', unit: '%' },
  GC_PAUSE_TIME: { label: 'GC 일시정지시간', unit: '초' },
  HTTP_AVG_LATENCY: { label: 'HTTP 평균 응답지연', unit: 'ms' },
  HTTP_ERROR_RATE: { label: 'HTTP 에러율', unit: '%' },
  HIKARICP_POOL_USAGE: { label: 'DB 커넥션 풀 사용률', unit: '%' },
  THREADPOOL_QUEUE_USAGE: { label: '스레드풀 큐 적체율', unit: '%' },
}

interface EditableRow {
  metricKey: string
  label: string
  unit: string
  warning: string
  critical: string
  isCustomized: boolean
}

export default function ThresholdPage() {
  const { scope } = useParams<{ scope?: string }>()
  const [searchParams] = useSearchParams()
  const [tab, setTab] = useState<'instance' | 'app'>(scope === 'app' ? 'app' : 'instance')

  const instanceIdParam = searchParams.get('instance')
  const serverIdParam = searchParams.get('server')

  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [servers, setServers] = useState<ServerItem[]>([])
  const [rows, setRows] = useState<EditableRow[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    getInstances()
      .then(setInstances)
      .catch((err) => setError(err instanceof ApiError ? err.message : '인스턴스를 불러오지 못했습니다.'))
  }, [])

  const instance = instanceIdParam
    ? instances.find((i) => i.instanceId === Number(instanceIdParam))
    : instances[0]

  useEffect(() => {
    if (!instance) return
    getServers(instance.instanceId)
      .then(setServers)
      .catch(() => setServers([]))
  }, [instance])

  const server = serverIdParam
    ? servers.find((s) => s.serverId === Number(serverIdParam))
    : servers[0]

  useEffect(() => {
    setSaved(false)
    setSaveError(null)

    if (tab === 'instance' && instance) {
      setLoading(true)
      getInstanceThresholds(instance.instanceId)
        .then((res) => setRows(res.map(toInstanceRow)))
        .catch((err) => setError(err instanceof ApiError ? err.message : '임계치를 불러오지 못했습니다.'))
        .finally(() => setLoading(false))
    } else if (tab === 'app' && server) {
      setLoading(true)
      getServerThresholds(server.serverId)
        .then((res) => setRows(res.map(toServerRow)))
        .catch((err) => setError(err instanceof ApiError ? err.message : '임계치를 불러오지 못했습니다.'))
        .finally(() => setLoading(false))
    }
  }, [tab, instance, server])

  function toInstanceRow(t: InstanceThresholdItem): EditableRow {
    const meta = instanceMetricMeta[t.metricKey]
    return {
      metricKey: t.metricKey,
      label: meta.label,
      unit: meta.unit,
      warning: String(t.warningValue),
      critical: String(t.criticalValue),
      isCustomized: t.isCustomized,
    }
  }

  function toServerRow(t: ServerThresholdItem): EditableRow {
    const meta = serverMetricMeta[t.metricKey]
    return {
      metricKey: t.metricKey,
      label: meta.label,
      unit: meta.unit,
      warning: String(t.warningValue),
      critical: String(t.criticalValue),
      isCustomized: t.isCustomized,
    }
  }

  const updateRow = (metricKey: string, field: 'warning' | 'critical', value: string) => {
    setRows((prev) => prev.map((r) => (r.metricKey === metricKey ? { ...r, [field]: value } : r)))
  }

  const invalidRow = rows.find((r) => Number(r.warning) >= Number(r.critical))

  const handleSave = async () => {
    if (invalidRow) return
    setSaving(true)
    setSaveError(null)
    setSaved(false)
    try {
      if (tab === 'instance' && instance) {
        await updateInstanceThresholds(
          instance.instanceId,
          rows.map((r) => ({
            metricKey: r.metricKey as InstanceMetricKey,
            warningValue: Number(r.warning),
            criticalValue: Number(r.critical),
          })),
        )
      } else if (tab === 'app' && server) {
        await updateServerThresholds(
          server.serverId,
          rows.map((r) => ({
            metricKey: r.metricKey as ServerMetricKey,
            warningValue: Number(r.warning),
            criticalValue: Number(r.critical),
          })),
        )
      }
      setSaved(true)
    } catch (err) {
      setSaveError(err instanceof ApiError ? `${err.message} (${err.status})` : '서버에 연결할 수 없습니다.')
    } finally {
      setSaving(false)
    }
  }

  if (error) {
    return (
      <p className="rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">{error}</p>
    )
  }

  if (!instance) {
    return <p className="text-sm text-slate-400">불러오는 중...</p>
  }

  return (
    <div className="max-w-4xl">
      <div className="mb-1 text-sm text-slate-400">
        <Link to="/dashboard" className="hover:underline">
          대시보드
        </Link>{' '}
        › {instance.name} {tab === 'app' && server && `› ${server.name}`} › 임계치
      </div>
      <h1 className="flex items-baseline gap-3 text-2xl font-bold text-slate-900">
        임계치 설정
        <span className="text-sm font-normal text-slate-400">
          {tab === 'instance'
            ? `${instance.name} · 값을 넘기면 목록과 차트에 색으로 표시됩니다`
            : server
              ? `${server.name} · 앱마다 따로 설정합니다`
              : '이 인스턴스에 등록된 앱이 없습니다'}
        </span>
      </h1>

      <div className="mt-5 mb-4 flex gap-6 border-b border-slate-200 text-sm font-medium">
        <button
          type="button"
          onClick={() => setTab('instance')}
          className={`-mb-px border-b-2 pb-3 ${tab === 'instance' ? 'border-slate-800 text-slate-900' : 'border-transparent text-slate-400'}`}
        >
          인스턴스
        </button>
        <button
          type="button"
          onClick={() => setTab('app')}
          className={`-mb-px border-b-2 pb-3 ${tab === 'app' ? 'border-slate-800 text-slate-900' : 'border-transparent text-slate-400'}`}
        >
          앱
        </button>
      </div>

      {loading && <p className="py-6 text-center text-sm text-slate-400">불러오는 중...</p>}

      {!loading && tab === 'app' && !server && (
        <p className="py-6 text-center text-sm text-slate-400">이 인스턴스에 등록된 앱이 없습니다.</p>
      )}

      {!loading && (tab === 'instance' || server) && (
        <>
          <Card className="overflow-hidden">
            <table className="w-full border-collapse text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
                  <th className="px-4 py-3 font-medium">메트릭</th>
                  <th className="px-4 py-3 font-medium">경고</th>
                  <th className="px-4 py-3 font-medium">심각</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => {
                  const isError = Number(row.warning) >= Number(row.critical)
                  return (
                    <tr
                      key={row.metricKey}
                      className={`border-b border-slate-100 ${isError ? 'bg-danger-50/40' : row.isCustomized ? 'bg-brand-50/40' : ''}`}
                    >
                      <td className="px-4 py-3">
                        <div className="font-semibold text-slate-800">{row.label}</div>
                        <div className="text-xs text-slate-400">
                          {row.metricKey} · {row.unit}
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        <input
                          value={row.warning}
                          onChange={(e) => updateRow(row.metricKey, 'warning', e.target.value)}
                          className={`w-24 rounded-md border px-3 py-2 text-sm ${
                            isError ? 'border-danger-500 text-danger-600' : 'border-slate-300'
                          }`}
                        />
                      </td>
                      <td className="px-4 py-3">
                        <input
                          value={row.critical}
                          onChange={(e) => updateRow(row.metricKey, 'critical', e.target.value)}
                          className={`w-24 rounded-md border px-3 py-2 text-sm ${
                            isError ? 'border-danger-500 text-danger-600' : 'border-slate-300'
                          }`}
                        />
                      </td>
                      <td className="px-4 py-3 text-xs text-slate-400">
                        {isError ? (
                          <span className="text-danger-600">오류</span>
                        ) : row.isCustomized ? (
                          <span className="text-brand-600">변경됨</span>
                        ) : (
                          '기본값'
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </Card>

          {invalidRow && (
            <p className="mt-4 rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
              {invalidRow.label} – 심각값은 경고값보다 커야 합니다. (400 THRESHOLD_INVALID_RANGE)
            </p>
          )}
          {saveError && (
            <p className="mt-4 rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
              {saveError}
            </p>
          )}
          {saved && (
            <p className="mt-4 rounded-md border border-brand-500/40 bg-brand-50 px-3 py-2.5 text-sm text-brand-600">
              저장되었습니다.
            </p>
          )}

          <div className="mt-4 flex items-center justify-end">
            <button
              type="button"
              onClick={handleSave}
              disabled={saving || !!invalidRow}
              className="rounded-md bg-brand-500 px-5 py-2 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
            >
              {saving ? '저장 중...' : '저장'}
            </button>
          </div>
        </>
      )}
    </div>
  )
}