import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import Card from '../ui/Card'

interface StatCell {
  label: string
  value: string
  tone?: 'default' | 'warn' | 'danger'
}

export default function MiniAreaCard<T extends { time: string }>({
  icon,
  title,
  stats,
  data,
  dataKey,
  color,
  onDetail,
}: {
  icon: string
  title: string
  stats: StatCell[]
  data: readonly T[]
  dataKey: keyof T & string
  color: string
  onDetail?: () => void
}) {
  const key = dataKey as string
  return (
    <Card className="p-5">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-slate-800">
          <span>{icon}</span>
          {title}
        </h3>
        {onDetail && (
          <button type="button" onClick={onDetail} className="text-xs text-brand-600 hover:underline">
            상세보기 →
          </button>
        )}
      </div>

      <div className={`mb-4 grid gap-2`} style={{ gridTemplateColumns: `repeat(${stats.length}, minmax(0, 1fr))` }}>
        {stats.map((s) => (
          <div
            key={s.label}
            className={`rounded-md px-3 py-2.5 text-center ${
              s.tone === 'warn' ? 'bg-warn-50' : s.tone === 'danger' ? 'bg-danger-50' : 'bg-slate-50'
            }`}
          >
            <div className="text-[11px] text-slate-500">{s.label}</div>
            <div
              className={`mt-0.5 text-base font-bold ${
                s.tone === 'warn' ? 'text-warn-600' : s.tone === 'danger' ? 'text-danger-600' : 'text-slate-900'
              }`}
            >
              {s.value}
            </div>
          </div>
        ))}
      </div>

      <div className="h-40">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 4, right: 4, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id={`grad-${key}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={color} stopOpacity={0.25} />
                <stop offset="100%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={{ stroke: '#e2e8f0' }} tickLine={false} />
            <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={false} tickLine={false} width={34} />
            <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#e2e8f0' }} />
            <Area
              type="monotone"
              dataKey={key}
              stroke={color}
              strokeWidth={2}
              fill={`url(#grad-${key})`}
              dot={{ r: 3, fill: color }}
              isAnimationActive
              animationDuration={600}
              animationEasing="ease-out"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </Card>
  )
}