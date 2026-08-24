import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts'
import Card from '../ui/Card'
import CustomChartTooltip from './CustomChartTooltip'
import { getTone, DEFAULT_THRESHOLDS } from '../../lib/thresholdUtils'
import type { HikariCpPoolMetric, ServerRealtimeSeriesPoint } from '../../lib/servers'

function shortTime(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}

export default function HikariTab({
  pools,
  series,
  totalActive,
  totalMax,
  totalIdle,
  totalPending,
}: {
  pools: HikariCpPoolMetric[]
  series: ServerRealtimeSeriesPoint[]
  totalActive: number
  totalMax: number
  totalIdle: number
  totalPending: number
}) {
  const usagePct = totalMax > 0 ? Number(((totalActive / totalMax) * 100).toFixed(1)) : 0
  const activeTone = getTone(usagePct, DEFAULT_THRESHOLDS.HIKARICP_POOL_USAGE.warn, DEFAULT_THRESHOLDS.HIKARICP_POOL_USAGE.crit)
  const pendingTone = totalPending > 0 ? 'warn' : 'default'

  const timelineData = series.map((p) => ({
    time: shortTime(p.collectedAt),
    active: p.hikaricpActiveTotal,
    max: totalMax,
  }))

  return (
    <div className="flex-1 flex flex-col min-h-0 gap-4 h-full">
      {/* 상단 4개 슬림 지표 카드 */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 shrink-0">
        <Card className="p-3">
          <div className="text-[11px] text-slate-500">Active 활성 커넥션</div>
          <div className={`mt-0.5 text-xl font-bold ${activeTone === 'danger' ? 'text-danger-600' : activeTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {totalActive} <span className="text-xs font-normal text-slate-400">/ {totalMax}</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">가용률 {usagePct}%</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">Idle 유휴 커넥션</div>
          <div className="mt-0.5 text-xl font-bold text-slate-900">
            {totalIdle} <span className="text-xs font-normal text-slate-400">개</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">대기 중인 풀 커넥션</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">Pending 대기 요청</div>
          <div className={`mt-0.5 text-xl font-bold ${pendingTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-900'}`}>
            {totalPending} <span className="text-xs font-normal text-slate-400">개</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">커넥션 획득 대기 스레드</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">최대 풀 크기 (Max)</div>
          <div className="mt-0.5 text-xl font-bold text-slate-900">
            {totalMax} <span className="text-xs font-normal text-slate-400">개</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">설정된 커넥션 상한</div>
        </Card>
      </div>

      {/* 하단 2열 스플릿 영역 (좌측 차트 7 : 우측 테이블 5) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 flex-1 min-h-0 h-full">
        {/* 좌측: 실시간 커넥션 풀 활성 추이 차트 */}
        <Card className="lg:col-span-7 p-4 flex flex-col justify-between flex-1 min-h-0 h-full shadow-sm hover:shadow-md transition-shadow">
          <h3 className="mb-2 text-sm font-semibold text-slate-800 shrink-0">
            HikariCP DB 커넥션 풀 사용 추이 <span className="text-xs font-normal text-slate-400">(Active Connections)</span>
          </h3>
          <div className="flex-1 w-full min-h-[180px] pt-1">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={timelineData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="grad-hikari" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#4a8c6f" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#4a8c6f" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} interval="preserveStartEnd" minTickGap={45} />
                <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={40} />
                <Tooltip content={<CustomChartTooltip />} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Area
                  type="monotone"
                  dataKey="active"
                  name="Active Connections"
                  stroke="#4a8c6f"
                  strokeWidth={2}
                  fill="url(#grad-hikari)"
                  dot={false}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* 우측: DB 커넥션 풀 목록 테이블 */}
        <Card className="lg:col-span-5 p-4 flex flex-col flex-1 min-h-0 h-full overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          <h3 className="mb-3 text-sm font-semibold text-slate-800 shrink-0">
            커넥션 풀 목록 <span className="text-xs font-normal text-slate-400">({pools.length}개 풀 등록됨)</span>
          </h3>
          {pools.length === 0 ? (
            <p className="text-sm text-slate-400 my-auto text-center">수집된 커넥션 풀 데이터가 없습니다.</p>
          ) : (
            <div className="flex-1 overflow-y-auto min-h-0">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs text-slate-400 sticky top-0 bg-white">
                    <th className="pb-2 font-medium">풀 명칭</th>
                    <th className="pb-2 font-medium">Active</th>
                    <th className="pb-2 font-medium">Idle</th>
                    <th className="pb-2 font-medium">Pending</th>
                    <th className="pb-2 font-medium">가용률</th>
                    <th className="pb-2 font-medium">상태</th>
                  </tr>
                </thead>
                <tbody>
                  {pools.map((p) => {
                    const poolUsage = p.max > 0 ? Number(((p.active / p.max) * 100).toFixed(1)) : 0
                    const poolTone = getTone(poolUsage, DEFAULT_THRESHOLDS.HIKARICP_POOL_USAGE.warn, DEFAULT_THRESHOLDS.HIKARICP_POOL_USAGE.crit)
                    return (
                      <tr key={p.poolName} className="border-b border-slate-100">
                        <td className="py-2.5 pr-2 font-mono text-xs font-semibold text-slate-800 truncate max-w-[120px]" title={p.poolName}>
                          {p.poolName}
                        </td>
                        <td className={`py-2.5 pr-2 whitespace-nowrap text-xs ${poolTone === 'danger' ? 'text-danger-600 font-bold' : poolTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-800'}`}>
                          {p.active} <span className="text-[10px] text-slate-400">/ {p.max}</span>
                        </td>
                        <td className="py-2.5 pr-2 whitespace-nowrap text-xs text-slate-600">{p.idle}</td>
                        <td className={`py-2.5 pr-2 whitespace-nowrap text-xs ${p.pending > 0 ? 'text-warn-600 font-bold' : 'text-slate-600'}`}>
                          {p.pending}
                        </td>
                        <td className="py-2.5 pr-2 whitespace-nowrap text-xs text-slate-700">{poolUsage}%</td>
                        <td className="py-2.5 whitespace-nowrap">
                          <span
                            className={`rounded px-1.5 py-0.5 text-[10px] font-medium ${
                              poolTone === 'danger'
                                ? 'bg-danger-50 text-danger-700'
                                : poolTone === 'warn'
                                ? 'bg-warn-50 text-warn-700'
                                : 'bg-emerald-50 text-emerald-700'
                            }`}
                          >
                            {poolTone === 'danger' ? '위험' : poolTone === 'warn' ? '주의' : '정상'}
                          </span>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}
