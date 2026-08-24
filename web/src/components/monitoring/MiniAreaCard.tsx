import { Area, AreaChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import Card from '../ui/Card'

interface StatCell {
  label: string
  value: string
  tone?: 'default' | 'warn' | 'danger'
}

export default function MiniAreaCard<T extends { time: string }>({
  title,
  stats,
  data,
  dataKey,
  color,
  onDetail,
}: {
  title: string
  stats: StatCell[]
  data: readonly T[]
  dataKey: keyof T & string
  color: string
  onDetail?: () => void
}) {
  const key = dataKey as string
  return (
    <Card className="p-5 flex flex-col justify-between h-full shadow-sm hover:shadow-md transition-shadow">
      <div>
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-sm font-semibold text-slate-800">
            {title}
          </h3>
          {onDetail && (
            <button
              type="button"
              onClick={onDetail}
              className="group flex items-center gap-1 rounded-md px-2 py-1 text-xs font-semibold text-brand-600 transition-colors hover:bg-brand-50 hover:text-brand-700"
            >
              <span>상세보기</span>
              <svg
                className="h-3.5 w-3.5 transition-transform duration-150 group-hover:translate-x-0.5"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2.5}
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
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
      </div>

      <div className="flex-1 w-full min-h-[160px] pt-1">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 4, right: 4, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id={`grad-${key}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={color} stopOpacity={0.25} />
                <stop offset="100%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis
              dataKey="time"
              tick={{ fontSize: 10, fill: '#94a3b8' }}
              axisLine={{ stroke: '#e2e8f0' }}
              tickLine={false}
              interval="preserveStartEnd"
              minTickGap={45}
            />
            <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={false} tickLine={false} width={40} />
            <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#e2e8f0' }} />
            <Area
              type="monotone"
              dataKey={key}
              stroke={color}
              strokeWidth={2}
              fill={`url(#grad-${key})`}
              dot={false}
              activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
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