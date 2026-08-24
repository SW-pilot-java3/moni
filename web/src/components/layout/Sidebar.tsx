import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import {
  getInstances,
  getServers,
  type InstanceListItem,
  type ServerItem,
} from '../../lib/instances'

interface UserProfile {
  username?: string
  name?: string
  email?: string
}

export default function Sidebar() {
  const location = useLocation()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [serversByInstance, setServersByInstance] = useState<Record<number, ServerItem[]>>({})
  const [expanded, setExpanded] = useState<Record<number, boolean>>({})
  const [user, setUser] = useState<UserProfile | null>(null)

  useEffect(() => {
    const raw = localStorage.getItem('moni_user')
    if (raw) {
      try {
        setUser(JSON.parse(raw))
      } catch {
        setUser(null)
      }
    }
  }, [])

  useEffect(() => {
    getInstances()
      .then((res) => {
        setInstances(res)
        // 첫 번째 인스턴스는 기본 펼침
        if (res.length > 0) {
          setExpanded((prev) => ({ ...prev, [res[0].instanceId]: true }))
          loadServersForInstance(res[0].instanceId)
        }
      })
      .catch(() => {})
  }, [])

  const loadServersForInstance = (instanceId: number) => {
    if (serversByInstance[instanceId]) return
    getServers(instanceId)
      .then((res) => setServersByInstance((prev) => ({ ...prev, [instanceId]: res })))
      .catch(() => setServersByInstance((prev) => ({ ...prev, [instanceId]: [] })))
  }

  const toggleExpand = (e: React.MouseEvent, instanceId: number) => {
    e.preventDefault()
    e.stopPropagation()
    setExpanded((prev) => {
      const next = !prev[instanceId]
      if (next) loadServersForInstance(instanceId)
      return { ...prev, [instanceId]: next }
    })
  }

  const handleLogout = () => {
    localStorage.removeItem('moni_token')
    localStorage.removeItem('moni_user')
    navigate('/login')
  }

  const currentInstanceId = searchParams.get('instance')
  const currentServerId = searchParams.get('app')
  const isMonitoring = location.pathname.startsWith('/monitoring')

  return (
    <aside className="w-64 border-r border-slate-200 bg-white flex flex-col justify-between h-screen shrink-0">
      <div className="p-4 overflow-y-auto">
        {/* 서비스 로고 */}
        <Link to="/dashboard" className="flex items-center gap-2 px-2 py-3 mb-4">
          <div className="h-8 w-8 rounded-lg bg-brand-500 flex items-center justify-center text-white font-bold shadow-sm">
            M
          </div>
          <span className="text-xl font-bold text-slate-900 tracking-tight">Moni</span>
        </Link>

        <nav className="flex flex-col gap-4">
          {/* 섹션 1: 모니터링 */}
          <div>
            <div className="px-3 pb-2 text-xs font-bold text-slate-700 tracking-tight">
              모니터링
            </div>
            <div className="flex flex-col gap-1">
              <div>
                <Link
                  to={instances.length > 0 ? `/monitoring?instance=${instances[0].instanceId}` : '/monitoring'}
                  className={`flex w-full items-center justify-between rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    isMonitoring
                      ? 'bg-brand-50/70 font-semibold text-brand-700'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`}
                >
                  <span>인프라 모니터링</span>
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
                              ? 'bg-brand-100/80 font-bold text-brand-800'
                              : 'text-slate-700 hover:bg-slate-50 hover:text-slate-900'
                          }`}
                        >
                          <Link
                            to={`/monitoring?instance=${inst.instanceId}`}
                            className={`flex-1 truncate ${isInstSelected ? 'font-bold text-brand-800' : 'font-medium'}`}
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
            </div>
          </div>

          {/* 섹션 구분선 */}
          <div className="border-t border-slate-100" />

          {/* 섹션 2: 인스턴스 · 앱 */}
          <div>
            <div className="px-3 pb-2 text-xs font-bold text-slate-700 tracking-tight">
              인스턴스 · 앱
            </div>
            <div className="flex flex-col gap-1.5">
              {/* 시작하기 서브 메뉴 (최상단) */}
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
                    Spring Boot 앱 연동
                  </NavLink>
                </div>
              </div>

              {/* 인프라 관리 */}
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
                인프라 관리
              </NavLink>

              {/* 임계치 설정 */}
              <NavLink
                to="/thresholds"
                className={({ isActive }) =>
                  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-brand-50 text-brand-700 font-semibold'
                      : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                임계치 설정
              </NavLink>
            </div>
          </div>
        </nav>
      </div>

      {/* 하단 사용자 프로필 및 로그아웃 */}
      <div className="p-4 border-t border-slate-100 bg-slate-50/50">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2.5 overflow-hidden">
            <div className="h-8 w-8 rounded-full bg-slate-200 border border-slate-300 flex items-center justify-center text-slate-700 text-xs font-bold shrink-0">
              {(user?.name || user?.username || 'U')[0].toUpperCase()}
            </div>
            <div className="overflow-hidden text-xs">
              <p className="font-bold text-slate-900 truncate">{user?.name || user?.username || '사용자'}</p>
              <p className="text-slate-400 truncate">{user?.email || 'user@example.com'}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
            title="로그아웃"
          >
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
            </svg>
          </button>
        </div>
      </div>
    </aside>
  )
}