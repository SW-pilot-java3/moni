import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError } from '../lib/api'
import { login, saveSession, signup } from '../lib/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const [tab, setTab] = useState<'login' | 'signup'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      if (tab === 'login') {
        const res = await login(email, password)
        saveSession(res)
        navigate('/dashboard')
      } else {
        await signup(email, password)
        setTab('login')
        setPassword('')
        setError(null)
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(`${err.message} (${err.status})`)
      } else {
        setError('서버에 연결할 수 없습니다.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 px-4">
      <div className="w-full max-w-md rounded-xl border border-slate-200 bg-white p-10 shadow-sm">
        <h1 className="mb-6 font-mono text-2xl font-bold tracking-wide text-brand-500">
          MONITORING
        </h1>

        <div className="mb-6 flex gap-6 border-b border-slate-200">
          <button
            type="button"
            onClick={() => {
              setTab('login')
              setError(null)
            }}
            className={`-mb-px border-b-2 pb-3 text-sm font-semibold ${
              tab === 'login'
                ? 'border-slate-800 text-slate-900'
                : 'border-transparent text-slate-400'
            }`}
          >
            로그인
          </button>
          <button
            type="button"
            onClick={() => {
              setTab('signup')
              setError(null)
            }}
            className={`-mb-px border-b-2 pb-3 text-sm font-semibold ${
              tab === 'signup'
                ? 'border-slate-800 text-slate-900'
                : 'border-transparent text-slate-400'
            }`}
          >
            회원가입
          </button>
        </div>

        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
            이메일
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              className="rounded-md border border-slate-300 px-3 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
            />
          </label>

          <label className="flex flex-col gap-1.5 text-sm font-medium text-slate-700">
            비밀번호
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="********"
              required
              minLength={8}
              className="rounded-md border border-slate-300 px-3 py-2.5 text-sm text-slate-800 placeholder:text-slate-400 focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
            />
          </label>

          {error && (
            <p className="rounded-md border border-danger-500/40 bg-danger-50 px-3 py-2.5 text-sm text-danger-600">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="mt-1 rounded-md bg-brand-500 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-brand-600 disabled:opacity-60"
          >
            {submitting ? '처리 중...' : tab === 'login' ? '로그인' : '회원가입'}
          </button>

          {tab === 'login' && (
            <button type="button" className="text-center text-sm text-slate-400 hover:text-slate-600">
              비밀번호 찾기
            </button>
          )}
        </form>
      </div>
    </div>
  )
}