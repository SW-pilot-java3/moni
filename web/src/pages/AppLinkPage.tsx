import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import Card from '../components/ui/Card'
import { ApiError } from '../lib/api'
import { getInstances, type InstanceListItem } from '../lib/instances'
import { createApiKey, createServer } from '../lib/servers'

function CopyableSnippet({ code, label }: { code: string; label?: string }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <div className="relative group mt-1.5">
      <button
        type="button"
        onClick={handleCopy}
        className="absolute top-2.5 right-2.5 z-10 inline-flex items-center gap-1.5 rounded-md bg-slate-800/90 hover:bg-slate-700 border border-slate-700 px-2.5 py-1 text-xs font-semibold text-white shadow-2xs transition-all"
        title="복사"
      >
        {copied ? (
          <>
            <svg className="h-3.5 w-3.5 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
            <span className="text-emerald-300">복사됨</span>
          </>
        ) : (
          <>
            <svg className="h-3.5 w-3.5 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
            <span>{label || '복사'}</span>
          </>
        )}
      </button>
      <pre className="overflow-x-auto rounded-lg bg-slate-900 p-3.5 pr-20 font-mono text-xs leading-relaxed whitespace-pre text-slate-200 shadow-inner border border-slate-800">
        {code}
      </pre>
    </div>
  )
}

export default function AppLinkPage() {
  const [searchParams] = useSearchParams()
  const instanceParam = searchParams.get('instance')

  const [step, setStep] = useState<1 | 2>(1)
  const [instances, setInstances] = useState<InstanceListItem[]>([])
  const [instanceId, setInstanceId] = useState<number | null>(instanceParam ? Number(instanceParam) : null)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [appName, setAppName] = useState('')
  const [port, setPort] = useState('')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // 발급된 앱 & API Key 정보
  const [issuedKey, setIssuedKey] = useState<{
    serverId: number
    apiKey: string
    appName: string
    port: number
    instanceName: string
  } | null>(null)
  const [copiedKey, setCopiedKey] = useState(false)
  const [configFormat, setConfigFormat] = useState<'yaml' | 'properties'>('yaml')

  useEffect(() => {
    getInstances()
      .then((res) => {
        setInstances(res)
        if (res.length > 0 && !instanceParam) {
          setInstanceId(res[0].instanceId)
        }
      })
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : '인스턴스 목록을 불러오지 못했습니다.'))
  }, [instanceParam])

  const selectedInstance = instances.find((i) => i.instanceId === instanceId)

  const handleAddServer = async (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (instanceId === null || instances.length === 0) {
      setSubmitError('연동할 대상 인스턴스를 선택해주세요.')
      return
    }
    if (!appName.trim()) {
      setSubmitError('애플리케이션 이름을 입력해주세요.')
      return
    }
    if (!port.trim()) {
      setSubmitError('서버 포트 번호를 입력해주세요.')
      return
    }
    const portNum = Number(port)
    if (isNaN(portNum) || portNum < 1 || portNum > 65535) {
      setSubmitError('포트 번호는 1에서 65535 사이의 숫자여야 합니다.')
      return
    }

    setSubmitError(null)
    setSubmitting(true)
    try {
      const server = await createServer(instanceId, appName.trim(), portNum)
      const key = await createApiKey(server.serverId)
      setIssuedKey({
        serverId: server.serverId,
        apiKey: key.apiKey,
        appName: server.name,
        port: portNum,
        instanceName: selectedInstance?.name || '선택된 인스턴스',
      })
      // 등록 완료 후 2단계로 전환
      setStep(2)
    } catch (err) {
      setSubmitError(err instanceof ApiError ? `${err.message} (${err.status})` : '서버에 연결할 수 없습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleReset = () => {
    setIssuedKey(null)
    setAppName('')
    setPort('')
    setSubmitError(null)
    setStep(1)
  }

  const gradleSnippet = `implementation 'com.github.SW-pilot-java3:moni-spring-boot-starter:1.0.0'`

  const configSnippet =
    configFormat === 'yaml'
      ? `moni:
  api-key: ${issuedKey?.apiKey ?? '<발급받은_API_KEY>'}
  server-url: https://<moni-api-domain>`
      : `moni.api-key=${issuedKey?.apiKey ?? '<발급받은_API_KEY>'}
moni.server-url=https://<moni-api-domain>`

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6">
      {/* 1. 상단 글로벌 헤더 */}
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">앱 연동 (Spring Boot)</h1>
        <p className="mt-1 text-sm text-slate-500">
          Spring Boot 애플리케이션을 등록하고 API Key를 발급받아 모니터링 스타터를 단계별로 설정합니다.
        </p>
      </div>

      {loadError && (
        <div className="rounded-lg border border-danger-500/40 bg-danger-50 p-4 text-sm text-danger-600">
          {loadError}
        </div>
      )}

      {/* 2. 2단계 상단 액션 내비게이션 바 */}
      {step === 2 && (
        <div className="flex flex-col sm:flex-row items-center justify-between gap-3">
          {/* 좌측: 이전 단계 복귀 */}
          <div>
            <button
              type="button"
              onClick={() => setStep(1)}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors shadow-2xs"
            >
              <svg className="h-4 w-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
              <span>1단계로 돌아가기</span>
            </button>
          </div>

          {/* 우측: 추가 연동 및 인프라 관리 이동 액션 그룹 */}
          <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto justify-end">
            <button
              type="button"
              onClick={handleReset}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors shadow-2xs"
            >
              <svg className="h-3.5 w-3.5 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
              </svg>
              <span>새 앱 추가 연동</span>
            </button>

            <Link
              to="/dashboard"
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-brand-500 px-5 py-2.5 text-xs font-bold text-white hover:bg-brand-600 shadow-sm transition-colors"
            >
              <span>인프라 관리로 이동</span>
              <svg className="h-4 w-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </Link>
          </div>
        </div>
      )}

      {/* 3. 1단계 뷰: 애플리케이션 정보 입력 */}
      {step === 1 && (
        <div className="space-y-6">
          <Card className="p-6">
            <div className="mb-4">
              <h2 className="text-base font-bold text-slate-900">1. 애플리케이션 정보 입력</h2>
              <p className="text-xs text-slate-500 mt-0.5">
                해당 호스트 인스턴스에서 구동될 Spring Boot 앱의 이름과 포트를 입력하세요.
              </p>
            </div>

            {/* 이미 등록된 상태에서 1단계를 다시 볼 때 안내 배너 */}
            {issuedKey && (
              <div className="mb-4 rounded-lg border border-brand-200 bg-brand-50/50 p-3.5 flex items-center justify-between">
                <div className="text-xs text-brand-900">
                  <span className="font-bold">발급 완료된 앱:</span> {issuedKey.appName} (포트: {issuedKey.port})
                </div>
                <button
                  type="button"
                  onClick={() => setStep(2)}
                  className="text-xs font-bold text-brand-700 hover:text-brand-900 underline"
                >
                  2단계 설정 가이드로 이동 ›
                </button>
              </div>
            )}

            <form onSubmit={handleAddServer} className="space-y-4">
              {/* 대상 인스턴스 선택 */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  대상 호스트 인스턴스
                </label>
                <select
                  value={instanceId ?? ''}
                  onChange={(e) => setInstanceId(Number(e.target.value))}
                  className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 shadow-2xs focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                >
                  {instances.map((inst) => (
                    <option key={inst.instanceId} value={inst.instanceId}>
                      {inst.name} ({inst.ip})
                    </option>
                  ))}
                </select>
                <span className="mt-1 block text-[11px] text-slate-400">
                  애플리케이션이 배포되어 실행 중인 서버 인스턴스를 선택하세요.
                </span>
              </div>

              {/* 앱 이름 */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  애플리케이션 이름 (app-name)
                </label>
                <input
                  value={appName}
                  onChange={(e) => setAppName(e.target.value)}
                  placeholder="예: order-service"
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                />
                <span className="mt-1 block text-[11px] text-slate-400">
                  대시보드에서 식별할 고유한 서비스 이름입니다.
                </span>
              </div>

              {/* 서버 포트 */}
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
                <span className="mt-1 block text-[11px] text-slate-400">
                  Spring Boot 내장 톰캣 서버 포트 번호입니다.
                </span>
              </div>

              {submitError && (
                <div className="rounded-md border border-danger-500/40 bg-danger-50 p-3 text-xs text-danger-600">
                  {submitError}
                </div>
              )}

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full inline-flex items-center justify-center gap-2 rounded-lg bg-brand-500 py-2.5 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors disabled:opacity-60"
                >
                  <span>{submitting ? '등록 및 키 발급 중...' : '앱 등록하고 API Key 발급받기'}</span>
                  <svg className="h-4 w-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>
            </form>
          </Card>

          {/* 스타터 자동 수집 지표 안내 카드 */}
          <Card className="p-5 bg-slate-50/70 border-slate-200">
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wide">
              Moni Spring Boot Starter 자동 수집 지표
            </h3>
            <div className="mt-3 grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs text-slate-600">
              <div className="rounded bg-white p-3 border border-slate-200/80 shadow-2xs">
                <span className="font-bold text-slate-900">JVM 메트릭</span>
                <p className="text-[11px] text-slate-500 mt-0.5">Heap / Old Gen 메모리 사용량, GC 일시정지(STW), Live 스레드</p>
              </div>
              <div className="rounded bg-white p-3 border border-slate-200/80 shadow-2xs">
                <span className="font-bold text-slate-900">HTTP API 지표</span>
                <p className="text-[11px] text-slate-500 mt-0.5">엔드포인트별 RPS, 평균/최대 응답속도, 4xx/5xx 오류율</p>
              </div>
              <div className="rounded bg-white p-3 border border-slate-200/80 shadow-2xs">
                <span className="font-bold text-slate-900">HikariCP 커넥션 풀</span>
                <p className="text-[11px] text-slate-500 mt-0.5">Active / Idle / Pending 커넥션 수 및 연결 타임아웃</p>
              </div>
              <div className="rounded bg-white p-3 border border-slate-200/80 shadow-2xs">
                <span className="font-bold text-slate-900">ThreadPool</span>
                <p className="text-[11px] text-slate-500 mt-0.5">Executor 활성 스레드 수, 대기 큐(Queue) 잔여량</p>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* 4. 2단계 뷰: 스타터 의존성 & 설정 적용 가이드 */}
      {step === 2 && issuedKey && (
        <div className="space-y-6">
          <Card className="p-6">
            {/* 카드 헤더 & 대상 앱 뱃지 */}
            <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 pb-3 border-b border-slate-100">
              <div>
                <h2 className="text-base font-bold text-slate-900">2. Spring Boot 설정 및 모니터링 연동</h2>
                <p className="text-xs text-slate-500 mt-0.5">
                  아래 발급된 API Key와 스타터 설정을 프로젝트에 적용한 뒤 애플리케이션을 시작하세요.
                </p>
              </div>

              <div className="inline-flex items-center gap-1.5 rounded-lg bg-brand-50 border border-brand-200 px-3 py-1 text-xs text-brand-900 self-start sm:self-auto">
                <span className="font-semibold text-brand-700">대상 앱:</span>
                <span className="font-bold text-slate-900">{issuedKey.appName}</span>
                <span className="font-mono text-brand-800 font-medium">(포트: {issuedKey.port})</span>
                <span className="text-brand-300">|</span>
                <span className="text-slate-600">{issuedKey.instanceName}</span>
              </div>
            </div>

            {/* 1. 발급된 API Key (한 줄 표시 + 복사 버튼, Moni Blue 테마) */}
            <div className="mb-5 rounded-lg border border-brand-200 bg-brand-50/50 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-slate-900">발급된 API Key</span>
                  <span className="rounded bg-brand-200 px-1.5 py-0.5 text-[10px] font-bold text-brand-900">
                    1회만 표시
                  </span>
                </div>
                <button
                  type="button"
                  onClick={async () => {
                    await navigator.clipboard.writeText(issuedKey.apiKey)
                    setCopiedKey(true)
                    setTimeout(() => setCopiedKey(false), 1500)
                  }}
                  className="inline-flex items-center gap-1.5 rounded-md bg-brand-500 hover:bg-brand-600 px-2.5 py-1 text-xs font-semibold text-white shadow-2xs transition-colors"
                >
                  {copiedKey ? (
                    <>
                      <svg className="h-3.5 w-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                      <span>복사됨</span>
                    </>
                  ) : (
                    <>
                      <svg className="h-3.5 w-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                      </svg>
                      <span>키 복사</span>
                    </>
                  )}
                </button>
              </div>
              <div className="rounded-md border border-slate-200 bg-white p-2.5">
                <code className="block font-mono text-xs text-slate-900 break-all whitespace-nowrap overflow-x-auto select-all">
                  {issuedKey.apiKey}
                </code>
              </div>
              <p className="text-[11px] text-slate-500">
                * 보안을 위해 생성 직후 1회만 표시됩니다. 분실 시 <strong className="text-slate-700">인프라 관리</strong>에서 언제든 재발급할 수 있습니다.
              </p>
            </div>

            {/* 2. Gradle 의존성 (build.gradle) */}
            <div className="mb-5 space-y-1.5">
              <span className="text-xs font-bold text-slate-800">1. 의존성 추가 (build.gradle)</span>
              <CopyableSnippet code={gradleSnippet} label="의존성 복사" />
            </div>

            {/* 3. 애플리케이션 설정 (application.yml / properties) */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-slate-800">
                  2. 설정 파일 ({configFormat === 'yaml' ? 'application.yml' : 'application.properties'})
                </span>
                <div className="flex rounded-md border border-slate-200 bg-slate-100 p-0.5 text-xs">
                  <button
                    type="button"
                    onClick={() => setConfigFormat('yaml')}
                    className={`rounded px-2 py-0.5 font-semibold transition-colors ${
                      configFormat === 'yaml' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600'
                    }`}
                  >
                    YAML
                  </button>
                  <button
                    type="button"
                    onClick={() => setConfigFormat('properties')}
                    className={`rounded px-2 py-0.5 font-semibold transition-colors ${
                      configFormat === 'properties' ? 'bg-white text-slate-900 shadow-2xs' : 'text-slate-600'
                    }`}
                  >
                    Properties
                  </button>
                </div>
              </div>
              <CopyableSnippet code={configSnippet} label="설정 복사" />
            </div>

            <p className="mt-3 text-[11px] leading-relaxed text-slate-400">
              * <code className="text-slate-600 font-semibold font-mono">server-url</code>은 Moni API 서버 접속 주소(예: <code className="text-slate-600 font-mono">http://192.168.1.10:8080</code> 또는 도메인)로 변경하세요.
            </p>
          </Card>
        </div>
      )}
    </div>
  )
}