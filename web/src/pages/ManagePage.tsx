import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ApiKeyModal from '../components/manage/ApiKeyModal'
import Card from '../components/ui/Card'
import StatusDot from '../components/ui/StatusDot'
import { ApiError } from '../lib/api'
import { getInstances, getServers, type InstanceListItem, type ServerItem } from '../lib/instances'
import { deleteServer } from '../lib/servers'

function formatDate(value: string | null) {
  if (!value) return '없음'
  return new Date(value).toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function ManagePage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const instanceIdParam = searchParams.get('instance')

  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [servers, setServers] = useState<ServerItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedAppForKeys, setSelectedAppForKeys] = useState<ServerItem | null>(null)

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

  const handleDeleteServer = async (serverId: number) => {
    if (!confirm('정말 이 앱을 삭제하시겠습니까? 관련 수집 데이터와 API 키가 모두 삭제됩니다.')) return
    try {
      await deleteServer(serverId)
      setServers((prev) => prev.filter((s) => s.serverId !== serverId))
    } catch {
      // 무시하고 목록 유지
    }
  }

  if (loading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <p className="text-sm text-slate-400">인스턴스 및 앱 정보를 불러오는 중...</p>
      </div>
    )
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
      <Card className="p-8 text-center">
        <h2 className="text-lg font-semibold text-slate-800">등록된 인스턴스가 없습니다</h2>
        <p className="mt-2 text-sm text-slate-500">모니터링을 시작하려면 먼저 인스턴스를 등록해 보세요.</p>
        <Link
          to="/instances/new"
          className="mt-4 inline-block rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm"
        >
          인스턴스 등록하기
        </Link>
      </Card>
    )
  }

  const connectedServersCount = servers.filter((s) => s.status === 'CONNECTED').length

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6">
      {/* 상단 헤더 & 인스턴스 셀렉터 바 */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">상세 관리</h1>
          <p className="mt-1 text-sm text-slate-500">
            호스트 인스턴스 정보와 소속된 애플리케이션(서버) 및 API Key를 통합 관리합니다.
          </p>
        </div>

        {/* 인스턴스 전환 셀렉터 */}
        <div className="flex items-center gap-2">
          <select
            value={instance.instanceId}
            onChange={(e) => setSearchParams({ instance: e.target.value })}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            {instances.map((i) => (
              <option key={i.instanceId} value={i.instanceId}>
                {i.name} ({i.ip})
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* 2열 반응형 그리드 레이아웃 */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* 좌측 컬럼: 인스턴스 정보 요약 + 위험 구역 (4 cols) */}
        <div className="lg:col-span-4 space-y-6">
          {/* 인스턴스 정보 카드 */}
          <Card className="p-5">
            <div className="flex items-center justify-between border-b border-slate-100 pb-3 mb-4">
              <h2 className="font-bold text-slate-900">인스턴스 정보</h2>
              <StatusDot
                tone={instance.status === 'CONNECTED' ? 'normal' : 'idle'}
                label={instance.status === 'CONNECTED' ? '수집 정상' : '수집 대기'}
              />
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">인스턴스 별칭</span>
                <span className="font-semibold text-slate-800">{instance.name}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">IP 주소</span>
                <span className="font-mono text-slate-800">{instance.ip}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">인스턴스 ID</span>
                <span className="font-mono text-slate-600">#{instance.instanceId}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-50">
                <span className="text-slate-500">등록된 앱 수</span>
                <span className="font-medium text-slate-800">{servers.length}개</span>
              </div>
              <div className="flex justify-between py-1">
                <span className="text-slate-500">최근 호스트 수신</span>
                <span className="text-xs text-slate-600">{formatDate(instance.lastReceivedAt)}</span>
              </div>
            </div>

            <div className="mt-5 pt-4 border-t border-slate-100 flex flex-col gap-2">
              <Link
                to={`/monitoring?instance=${instance.instanceId}`}
                className="w-full text-center rounded-md bg-brand-50 px-3 py-2 text-xs font-semibold text-brand-700 hover:bg-brand-100 transition-colors"
              >
                실시간 모니터링 바로가기 ›
              </Link>
            </div>
          </Card>

          {/* 위험 구역 카드 (인스턴스 삭제) */}
          <Card className="border-danger-200 bg-danger-50/40 p-5">
            <h2 className="font-bold text-danger-700">인스턴스 삭제 (Danger Zone)</h2>
            <p className="mt-2 text-xs leading-relaxed text-danger-600/90">
              인스턴스를 삭제하면 소속된 앱 {servers.length}개와 모든 발급 키 및 시계열 메트릭이 함께 영구 삭제됩니다.
            </p>
            <button
              type="button"
              onClick={() => alert('인스턴스 삭제는 안전을 위해 하위 앱을 모두 삭제한 뒤 진행해주세요.')}
              className="mt-4 w-full rounded-md border border-danger-300 bg-white px-3 py-2 text-xs font-semibold text-danger-600 hover:bg-danger-100 transition-colors"
            >
              인스턴스 삭제
            </button>
          </Card>
        </div>

        {/* 우측 컬럼: 소속된 앱 목록 및 API 키 발급 (8 cols) */}
        <div className="lg:col-span-8 space-y-6">
          <Card className="p-6">
            <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-slate-100 pb-3">
              <div>
                <h2 className="text-base font-bold text-slate-900">
                  {instance.name} 소속 애플리케이션
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  총 {servers.length}개 앱 중 {connectedServersCount}개 정상 연결됨
                </p>
              </div>
              <Link
                to={`/apps/link?instance=${instance.instanceId}`}
                className="inline-flex items-center justify-center rounded-md bg-brand-500 px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors"
              >
                + 새 앱 연동하기
              </Link>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    <th className="py-2.5 px-3">앱 이름</th>
                    <th className="py-2.5 px-3">포트</th>
                    <th className="py-2.5 px-3">연결 상태</th>
                    <th className="py-2.5 px-3">최근 수신</th>
                    <th className="py-2.5 px-3 text-right">작업</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {servers.length === 0 && (
                    <tr>
                      <td colSpan={5} className="py-10 text-center text-sm text-slate-400">
                        이 인스턴스에 등록된 애플리케이션이 없습니다.{' '}
                        <Link to="/apps/link" className="text-brand-600 font-medium hover:underline">
                          새 앱을 연동해 보세요
                        </Link>
                        .
                      </td>
                    </tr>
                  )}
                  {servers.map((app) => (
                    <tr key={app.serverId} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 px-3 font-semibold text-slate-800">{app.name}</td>
                      <td className="py-3 px-3 font-mono text-xs text-slate-600">{app.port ?? '—'}</td>
                      <td className="py-3 px-3">
                        <StatusDot
                          tone={app.status === 'CONNECTED' ? 'normal' : 'idle'}
                          label={app.status === 'CONNECTED' ? '연결됨' : '대기 중'}
                        />
                      </td>
                      <td className="py-3 px-3 text-xs text-slate-500">{formatDate(app.lastReceivedAt)}</td>
                      <td className="py-3 px-3 text-right space-x-2">
                        <button
                          type="button"
                          onClick={() => setSelectedAppForKeys(app)}
                          className="text-xs font-medium text-brand-600 hover:text-brand-800 font-semibold"
                        >
                          키 관리
                        </button>
                        <span className="text-slate-300">|</span>
                        <button
                          type="button"
                          onClick={() => handleDeleteServer(app.serverId)}
                          className="text-xs font-medium text-danger-500 hover:text-danger-700"
                        >
                          삭제
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-4 pt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
              <span>* API 키는 애플리케이션 단위로 발급되며, Spring Boot 모니터링 스타터 설정에 사용됩니다.</span>
              <Link to="/apps/link" className="text-brand-600 font-medium hover:underline">
                연동 가이드 보기 ›
              </Link>
            </div>
          </Card>
        </div>
      </div>

      {/* API Key 관리 및 재발급 모달 */}
      <ApiKeyModal
        app={selectedAppForKeys}
        instanceName={instance.name}
        onClose={() => setSelectedAppForKeys(null)}
      />
    </div>
  )
}