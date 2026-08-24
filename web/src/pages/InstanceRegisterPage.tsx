import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import Card from '../components/ui/Card'
import NodeExporterSetupCard from '../components/instance/NodeExporterSetupCard'
import { ApiError } from '../lib/api'
import { createInstance } from '../lib/instances'

export default function InstanceRegisterPage() {
  const [step, setStep] = useState<1 | 2>(1)
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
      // 등록 성공 시 2단계로 자동 전환
      setStep(2)
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
    setStep(1)
  }

  return (
    <div className="w-full max-w-4xl mx-auto space-y-6">
      {/* 1. 상단 글로벌 헤더 */}
      <div className="border-b border-slate-200 pb-4">
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">인스턴스 등록</h1>
        <p className="mt-1 text-sm text-slate-500">
          모니터링할 EC2 호스트 인스턴스를 등록하고 Node Exporter 수집 환경을 단계별로 구성합니다.
        </p>
      </div>

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

          {/* 우측: 추가 등록 및 다음 단계 액션 그룹 */}
          <div className="flex flex-wrap items-center gap-2.5 w-full sm:w-auto justify-end">
            <button
              type="button"
              onClick={handleReset}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 bg-white px-3.5 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors shadow-2xs"
            >
              <svg className="h-3.5 w-3.5 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
              </svg>
              <span>새 인스턴스 추가 등록</span>
            </button>

            <Link
              to="/apps/link"
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-brand-500 px-5 py-2.5 text-xs font-bold text-white hover:bg-brand-600 shadow-sm transition-colors"
            >
              <span>Spring Boot 앱 연동하러 가기</span>
              <svg className="h-4 w-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </Link>
          </div>
        </div>
      )}

      {/* 3. 1단계 뷰: 인스턴스 정보 입력 */}
      {step === 1 && (
        <div className="space-y-6">
          <Card className="p-6">
            <div className="mb-4">
              <h2 className="text-base font-bold text-slate-900">1. 인스턴스 정보 입력</h2>
              <p className="text-xs text-slate-500 mt-0.5">
                대시보드와 사이드바에서 식별할 인스턴스 별칭과 호스트 IP 주소를 입력하세요.
              </p>
            </div>

            {/* 이미 등록된 상태에서 1단계를 다시 볼 때 안내 배너 */}
            {created && (
              <div className="mb-4 rounded-lg border border-brand-200 bg-brand-50/50 p-3.5 flex items-center justify-between">
                <div className="text-xs text-brand-900">
                  <span className="font-bold">등록 완료된 인스턴스:</span> {created.name} ({created.ip})
                </div>
                <button
                  type="button"
                  onClick={() => setStep(2)}
                  className="text-xs font-bold text-brand-700 hover:text-brand-900 underline"
                >
                  2단계 설치로 이동 ›
                </button>
              </div>
            )}

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

              <div className="flex gap-2 pt-2">
                <button
                  type="submit"
                  disabled={submitting}
                  className="flex-1 inline-flex items-center justify-center gap-2 rounded-lg bg-brand-500 py-2.5 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors disabled:opacity-60"
                >
                  <span>{submitting ? '인스턴스 등록 중...' : '인스턴스 등록하고 2단계로 이동'}</span>
                  <svg className="h-4 w-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                  </svg>
                </button>
              </div>
            </form>
          </Card>

          {/* 사전 안내 및 수집 원리 카드 */}
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
      )}

      {/* 4. 2단계 뷰: Node Exporter 설치 스크립트 */}
      {step === 2 && (
        <div className="space-y-6">
          <NodeExporterSetupCard targetInstance={created} />
        </div>
      )}
    </div>
  )
}