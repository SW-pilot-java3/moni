import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Card from '../components/ui/Card'
import AppDetailView from '../components/monitoring/AppDetailView'
import InstanceDetailView from '../components/monitoring/InstanceDetailView'
import { ApiError } from '../lib/api'
import { getInstances, getServers, type InstanceListItem, type ServerItem } from '../lib/instances'

type Selection = { kind: 'instance'; instanceId: number } | { kind: 'app'; instanceId: number; serverId: number }

export default function MonitoringPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [serversByInstance, setServersByInstance] = useState<Record<number, ServerItem[]>>({})
  const [expanded, setExpanded] = useState<Record<number, boolean>>({})
  const [error, setError] = useState<string | null>(null)

  // 선택 상태는 URL 쿼리(instance/app)에 반영해서, HMR 재연결 등으로 페이지가 리마운트돼도 보던 화면을 유지한다.
  const instanceIdParam = searchParams.get('instance')
  const serverIdParam = searchParams.get('app')
  const selection: Selection | null = instanceIdParam
    ? serverIdParam
      ? { kind: 'app', instanceId: Number(instanceIdParam), serverId: Number(serverIdParam) }
      : { kind: 'instance', instanceId: Number(instanceIdParam) }
    : null

  const selectInstance = (instanceId: number) => {
    setSearchParams({ instance: String(instanceId) })
  }

  const selectApp = (instanceId: number, serverId: number) => {
    setSearchParams({ instance: String(instanceId), app: String(serverId) })
  }

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
        if (res.length === 0) return

        const targetInstanceId = instanceIdParam ? Number(instanceIdParam) : res[0].instanceId
        const validInstanceId = res.some((i) => i.instanceId === targetInstanceId) ? targetInstanceId : res[0].instanceId

        if (!instanceIdParam) {
          setSearchParams({ instance: String(validInstanceId) }, { replace: true })
        }
        setExpanded({ [validInstanceId]: true })
        ensureServers(validInstanceId)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : '인스턴스를 불러오지 못했습니다.'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const toggle = (instanceId: number) => {
    setExpanded((prev) => ({ ...prev, [instanceId]: !prev[instanceId] }))
    ensureServers(instanceId)
  }

  const selectedInstance = selection ? instances.find((i) => i.instanceId === selection.instanceId) : undefined
  const selectedServer =
    selection?.kind === 'app'
      ? (serversByInstance[selection.instanceId] ?? []).find((s) => s.serverId === selection.serverId)
      : undefined

  if (error) {
    return (
      <p className="rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">{error}</p>
    )
  }

  if (instances.length === 0) {
    return <p className="text-sm text-slate-400">인스턴스가 없습니다.</p>
  }

  return (
    <div className="flex gap-6">
      <Card className="w-64 shrink-0 p-4">
        <div className="mb-3 px-1 text-xs font-medium text-slate-400">인스턴스 / 앱</div>
        <div className="flex flex-col gap-0.5">
          {instances.map((inst) => (
            <div key={inst.instanceId}>
              <button
                type="button"
                onClick={() => {
                  selectInstance(inst.instanceId)
                  toggle(inst.instanceId)
                }}
                className={`flex w-full items-center gap-1.5 rounded-md px-2 py-2 text-left text-sm font-semibold ${
                  selection?.kind === 'instance' && selection.instanceId === inst.instanceId
                    ? 'bg-brand-50 text-brand-700'
                    : 'text-slate-700 hover:bg-slate-50'
                }`}
              >
                <span className="w-3 text-slate-400">{expanded[inst.instanceId] ? '▾' : '▸'}</span>
                {inst.name}
              </button>
              {expanded[inst.instanceId] &&
                (serversByInstance[inst.instanceId] ?? []).map((srv) => (
                  <button
                    key={srv.serverId}
                    type="button"
                    onClick={() => selectApp(inst.instanceId, srv.serverId)}
                    className={`flex w-full items-center justify-between rounded-md py-2 pr-2 pl-8 text-left text-sm ${
                      selection?.kind === 'app' && selection.serverId === srv.serverId
                        ? 'bg-brand-50 font-medium text-brand-700'
                        : 'text-slate-600 hover:bg-slate-50'
                    }`}
                  >
                    {srv.name}
                    <span
                      className={`h-2 w-2 rounded-full ${srv.status === 'CONNECTED' ? 'bg-brand-500' : 'border border-slate-300'}`}
                    />
                  </button>
                ))}
              {expanded[inst.instanceId] && (serversByInstance[inst.instanceId] ?? []).length === 0 && (
                <p className="py-1.5 pl-8 text-xs text-slate-400">등록된 앱 없음</p>
              )}
            </div>
          ))}
        </div>
        <p className="mt-4 border-t border-slate-100 pt-3 text-xs leading-relaxed text-slate-400">
          인스턴스를 선택하면 호스트 지표, 앱을 선택하면 앱 지표가 열립니다.
        </p>
      </Card>

      {selection?.kind === 'instance' && selectedInstance ? (
        <div className="flex-1">
          <InstanceDetailView
            instance={selectedInstance}
            apps={serversByInstance[selectedInstance.instanceId] ?? []}
            onSelectApp={(serverId) => {
              selectApp(selectedInstance.instanceId, serverId)
              setExpanded((prev) => ({ ...prev, [selectedInstance.instanceId]: true }))
            }}
          />
        </div>
      ) : selection?.kind === 'app' && selectedServer ? (
        <div className="flex-1">
          <AppDetailView server={selectedServer} />
        </div>
      ) : null}
    </div>
  )
}