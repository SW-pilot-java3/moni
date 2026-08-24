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

const instanceMetricMeta: Record<InstanceMetricKey, { label: string; unit: string; desc: string }> = {
  CPU_USAGE: { label: 'CPU 사용률', unit: '%', desc: '호스트 CPU 코어 전체 사용률' },
  MEM_USAGE: { label: '메모리 사용률', unit: '%', desc: '호스트 물리 메모리 점유율' },
  DISK_USAGE: { label: '디스크 사용률', unit: '%', desc: '루트 파티션 디스크 사용량' },
  DISK_LATENCY: { label: '디스크 지연시간', unit: 'ms', desc: '디스크 I/O 처리 평균 지연' },
  NET_ERROR_RATE: { label: '네트워크 오류율', unit: '%', desc: '네트워크 인터페이스 패킷 에러율' },
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

interface EditableRow {
  metricKey: string
  label: string
  unit: string
  desc: string
  warning: string
  critical: string
  isCustomized: boolean
}

export default function ThresholdPage() {
  const { scope } = useParams<{ scope?: string }>()
  const [searchParams, setSearchParams] = useSearchParams()
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
      desc: meta.desc,
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
      desc: meta.desc,
      warning: String(t.warningValue),
      critical: String(t.criticalValue),
      isCustomized: t.isCustomized,
    }
  }

  const updateRow = (metricKey: string, field: 'warning' | 'critical', value: string) => {
    setSaved(false)
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

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6">
      {/* 상단 헤더 & 셀렉터/탭 바 */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">임계치 설정</h1>
          <p className="mt-1 text-sm text-slate-500">
            호스트 및 애플리케이션 메트릭의 경고(Warning) · 심각(Critical) 기준값을 정의합니다.
          </p>
        </div>

        {/* 인스턴스/앱 전환 셀렉터 */}
        <div className="flex flex-wrap items-center gap-2">
          {/* 인스턴스 선택 */}
          <select
            value={instance.instanceId}
            onChange={(e) => {
              const newParams = new URLSearchParams(searchParams)
              newParams.set('instance', e.target.value)
              newParams.delete('server')
              setSearchParams(newParams)
            }}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            {instances.map((i) => (
              <option key={i.instanceId} value={i.instanceId}>
                {i.name} ({i.ip})
              </option>
            ))}
          </select>

          {/* 앱 선택 (앱 탭일 때만 활성화) */}
          {tab === 'app' && servers.length > 0 && (
            <select
              value={server?.serverId ?? ''}
              onChange={(e) => {
                const newParams = new URLSearchParams(searchParams)
                newParams.set('server', e.target.value)
                setSearchParams(newParams)
              }}
              className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            >
              {servers.map((s) => (
                <option key={s.serverId} value={s.serverId}>
                  {s.name} (포트: {s.port ?? '—'})
                </option>
              ))}
            </select>
          )}

          {/* 알약형 탭 버튼 */}
          <div className="flex rounded-lg border border-slate-200 bg-slate-100 p-1">
            <button
              type="button"
              onClick={() => setTab('instance')}
              className={`rounded-md px-3 py-1.5 text-xs font-semibold transition-colors ${
                tab === 'instance'
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              인스턴스 (호스트)
            </button>
            <button
              type="button"
              onClick={() => setTab('app')}
              className={`rounded-md px-3 py-1.5 text-xs font-semibold transition-colors ${
                tab === 'app'
                  ? 'bg-white text-slate-900 shadow-2xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              앱 (Spring Boot)
            </button>
          </div>
        </div>
      </div>

      {/* 2열 반응형 그리드 레이아웃 */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* 좌측 컬럼: 대상 정보 요약 + 임계치 기준 안내 (4 cols) */}
        <div className="lg:col-span-4 space-y-6">
          {/* 대상 정보 카드 */}
          <Card className="p-5">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
              <h2 className="font-bold text-slate-900">설정 대상</h2>
              <span className="rounded bg-brand-50 px-2 py-0.5 text-xs font-semibold text-brand-700">
                {tab === 'instance' ? '호스트 인스턴스' : 'Spring Boot 앱'}
              </span>
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">대상 이름</span>
                <span className="font-semibold text-slate-800">
                  {tab === 'instance' ? instance.name : server?.name ?? '선택 없음'}
                </span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">{tab === 'instance' ? '호스트 IP' : '서버 포트'}</span>
                <span className="font-mono text-slate-800">
                  {tab === 'instance' ? instance.ip : server?.port ?? '—'}
                </span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">임계치 관리 메트릭</span>
                <span className="font-medium text-slate-800">{rows.length}개 항목</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">사용자 정의 상태</span>
                <span className="text-xs font-semibold text-slate-700">
                  {customizedCount > 0 ? `${customizedCount}개 커스텀 설정` : '모두 기본값 적용'}
                </span>
              </div>
            </div>

            <div className="mt-5 pt-4 border-t border-slate-100">
              <Link
                to={
                  tab === 'instance'
                    ? `/monitoring?instance=${instance.instanceId}`
                    : `/monitoring?instance=${instance.instanceId}&app=${server?.serverId ?? ''}`
                }
                className="block w-full text-center rounded-md bg-brand-50 px-3 py-2 text-xs font-semibold text-brand-700 hover:bg-brand-100 transition-colors"
              >
                해당 대상 실시간 모니터링 바로가기 ›
              </Link>
            </div>
          </Card>

          {/* 임계치 판정 가이드 카드 */}
          <Card className="p-5 bg-slate-50/70 border-slate-200 space-y-3">
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wide">
              임계치 상태 판정 가이드
            </h3>
            
            <div className="rounded-lg border border-amber-200 bg-amber-50/60 p-3 text-xs leading-relaxed text-amber-900">
              <div className="font-bold flex items-center gap-1.5 text-amber-800 mb-1">
                <span className="h-2 w-2 rounded-full bg-amber-500" />
                경고 (Warning)
              </div>
              지표가 경고 기준값을 초과할 때 모니터링 차트와 대시보드 상태 점이 <strong>주황색 주의</strong> 상태로 전환됩니다.
            </div>

            <div className="rounded-lg border border-rose-200 bg-rose-50/60 p-3 text-xs leading-relaxed text-rose-900">
              <div className="font-bold flex items-center gap-1.5 text-rose-800 mb-1">
                <span className="h-2 w-2 rounded-full bg-rose-500" />
                심각 (Critical)
              </div>
              지표가 심각 기준값을 초과할 때 즉시 <strong>붉은색 장애/위험</strong> 상태로 강조 표시됩니다.
            </div>

            <p className="text-[11px] text-slate-500 pt-1">
              * 규칙: <code className="font-semibold text-slate-700">경고값 &lt; 심각값</code> 관계를 만족해야 저장이 가능합니다.
            </p>
          </Card>
        </div>

        {/* 우측 컬럼: 임계치 설정 테이블 및 저장 버튼 (8 cols) */}
        <div className="lg:col-span-8 space-y-6">
          <Card className="p-6">
            <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-slate-100 pb-3">
              <div>
                <h2 className="text-base font-bold text-slate-900">
                  {tab === 'instance' ? `${instance.name} 호스트 임계치` : `${server?.name ?? ''} 앱 임계치`}
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  각 지표별 경고 및 심각 수치를 입력한 후 우측 하단 저장을 클릭하세요.
                </p>
              </div>
              {customizedCount > 0 && (
                <span className="inline-flex items-center rounded-full bg-brand-100 px-2.5 py-0.5 text-xs font-semibold text-brand-800">
                  {customizedCount}개 항목 커스텀
                </span>
              )}
            </div>

            {loading ? (
              <div className="py-12 text-center text-sm text-slate-400">
                임계치 설정을 불러오는 중...
              </div>
            ) : tab === 'app' && !server ? (
              <div className="py-12 text-center text-sm text-slate-400">
                이 인스턴스에 등록된 애플리케이션이 없습니다.
              </div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse text-sm">
                    <thead>
                      <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                        <th className="py-2.5 px-3">메트릭 항목</th>
                        <th className="py-2.5 px-3 w-36">
                          <span className="inline-flex items-center gap-1 text-amber-700">
                            <span className="h-2 w-2 rounded-full bg-amber-500" />
                            경고 (Warning)
                          </span>
                        </th>
                        <th className="py-2.5 px-3 w-36">
                          <span className="inline-flex items-center gap-1 text-rose-700">
                            <span className="h-2 w-2 rounded-full bg-rose-500" />
                            심각 (Critical)
                          </span>
                        </th>
                        <th className="py-2.5 px-3 text-right">설정 상태</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {rows.map((row) => {
                        const isError = Number(row.warning) >= Number(row.critical)
                        return (
                          <tr
                            key={row.metricKey}
                            className={`hover:bg-slate-50/60 transition-colors ${
                              isError ? 'bg-rose-50/50' : row.isCustomized ? 'bg-brand-50/30' : ''
                            }`}
                          >
                            <td className="py-3 px-3">
                              <div className="font-semibold text-slate-800">{row.label}</div>
                              <div className="text-[11px] text-slate-400">{row.desc}</div>
                            </td>

                            {/* 경고 입력 */}
                            <td className="py-3 px-3">
                              <div className="relative flex items-center">
                                <input
                                  type="number"
                                  value={row.warning}
                                  onChange={(e) => updateRow(row.metricKey, 'warning', e.target.value)}
                                  className={`w-full rounded-md border bg-white py-1.5 pl-2.5 pr-8 text-sm font-mono transition-colors focus:outline-none focus:ring-1 ${
                                    isError
                                      ? 'border-rose-400 text-rose-600 focus:border-rose-500 focus:ring-rose-500'
                                      : 'border-slate-300 focus:border-amber-500 focus:ring-amber-500'
                                  }`}
                                />
                                <span className="absolute right-2 text-xs font-semibold text-slate-400 pointer-events-none">
                                  {row.unit}
                                </span>
                              </div>
                            </td>

                            {/* 심각 입력 */}
                            <td className="py-3 px-3">
                              <div className="relative flex items-center">
                                <input
                                  type="number"
                                  value={row.critical}
                                  onChange={(e) => updateRow(row.metricKey, 'critical', e.target.value)}
                                  className={`w-full rounded-md border bg-white py-1.5 pl-2.5 pr-8 text-sm font-mono transition-colors focus:outline-none focus:ring-1 ${
                                    isError
                                      ? 'border-rose-400 text-rose-600 focus:border-rose-500 focus:ring-rose-500'
                                      : 'border-slate-300 focus:border-rose-500 focus:ring-rose-500'
                                  }`}
                                />
                                <span className="absolute right-2 text-xs font-semibold text-slate-400 pointer-events-none">
                                  {row.unit}
                                </span>
                              </div>
                            </td>

                            {/* 상태 배지 */}
                            <td className="py-3 px-3 text-right">
                              {isError ? (
                                <span className="inline-block rounded bg-rose-100 px-2 py-0.5 text-xs font-bold text-rose-700">
                                  범위 오류
                                </span>
                              ) : row.isCustomized ? (
                                <span className="inline-block rounded bg-brand-100 px-2 py-0.5 text-xs font-semibold text-brand-700">
                                  커스텀
                                </span>
                              ) : (
                                <span className="inline-block text-xs text-slate-400">기본값</span>
                              )}
                            </td>
                          </tr>
                        )
                      })}
                    </tbody>
                  </table>
                </div>

                {/* 에러 및 피드백 메시지 */}
                {invalidRow && (
                  <div className="mt-4 rounded-lg border border-rose-300 bg-rose-50 p-3 text-xs font-medium text-rose-700">
                    [{invalidRow.label}] 심각값은 경고값보다 커야 합니다.
                  </div>
                )}
                {saveError && (
                  <div className="mt-4 rounded-lg border border-rose-300 bg-rose-50 p-3 text-xs font-medium text-rose-700">
                    {saveError}
                  </div>
                )}
                {saved && (
                  <div className="mt-4 rounded-lg border border-emerald-300 bg-emerald-50 p-3 text-xs font-semibold text-emerald-800">
                    임계치 설정이 성공적으로 저장되었습니다.
                  </div>
                )}

                {/* 하단 저장 바 */}
                <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between">
                  <span className="text-xs text-slate-400">
                    * 임계치를 변경하면 실시간 모니터링 상태 점 및 알림 기준에 즉시 반영됩니다.
                  </span>
                  <button
                    type="button"
                    onClick={handleSave}
                    disabled={saving || !!invalidRow}
                    className="rounded-md bg-brand-500 px-6 py-2 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors disabled:opacity-50"
                  >
                    {saving ? '저장 중...' : '임계치 저장'}
                  </button>
                </div>
              </>
            )}
          </Card>
        </div>
      </div>
    </div>
  )
}