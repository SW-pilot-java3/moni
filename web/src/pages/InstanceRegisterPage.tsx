import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import Card from '../components/ui/Card'
import NodeExporterSetupCard from '../components/instance/NodeExporterSetupCard'
import { ApiError } from '../lib/api'
import { createInstance } from '../lib/instances'

const steps = [
  { num: '①', label: '인스턴스 정보 입력', desc: '별칭 및 IP 주소 등록' },
  { num: '②', label: 'Node Exporter 설치', desc: 'EC2 호스트 지표 수집' },
  { num: '③', label: '앱(Spring Boot) 연동', desc: 'JVM · API 지표 연동' },
]

export default function InstanceRegisterPage() {
  const [name, setName] = useState('')
  const [ip, setIp] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [created, setCreated] = useState<{ instanceId: number; name: string; ip: string } | null>(null)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const res = await createInstance(name, ip)
      setCreated({ instanceId: res.instanceId, name: res.name, ip: res.ip })
    } catch (err) {
      setError(err instanceof ApiError ? `${err.message} (${err.status})` : '서버에 연결할 수 없습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  const handleReset = () => {
    setCreated(null)
    setName('')
    setIp('')
    setError(null)
  }

  return (
    <div className="w-full max-w-7xl mx-auto space-y-6">
      {/* 상단 헤더 */}
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">인스턴스 등록</h1>
        <p className="mt-1 text-sm text-slate-500">
          모니터링할 EC2 호스트 인스턴스를 등록하고 CPU · 메모리 · 디스크 수집 환경을 구성합니다.
        </p>
      </div>

      {/* 3단계 프로세스 바 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {steps.map((step, i) => {
          const isCurrent = (!created && i === 0) || (created && i === 1)
          const isCompleted = created && i === 0

          return (
            <div
              key={step.num}
              className={`flex items-start gap-3 rounded-lg border p-3.5 transition-colors ${
                isCurrent
                  ? 'border-brand-500 bg-brand-50/40 shadow-xs'
                  : isCompleted
                    ? 'border-emerald-300 bg-emerald-50/40'
                    : 'border-slate-200 bg-white'
              }`}
            >
              <span
                className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-xs font-bold ${
                  isCurrent
                    ? 'bg-brand-500 text-white'
                    : isCompleted
                      ? 'bg-emerald-500 text-white'
                      : 'bg-slate-100 text-slate-500'
                }`}
              >
                {isCompleted ? '✓' : step.num}
              </span>
              <div>
                <div className={`text-xs font-bold ${isCurrent ? 'text-brand-900' : 'text-slate-800'}`}>
                  {step.label}
                </div>
                <div className="text-[11px] text-slate-500 mt-0.5">{step.desc}</div>
              </div>
            </div>
          )
        })}
      </div>

      {/* 2열 반응형 그리드 */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* 좌측 컬럼: 입력 폼 & 사전 안내 (5 cols) */}
        <div className="lg:col-span-5 space-y-6">
          <Card className="p-6">
            <h2 className="text-base font-bold text-slate-900 mb-1">
              {created ? '인스턴스 등록 완료' : '① 인스턴스 정보 입력'}
            </h2>
            <p className="text-xs text-slate-500 mb-4">
              {created
                ? '인스턴스가 성공적으로 생성되었습니다. 우측의 Node Exporter 설치를 진행해 주세요.'
                : '대시보드에서 식별할 인스턴스 별칭과 IP 주소를 입력하세요.'}
            </p>

            {created ? (
              <div className="space-y-4">
                <div className="rounded-lg border border-emerald-200 bg-emerald-50/50 p-4 space-y-2.5 text-sm">
                  <div className="flex justify-between">
                    <span className="text-slate-500 text-xs">인스턴스 ID</span>
                    <span className="font-mono font-semibold text-slate-800">#{created.instanceId}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 text-xs">별칭</span>
                    <span className="font-medium text-slate-800">{created.name}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500 text-xs">IP 주소</span>
                    <span className="font-mono text-slate-800">{created.ip}</span>
                  </div>
                </div>

                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={handleReset}
                    className="flex-1 rounded-md border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 hover:bg-slate-50 transition-colors"
                  >
                    + 다른 인스턴스 등록
                  </button>
                  <Link
                    to="/apps/link"
                    className="flex-1 text-center rounded-md bg-brand-500 px-3 py-2 text-xs font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors"
                  >
                    앱 등록으로 이동 ›
                  </Link>
                </div>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    인스턴스 별칭 (Name)
                  </label>
                  <input
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="예: Production-AP-East"
                    required
                    className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                  />
                  <span className="mt-1 block text-[11px] text-slate-400">
                    대시보드와 사이드바에 표시될 고유한 이름입니다.
                  </span>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">
                    IP 주소 (IPv4)
                  </label>
                  <input
                    value={ip}
                    onChange={(e) => setIp(e.target.value)}
                    placeholder="예: 192.168.1.10"
                    required
                    pattern="^(\d{1,3}\.){3}\d{1,3}$"
                    className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm font-mono placeholder:font-sans placeholder:text-slate-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
                  />
                  <span className="mt-1 block text-[11px] text-slate-400">
                    호스트 EC2의 내부 또는 외부 IP 주소입니다.
                  </span>
                </div>

                {error && (
                  <div className="rounded-md border border-danger-500/40 bg-danger-50 p-3 text-xs text-danger-600">
                    {error}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={submitting}
                  className="w-full rounded-md bg-brand-500 py-2.5 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors disabled:opacity-60"
                >
                  {submitting ? '인스턴스 등록 중...' : '인스턴스 등록하기'}
                </button>
              </form>
            )}
          </Card>

          {/* 사전 안내 및 보안 가이드 카드 */}
          <Card className="p-5 bg-slate-50/70 border-slate-200">
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wide">
              호스트 메트릭 수집 원리
            </h3>
            <ul className="mt-2.5 space-y-2 text-xs leading-relaxed text-slate-600">
              <li className="flex items-start gap-1.5">
                <span className="text-brand-500 font-bold">•</span>
                <span>
                  <strong>Node Exporter</strong>는 EC2 호스트의 CPU, Memory, Disk, Network I/O를 측정하여 로컬 포트(<code className="bg-white px-1 py-0.5 rounded border border-slate-200 text-slate-700">9100</code>)로 노출합니다.
                </span>
              </li>
              <li className="flex items-start gap-1.5">
                <span className="text-brand-500 font-bold">•</span>
                <span>
                  <strong>안전한 로컬 바인딩</strong>: 127.0.0.1:9100에 바인딩되므로 외부 보안그룹(Security Group)을 열지 않아도 안전하게 동작합니다.
                </span>
              </li>
            </ul>
          </Card>
        </div>

        {/* 우측 컬럼: Node Exporter 설치 스크립트 가이드 (7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          <NodeExporterSetupCard />

          <Card className="p-5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-brand-50/40 border-brand-200">
            <div>
              <h3 className="text-sm font-bold text-brand-900">다음 단계: Spring Boot 앱 연동</h3>
              <p className="text-xs text-brand-700 mt-0.5">
                인스턴스 등록 후 Spring Boot 애플리케이션을 연동하여 JVM 및 HTTP API 모니터링을 시작하세요.
              </p>
            </div>
            <Link
              to="/apps/link"
              className="inline-flex shrink-0 items-center justify-center rounded-md bg-brand-500 px-4 py-2 text-xs font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors"
            >
              앱 연동으로 이동 ›
            </Link>
          </Card>
        </div>
      </div>
    </div>
  )
}