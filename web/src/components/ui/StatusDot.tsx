type Tone = 'normal' | 'warning' | 'critical' | 'idle' | 'muted'

const toneClasses: Record<Tone, string> = {
  normal: 'bg-brand-500',
  warning: 'bg-warn-500',
  critical: 'bg-danger-500',
  idle: 'border border-slate-300 bg-white',
  muted: 'bg-slate-300',
}

const toneTextClasses: Record<Tone, string> = {
  normal: 'text-brand-600',
  warning: 'text-warn-600',
  critical: 'text-danger-600',
  idle: 'text-slate-400',
  muted: 'text-slate-400',
}

export default function StatusDot({ tone, label }: { tone: Tone; label: string }) {
  return (
    <span className={`inline-flex items-center gap-1.5 text-sm ${toneTextClasses[tone]}`}>
      <span className={`h-2 w-2 shrink-0 rounded-full ${toneClasses[tone]}`} />
      {label}
    </span>
  )
}