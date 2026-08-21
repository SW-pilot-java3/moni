import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import Card from '../components/ui/Card'
import { cpuHistory, instances } from '../mock/data'

export default function HistoryPage() {
  const result = cpuHistory
  const instance = instances.find((i) => i.name === result.instanceName) ?? instances[0]

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900">과거 데이터 조회</h1>

      <Card className="mt-5 mb-2 flex flex-wrap items-center gap-3 p-4">
        <select className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800">
          {instances.map((inst) => (
            <option key={inst.id}>{inst.name}</option>
          ))}
        </select>
        <select className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600">
          <option>앱: 전체 (호스트 지표)</option>
          {instance.apps.map((app) => (
            <option key={app.name}>{app.name}</option>
          ))}
        </select>
        <div className="flex items-center gap-2 text-sm text-slate-600">
          <input type="date" defaultValue={result.rangeStart} className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
          <span className="text-slate-400">→</span>
          <input type="date" defaultValue={result.rangeEnd} className="rounded-md border border-slate-300 px-3 py-2 text-sm" />
        </div>
        <select className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-600">
          <option>{result.metricLabel}</option>
          <option>메모리 사용률</option>
          <option>디스크 사용률</option>
        </select>
        <button
          type="button"
          className="ml-auto rounded-md bg-brand-500 px-5 py-2 text-sm font-semibold text-white hover:bg-brand-600"
        >
          조회
        </button>
      </Card>
      <p className="mb-6 text-xs leading-relaxed text-slate-400">
        앱을 비워두면 인스턴스의 호스트 지표(CPU · 메모리 · 디스크 · 네트워크), 앱을 고르면 그 앱의 JVM · HikariCP · HTTP · API
        지표를 조회합니다.
      </p>

      <div className="flex gap-6">
        <Card className="flex-1 p-6">
          <h2 className="mb-6 font-semibold text-slate-900">
            {result.instanceName} · {result.metricLabel} · {result.rangeLabel}
          </h2>
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={result.points} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="grad-history" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#5b7fa6" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#5b7fa6" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="date" tick={{ fontSize: 11, fill: '#94a3b8' }} tickLine={false} />
                <YAxis tick={{ fontSize: 11, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={34} />
                <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#e2e8f0' }} />
                <Area
                  type="monotone"
                  dataKey="value"
                  stroke="#5b7fa6"
                  strokeWidth={2}
                  fill="url(#grad-history)"
                  dot={{ r: 3, fill: '#5b7fa6' }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        <div className="flex w-64 shrink-0 flex-col gap-4">
          <Card className="p-4">
            <div className="text-xs text-slate-500">기간 평균</div>
            <div className="mt-1 text-2xl font-bold text-slate-900">{result.avg}%</div>
          </Card>
          <Card className="p-4">
            <div className="text-xs text-slate-500">최댓값</div>
            <div className="mt-1 text-2xl font-bold text-warn-600">{result.max}%</div>
            <div className="mt-1 text-xs text-slate-400">{result.maxAt}</div>
          </Card>
          <Card className="p-4">
            <div className="text-xs text-slate-500">최솟값</div>
            <div className="mt-1 text-2xl font-bold text-slate-900">{result.min}%</div>
          </Card>
          <p className="text-xs leading-relaxed text-slate-400">
            통계는 조회 시점에 원본 메트릭에서 즉시 집계됩니다.
          </p>
        </div>
      </div>
    </div>
  )
}