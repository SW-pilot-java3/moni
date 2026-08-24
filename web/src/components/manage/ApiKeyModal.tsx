import { useEffect, useState } from 'react'
import StatusDot from '../ui/StatusDot'
import { ApiError } from '../../lib/api'
import {
  createApiKey,
  getApiKeyStatus,
  rotateApiKey,
  type ApiKeyStatus,
  type ServerItem,
} from '../../lib/servers'

interface ApiKeyModalProps {
  app: ServerItem | null
  instanceName?: string
  onClose: () => void
}

function formatDate(value: string | null) {
  if (!value) return '없음'
  return new Date(value).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export default function ApiKeyModal({ app, instanceName, onClose }: ApiKeyModalProps) {
  const [status, setStatus] = useState<ApiKeyStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [newKey, setNewKey] = useState<string | null>(null)
  const [copiedKey, setCopiedKey] = useState(false)
  const [confirmRotate, setConfirmRotate] = useState(false)

  useEffect(() => {
    if (!app) return
    setLoading(true)
    setError(null)
    setNewKey(null)
    setConfirmRotate(false)

    getApiKeyStatus(app.serverId)
      .then((res) => setStatus(res))
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : 'API Key 상태를 불러오지 못했습니다.')
      )
      .finally(() => setLoading(false))
  }, [app])

  if (!app) return null

  const handleCreate = async () => {
    setActionLoading(true)
    setError(null)
    try {
      const res = await createApiKey(app.serverId)
      setNewKey(res.apiKey)
      setStatus({
        hasApiKey: true,
        createdAt: res.createdAt,
        revokedAt: null,
      })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'API Key 발급에 실패했습니다.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleRotate = async () => {
    setActionLoading(true)
    setError(null)
    try {
      const res = await rotateApiKey(app.serverId)
      setNewKey(res.newApiKey)
      setStatus({
        hasApiKey: true,
        createdAt: res.createdAt,
        revokedAt: null,
      })
      setConfirmRotate(false)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'API Key 재발급에 실패했습니다.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleCopyKey = async (key: string) => {
    await navigator.clipboard.writeText(key)
    setCopiedKey(true)
    setTimeout(() => setCopiedKey(false), 1500)
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="w-full max-w-2xl rounded-xl bg-white shadow-2xl border border-slate-200 overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 모달 헤더 */}
        <div className="flex items-center justify-between border-b border-slate-200 px-6 py-4 bg-slate-50">
          <div>
            <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2.5">
              <span>API Key 관리</span>
              <span className="inline-flex items-center rounded-md bg-brand-100 px-2.5 py-0.5 text-xs font-bold text-brand-800 border border-brand-300/80 shadow-2xs">
                {app.name}
              </span>
            </h2>
            <p className="text-xs text-slate-600 mt-1 font-medium">
              소속 인스턴스: <span className="font-semibold text-slate-800">{instanceName ?? '—'}</span> (포트: <span className="font-mono font-medium text-slate-800">{app.port ?? '—'}</span>)
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-200/60 hover:text-slate-700 transition-colors"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* 모달 바디 */}
        <div className="p-6 space-y-5 max-h-[calc(85vh-120px)] overflow-y-auto">
          {error && (
            <div className="rounded-lg border border-danger-200 bg-danger-50 p-3 text-xs text-danger-700">
              {error}
            </div>
          )}

          {loading ? (
            <div className="py-8 text-center text-sm text-slate-400">
              API Key 상태를 확인하는 중...
            </div>
          ) : (
            <>
              {/* 1. 현재 API Key 상태 카드 */}
              <div className="rounded-lg border border-slate-200 bg-slate-50/50 p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-700">활성 API Key 상태</span>
                  {status?.hasApiKey ? (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 border border-emerald-200">
                      <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                      발급됨
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-semibold text-slate-600 border border-slate-200">
                      <span className="h-1.5 w-1.5 rounded-full bg-slate-400" />
                      미발급
                    </span>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-2 pt-2 text-xs border-t border-slate-100">
                  <div>
                    <span className="text-slate-400 block">발급 일시</span>
                    <span className="font-medium text-slate-800">{formatDate(status?.createdAt ?? null)}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">연결 상태</span>
                    <div className="mt-0.5">
                      <StatusDot
                        tone={app.status === 'CONNECTED' ? 'normal' : 'idle'}
                        label={app.status === 'CONNECTED' ? '연결됨' : '대기 중'}
                      />
                    </div>
                  </div>
                </div>
              </div>

              {/* 2. 새로 발급/재발급된 키 표시 (존재 시) */}
              {newKey && (
                <div className="rounded-lg border border-brand-200 bg-brand-50/50 p-4 space-y-2.5 shadow-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-brand-900 flex items-center gap-1.5">
                      <span className="h-2 w-2 rounded-full bg-brand-500 animate-pulse" />
                      발급된 API Key (보안을 위해 1회만 표시됩니다)
                    </span>
                    <button
                      type="button"
                      onClick={() => handleCopyKey(newKey)}
                      className="rounded-md bg-brand-500 px-3 py-1 text-xs font-semibold text-white hover:bg-brand-600 shadow-2xs transition-colors"
                    >
                      {copiedKey ? '✓ 복사됨!' : '키 복사'}
                    </button>
                  </div>

                  <code className="block whitespace-nowrap overflow-x-auto rounded border border-brand-200 bg-white px-3 py-2.5 font-mono text-xs text-slate-900 select-all tracking-wide shadow-2xs">
                    {newKey}
                  </code>
                </div>
              )}

              {/* 3. 키 발급 / 재발급 액션 영역 (새 키가 발급된 직후에는 중복 노출 방지) */}
              {newKey ? (
                <div className="rounded-lg border border-brand-200 bg-brand-50/70 p-3.5 text-xs text-brand-900 flex items-center gap-2">
                  <span className="h-2 w-2 rounded-full bg-brand-500 shrink-0" />
                  <span>
                    API Key가 성공적으로 발급되었습니다. 위 키를 복사하여 애플리케이션의 설정 파일에 적용해 주세요.
                  </span>
                </div>
              ) : !status?.hasApiKey ? (
                /* 아직 키가 없는 경우: 최초 발급 버튼 */
                <div className="space-y-2">
                  <p className="text-xs text-slate-500">
                    아직 발급된 API Key가 없습니다. Spring Boot 애플리케이션 연동을 위해 키를 최초 발급해 주세요.
                  </p>
                  <button
                    type="button"
                    disabled={actionLoading}
                    onClick={handleCreate}
                    className="w-full rounded-lg bg-brand-500 py-2.5 text-sm font-semibold text-white hover:bg-brand-600 shadow-sm transition-colors disabled:opacity-50"
                  >
                    {actionLoading ? '발급 처리 중...' : '+ API Key 최초 발급하기'}
                  </button>
                </div>
              ) : (
                /* 이미 키가 있는 경우: 재발급 안내 및 Rotate 버튼 */
                <div className="space-y-3">
                  <div className="rounded-lg border border-slate-200 bg-slate-50 p-3.5 space-y-1.5">
                    <span className="text-xs font-bold text-slate-800">API Key 재발급 안내</span>
                    <p className="text-xs leading-relaxed text-slate-600">
                      새 키를 재발급하면 <strong className="font-bold text-slate-800">기존 키는 즉시 만료</strong>되며, 애플리케이션의 설정 파일에 새 키를 반영한 뒤 앱을 재시작해야 모니터링 수집이 지속됩니다.
                    </p>
                  </div>

                  {!confirmRotate ? (
                    <button
                      type="button"
                      onClick={() => setConfirmRotate(true)}
                      className="w-full rounded-lg border border-slate-300 bg-white py-2 text-xs font-semibold text-slate-700 hover:bg-slate-50 hover:border-slate-400 shadow-2xs transition-colors"
                    >
                      API Key 재발급
                    </button>
                  ) : (
                    <div className="rounded-lg border border-rose-200 bg-rose-50 p-4 space-y-3">
                      <p className="text-xs font-bold text-rose-800">
                        정말 재발급하시겠습니까? 기존 키로 수집 중인 연결이 중단됩니다.
                      </p>
                      <div className="flex gap-2">
                        <button
                          type="button"
                          disabled={actionLoading}
                          onClick={handleRotate}
                          className="flex-1 rounded-md bg-rose-600 py-1.5 text-xs font-bold text-white hover:bg-rose-700 shadow-2xs transition-colors disabled:opacity-50"
                        >
                          {actionLoading ? '재발급 중...' : '확인 및 새 키 발급'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setConfirmRotate(false)}
                          className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-50"
                        >
                          취소
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
