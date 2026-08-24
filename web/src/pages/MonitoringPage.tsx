import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import AppDetailView from '../components/monitoring/AppDetailView'
import InstanceDetailView from '../components/monitoring/InstanceDetailView'
import { ApiError } from '../lib/api'
import { getInstances, getServers, type InstanceListItem, type ServerItem } from '../lib/instances'

type Selection = { kind: 'instance'; instanceId: number } | { kind: 'app'; instanceId: number; serverId: number }

export default function MonitoringPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [serversByInstance, setServersByInstance] = useState<Record<number, ServerItem[]>>({})
  const [error, setError] = useState<string | null>(null)

  const instanceIdParam = searchParams.get('instance')
  const serverIdParam = searchParams.get('app')
  const selection: Selection | null = instanceIdParam
    ? serverIdParam
      ? { kind: 'app', instanceId: Number(instanceIdParam), serverId: Number(serverIdParam) }
      : { kind: 'instance', instanceId: Number(instanceIdParam) }
    : null

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
        ensureServers(validInstanceId)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : '인스턴스를 불러오지 못했습니다.'))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (instanceIdParam) {
      ensureServers(Number(instanceIdParam))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [instanceIdParam])

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
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-slate-400">
        등록된 인스턴스가 없습니다. 먼저 인스턴스를 등록해 주세요.
      </div>
    )
  }

  return (
    <div className="w-full flex-1 flex flex-col min-h-0">
      {selection?.kind === 'instance' && selectedInstance ? (
        <InstanceDetailView
          instance={selectedInstance}
          apps={serversByInstance[selectedInstance.instanceId] ?? []}
          onSelectApp={(serverId) => selectApp(selectedInstance.instanceId, serverId)}
        />
      ) : selection?.kind === 'app' && selectedServer ? (
        <AppDetailView server={selectedServer} />
      ) : (
        <div className="rounded-xl border border-slate-200 bg-white p-8 text-center text-slate-400">
          좌측 사이드바에서 모니터링할 인스턴스 또는 앱을 선택해 주세요.
        </div>
      )}
    </div>
  )
}