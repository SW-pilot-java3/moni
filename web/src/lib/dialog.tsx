import { createContext, useCallback, useContext, useRef, useState } from 'react'

type DialogType = 'alert' | 'confirm'

interface DialogOptions {
  type: DialogType
  title?: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  variant?: 'default' | 'danger'
}

interface DialogState extends DialogOptions {
  resolve: (value: boolean) => void
}

interface DialogContextValue {
  alert: (message: string, options?: Omit<DialogOptions, 'type' | 'message'>) => Promise<void>
  confirm: (message: string, options?: Omit<DialogOptions, 'type' | 'message'>) => Promise<boolean>
}

const DialogContext = createContext<DialogContextValue | null>(null)

export function DialogProvider({ children }: { children: React.ReactNode }) {
  const [dialog, setDialog] = useState<DialogState | null>(null)
  const resolveRef = useRef<((value: boolean) => void) | null>(null)

  const openDialog = useCallback((options: DialogOptions): Promise<boolean> => {
    return new Promise((resolve) => {
      resolveRef.current = resolve
      setDialog({ ...options, resolve })
    })
  }, [])

  const handleClose = useCallback((value: boolean) => {
    resolveRef.current?.(value)
    setDialog(null)
  }, [])

  const alert = useCallback(
    async (message: string, options?: Omit<DialogOptions, 'type' | 'message'>) => {
      await openDialog({ type: 'alert', message, ...options })
    },
    [openDialog],
  )

  const confirm = useCallback(
    (message: string, options?: Omit<DialogOptions, 'type' | 'message'>) => {
      return openDialog({ type: 'confirm', message, ...options })
    },
    [openDialog],
  )

  return (
    <DialogContext.Provider value={{ alert, confirm }}>
      {children}
      {dialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          {/* 오버레이 */}
          <div
            className="absolute inset-0 bg-slate-900/40 backdrop-blur-sm"
            onClick={() => handleClose(false)}
          />

          {/* 모달 */}
          <div className="relative z-10 w-full max-w-sm mx-4 rounded-2xl bg-white shadow-xl ring-1 ring-slate-900/5 animate-fade-in">
            {/* 헤더 */}
            {dialog.title && (
              <div className="px-6 pt-6 pb-0">
                <h3
                  className={`text-base font-bold ${
                    dialog.variant === 'danger' ? 'text-rose-700' : 'text-slate-900'
                  }`}
                >
                  {dialog.title}
                </h3>
              </div>
            )}

            {/* 바디 */}
            <div className={`px-6 ${dialog.title ? 'pt-3 pb-6' : 'py-6'}`}>
              <p className="text-sm text-slate-600 leading-relaxed whitespace-pre-wrap">
                {dialog.message}
              </p>
            </div>

            {/* 액션 버튼 */}
            <div className="flex items-center justify-end gap-2.5 border-t border-slate-100 px-6 py-4 rounded-b-2xl bg-slate-50/60">
              {dialog.type === 'confirm' && (
                <button
                  type="button"
                  onClick={() => handleClose(false)}
                  className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors shadow-2xs"
                >
                  {dialog.cancelLabel ?? '취소'}
                </button>
              )}
              <button
                type="button"
                onClick={() => handleClose(true)}
                className={`rounded-lg px-4 py-2 text-sm font-bold text-white shadow-sm transition-colors ${
                  dialog.variant === 'danger'
                    ? 'bg-rose-600 hover:bg-rose-700'
                    : 'bg-brand-500 hover:bg-brand-600'
                }`}
              >
                {dialog.confirmLabel ?? (dialog.type === 'confirm' ? '확인' : '닫기')}
              </button>
            </div>
          </div>
        </div>
      )}
    </DialogContext.Provider>
  )
}

export function useDialog() {
  const ctx = useContext(DialogContext)
  if (!ctx) throw new Error('useDialog must be used within DialogProvider')
  return ctx
}
