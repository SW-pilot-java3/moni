export type Tone = 'normal' | 'warning' | 'critical' | 'idle' | 'muted'

const toneClasses: Record<Tone, string> = {
  normal: 'bg-emerald-500 shadow-sm shadow-emerald-500/50',
  warning: 'bg-amber-500 shadow-sm shadow-amber-500/50 animate-pulse',
  critical: 'bg-rose-500 shadow-sm shadow-rose-500/50',
  idle: 'bg-slate-300',
  muted: 'bg-slate-300',
}

const toneTextClasses: Record<Tone, string> = {
  normal: 'text-emerald-700 font-medium',
  warning: 'text-amber-700 font-medium',
  critical: 'text-rose-700 font-medium',
  idle: 'text-slate-500',
  muted: 'text-slate-400',
}

export type StreamStatus = 'syncing' | 'connecting' | 'connected' | 'disconnected'

export const STREAM_STATUS_CONFIG: Record<StreamStatus, { tone: Tone; label: string }> = {
  syncing: { tone: 'idle', label: '데이터 동기화 중...' },
  connecting: { tone: 'warning', label: '연결 시도 중...' },
  connected: { tone: 'normal', label: '연결됨 (실시간)' },
  disconnected: { tone: 'critical', label: '연결 끊김' },
}

export default function StatusDot({ tone, label }: { tone: Tone; label: string }) {
  return (
    <span className={`inline-flex items-center gap-1.5 text-xs ${toneTextClasses[tone]}`}>
      <span className={`h-2 w-2 shrink-0 rounded-full ${toneClasses[tone]}`} />
      {label}
    </span>
  )
}