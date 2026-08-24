import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useDialog } from '../lib/dialog'
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

const instanceMetricMeta: Record<InstanceMetricKey, { label: string; unit: string; desc: string }> = {
  CPU_USAGE: { label: 'CPU 사용률', unit: '%', desc: '호스트 CPU 코어 전체 사용률' },
  MEM_USAGE: { label: '메모리 사용률', unit: '%', desc: '호스트 물리 메모리 점유율' },
  DISK_USAGE: { label: '디스크 사용률', unit: '%', desc: '루트 파티션 디스크 사용량' },
  DISK_LATENCY: { label: '디스크 지연시간', unit: 'ms', desc: '디스크 I/O 처리 평균 지연' },
  NET_ERROR_RATE: { label: '네트워크 오류율', unit: '%', desc: '네트워크 인터페이스 패킷 에러율' },
}

const instanceDefaults: Record<InstanceMetricKey, { warning: number; critical: number }> = {
  CPU_USAGE: { warning: 80.0, critical: 95.0 },
  MEM_USAGE: { warning: 80.0, critical: 95.0 },
  DISK_USAGE: { warning: 80.0, critical: 95.0 },
  DISK_LATENCY: { warning: 100.0, critical: 500.0 },
  NET_ERROR_RATE: { warning: 1.0, critical: 5.0 },
}

const serverMetricMeta: Record<ServerMetricKey, { label: string; unit: string; desc: string }> = {
  JVM_HEAP_USAGE: { label: 'JVM Heap 사용률', unit: '%', desc: '전체 힙 메모리 중 실사용량 비율' },
  JVM_OLD_GEN_USAGE: { label: 'JVM Old Gen 사용률', unit: '%', desc: 'Old Generation 메모리 적체율' },
  GC_PAUSE_TIME: { label: 'GC 일시정지시간', unit: '초', desc: 'Stop-The-World GC 중단 지속 시간' },
  HTTP_AVG_LATENCY: { label: 'HTTP 평균 응답지연', unit: 'ms', desc: '요청 처리 평균 Latency' },
  HTTP_ERROR_RATE: { label: 'HTTP 에러율', unit: '%', desc: '4xx 및 5xx 응답 상태코드 비율' },
  HIKARICP_POOL_USAGE: { label: 'DB 커넥션 풀 사용률', unit: '%', desc: '활성 DB Connection 점유율' },
  THREADPOOL_QUEUE_USAGE: { label: '스레드풀 큐 적체율', unit: '%', desc: 'Executor 작업 대기 큐 점유율' },
}

const serverDefaults: Record<ServerMetricKey, { warning: number; critical: number }> = {
  JVM_HEAP_USAGE: { warning: 80.0, critical: 90.0 },
  JVM_OLD_GEN_USAGE: { warning: 75.0, critical: 85.0 },
  GC_PAUSE_TIME: { warning: 0.5, critical: 1.0 },
  HTTP_AVG_LATENCY: { warning: 500.0, critical: 1000.0 },
  HTTP_ERROR_RATE: { warning: 1.0, critical: 5.0 },
  HIKARICP_POOL_USAGE: { warning: 80.0, critical: 90.0 },
  THREADPOOL_QUEUE_USAGE: { warning: 50.0, critical: 80.0 },
}

interface EditableRow {
  metricKey: string
  label: string
  unit: string
  desc: string
  warning: string
  critical: string
  isCustomized: boolean
  defaultWarning: number
  defaultCritical: number
}

export default function ThresholdPage() {
  const dialog = useDialog()
  const { scope } = useParams<{ scope?: string }>()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [tab, setTab] = useState<'instance' | 'app'>(scope === 'app' ? 'app' : 'instance')

  useEffect(() => {
    if (scope === 'app') setTab('app')
    else if (scope === 'instance') setTab('instance')
  }, [scope])

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
    const def = instanceDefaults[t.metricKey]
    return {
      metricKey: t.metricKey,
      label: meta.label,
      unit: meta.unit,
      desc: meta.desc,
      warning: String(t.warningValue),
      critical: String(t.criticalValue),
      isCustomized: t.isCustomized,
      defaultWarning: def.warning,
      defaultCritical: def.critical,
    }
  }

  function toServerRow(t: ServerThresholdItem): EditableRow {
    const meta = serverMetricMeta[t.metricKey]
    const def = serverDefaults[t.metricKey]
    return {
      metricKey: t.metricKey,
      label: meta.label,
      unit: meta.unit,
      desc: meta.desc,
      warning: String(t.warningValue),
      critical: String(t.criticalValue),
      isCustomized: t.isCustomized,
      defaultWarning: def.warning,
      defaultCritical: def.critical,
    }
  }

  const updateRow = (metricKey: string, field: 'warning' | 'critical', value: string) => {
    setSaved(false)
    setRows((prev) =>
      prev.map((r) => {
        if (r.metricKey !== metricKey) return r
        const updated = { ...r, [field]: value }
        // 수치가 기본값과 다르면 커스텀으로 자동 판정
        const isCustom =
          Number(updated.warning) !== updated.defaultWarning ||
          Number(updated.critical) !== updated.defaultCritical
        return { ...updated, isCustomized: isCustom }
      }),
    )
  }

  const handleResetToDefaults = async () => {
    const ok = await dialog.confirm(
      '모든 메트릭의 임계치를 기본 권장값으로 초기화하시겠습니까? (저장 버튼을 눌러야 최종 반영됩니다)',
      {
        title: '기본값으로 복원',
        confirmLabel: '초기화',
      },
    )
    if (!ok) return
    setSaved(false)
    setRows((prev) =>
      prev.map((r) => ({
        ...r,
        warning: String(r.defaultWarning),
        critical: String(r.defaultCritical),
        isCustomized: false,
      })),
    )
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
      setTimeout(() => setSaved(false), 3000)
    } catch (err) {
      setSaveError(err instanceof ApiError ? `${err.message} (${err.status})` : '서버에 연결할 수 없습니다.')
    } finally {
      setSaving(false)
    }
  }

  if (error) {
    return (
      <div className="rounded-lg border border-danger-500/40 bg-danger-50 p-4 text-sm text-danger-600">
        {error}
      </div>
    )
  }

  if (!instance) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-sm text-slate-400">인스턴스 정보를 불러오는 중...</p>
      </div>
    )
  }

  const customizedCount = rows.filter((r) => r.isCustomized).length
  const defaultCount = rows.length - customizedCount

  return (
    <div className="w-full max-w-6xl mx-auto space-y-6">
      {/* 1. 상단 글로벌 헤더 */}
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">임계치 설정</h1>
        <p className="mt-1 text-sm text-slate-500">
          호스트 인스턴스 및 Spring Boot 애플리케이션의 경고 및 심각 알림 임계치를 설정합니다.
        </p>
      </div>

      {/* 2. 상단 탭 & 대상 필터 바 (Dashboard와 일관된 디자인) */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 rounded-xl border border-slate-200 bg-white p-3.5 shadow-2xs">
        {/* 좌측: 호스트 / 앱 탭 전환 세그먼트 */}
        <div className="flex rounded-lg border border-slate-200 bg-slate-100 p-1">
          <button
            type="button"
            onClick={() => {
              setTab('instance')
              navigate('/thresholds/instance')
            }}
            className={`flex items-center gap-2 rounded-md px-3.5 py-1.5 text-xs font-bold transition-all ${
              tab === 'instance'
                ? 'bg-slate-800 text-white shadow-2xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <span>Host 인스턴스 (EC2)</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setTab('app')
              navigate('/thresholds/app')
            }}
            className={`flex items-center gap-2 rounded-md px-3.5 py-1.5 text-xs font-bold transition-all ${
              tab === 'app'
                ? 'bg-emerald-700 text-white shadow-2xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <span>Spring Boot 앱</span>
          </button>
        </div>

        {/* 우측: 대상 인스턴스 및 앱 선택 드롭다운 */}
        <div className="flex flex-wrap items-center gap-2.5">
          <div className="flex items-center gap-1.5 text-xs">
            <span className="font-semibold text-slate-500">대상 인스턴스:</span>
            <select
              value={instance.instanceId}
              onChange={(e) => {
                const newParams = new URLSearchParams(searchParams)
                newParams.set('instance', e.target.value)
                newParams.delete('server')
                setSearchParams(newParams)
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-800 shadow-2xs focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            >
              {instances.map((i) => (
                <option key={i.instanceId} value={i.instanceId}>
                  {i.name} ({i.ip})
                </option>
              ))}
            </select>
          </div>

          {tab === 'app' && servers.length > 0 && (
            <div className="flex items-center gap-1.5 text-xs">
              <span className="font-semibold text-slate-500">소속 앱:</span>
              <select
                value={server?.serverId ?? ''}
                onChange={(e) => {
                  const newParams = new URLSearchParams(searchParams)
                  newParams.set('server', e.target.value)
                  setSearchParams(newParams)
                }}
                className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-800 shadow-2xs focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
              >
                {servers.map((s) => (
                  <option key={s.serverId} value={s.serverId}>
                    {s.name} (포트: {s.port ?? '—'})
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      </div>

      {/* 3. 4대 KPI 요약 스트립 (Dashboard 톤앤매너 통일) */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {/* 카드 1: 설정 대상 */}
        <Card className="p-4">
          <div className="text-xs font-semibold text-slate-500">설정 대상</div>
          <div className="mt-1 text-base font-bold text-slate-900 truncate">
            {tab === 'instance' ? instance.name : server?.name ?? '선택 없음'}
          </div>
          <div className="mt-0.5 text-xs font-mono text-slate-500 truncate">
            {tab === 'instance' ? instance.ip : `포트: ${server?.port ?? '—'}`}
          </div>
        </Card>

        {/* 카드 2: 관리 메트릭 수 */}
        <Card className="p-4">
          <div className="text-xs font-semibold text-slate-500">관리 메트릭</div>
          <div className="mt-1 text-2xl font-bold text-slate-900">{rows.length}개</div>
          <div className="mt-0.5 text-xs text-slate-500">실시간 임계치 감시 항목</div>
        </Card>

        {/* 카드 3: 커스텀 설정 항목 */}
        <Card className="p-4">
          <div className="text-xs font-semibold text-slate-500">커스텀 설정</div>
          <div className="mt-1 text-2xl font-bold text-brand-700">{customizedCount}개</div>
          <div className="mt-0.5 text-xs text-slate-500">사용자 지정 기준값</div>
        </Card>

        {/* 카드 4: 기본값 유지 항목 */}
        <Card className="p-4">
          <div className="text-xs font-semibold text-slate-500">기본값 유지</div>
          <div className="mt-1 text-2xl font-bold text-emerald-600">{defaultCount}개</div>
          <div className="mt-0.5 text-xs text-slate-500">권장 표준 기준값</div>
        </Card>
      </div>

      {/* 4. 메인 임계치 관리 테이블 카드 (Full-Width) */}
      <Card className="p-6">
        {/* 테이블 카드 상단 헤더 */}
        <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-slate-100 pb-3.5">
          <div>
            <h2 className="text-base font-bold text-slate-900">
              {tab === 'instance' ? `${instance.name} 호스트 임계치` : `${server?.name ?? ''} 앱 임계치`}
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              각 지표별 경고 및 심각 수치를 입력한 후 우측 하단 저장 버튼을 클릭하세요.
            </p>
          </div>

          <button
            type="button"
            onClick={handleResetToDefaults}
            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors shadow-2xs self-start sm:self-auto"
          >
            <svg className="h-3.5 w-3.5 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>전체 기본값으로 복원</span>
          </button>
        </div>

        {loading ? (
          <div className="py-16 text-center text-sm text-slate-400">
            임계치 설정을 불러오는 중...
          </div>
        ) : tab === 'app' && !server ? (
          <div className="py-16 text-center text-sm text-slate-400">
            이 인스턴스에 등록된 애플리케이션이 없습니다.
          </div>
        ) : (
          <div className="space-y-4">
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    <th className="py-3 px-3">메트릭 항목</th>
                    <th className="py-3 px-3">기본 권장 기준</th>
                    <th className="py-3 px-3 w-40">
                      <span className="inline-flex items-center gap-1.5 text-amber-700">
                        <span className="h-2 w-2 rounded-full bg-amber-500" />
                        경고 (Warning)
                      </span>
                    </th>
                    <th className="py-3 px-3 w-40">
                      <span className="inline-flex items-center gap-1.5 text-rose-700">
                        <span className="h-2 w-2 rounded-full bg-rose-500" />
                        심각 (Critical)
                      </span>
                    </th>
                    <th className="py-3 px-3 text-right">설정 상태</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {rows.map((row) => {
                    const isError = Number(row.warning) >= Number(row.critical)
                    return (
                      <tr
                        key={row.metricKey}
                        className={`hover:bg-slate-50/70 transition-colors ${
                          isError ? 'bg-rose-50/40' : row.isCustomized ? 'bg-brand-50/20' : ''
                        }`}
                      >
                        {/* 메트릭 이름 & 설명 */}
                        <td className="py-3.5 px-3">
                          <div className="font-bold text-slate-900">{row.label}</div>
                          <div className="text-xs text-slate-400 mt-0.5">{row.desc}</div>
                        </td>

                        {/* 기본값 가이드 */}
                        <td className="py-3.5 px-3">
                          <div className="flex flex-col gap-0.5 text-xs font-mono">
                            <span className="text-amber-600 font-semibold">경고: {row.defaultWarning}{row.unit}</span>
                            <span className="text-rose-600 font-semibold">심각: {row.defaultCritical}{row.unit}</span>
                          </div>
                        </td>

                        {/* 경고 입력 */}
                        <td className="py-3.5 px-3">
                          <div className="relative flex items-center">
                            <input
                              type="number"
                              step="any"
                              value={row.warning}
                              onChange={(e) => updateRow(row.metricKey, 'warning', e.target.value)}
                              className={`w-full rounded-lg border bg-white py-1.5 pl-3 pr-8 text-sm font-mono transition-colors focus:outline-none focus:ring-1 ${
                                isError
                                  ? 'border-rose-400 text-rose-600 focus:border-rose-500 focus:ring-rose-500'
                                  : 'border-slate-300 focus:border-amber-500 focus:ring-amber-500'
                              }`}
                            />
                            <span className="absolute right-2.5 text-xs font-semibold text-slate-400 pointer-events-none">
                              {row.unit}
                            </span>
                          </div>
                        </td>

                        {/* 심각 입력 */}
                        <td className="py-3.5 px-3">
                          <div className="relative flex items-center">
                            <input
                              type="number"
                              step="any"
                              value={row.critical}
                              onChange={(e) => updateRow(row.metricKey, 'critical', e.target.value)}
                              className={`w-full rounded-lg border bg-white py-1.5 pl-3 pr-8 text-sm font-mono transition-colors focus:outline-none focus:ring-1 ${
                                isError
                                  ? 'border-rose-400 text-rose-600 focus:border-rose-500 focus:ring-rose-500'
                                  : 'border-slate-300 focus:border-rose-500 focus:ring-rose-500'
                              }`}
                            />
                            <span className="absolute right-2.5 text-xs font-semibold text-slate-400 pointer-events-none">
                              {row.unit}
                            </span>
                          </div>
                        </td>

                        {/* 설정 상태 뱃지 */}
                        <td className="py-3.5 px-3 text-right">
                          {isError ? (
                            <span className="inline-block rounded-md border border-rose-300 bg-rose-100 px-2.5 py-0.5 text-xs font-bold text-rose-700">
                              범위 오류
                            </span>
                          ) : row.isCustomized ? (
                            <span className="inline-block rounded-md bg-brand-600 px-2.5 py-0.5 text-xs font-bold text-white shadow-2xs">
                              커스텀
                            </span>
                          ) : (
                            <span className="inline-block rounded-md border border-slate-300 bg-slate-200/90 px-2.5 py-0.5 text-xs font-semibold text-slate-700 shadow-2xs">
                              기본값
                            </span>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>

            {/* 에러 피드백 메시지 */}
            {invalidRow && (
              <div className="rounded-lg border border-rose-300 bg-rose-50 p-3 text-xs font-medium text-rose-700 flex items-center gap-2">
                <svg className="h-4 w-4 shrink-0 text-rose-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <span>[{invalidRow.label}] 심각값은 경고값보다 커야 합니다. (경고 &lt; 심각)</span>
              </div>
            )}

            {saveError && (
              <div className="rounded-lg border border-rose-300 bg-rose-50 p-3 text-xs font-medium text-rose-700">
                {saveError}
              </div>
            )}

            {saved && (
              <div className="rounded-lg border border-emerald-300 bg-emerald-50 p-3 text-xs font-bold text-emerald-800 flex items-center gap-2">
                <svg className="h-4 w-4 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                </svg>
                <span>임계치 설정이 성공적으로 저장되었습니다. 실시간 감시에 즉시 반영됩니다.</span>
              </div>
            )}

            {/* 하단 액션 바 */}
            <div className="mt-4 flex flex-col sm:flex-row items-center justify-between gap-3 pt-4 border-t border-slate-100">
              <p className="text-xs text-slate-400">
                * 지표가 경고값을 초과하면 주황색(Warning), 심각값을 초과하면 붉은색(Critical) 알림이 발생합니다.
              </p>

              <div className="flex items-center gap-2.5 w-full sm:w-auto justify-end">


                <button
                  type="button"
                  onClick={handleSave}
                  disabled={saving || Boolean(invalidRow)}
                  className="inline-flex items-center justify-center gap-2 rounded-lg bg-brand-500 px-6 py-2.5 text-xs font-bold text-white hover:bg-brand-600 shadow-sm transition-colors disabled:opacity-60"
                >
                  {saving ? (
                    <span>저장 중...</span>
                  ) : (
                    <span>저장</span>
                  )}
                </button>
              </div>
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}