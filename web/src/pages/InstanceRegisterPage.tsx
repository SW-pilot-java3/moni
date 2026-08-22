import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import Card from '../components/ui/Card'
import NodeExporterSetupCard from '../components/instance/NodeExporterSetupCard'
import { ApiError } from '../lib/api'
import { createInstance } from '../lib/instances'

const steps = ['① 별칭 입력', '② Node Exporter 설치', '③ 앱 등록']

export default function InstanceRegisterPage() {
  const navigate = useNavigate()
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

  return (
    <div className="max-w-4xl">
      <h1 className="text-2xl font-bold text-slate-900">인스턴스 등록</h1>
      <p className="mt-1 text-sm text-slate-500">
        모니터링할 EC2 한 대를 연결합니다. 호스트의 CPU · 메모리 · 네트워크를 수집합니다.
      </p>

      <div className="mt-6 mb-6 flex items-center gap-3 text-sm font-medium text-slate-500">
        {steps.map((step, i) => (
          <div key={step} className="flex items-center gap-3">
            <span className={(!created && i === 0) || (created && i === 1) ? 'text-slate-900' : ''}>{step}</span>
            {i < steps.length - 1 && <span className="text-slate-300">──</span>}
          </div>
        ))}
      </div>

      <Card className="mb-6 p-6">
        <h2 className="mb-4 font-semibold text-slate-900">① 인스턴스 정보</h2>
        {created ? (
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <div className="text-xs text-slate-500">별칭</div>
              <div className="mt-1 font-medium text-slate-800">{created.name}</div>
            </div>
            <div>
              <div className="text-xs text-slate-500">IP</div>
              <div className="mt-1 font-mono text-slate-800">{created.ip}</div>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-4">
              <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
                별칭
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="prod-ec2-a"
                  required
                  className="rounded-md border border-slate-300 px-3 py-2.5 text-sm placeholder:text-slate-400 focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
                />
              </label>
              <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
                IP 주소
                <input
                  value={ip}
                  onChange={(e) => setIp(e.target.value)}
                  placeholder="10.0.1.15"
                  required
                  pattern="^(\d{1,3}\.){3}\d{1,3}$"
                  className="rounded-md border border-slate-300 px-3 py-2.5 text-sm font-mono placeholder:font-sans placeholder:text-slate-400 focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
                />
              </label>
            </div>

            {error && (
              <p className="mt-3 rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
                {error}
              </p>
            )}

            <div className="mt-4 flex items-center justify-between">
              <p className="text-xs text-slate-400">별칭과 IP를 입력하면 인스턴스가 등록됩니다.</p>
              <button
                type="submit"
                disabled={submitting}
                className="rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600 disabled:opacity-60"
              >
                {submitting ? '등록 중...' : '인스턴스 등록'}
              </button>
            </div>
          </form>
        )}
      </Card>

      {created && <NodeExporterSetupCard />}

      <Card className="flex items-center justify-between p-6">
        <p className="text-sm text-slate-500">
          인스턴스 등록만으로는 앱 지표(힙 · GC · HTTP)가 수집되지 않습니다. 이어서 앱을 등록하세요.
        </p>
        {created ? (
          <Link
            to="/apps/link"
            className="shrink-0 rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-600"
          >
            앱 등록으로 이동 ›
          </Link>
        ) : (
          <button
            type="button"
            onClick={() => navigate('/dashboard')}
            className="shrink-0 rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            대시보드로
          </button>
        )}
      </Card>
    </div>
  )
}