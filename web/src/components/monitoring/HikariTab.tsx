import { useState } from 'react'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
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
  const [selectedPoolName, setSelectedPoolName] = useState<string | null>(pools[0]?.poolName ?? null)
  const [chartMetric, setChartMetric] = useState<'usage' | 'active'>('usage')
  const selected = pools.find((p) => p.poolName === selectedPoolName) ?? pools[0]

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
          <div className="mt-0.5 text-[10px] text-slate-400">풀 사용률 {usagePct}%</div>
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

        {/* 우측: DB 커넥션 풀 비교 바 차트 & 목록 통합 카드 */}
        <Card className="lg:col-span-5 p-4 flex flex-col flex-1 min-h-0 h-full overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          {/* 1. 상단: 타이틀 & 지표 선택 세그먼트 */}
          <div className="mb-2 flex items-center justify-between shrink-0">
            <div className="flex items-center gap-1.5">
              <h3 className="text-sm font-semibold text-slate-800">
                커넥션 풀별 부하 비교
              </h3>
              <span className="text-xs text-slate-400 font-normal">
                ({pools.length}개 풀)
              </span>
            </div>

            {/* 사용률 / 활성 커넥션 토글 */}
            <div className="flex rounded-md border border-slate-200 bg-slate-100 p-0.5 text-[11px] font-semibold">
              <button
                type="button"
                onClick={() => setChartMetric('usage')}
                className={`rounded px-2 py-0.5 transition-all ${
                  chartMetric === 'usage'
                    ? 'bg-white text-emerald-700 shadow-2xs font-bold'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                사용률(%)
              </button>
              <button
                type="button"
                onClick={() => setChartMetric('active')}
                className={`rounded px-2 py-0.5 transition-all ${
                  chartMetric === 'active'
                    ? 'bg-white text-brand-700 shadow-2xs font-bold'
                    : 'text-slate-500 hover:text-slate-800'
                }`}
              >
                활성 수
              </button>
            </div>
          </div>

          {pools.length === 0 ? (
            <p className="text-sm text-slate-400 my-auto text-center">수집된 커넥션 풀 데이터가 없습니다.</p>
          ) : (
            <>
              {/* 2. 슬림 & 세련된 풀별 세로 막대그래프 */}
              <div className="h-28 w-full shrink-0 pt-1 pb-1">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={pools.map((p) => {
                      const poolUsage = p.max > 0 ? Number(((p.active / p.max) * 100).toFixed(1)) : 0
                      const shortName = p.poolName.length > 14 ? p.poolName.slice(0, 12) + '…' : p.poolName
                      return {
                        name: p.poolName,
                        label: shortName,
                        poolName: p.poolName,
                        active: p.active,
                        max: p.max,
                        usagePct: poolUsage,
                      }
                    })}
                    margin={{ top: 4, right: 8, left: -15, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                    <XAxis
                      dataKey="label"
                      tick={{ fontSize: 9, fill: '#94a3b8' }}
                      tickLine={false}
                      axisLine={{ stroke: '#e2e8f0' }}
                      interval={0}
                    />
                    <YAxis
                      tick={{ fontSize: 9, fill: '#94a3b8' }}
                      tickLine={false}
                      axisLine={false}
                      width={35}
                    />
                    <Tooltip
                      content={
                        <CustomChartTooltip
                          unit={chartMetric === 'usage' ? '%' : '개'}
                        />
                      }
                    />
                    <Bar
                      dataKey={chartMetric === 'usage' ? 'usagePct' : 'active'}
                      name={chartMetric === 'usage' ? '풀 사용률' : '활성 커넥션'}
                      barSize={18}
                      radius={[3, 3, 0, 0]}
                      onClick={(data: any) => {
                        if (data && data.poolName) setSelectedPoolName(data.poolName)
                      }}
                      className="cursor-pointer"
                    >
                      {pools.map((p) => {
                        const isSelected = selected?.poolName === p.poolName
                        const activeColor = chartMetric === 'usage' ? '#10b981' : '#3b82f6'
                        const defaultColor = chartMetric === 'usage' ? '#a7f3d0' : '#bfdbfe'
                        return (
                          <Cell
                            key={p.poolName}
                            fill={isSelected ? activeColor : defaultColor}
                            stroke={isSelected ? activeColor : undefined}
                            strokeWidth={isSelected ? 1.5 : 0}
                          />
                        )
                      })}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>

              {/* 3. 구분선 및 풀 상세 목록 테이블 */}
              <div className="mt-2 pt-2 border-t border-slate-100 flex-1 flex flex-col min-h-0">
                <div className="mb-1.5 flex items-center justify-between text-[11px] text-slate-400 font-medium shrink-0">
                  <span>커넥션 풀 목록</span>
                  <span>* 행 또는 막대 클릭 시 선택</span>
                </div>
                <div className="flex-1 overflow-y-auto min-h-0">
                  <table className="w-full text-left text-sm">
                    <thead>
                      <tr className="border-b border-slate-200 text-xs text-slate-400 sticky top-0 bg-white">
                        <th className="pb-1.5 px-2 font-medium border-r border-slate-100">풀 명칭</th>
                        <th className="pb-1.5 px-2 font-medium text-right w-[65px] whitespace-nowrap border-r border-slate-100">Active</th>
                        <th className="pb-1.5 px-2 font-medium text-right w-[50px] whitespace-nowrap border-r border-slate-100">Idle</th>
                        <th className="pb-1.5 px-2 font-medium text-right w-[58px] whitespace-nowrap border-r border-slate-100">Pending</th>
                        <th className="pb-1.5 px-2 font-medium text-right w-[55px] whitespace-nowrap border-r border-slate-100">사용률</th>
                        <th className="pb-1.5 px-2 font-medium text-center w-[48px] whitespace-nowrap">상태</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pools.map((p) => {
                        const isSelected = selected?.poolName === p.poolName
                        const poolUsage = p.max > 0 ? Number(((p.active / p.max) * 100).toFixed(1)) : 0
                        const poolTone = getTone(poolUsage, DEFAULT_THRESHOLDS.HIKARICP_POOL_USAGE.warn, DEFAULT_THRESHOLDS.HIKARICP_POOL_USAGE.crit)
                        return (
                          <tr
                            key={p.poolName}
                            onClick={() => setSelectedPoolName(p.poolName)}
                            className={`cursor-pointer border-b border-slate-100 transition-colors ${
                              isSelected ? 'bg-emerald-50/90 font-medium' : 'hover:bg-slate-50'
                            }`}
                          >
                            <td className="py-1.5 px-2 font-mono text-xs font-semibold text-slate-800 truncate border-r border-slate-100/80" title={p.poolName}>
                              {p.poolName}
                            </td>
                            <td className={`py-1.5 px-2 whitespace-nowrap text-xs text-right font-mono border-r border-slate-100/80 ${
                              poolTone === 'danger' ? 'text-danger-600 font-bold' : poolTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-800'
                            }`}>
                              {p.active} <span className="text-[10px] text-slate-400">/{p.max}</span>
                            </td>
                            <td className="py-1.5 px-2 whitespace-nowrap text-xs text-slate-600 text-right font-mono border-r border-slate-100/80">
                              {p.idle}
                            </td>
                            <td className={`py-1.5 px-2 whitespace-nowrap text-xs text-right font-mono border-r border-slate-100/80 ${
                              p.pending > 0 ? 'text-warn-600 font-bold' : 'text-slate-600'
                            }`}>
                              {p.pending}
                            </td>
                            <td className="py-1.5 px-2 whitespace-nowrap text-xs text-slate-700 text-right font-mono border-r border-slate-100/80">
                              {poolUsage}%
                            </td>
                            <td className="py-1.5 px-2 whitespace-nowrap text-center">
                              <span
                                className={`rounded px-1.5 py-0.5 text-[10px] font-semibold ${
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
              </div>
            </>
          )}
        </Card>
      </div>
    </div>
  )
}

