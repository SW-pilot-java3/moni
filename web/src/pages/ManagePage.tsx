import { Fragment, useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Card from '../components/ui/Card'
import StatusDot from '../components/ui/StatusDot'
import { ApiError } from '../lib/api'
import { getInstances, getServers, type InstanceListItem, type ServerItem } from '../lib/instances'
import { createApiKey, deleteServer } from '../lib/servers'

function formatDate(value: string | null) {
  if (!value) return '없음'
  return new Date(value).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

export default function ManagePage() {
  const [searchParams] = useSearchParams()
  const instanceIdParam = searchParams.get('instance')

  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [servers, setServers] = useState<ServerItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [issuedKeys, setIssuedKeys] = useState<Record<number, string>>({})
  const [keyError, setKeyError] = useState<string | null>(null)

  useEffect(() => {
    getInstances()
      .then(setInstances)
      .catch((err) => setError(err instanceof ApiError ? err.message : '인스턴스를 불러오지 못했습니다.'))
      .finally(() => setLoading(false))
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

  const handleIssueKey = async (serverId: number) => {
    setKeyError(null)
    try {
      const res = await createApiKey(serverId)
      setIssuedKeys((prev) => ({ ...prev, [serverId]: res.apiKey }))
    } catch (err) {
      setKeyError(err instanceof ApiError ? `${err.message} (${err.status})` : '서버에 연결할 수 없습니다.')
    }
  }

  const handleDeleteServer = async (serverId: number) => {
    try {
      await deleteServer(serverId)
      setServers((prev) => prev.filter((s) => s.serverId !== serverId))
    } catch {
      // 무시하고 목록 유지
    }
  }

  if (loading) {
    return <p className="text-sm text-slate-400">불러오는 중...</p>
  }

  if (error) {
    return (
      <p className="rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">{error}</p>
    )
  }

  if (!instance) {
    return (
      <p className="text-sm text-slate-500">
        등록된 인스턴스가 없습니다.{' '}
        <Link to="/instances/new" className="text-brand-600 hover:underline">
          인스턴스를 등록해 보세요
        </Link>
        .
      </p>
    )
  }

  return (
    <div className="max-w-4xl">
      <div className="mb-1 flex items-center justify-between">
        <h1 className="flex items-center gap-3 text-xl font-bold text-slate-900">
          {instance.name}
          <span className="text-sm font-normal text-slate-400">
            {instance.ip} · ID {instance.instanceId}
          </span>
        </h1>
        <StatusDot
          tone={instance.status === 'CONNECTED' ? 'normal' : 'idle'}
          label={`호스트 수집 ${instance.status === 'CONNECTED' ? '정상' : '없음'} · ${formatDate(instance.lastReceivedAt)}`}
        />
      </div>

      <div className="mt-4 mb-6 border-b border-slate-200 text-sm font-medium">
        <span className="-mb-px inline-block border-b-2 border-slate-800 pb-3 text-slate-900">인스턴스 설정</span>
      </div>

      <p className="mb-3 text-sm text-slate-500">
        API 키는 앱(서버) 단위로 발급됩니다. 새 앱을 붙이려면{' '}
        <Link to="/apps/link" className="text-brand-600 hover:underline">
          '앱 연동'
        </Link>
        에서 먼저 등록하세요.
      </p>

      {keyError && (
        <p className="mb-3 rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
          {keyError}
        </p>
      )}

      <Card className="mb-6 p-6">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="font-semibold text-slate-900">이 인스턴스의 앱</h2>
          <span className="text-xs text-slate-400">
            등록됨 {servers.length} · 연결됨 {servers.filter((s) => s.status === 'CONNECTED').length}
          </span>
        </div>
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-left text-slate-500">
              <th className="py-2.5 font-medium">앱</th>
              <th className="py-2.5 font-medium">포트</th>
              <th className="py-2.5 font-medium">상태</th>
              <th className="py-2.5 font-medium">최근 수신</th>
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
              <Fragment key={app.serverId}>
                <tr className="border-b border-slate-100">
                  <td className="py-3 font-medium text-slate-800">{app.name}</td>
                  <td className="py-3 text-slate-600">{app.port ?? '—'}</td>
                  <td className="py-3">
                    <StatusDot
                      tone={app.status === 'CONNECTED' ? 'normal' : 'idle'}
                      label={app.status === 'CONNECTED' ? '연결됨' : '대기'}
                    />
                  </td>
                  <td className="py-3 text-slate-500">{formatDate(app.lastReceivedAt)}</td>
                  <td className="py-3">
                    <Link
                      to={`/thresholds/app?instance=${instance.instanceId}&server=${app.serverId}`}
                      className="text-brand-600 hover:underline"
                    >
                      임계치
                    </Link>
                    <span className="mx-1.5 text-slate-300">·</span>
                    <button type="button" onClick={() => handleIssueKey(app.serverId)} className="text-brand-600 hover:underline">
                      키 발급
                    </button>
                    <span className="mx-1.5 text-slate-300">·</span>
                    <button
                      type="button"
                      onClick={() => handleDeleteServer(app.serverId)}
                      className="text-danger-500 hover:underline"
                    >
                      삭제
                    </button>
                  </td>
                </tr>
                {issuedKeys[app.serverId] && (
                  <tr className="border-b border-slate-100 bg-warn-50/40">
                    <td colSpan={5} className="px-2 py-3">
                      <p className="mb-1 text-xs text-warn-600">키는 지금 한 번만 표시됩니다.</p>
                      <code className="rounded-md border border-slate-200 bg-white px-3 py-2 font-mono text-xs text-slate-700">
                        {issuedKeys[app.serverId]}
                      </code>
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      </Card>

      <Card className="flex items-center justify-between border-danger-500/40 bg-danger-50 p-6">
        <div>
          <h2 className="font-semibold text-danger-600">인스턴스 삭제</h2>
          <p className="mt-1 text-sm text-danger-600/80">
            앱 {servers.length}개와 키 · 수집 메트릭이 모두 함께 삭제됩니다. 이후 이 키로 오는 요청은 401로 거부됩니다.
          </p>
        </div>
        <button
          type="button"
          className="shrink-0 rounded-md bg-danger-500 px-4 py-2 text-sm font-semibold text-white hover:bg-danger-600"
        >
          삭제
        </button>
      </Card>
    </div>
  )
}