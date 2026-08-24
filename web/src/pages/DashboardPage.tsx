import { Fragment, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import StatusDot from '../components/ui/StatusDot'
import { ApiError } from '../lib/api'
import { getInstances, getServers, type InstanceListItem, type ServerItem } from '../lib/instances'

const instanceStatusMeta = {
  CONNECTED: { tone: 'normal' as const, label: '정상' },
  DISCONNECTED: { tone: 'idle' as const, label: '호스트 미수집' },
}

const serverStatusMeta = {
  CONNECTED: { tone: 'normal' as const, label: '연결됨' },
  DISCONNECTED: { tone: 'idle' as const, label: '대기' },
}

function formatDate(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export default function DashboardPage() {
  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [expanded, setExpanded] = useState<Record<number, boolean>>({})
  const [servers, setServers] = useState<Record<number, ServerItem[]>>({})
  const [serversLoading, setServersLoading] = useState<Record<number, boolean>>({})

  useEffect(() => {
    getInstances()
      .then(setInstances)
      .catch((err) => setError(err instanceof ApiError ? err.message : '인스턴스 목록을 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
  }, [])

  const toggle = (instanceId: number) => {
    setExpanded((prev) => ({ ...prev, [instanceId]: !prev[instanceId] }))

    if (!servers[instanceId] && !serversLoading[instanceId]) {
      setServersLoading((prev) => ({ ...prev, [instanceId]: true }))
      getServers(instanceId)
        .then((res) => setServers((prev) => ({ ...prev, [instanceId]: res })))
        .catch(() => setServers((prev) => ({ ...prev, [instanceId]: [] })))
        .finally(() => setServersLoading((prev) => ({ ...prev, [instanceId]: false })))
    }
  }

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6">
      <div className="flex items-center justify-between border-b border-slate-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">연동 현황</h1>
          <p className="mt-1 text-sm text-slate-500">
            등록된 모든 인스턴스와 애플리케이션의 계층 구조 및 실시간 가동 상태를 조회합니다.
          </p>
        </div>
        <Link
          to="/instances/new"
          className="rounded-lg bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors"
        >
          + 인스턴스 등록
        </Link>
      </div>

      <div className="mb-4 flex gap-3">
        <input
          type="text"
          placeholder="인스턴스 / 앱 이름 검색"
          className="flex-1 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm placeholder:text-slate-400 focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
        />
        <select className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600">
          <option>상태: 전체</option>
          <option>정상</option>
          <option>호스트 미수집</option>
        </select>
        <select className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600">
          <option>보기: 계층</option>
          <option>보기: 목록</option>
        </select>
      </div>

      {error && (
        <p className="mb-4 rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
          {error}
        </p>
      )}

      <div className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50 text-left text-slate-500">
              <th className="px-4 py-3 font-medium">이름</th>
              <th className="px-4 py-3 font-medium">상태</th>
              <th className="px-4 py-3 font-medium">CPU</th>
              <th className="px-4 py-3 font-medium">MEM</th>
              <th className="px-4 py-3 font-medium">힙 / NET</th>
              <th className="px-4 py-3 font-medium">최근 수신</th>
              <th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody>
            {!loading && instances.length === 0 && !error && (
              <tr>
                <td colSpan={7} className="px-4 py-10 text-center text-slate-400">
                  등록된 인스턴스가 없습니다.{' '}
                  <Link to="/instances/new" className="text-brand-600 hover:underline">
                    인스턴스를 등록해 보세요
                  </Link>
                  .
                </td>
              </tr>
            )}
            {instances.map((inst) => {
              const meta = instanceStatusMeta[inst.status]
              const isOpen = expanded[inst.instanceId]
              return (
                <Fragment key={inst.instanceId}>
                  <tr className="border-b border-slate-100 bg-slate-50/60">
                    <td className="px-4 py-3 font-semibold text-slate-800">
                      <button
                        type="button"
                        onClick={() => toggle(inst.instanceId)}
                        className="mr-2 inline-block w-3 text-slate-400"
                      >
                        {isOpen ? '▾' : '▸'}
                      </button>
                      {inst.name}{' '}
                      <span className="ml-1 text-xs font-normal text-slate-400">
                        인스턴스 · 앱 {inst.serverCount}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <StatusDot tone={meta.tone} label={meta.label} />
                    </td>
                    <td className="px-4 py-3 text-slate-400">—</td>
                    <td className="px-4 py-3 text-slate-400">—</td>
                    <td className="px-4 py-3 text-slate-400">—</td>
                    <td className="px-4 py-3 text-slate-500">{formatDate(inst.lastReceivedAt)}</td>
                    <td className="px-4 py-3 text-right">
                      <Link to={`/manage?instance=${inst.instanceId}`} className="text-brand-600 hover:underline">
                        {inst.status === 'DISCONNECTED' ? '가이드 ›' : '관리 ›'}
                      </Link>
                    </td>
                  </tr>
                  {isOpen && serversLoading[inst.instanceId] && (
                    <tr>
                      <td colSpan={7} className="px-4 py-3 pl-10 text-slate-400">
                        불러오는 중...
                      </td>
                    </tr>
                  )}
                  {isOpen &&
                    !serversLoading[inst.instanceId] &&
                    (servers[inst.instanceId] ?? []).map((app) => {
                      const appMeta = serverStatusMeta[app.status]
                      return (
                        <tr key={app.serverId} className="border-b border-slate-100">
                          <td className="px-4 py-3 pl-10 text-slate-700">{app.name}</td>
                          <td className="px-4 py-3">
                            <StatusDot tone={appMeta.tone} label={appMeta.label} />
                          </td>
                          <td className="px-4 py-3 text-slate-400">—</td>
                          <td className="px-4 py-3 text-slate-400">—</td>
                          <td className="px-4 py-3 text-slate-400">—</td>
                          <td className="px-4 py-3 text-slate-500">{formatDate(app.lastReceivedAt)}</td>
                          <td className="px-4 py-3 text-right">
                            <Link
                              to={`/monitoring?instance=${inst.instanceId}&app=${app.serverId}`}
                              className="text-brand-600 hover:underline"
                            >
                              {app.status === 'DISCONNECTED' ? '가이드 ›' : '상세 ›'}
                            </Link>
                          </td>
                        </tr>
                      )
                    })}
                  {isOpen && !serversLoading[inst.instanceId] && (servers[inst.instanceId] ?? []).length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-4 py-3 pl-10 text-slate-400">
                        등록된 앱이 없습니다.
                      </td>
                    </tr>
                  )}
                </Fragment>
              )
            })}
          </tbody>
        </table>
      </div>

      <p className="mt-4 text-xs text-slate-400">
        CPU · 메모리 · 네트워크는 인스턴스(호스트) 단위, 힙 · GC · HTTP · 커넥션 풀은 앱 단위로 수집됩니다.
      </p>
    </div>
  )
}