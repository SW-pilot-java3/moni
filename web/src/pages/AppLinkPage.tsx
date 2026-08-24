import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Card from '../components/ui/Card'
import StatusDot from '../components/ui/StatusDot'
import { ApiError } from '../lib/api'
import { getInstances, getServers, type InstanceListItem, type ServerItem } from '../lib/instances'
import { createApiKey, createServer, deleteServer } from '../lib/servers'

function formatDate(value: string | null) {
  if (!value) return '없음'
  return new Date(value).toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
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
  const [copiedKey, setCopiedKey] = useState(false)

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
      setAppName('')
      setPort('')
    } catch (err) {
      setSubmitError(err instanceof ApiError ? `${err.message} (${err.status})` : '서버에 연결할 수 없습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (serverId: number) => {
    if (!confirm('정말 이 앱을 삭제하시겠습니까? 관련 지표와 API 키가 모두 삭제됩니다.')) return
    try {
      await deleteServer(serverId)
      setServers((prev) => prev.filter((s) => s.serverId !== serverId))
      if (issuedKey?.serverId === serverId) setIssuedKey(null)
    } catch {
      // 목록은 유지하고 별도 처리 없이 무시
    }
  }

  const [configFormat, setConfigFormat] = useState<'yaml' | 'properties'>('yaml')
  const [copiedGradle, setCopiedGradle] = useState(false)
  const [copiedConfig, setCopiedConfig] = useState(false)

  const gradleSnippet = `implementation 'com.github.SW-pilot-java3:moni-spring-boot-starter:1.0.0'`

  const configSnippet = configFormat === 'yaml'
    ? `moni:
  api-key: ${issuedKey?.apiKey ?? '<발급받은_API_KEY>'}
  server-url: https://<moni-api-domain>`
    : `moni.api-key=${issuedKey?.apiKey ?? '<발급받은_API_KEY>'}
moni.server-url=https://<moni-api-domain>`

  const handleCopyGradle = async () => {
    await navigator.clipboard.writeText(gradleSnippet)
    setCopiedGradle(true)
    setTimeout(() => setCopiedGradle(false), 1500)
  }

  const handleCopyConfig = async () => {
    await navigator.clipboard.writeText(configSnippet)
    setCopiedConfig(true)
    setTimeout(() => setCopiedConfig(false), 1500)
  }

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6">
      {/* 상단 헤더 & 인스턴스 선택기 */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between border-b border-slate-200 pb-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">앱 연동 (Spring Boot)</h1>
          <p className="mt-1 text-sm text-slate-500">
            Spring Boot 애플리케이션을 등록하고 API Key를 발급받아 모니터링 스타터를 설정합니다.
          </p>
        </div>

        {/* 인스턴스 전환 셀렉터 */}
        <div className="flex items-center gap-3">
          <span className="text-xs font-semibold text-slate-500">대상 인스턴스:</span>
          <select
            value={instanceId ?? ''}
            onChange={(e) => setInstanceId(Number(e.target.value))}
            className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 shadow-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          >
            {instances.map((inst) => (
              <option key={inst.instanceId} value={inst.instanceId}>
                {inst.name} ({inst.ip})
              </option>
            ))}
          </select>
        </div>
      </div>

      {loadError && (
        <div className="rounded-lg border border-danger-500/40 bg-danger-50 p-4 text-sm text-danger-600">
          {loadError}
        </div>
      )}

      {/* 2열 반응형 그리드 레이아웃 */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* 좌측 컬럼: 새 앱 등록 폼 + 발급 키/설정 가이드 (5 cols) */}
        <div className="lg:col-span-5 space-y-6">
          {/* 앱 등록 폼 */}
          <Card className="p-6">
            <h2 className="text-base font-bold text-slate-900 mb-1">① 새 애플리케이션 등록</h2>
            <p className="text-xs text-slate-500 mb-4">
              해당 호스트 인스턴스에서 구동될 Spring Boot 앱의 이름과 포트를 입력하세요.
            </p>

            <div className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  앱 이름 (app-name)
                </label>
                <input
                  value={appName}
                  onChange={(e) => setAppName(e.target.value)}
                  placeholder="예: order-service"
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  서버 포트 (Port)
                </label>
                <input
                  value={port}
                  onChange={(e) => setPort(e.target.value)}
                  placeholder="예: 8080"
                  inputMode="numeric"
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm font-mono placeholder:font-sans placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                />
              </div>

              {submitError && (
                <div className="rounded-md border border-danger-500/40 bg-danger-50 p-3 text-xs text-danger-600">
                  {submitError}
                </div>
              )}

              <button
                type="button"
                onClick={handleAddServer}
                disabled={submitting || instanceId === null || !appName.trim() || !port.trim()}
                className="w-full rounded-md bg-brand-500 py-2.5 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors disabled:opacity-60"
              >
                {submitting ? '등록 및 키 발급 중...' : '앱 등록 및 API Key 발급'}
              </button>
            </div>
          </Card>

          {/* 발급된 API 키 및 코드 블록 가이드 */}
          <Card className={`p-6 ${issuedKey ? 'border-amber-300 bg-amber-50/30' : ''}`}>
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-base font-bold text-slate-900">
                {issuedKey ? '② 발급된 설정 적용' : '② Spring Boot 설정 가이드'}
              </h2>
              {issuedKey && (
                <span className="text-[11px] font-semibold text-amber-700 bg-amber-100 px-2 py-0.5 rounded">
                  보안 주의: 1회만 표시
                </span>
              )}
            </div>

            <p className="text-xs text-slate-500 mb-4">
              {issuedKey
                ? `[${issuedKey.appName}] 에 아래 의존성과 설정을 적용한 후 앱을 재시작하세요.`
                : '앱 등록 시 발급되는 API Key를 아래 설정 파일에 추가하여 구동하면 자동으로 메트릭이 수집됩니다.'}
            </p>

            {/* 1. 발급된 API Key (발급 시에만 강조 표시) */}
            {issuedKey && (
              <div className="mb-4 rounded-md border border-amber-200 bg-white p-3 space-y-1.5 shadow-2xs">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-700">발급된 API Key</span>
                  <button
                    type="button"
                    onClick={async () => {
                      await navigator.clipboard.writeText(issuedKey.apiKey)
                      setCopiedKey(true)
                      setTimeout(() => setCopiedKey(false), 1500)
                    }}
                    className="text-xs font-bold text-amber-700 hover:text-amber-900"
                  >
                    {copiedKey ? '복사됨!' : '키 복사'}
                  </button>
                </div>
                <code className="block break-all font-mono text-xs text-slate-900 bg-slate-50 p-2 rounded">
                  {issuedKey.apiKey}
                </code>
              </div>
            )}

            {/* 2. Gradle 의존성 (build.gradle) */}
            <div className="mb-4 space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-800">1. 의존성 추가 (build.gradle)</span>
                <button
                  type="button"
                  onClick={handleCopyGradle}
                  className="rounded bg-white border border-slate-300 px-2 py-0.5 text-[11px] font-medium text-slate-700 hover:bg-slate-50 shadow-2xs transition-colors"
                >
                  {copiedGradle ? '복사됨!' : '의존성 복사'}
                </button>
              </div>
              <pre className="overflow-x-auto rounded-md bg-slate-900 p-3 font-mono text-xs text-slate-200">
                {gradleSnippet}
              </pre>
            </div>

            {/* 3. 애플리케이션 설정 (application.yml / properties) */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-800">
                  2. 설정 파일 ({configFormat === 'yaml' ? 'application.yml' : 'application.properties'})
                </span>
                <div className="flex items-center gap-1.5">
                  <div className="flex rounded border border-slate-200 bg-slate-100 p-0.5 text-[10px]">
                    <button
                      type="button"
                      onClick={() => setConfigFormat('yaml')}
                      className={`px-1.5 py-0.5 rounded font-medium ${
                        configFormat === 'yaml' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500'
                      }`}
                    >
                      YAML
                    </button>
                    <button
                      type="button"
                      onClick={() => setConfigFormat('properties')}
                      className={`px-1.5 py-0.5 rounded font-medium ${
                        configFormat === 'properties' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-500'
                      }`}
                    >
                      Properties
                    </button>
                  </div>
                  <button
                    type="button"
                    onClick={handleCopyConfig}
                    className="rounded bg-white border border-slate-300 px-2 py-0.5 text-[11px] font-medium text-slate-700 hover:bg-slate-50 shadow-2xs transition-colors"
                  >
                    {copiedConfig ? '복사됨!' : '설정 복사'}
                  </button>
                </div>
              </div>
              <pre className="overflow-x-auto rounded-md bg-slate-900 p-3 font-mono text-xs leading-relaxed text-slate-200">
                {configSnippet}
              </pre>
            </div>

            <p className="mt-3 text-[11px] leading-relaxed text-slate-400">
              * <code className="text-slate-600 font-semibold">server-url</code>은 Moni API 서버 접속 주소(예: <code className="text-slate-600">http://192.168.1.10:8080</code> 또는 도메인)로 변경하세요.
            </p>
          </Card>
        </div>

        {/* 우측 컬럼: 등록된 앱 목록 테이블 + 상태 요약 (7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          <Card className="p-6">
            <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-slate-100 pb-3">
              <div>
                <h2 className="text-base font-bold text-slate-900">
                  {selectedInstance?.name} 소속 앱 목록
                </h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  총 {servers.length}개 등록 · 정상 수신 {connectedCount}개 · 설정 대기 {pendingCount}개
                </p>
              </div>
              <Link
                to={`/monitoring?instance=${instanceId}`}
                className="inline-flex items-center justify-center rounded-md border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 shadow-2xs transition-colors"
              >
                실시간 모니터링 ›
              </Link>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    <th className="py-2.5 px-3">app-name</th>
                    <th className="py-2.5 px-3">포트</th>
                    <th className="py-2.5 px-3">등록일</th>
                    <th className="py-2.5 px-3">연결 상태</th>
                    <th className="py-2.5 px-3 text-right">작업</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {servers.length === 0 && (
                    <tr>
                      <td colSpan={5} className="py-12 text-center text-sm text-slate-400">
                        이 인스턴스에 등록된 애플리케이션이 없습니다.
                        <br />
                        <span className="text-xs text-slate-400 mt-1 block">
                          좌측 폼에서 첫 번째 앱을 등록해 보세요.
                        </span>
                      </td>
                    </tr>
                  )}
                  {servers.map((app) => (
                    <tr key={app.serverId} className="hover:bg-slate-50/80 transition-colors">
                      <td className="py-3 px-3 font-semibold text-slate-800">{app.name}</td>
                      <td className="py-3 px-3 font-mono text-xs text-slate-600">{app.port ?? '—'}</td>
                      <td className="py-3 px-3 text-xs text-slate-500">{formatDate(app.createdAt)}</td>
                      <td className="py-3 px-3">
                        <StatusDot
                          tone={app.status === 'CONNECTED' ? 'normal' : 'idle'}
                          label={app.status === 'CONNECTED' ? '연결됨' : '설정 대기 중'}
                        />
                      </td>
                      <td className="py-3 px-3 text-right space-x-2">
                        <Link
                          to={`/thresholds/app?instance=${instanceId}&server=${app.serverId}`}
                          className="inline-block text-xs font-medium text-brand-600 hover:text-brand-800"
                        >
                          임계치
                        </Link>
                        <span className="text-slate-300">|</span>
                        <button
                          type="button"
                          onClick={() => handleDelete(app.serverId)}
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

            <div className="mt-4 pt-4 border-t border-slate-100 text-xs text-slate-400 flex items-center justify-between">
              <span>* 앱을 시작하면 10초 이내에 상태가 '연결됨'으로 변경됩니다.</span>
              <Link to="/manage" className="text-brand-600 font-medium hover:underline">
                인스턴스 설정 가기 ›
              </Link>
            </div>
          </Card>

          {/* 스타터 자동 수집 메트릭 안내 카드 */}
          <Card className="p-5 bg-slate-50/70 border-slate-200">
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wide">
              Moni Spring Boot Starter 자동 수집 지표
            </h3>
            <div className="mt-3 grid grid-cols-2 gap-3 text-xs text-slate-600">
              <div className="rounded bg-white p-2.5 border border-slate-200/80">
                <span className="font-semibold text-slate-800">JVM 메트릭</span>
                <p className="text-[11px] text-slate-500 mt-0.5">Heap / Old Gen 메모리 사용량, GC 일시정지(STW), Live 스레드</p>
              </div>
              <div className="rounded bg-white p-2.5 border border-slate-200/80">
                <span className="font-semibold text-slate-800">HTTP API 지표</span>
                <p className="text-[11px] text-slate-500 mt-0.5">엔드포인트별 RPS, 평균/최대 응답속도, 4xx/5xx 오류율</p>
              </div>
              <div className="rounded bg-white p-2.5 border border-slate-200/80">
                <span className="font-semibold text-slate-800">HikariCP 풀</span>
                <p className="text-[11px] text-slate-500 mt-0.5">Active / Idle / Pending 커넥션 수 및 연결 타임아웃</p>
              </div>
              <div className="rounded bg-white p-2.5 border border-slate-200/80">
                <span className="font-semibold text-slate-800">ThreadPool</span>
                <p className="text-[11px] text-slate-500 mt-0.5">Executor 활성 스레드 수, 대기 큐(Queue) 잔여량</p>
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}