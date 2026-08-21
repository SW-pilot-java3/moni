import { useEffect, useState } from 'react'
import Card from '../components/ui/Card'
import StatusDot from '../components/ui/StatusDot'
import { ApiError } from '../lib/api'
import { getInstances, getServers, type InstanceListItem, type ServerItem } from '../lib/instances'
import { createApiKey, createServer, deleteServer } from '../lib/servers'

function formatDate(value: string | null) {
  if (!value) return '없음'
  return new Date(value).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export default function AppLinkPage() {
  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [instanceId, setInstanceId] = useState<number | null>(null)
  const [servers, setServers] = useState<ServerItem[]>([])
  const [loadError, setLoadError] = useState<string | null>(null)

  const [appName, setAppName] = useState('')
  const [port, setPort] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [issuedKey, setIssuedKey] = useState<{ serverId: number; apiKey: string; appName: string } | null>(null)

  useEffect(() => {
    getInstances()
      .then((res) => {
        setInstances(res)
        if (res.length > 0) setInstanceId(res[0].instanceId)
      })
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : '인스턴스 목록을 불러오지 못했습니다.'))
  }, [])

  useEffect(() => {
    if (instanceId === null) return
    getServers(instanceId)
      .then(setServers)
      .catch(() => setServers([]))
  }, [instanceId])

  const selectedInstance = instances.find((i) => i.instanceId === instanceId)
  const connectedCount = servers.filter((s) => s.status === 'CONNECTED').length
  const pendingCount = servers.length - connectedCount

  const handleAddServer = async () => {
    if (instanceId === null || !appName.trim() || !port.trim()) return
    setSubmitError(null)
    setSubmitting(true)
    try {
      const server = await createServer(instanceId, appName.trim(), Number(port))
      const key = await createApiKey(server.serverId)
      setIssuedKey({ serverId: server.serverId, apiKey: key.apiKey, appName: server.name })
      setServers((prev) => [...prev, server])
    } catch (err) {
      setSubmitError(err instanceof ApiError ? `${err.message} (${err.status})` : '서버에 연결할 수 없습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (serverId: number) => {
    try {
      await deleteServer(serverId)
      setServers((prev) => prev.filter((s) => s.serverId !== serverId))
      if (issuedKey?.serverId === serverId) setIssuedKey(null)
    } catch {
      // 목록은 유지하고 별도 처리 없이 무시
    }
  }

  return (
    <div className="max-w-4xl">
      <h1 className="text-2xl font-bold text-slate-900">앱 연동</h1>
      <p className="mt-1 text-sm text-slate-500">
        앱을 먼저 등록하고, 발급된 API 키를 앱에 넣어 재시작합니다. 유효하지 않은 API 키의 요청은 거부됩니다.
      </p>

      {loadError && (
        <p className="mt-4 rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
          {loadError}
        </p>
      )}

      <div className="mt-5 mb-6 flex items-center justify-between">
        <label className="flex items-center gap-3 text-sm text-slate-600">
          인스턴스
          <select
            value={instanceId ?? ''}
            onChange={(e) => setInstanceId(Number(e.target.value))}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800"
          >
            {instances.map((inst) => (
              <option key={inst.instanceId} value={inst.instanceId}>
                {inst.name} · {inst.ip} · 앱 {inst.serverCount}개
              </option>
            ))}
          </select>
        </label>
      </div>

      <Card className="mb-6 p-6">
        <h2 className="mb-1 font-semibold text-slate-900">
          ① 앱 등록 <span className="ml-2 text-xs font-normal text-slate-400">app-name 을 먼저 등록해야 데이터가 수신됩니다</span>
        </h2>
        <div className="mt-4 flex items-end gap-3">
          <label className="flex flex-1 flex-col gap-1.5 text-sm font-medium text-slate-700">
            app-name
            <input
              value={appName}
              onChange={(e) => setAppName(e.target.value)}
              placeholder="payment-api"
              className="rounded-md border border-slate-300 px-3 py-2.5 text-sm placeholder:text-slate-400 focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
            />
          </label>
          <label className="flex w-40 flex-col gap-1.5 text-sm font-medium text-slate-700">
            포트
            <input
              value={port}
              onChange={(e) => setPort(e.target.value)}
              placeholder="8081"
              inputMode="numeric"
              className="rounded-md border border-slate-300 px-3 py-2.5 text-sm placeholder:text-slate-400 focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
            />
          </label>
          <button
            type="button"
            onClick={handleAddServer}
            disabled={submitting || instanceId === null || !appName.trim() || !port.trim()}
            className="rounded-md bg-brand-500 px-5 py-2.5 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
          >
            {submitting ? '등록 중...' : '추가'}
          </button>
        </div>
        {submitError && (
          <p className="mt-3 rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
            {submitError}
          </p>
        )}

        {issuedKey && (
          <>
            <div className="mt-6 flex items-center justify-between">
              <h2 className="font-semibold text-slate-900">② 생성된 설정을 앱에 넣기</h2>
              <span className="text-xs text-slate-400">API 키는 지금 한 번만 표시됩니다</span>
            </div>
            <pre className="mt-3 overflow-x-auto rounded-md bg-slate-50 p-4 font-mono text-xs leading-relaxed text-slate-700">
{`implementation 'com.github.SW-pilot-java3:moni-spring-boot-starter:1.0.0'

moni:
  api-key: ${issuedKey.apiKey}
  server-url: https://<moni-api-domain>`}
            </pre>
            <p className="mt-3 text-xs text-slate-400">
              server-url 은 이 Moni API 서버에 접근 가능한 주소로 바꿔서 넣으세요 (로컬 개발 중이면 ngrok 등으로 터널링한 주소).
              api-key 가 유효하지 않으면 요청이 거부됩니다. 앱 이름 "{issuedKey.appName}" 은 앱 목록 화면에서 구분용으로만 표시됩니다.
            </p>
          </>
        )}
      </Card>

      <Card className="p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-semibold text-slate-900">등록된 앱 {selectedInstance ? `— ${selectedInstance.name}` : ''}</h2>
          <span className="text-xs text-slate-400">
            연결됨 {connectedCount} · 대기 {pendingCount}
          </span>
        </div>
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-left text-slate-500">
              <th className="py-2.5 font-medium">app-name</th>
              <th className="py-2.5 font-medium">포트</th>
              <th className="py-2.5 font-medium">등록일</th>
              <th className="py-2.5 font-medium">상태</th>
              <th className="py-2.5 font-medium">작업</th>
            </tr>
          </thead>
          <tbody>
            {servers.length === 0 && (
              <tr>
                <td colSpan={5} className="py-6 text-center text-slate-400">
                  등록된 앱이 없습니다.
                </td>
              </tr>
            )}
            {servers.map((app) => (
              <tr key={app.serverId} className="border-b border-slate-100">
                <td className="py-3 font-medium text-slate-800">{app.name}</td>
                <td className="py-3 text-slate-600">{app.port ?? '—'}</td>
                <td className="py-3 text-slate-600">{formatDate(app.createdAt)}</td>
                <td className="py-3">
                  <StatusDot
                    tone={app.status === 'CONNECTED' ? 'normal' : 'idle'}
                    label={app.status === 'CONNECTED' ? '연결됨' : '설정 대기 중'}
                  />
                </td>
                <td className="py-3">
                  <button
                    type="button"
                    onClick={() => handleDelete(app.serverId)}
                    className="text-danger-500 hover:underline"
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="mt-4 text-xs text-slate-400">
          유효하지 않거나 폐기된 API 키로 들어오는 요청은 401로 거부됩니다.
        </p>
      </Card>
    </div>
  )
}