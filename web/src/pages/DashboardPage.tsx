import { Fragment, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import ApiKeyModal from '../components/manage/ApiKeyModal'
import Card from '../components/ui/Card'
import StatusDot from '../components/ui/StatusDot'
import { ApiError } from '../lib/api'
import {
  getInstances,
  getServers,
  type InstanceListItem,
  type ServerItem,
} from '../lib/instances'
import { deleteServer } from '../lib/servers'

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function DashboardPage() {
  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [expanded, setExpanded] = useState<Record<number, boolean>>({})
  const [servers, setServers] = useState<Record<number, ServerItem[]>>({})
  const [serversLoading, setServersLoading] = useState<Record<number, boolean>>({})

  const [searchQuery, setSearchQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'CONNECTED' | 'DISCONNECTED'>('ALL')

  // API Key 관리 모달 상태
  const [selectedAppForKeys, setSelectedAppForKeys] = useState<ServerItem | null>(null)
  const [selectedInstanceName, setSelectedInstanceName] = useState<string>('')

  // 인스턴스 목록 로드
  useEffect(() => {
    fetchInstances()
  }, [])

  const fetchInstances = () => {
    setLoading(true)
    getInstances()
      .then((res) => {
        setInstances(res)
        // 기본적으로 모든 인스턴스를 펼쳐둠
        const initialExpanded: Record<number, boolean> = {}
        res.forEach((i) => {
          initialExpanded[i.instanceId] = true
          loadServersForInstance(i.instanceId)
        })
        setExpanded(initialExpanded)
      })
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : '인스턴스 목록을 불러오지 못했습니다.')
      )
      .finally(() => setLoading(false))
  }

  const loadServersForInstance = (instanceId: number) => {
    setServersLoading((prev) => ({ ...prev, [instanceId]: true }))
    getServers(instanceId)
      .then((res) => setServers((prev) => ({ ...prev, [instanceId]: res })))
      .catch(() => setServers((prev) => ({ ...prev, [instanceId]: [] })))
      .finally(() => setServersLoading((prev) => ({ ...prev, [instanceId]: false })))
  }

  const toggleExpand = (instanceId: number) => {
    setExpanded((prev) => {
      const nextState = !prev[instanceId]
      if (nextState && !servers[instanceId] && !serversLoading[instanceId]) {
        loadServersForInstance(instanceId)
      }
      return { ...prev, [instanceId]: nextState }
    })
  }

  const handleDeleteApp = async (instanceId: number, serverId: number) => {
    if (!confirm('정말 이 애플리케이션을 삭제하시겠습니까? 수집된 지표 데이터가 삭제됩니다.')) return
    try {
      await deleteServer(serverId)
      setServers((prev) => ({
        ...prev,
        [instanceId]: (prev[instanceId] ?? []).filter((s) => s.serverId !== serverId),
      }))
      setInstances((prev) =>
        prev.map((i) =>
          i.instanceId === instanceId ? { ...i, serverCount: Math.max(0, i.serverCount - 1) } : i
        )
      )
    } catch (err) {
      alert(err instanceof ApiError ? err.message : '삭제에 실패했습니다.')
    }
  }

  // 통계 집계
  const stats = useMemo(() => {
    const totalInstances = instances.length
    let totalApps = 0
    let connectedApps = 0
    let disconnectedApps = 0

    Object.values(servers).forEach((appList) => {
      appList.forEach((app) => {
        totalApps += 1
        if (app.status === 'CONNECTED') connectedApps += 1
        else disconnectedApps += 1
      })
    })

    return { totalInstances, totalApps, connectedApps, disconnectedApps }
  }, [instances, servers])

  // 검색 & 필터링 적용된 인스턴스 목록
  const filteredInstances = useMemo(() => {
    return instances.filter((inst) => {
      const appList = servers[inst.instanceId] ?? []
      const matchesSearch =
        searchQuery.trim() === '' ||
        inst.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (inst.ip && inst.ip.includes(searchQuery)) ||
        appList.some((a) => a.name.toLowerCase().includes(searchQuery.toLowerCase()))

      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'CONNECTED' &&
          (inst.status === 'CONNECTED' || appList.some((a) => a.status === 'CONNECTED'))) ||
        (statusFilter === 'DISCONNECTED' &&
          (inst.status === 'DISCONNECTED' || appList.some((a) => a.status === 'DISCONNECTED')))

      return matchesSearch && matchesStatus
    })
  }, [instances, servers, searchQuery, statusFilter])

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6">
      {/* 1. 상단 글로벌 헤더 */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">인프라 관리</h1>
          <p className="mt-1 text-sm text-slate-500">
            등록된 모든 호스트 인스턴스와 애플리케이션의 실시간 가동 상태를 조회하고, API Key 및 리소스를 관리합니다.
          </p>
        </div>
        <div className="flex items-center gap-2.5">
          <Link
            to="/instances/new"
            className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors"
          >
            + 인스턴스 등록
          </Link>
        </div>
      </div>

      {/* 2. 인프라 요약 KPI 통계 카드 바 (4-Card Grid) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="p-4 flex items-center justify-between border-slate-200">
          <div>
            <span className="text-xs font-medium text-slate-500">전체 인스턴스</span>
            <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.totalInstances}대</div>
          </div>
          <div className="rounded-lg bg-slate-100 p-2 text-slate-600">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2m-2-4h.01M17 16h.01" />
            </svg>
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between border-slate-200">
          <div>
            <span className="text-xs font-medium text-slate-500">전체 애플리케이션</span>
            <div className="text-2xl font-bold text-slate-900 mt-0.5">{stats.totalApps}개</div>
          </div>
          <div className="rounded-lg bg-brand-50 p-2 text-brand-600">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M10 20l4-16m4 4l4 4-4 4M6 16l-4-4 4-4" />
            </svg>
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between border-slate-200">
          <div>
            <span className="text-xs font-medium text-slate-500">정상 가동 중</span>
            <div className="text-2xl font-bold text-emerald-600 mt-0.5">{stats.connectedApps}개</div>
          </div>
          <div className="rounded-lg bg-emerald-50 p-2 text-emerald-600">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
          </div>
        </Card>

        <Card className="p-4 flex items-center justify-between border-slate-200">
          <div>
            <span className="text-xs font-medium text-slate-500">연결 대기</span>
            <div className="text-2xl font-bold text-slate-500 mt-0.5">{stats.disconnectedApps}개</div>
          </div>
          <div className="rounded-lg bg-slate-100 p-2 text-slate-400">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
        </Card>
      </div>

      {/* 3. 검색 및 스마트 필터 바 */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white p-3 rounded-lg border border-slate-200 shadow-2xs">
        {/* 검색 인풋 */}
        <div className="relative flex-1 w-full">
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="인스턴스명, 호스트 IP, 애플리케이션 이름으로 검색..."
            className="w-full rounded-md border border-slate-300 bg-white pl-9 pr-3 py-1.5 text-xs text-slate-800 placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
          <svg
            className="absolute left-2.5 top-2 h-4 w-4 text-slate-400"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
        </div>

        {/* 상태 필터 알약 버튼 */}
        <div className="flex items-center gap-2 w-full sm:w-auto justify-between sm:justify-end">
          <div className="flex rounded-lg border border-slate-200 bg-slate-100 p-0.5 text-xs">
            <button
              type="button"
              onClick={() => setStatusFilter('ALL')}
              className={`rounded-md px-2.5 py-1 font-medium transition-colors ${
                statusFilter === 'ALL' ? 'bg-white text-slate-900 shadow-2xs font-semibold' : 'text-slate-600'
              }`}
            >
              전체
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('CONNECTED')}
              className={`rounded-md px-2.5 py-1 font-medium transition-colors ${
                statusFilter === 'CONNECTED' ? 'bg-white text-emerald-700 shadow-2xs font-semibold' : 'text-slate-600'
              }`}
            >
              정상 연결
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('DISCONNECTED')}
              className={`rounded-md px-2.5 py-1 font-medium transition-colors ${
                statusFilter === 'DISCONNECTED' ? 'bg-white text-slate-800 shadow-2xs font-semibold' : 'text-slate-600'
              }`}
            >
              연결 대기
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div className="rounded-lg border border-danger-200 bg-danger-50 p-4 text-sm text-danger-700">
          {error}
        </div>
      )}

      {/* 4. 계층형 인프라 테이블 */}
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-2xs">
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50/80 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
              <th className="py-3 px-4">인스턴스 / 앱 이름</th>
              <th className="py-3 px-4">호스트 IP / 포트</th>
              <th className="py-3 px-4">상태</th>
              <th className="py-3 px-4">최근 데이터 수집</th>
              <th className="py-3 px-4 text-right">작업</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading && (
              <tr>
                <td colSpan={5} className="py-16 text-center text-sm text-slate-400">
                  인프라 연동 현황을 불러오는 중...
                </td>
              </tr>
            )}

            {!loading && filteredInstances.length === 0 && (
              <tr>
                <td colSpan={5} className="py-16 text-center text-sm text-slate-400">
                  {searchQuery || statusFilter !== 'ALL'
                    ? '조건에 일치하는 인스턴스 또는 애플리케이션이 없습니다.'
                    : '등록된 인스턴스가 없습니다. 상단에서 첫 번째 인스턴스를 등록해 보세요.'}
                </td>
              </tr>
            )}

            {!loading &&
              filteredInstances.map((inst) => {
                const isExpanded = !!expanded[inst.instanceId]
                const appList = servers[inst.instanceId] ?? []
                const isAppLoading = !!serversLoading[inst.instanceId]
                const totalApps = appList.length > 0 ? appList.length : inst.serverCount
                const connectedAppsCount = appList.filter((a) => a.status === 'CONNECTED').length

                return (
                  <Fragment key={inst.instanceId}>
                    {/* 부모 행: 인스턴스 (Host Row) */}
                    <tr className="bg-slate-50/70 hover:bg-slate-100/60 transition-colors border-t border-slate-200 font-medium">
                      {/* 1. 인스턴스 이름 & 토글 */}
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => toggleExpand(inst.instanceId)}
                            className="flex h-5 w-5 items-center justify-center rounded text-slate-400 hover:bg-slate-200 hover:text-slate-700 transition-colors"
                          >
                            <svg
                              className={`h-3.5 w-3.5 transition-transform duration-150 ${
                                isExpanded ? 'rotate-90 text-slate-700' : ''
                              }`}
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                              strokeWidth={2.5}
                            >
                              <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                            </svg>
                          </button>
                          <Link
                            to={`/monitoring?instance=${inst.instanceId}`}
                            className="font-bold text-slate-900 hover:text-brand-600 hover:underline transition-colors"
                          >
                            {inst.name}
                          </Link>
                          <span className="inline-flex items-center rounded-md bg-slate-800 px-2 py-0.5 text-[10px] font-bold text-white tracking-wide shadow-2xs">
                            Host
                          </span>
                        </div>
                      </td>

                      {/* 2. 호스트 IP */}
                      <td className="py-3 px-4 font-mono text-xs text-slate-600">
                        {inst.ip || '—'}
                      </td>

                      {/* 3. 상태 (호스트 수집 상태 + 소속 앱 가동 수) */}
                      <td className="py-3 px-4">
                        <StatusDot
                          tone={inst.status === 'CONNECTED' ? 'normal' : 'idle'}
                          label={
                            inst.status === 'CONNECTED'
                              ? `호스트 연결됨 (앱 ${connectedAppsCount}/${totalApps})`
                              : `호스트 대기 중 (앱 ${connectedAppsCount}/${totalApps})`
                          }
                        />
                      </td>

                      {/* 4. 등록일시 */}
                      <td className="py-3 px-4 text-xs text-slate-500 font-mono">
                        {formatDate(inst.createdAt)}
                      </td>

                      {/* 5. 인스턴스 액션 */}
                      <td className="py-3 px-4 text-right space-x-2">
                        <Link
                          to={`/apps/link?instance=${inst.instanceId}`}
                          className="inline-flex items-center rounded-md bg-white border border-slate-200 px-2.5 py-1 text-xs font-semibold text-slate-700 hover:bg-slate-50 shadow-2xs transition-colors"
                        >
                          + 새 앱 연동
                        </Link>
                        <span className="text-slate-300">|</span>
                        <button
                          type="button"
                          onClick={() =>
                            alert('인스턴스 삭제는 안전을 위해 소속된 앱을 모두 삭제한 뒤 진행해주세요.')
                          }
                          className="text-xs font-medium text-danger-500 hover:text-danger-700 px-1"
                        >
                          삭제
                        </button>
                      </td>
                    </tr>

                    {/* 자식 행: 소속된 Spring Boot 애플리케이션 목록 (App Sub-Rows) */}
                    {isExpanded && isAppLoading && (
                      <tr>
                        <td colSpan={5} className="py-3 pl-12 text-xs text-slate-400 bg-white">
                          소속 애플리케이션을 불러오는 중...
                        </td>
                      </tr>
                    )}

                    {isExpanded &&
                      !isAppLoading &&
                      appList.map((app) => (
                        <tr key={app.serverId} className="hover:bg-slate-50/80 transition-colors bg-white">
                          {/* 1. 앱 이름 (들여쓰기 트리 구조) */}
                          <td className="py-3 px-4 pl-10">
                            <div className="flex items-center gap-2">
                              <span className="text-slate-300 font-mono">└</span>
                              <Link
                                to={`/monitoring?instance=${inst.instanceId}&app=${app.serverId}`}
                                className="font-bold text-slate-800 hover:text-brand-600 hover:underline transition-colors"
                              >
                                {app.name}
                              </Link>
                              <span className="inline-flex items-center rounded-md bg-emerald-100 px-2 py-0.5 text-[10px] font-bold text-emerald-900 border border-emerald-300 shadow-2xs">
                                Spring Boot
                              </span>
                            </div>
                          </td>

                          {/* 2. 포트 번호 */}
                          <td className="py-3 px-4 font-mono text-xs text-slate-600">
                            포트: {app.port ?? '—'}
                          </td>

                          {/* 3. 연결 상태 */}
                          <td className="py-3 px-4">
                            <StatusDot
                              tone={app.status === 'CONNECTED' ? 'normal' : 'idle'}
                              label={app.status === 'CONNECTED' ? '연결됨' : '대기 중'}
                            />
                          </td>

                          {/* 4. 마지막 데이터 수신 시간 */}
                          <td className="py-3 px-4 text-xs text-slate-500 font-mono">
                            {formatDate(app.lastReceivedAt)}
                          </td>

                          {/* 5. 앱 액션 (키 관리 모달 | 삭제) */}
                          <td className="py-3 px-4 text-right space-x-2">
                            <button
                              type="button"
                              onClick={() => {
                                setSelectedAppForKeys(app)
                                setSelectedInstanceName(inst.name)
                              }}
                              className="text-xs font-semibold text-slate-800 hover:text-slate-900 hover:underline transition-colors"
                            >
                              키 관리
                            </button>
                            <span className="text-slate-300">|</span>
                            <button
                              type="button"
                              onClick={() => handleDeleteApp(inst.instanceId, app.serverId)}
                              className="text-xs font-medium text-danger-500 hover:text-danger-700"
                            >
                              삭제
                            </button>
                          </td>
                        </tr>
                      ))}

                    {isExpanded && !isAppLoading && appList.length === 0 && (
                      <tr>
                        <td colSpan={5} className="py-4 pl-10 text-xs text-slate-400 bg-white">
                          등록된 애플리케이션이 없습니다.{' '}
                          <Link
                            to={`/apps/link?instance=${inst.instanceId}`}
                            className="text-brand-600 font-medium hover:underline"
                          >
                            + 이 인스턴스에 첫 번째 앱 연동하기
                          </Link>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                )
              })}
          </tbody>
        </table>
      </div>

      <p className="text-xs text-slate-400">
        * 인스턴스 또는 애플리케이션 이름을 클릭하면 해당 실시간 모니터링 화면으로 바로 이동합니다.
      </p>

      {/* API Key 관리 및 재발급 모달 */}
      <ApiKeyModal
        app={selectedAppForKeys}
        instanceName={selectedInstanceName}
        onClose={() => setSelectedAppForKeys(null)}
      />
    </div>
  )
}