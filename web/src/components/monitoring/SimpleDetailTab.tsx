import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import Card from '../ui/Card'
import CustomChartTooltip from './CustomChartTooltip'

export default function SimpleDetailTab<T extends { time: string }>({
  title,
  stats,
  data,
  dataKey,
  color,
}: {
  title: string
  stats: { label: string; value: string; tone?: 'default' | 'warn' | 'danger' }[]
  data: readonly T[]
  dataKey: keyof T & string
  color: string
}) {
  const key = dataKey as string
  return (
    <Card className="p-5">
      <h3 className="mb-4 text-sm font-semibold text-slate-800">{title}</h3>
      <div className="mb-5 grid grid-cols-4 gap-3">
        {stats.map((s) => (
          <div
            key={s.label}
            className={`rounded-md px-3 py-3 text-center ${
              s.tone === 'warn' ? 'bg-warn-50' : s.tone === 'danger' ? 'bg-danger-50' : 'bg-slate-50'
            }`}
          >
            <div className="text-xs text-slate-500">{s.label}</div>
            <div
              className={`mt-1 text-lg font-bold ${
                s.tone === 'warn' ? 'text-warn-600' : s.tone === 'danger' ? 'text-danger-600' : 'text-slate-900'
              }`}
            >
              {s.value}
            </div>
          </div>
        ))}
      </div>
      <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
            <defs>
              <linearGradient id={`grad-detail-${key}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={color} stopOpacity={0.25} />
                <stop offset="100%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} interval="preserveStartEnd" minTickGap={20} />
            <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} axisLine={false} width={40} />
            <Tooltip content={<CustomChartTooltip />} />
            <Area
              type="monotone"
              dataKey={key}
              stroke={color}
              strokeWidth={2}
              fill={`url(#grad-detail-${key})`}
              dot={false}
              connectNulls={true}
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