import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useSearchParams } from 'react-router-dom'
import { getInstances, getServers, type InstanceListItem, type ServerItem } from '../../lib/instances'

export default function Sidebar() {
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const isMonitoring = location.pathname.startsWith('/monitoring')
  const currentInstanceId = searchParams.get('instance')
  const currentServerId = searchParams.get('app')

  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [serversByInstance, setServersByInstance] = useState<Record<number, ServerItem[]>>({})
  const [expanded, setExpanded] = useState<Record<number, boolean>>({})

  const ensureServers = (instanceId: number) => {
    if (serversByInstance[instanceId]) return
    getServers(instanceId)
      .then((res) => setServersByInstance((prev) => ({ ...prev, [instanceId]: res })))
      .catch(() => setServersByInstance((prev) => ({ ...prev, [instanceId]: [] })))
  }

  useEffect(() => {
    getInstances()
      .then((res) => {
        setInstances(res)
        if (res.length > 0) {
          const targetId = currentInstanceId ? Number(currentInstanceId) : res[0].instanceId
          setExpanded((prev) => ({ ...prev, [targetId]: true }))
          ensureServers(targetId)
        }
      })
      .catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (currentInstanceId) {
      const instId = Number(currentInstanceId)
      setExpanded((prev) => ({ ...prev, [instId]: true }))
      ensureServers(instId)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentInstanceId])

  const toggleExpand = (e: React.MouseEvent, instanceId: number) => {
    e.preventDefault()
    e.stopPropagation()
    setExpanded((prev) => ({ ...prev, [instanceId]: !prev[instanceId] }))
    ensureServers(instanceId)
  }

  return (
    <aside className="w-64 shrink-0 border-r border-slate-200 bg-white px-3 py-6 flex flex-col justify-between select-none">
      <div>
        <Link to="/dashboard" className="mb-7 flex items-center gap-3 px-3 group">
          <img src="/favicon/favicon.svg" alt="Moni Logo" className="h-8 w-8 rounded-lg shadow-2xs transition-transform group-hover:scale-105" />
          <span className="font-mono text-2xl font-extrabold tracking-tight text-brand-600">Moni</span>
        </Link>

        <nav className="flex flex-col gap-4">
          {/* 섹션 1: 모니터링 */}
          <div>
            <div className="px-3 pb-2 text-xs font-bold text-slate-700 tracking-tight">
              모니터링
            </div>
            <div className="flex flex-col gap-1">
              {/* 실시간 모니터링 섹션 */}
              <div>
                <Link
                  to="/monitoring"
                  className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    isMonitoring
                      ? 'bg-brand-50/70 font-semibold text-brand-700'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`}
                >
                  <span>실시간 모니터링</span>
                </Link>

                {/* 인스턴스 / 앱 아코디언 트리 */}
                <div className="mt-1 flex flex-col gap-0.5 pl-2 border-l border-slate-100 ml-3">
                  {instances.map((inst) => {
                    const isInstExpanded = !!expanded[inst.instanceId]
                    const isInstSelected = isMonitoring && currentInstanceId === String(inst.instanceId) && !currentServerId
                    const servers = serversByInstance[inst.instanceId] ?? []

                    return (
                      <div key={inst.instanceId} className="flex flex-col">
                        <div
                          className={`group flex items-center justify-between rounded-md px-2 py-1.5 text-xs transition-colors ${
                            isInstSelected
                              ? 'bg-brand-100/70 font-bold text-brand-800'
                              : 'text-slate-700 hover:bg-slate-50 hover:text-slate-900'
                          }`}
                        >
                          <Link
                            to={`/monitoring?instance=${inst.instanceId}`}
                            className="flex-1 truncate font-medium"
                          >
                            {inst.name}
                          </Link>
                          <button
                            type="button"
                            onClick={(e) => toggleExpand(e, inst.instanceId)}
                            className="flex h-6 w-6 items-center justify-center rounded text-slate-400 transition-colors hover:bg-slate-200/60 hover:text-slate-700"
                            title={isInstExpanded ? '접기' : '펼치기'}
                          >
                            <svg
                              className={`h-3.5 w-3.5 transition-transform duration-150 ${
                                isInstExpanded ? 'rotate-90 text-slate-600' : ''
                              }`}
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                              strokeWidth={2.5}
                            >
                              <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                            </svg>
                          </button>
                        </div>

                        {isInstExpanded && (
                          <div className="flex flex-col gap-0.5 pl-3 border-l border-slate-100 ml-2 my-0.5">
                            {servers.map((srv) => {
                              const isAppSelected =
                                isMonitoring &&
                                currentInstanceId === String(inst.instanceId) &&
                                currentServerId === String(srv.serverId)

                              return (
                                <Link
                                  key={srv.serverId}
                                  to={`/monitoring?instance=${inst.instanceId}&app=${srv.serverId}`}
                                  className={`flex items-center justify-between rounded-md px-2 py-1 text-[11px] transition-colors ${
                                    isAppSelected
                                      ? 'bg-brand-100/80 font-bold text-brand-800'
                                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                                  }`}
                                >
                                  <span className="truncate">{srv.name}</span>
                                  <span
                                    className={`h-1.5 w-1.5 shrink-0 rounded-full ${
                                      srv.status === 'CONNECTED' ? 'bg-emerald-500 shadow-sm shadow-emerald-500/50' : 'bg-slate-300'
                                    }`}
                                    title={srv.status === 'CONNECTED' ? '연결됨 (CONNECTED)' : '연결 대기 (DISCONNECTED)'}
                                  />
                                </Link>
                              )
                            })}
                            {servers.length === 0 && (
                              <span className="px-2 py-1 text-[10px] text-slate-400">등록된 앱 없음</span>
                            )}
                          </div>
                        )}
                      </div>
                    )
                  })}
                  {instances.length === 0 && (
                    <span className="px-2 py-1 text-xs text-slate-400">인스턴스 없음</span>
                  )}
                </div>
              </div>

              <NavLink
                to="/history"
                className={({ isActive }) =>
                  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-brand-50 text-brand-700'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                과거 데이터 조회
              </NavLink>
            </div>
          </div>

          {/* 섹션 구분선 */}
          <div className="border-t border-slate-100" />

          {/* 섹션 2: 인스턴스 · 앱 */}
          <div>
            <div className="px-3 pb-2 text-xs font-bold text-slate-700 tracking-tight">
              인스턴스 · 앱
            </div>
            <div className="flex flex-col gap-1">
              {/* 시작하기 서브 메뉴 */}
              <div>
                <Link
                  to="/instances/new"
                  className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    location.pathname.startsWith('/instances/new') || location.pathname.startsWith('/apps/link')
                      ? 'bg-brand-50/70 font-semibold text-brand-700'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`}
                >
                  <span>시작하기</span>
                </Link>

                <div className="mt-1 flex flex-col gap-0.5 pl-2 border-l border-slate-100 ml-3">
                  <NavLink
                    to="/instances/new"
                    className={({ isActive }) =>
                      `flex min-h-[36px] items-center rounded-md px-2 py-1.5 text-xs transition-colors ${
                        isActive
                          ? 'bg-brand-100/80 font-bold text-brand-800'
                          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                      }`
                    }
                  >
                    인스턴스 등록
                  </NavLink>
                  <NavLink
                    to="/apps/link"
                    className={({ isActive }) =>
                      `flex min-h-[36px] items-center rounded-md px-2 py-1.5 text-xs transition-colors ${
                        isActive
                          ? 'bg-brand-100/80 font-bold text-brand-800'
                          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                      }`
                    }
                  >
                    앱 연동 (Spring Boot)
                  </NavLink>
                </div>
              </div>

              <NavLink
                to="/dashboard"
                className={({ isActive }) =>
                  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-brand-50 text-brand-700 font-semibold'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                연동 현황
              </NavLink>

              <NavLink
                to="/manage"
                className={({ isActive }) =>
                  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-brand-50 text-brand-700 font-semibold'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                상세 관리
              </NavLink>

              {/* 임계치 설정 서브 메뉴 */}
              <div>
                <Link
                  to="/thresholds/instance"
                  className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    location.pathname.startsWith('/thresholds')
                      ? 'bg-brand-50/70 font-semibold text-brand-700'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`}
                >
                  <span>임계치 설정</span>
                </Link>

                <div className="mt-1 flex flex-col gap-0.5 pl-2 border-l border-slate-100 ml-3">
                  <NavLink
                    to="/thresholds/instance"
                    className={({ isActive }) =>
                      `flex min-h-[36px] items-center rounded-md px-2 py-1.5 text-xs transition-colors ${
                        isActive || location.pathname === '/thresholds'
                          ? 'bg-brand-100/80 font-bold text-brand-800'
                          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                      }`
                    }
                  >
                    인스턴스
                  </NavLink>
                  <NavLink
                    to="/thresholds/app"
                    className={({ isActive }) =>
                      `flex min-h-[36px] items-center rounded-md px-2 py-1.5 text-xs transition-colors ${
                        isActive
                          ? 'bg-brand-100/80 font-bold text-brand-800'
                          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                      }`
                    }
                  >
                    앱 (Spring Boot)
                  </NavLink>
                </div>
              </div>
            </div>
          </div>
        </nav>
      </div>
    </aside>
  )
}