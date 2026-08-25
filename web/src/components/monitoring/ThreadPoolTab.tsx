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
import type { ExecutorMetric, ServerRealtimeSeriesPoint } from '../../lib/servers'

function shortTime(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}

export default function ThreadPoolTab({
  executors,
  series,
  totalActive,
  totalMaxLabel,
  totalQueued,
  totalRemainingLabel,
}: {
  executors: ExecutorMetric[]
  series: ServerRealtimeSeriesPoint[]
  totalActive: number
  totalMaxLabel: string
  totalQueued: number
  totalRemainingLabel: string
}) {
  const [selectedExecutorName, setSelectedExecutorName] = useState<string | null>(executors[0]?.name ?? null)
  const [chartMetric, setChartMetric] = useState<'active' | 'queued'>('active')
  const selected = executors.find((e) => e.name === selectedExecutorName) ?? executors[0]

  const UNBOUNDED_THRESHOLD = 100_000_000
  const boundedExecutors = executors.filter((e) => e.max < UNBOUNDED_THRESHOLD)
  const totalMaxNum = boundedExecutors.reduce((sum, e) => sum + e.max, 0)
  const usagePct = totalMaxNum > 0 ? Number(((totalActive / totalMaxNum) * 100).toFixed(1)) : 0

  const activeTone = getTone(usagePct, DEFAULT_THRESHOLDS.THREADPOOL_QUEUE_USAGE.warn, DEFAULT_THRESHOLDS.THREADPOOL_QUEUE_USAGE.crit)
  const queuedTone = getTone(totalQueued, 10, 50)

  const timelineData = series.map((p) => ({
    time: shortTime(p.collectedAt),
    active: p.executorActiveTotal,
  }))

  return (
    <div className="flex-1 flex flex-col min-h-0 gap-4 h-full">
      {/* 상단 4개 슬림 지표 요약 카드 */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 shrink-0">
        <Card className="p-3">
          <div className="text-[11px] text-slate-500">Active 활성 스레드</div>
          <div className={`mt-0.5 text-xl font-bold ${activeTone === 'danger' ? 'text-danger-600' : activeTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {totalActive} <span className="text-xs font-normal text-slate-400">/ {totalMaxLabel}</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">현재 작업 처리 중</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">Queued 대기 작업</div>
          <div className={`mt-0.5 text-xl font-bold ${queuedTone === 'danger' ? 'text-danger-600' : queuedTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {totalQueued} <span className="text-xs font-normal text-slate-400">건</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">스레드 풀 큐 대기 건수</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">남은 큐 용량 (Remaining)</div>
          <div className="mt-0.5 text-xl font-bold text-slate-900">
            {totalRemainingLabel} <span className="text-xs font-normal text-slate-400">건</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">수용 가능한 잔여 용량</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">최대 스레드 풀 크기</div>
          <div className="mt-0.5 text-xl font-bold text-slate-900">
            {totalMaxLabel} <span className="text-xs font-normal text-slate-400">개</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">스레드 풀 최대 상한</div>
        </Card>
      </div>

      {/* 하단 2열 스플릿 영역 (좌측 차트 7 : 우측 테이블 5) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 flex-1 min-h-0 h-full">
        {/* 좌측: 스레드 풀 활성 추이 차트 */}
        <Card className="lg:col-span-7 p-4 flex flex-col justify-between flex-1 min-h-0 h-full shadow-sm hover:shadow-md transition-shadow">
          <h3 className="mb-2 text-sm font-semibold text-slate-800 shrink-0">
            ThreadPool 활성 스레드 추이 <span className="text-xs font-normal text-slate-400">(Active Threads Timeline)</span>
          </h3>
          <div className="flex-1 w-full min-h-[180px] pt-1">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={timelineData} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="grad-executor" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#c8922f" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#c8922f" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} interval="preserveStartEnd" minTickGap={45} />
                <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} allowDecimals={false} tickLine={false} axisLine={false} width={40} />
                <Tooltip content={<CustomChartTooltip />} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Area
                  type="monotone"
                  dataKey="active"
                  name="Active Threads"
                  stroke="#c8922f"
                  strokeWidth={2}
                  fill="url(#grad-executor)"
                  dot={false}
                  connectNulls={true}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* 우측: 스레드 풀 비교 바 차트 & 목록 통합 카드 */}
        <Card className="lg:col-span-5 p-4 flex flex-col flex-1 min-h-0 h-full overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          {(() => {
            const TOP_N_LIMIT = 6
            const sortedExecutors = [...executors].sort((a, b) => {
              if (chartMetric === 'active') {
                return b.active - a.active
              }
              return b.queuedTasks - a.queuedTasks
            })
            const chartExecutors = sortedExecutors.slice(0, TOP_N_LIMIT)

            return (
              <>
                {/* 1. 상단: 타이틀 & 지표 선택 세그먼트 */}
                <div className="mb-2 flex items-center justify-between shrink-0">
                  <div className="flex items-center gap-1.5">
                    <h3 className="text-sm font-semibold text-slate-800">
                      스레드 풀별 부하 비교
                    </h3>
                    <span className="text-xs text-slate-400 font-normal">
                      ({executors.length > TOP_N_LIMIT ? `상위 ${chartExecutors.length}개 / 전체 ${executors.length}개` : `${executors.length}개 풀`})
                    </span>
                  </div>

                  {/* 활성 스레드 / 큐 대기 토글 */}
                  <div className="flex rounded-md border border-slate-200 bg-slate-100 p-0.5 text-[11px] font-semibold">
                    <button
                      type="button"
                      onClick={() => setChartMetric('active')}
                      className={`rounded px-2 py-0.5 transition-all ${
                        chartMetric === 'active'
                          ? 'bg-white text-amber-700 shadow-2xs font-bold'
                          : 'text-slate-500 hover:text-slate-800'
                      }`}
                    >
                      활성 스레드
                    </button>
                    <button
                      type="button"
                      onClick={() => setChartMetric('queued')}
                      className={`rounded px-2 py-0.5 transition-all ${
                        chartMetric === 'queued'
                          ? 'bg-white text-rose-700 shadow-2xs font-bold'
                          : 'text-slate-500 hover:text-slate-800'
                      }`}
                    >
                      대기 큐
                    </button>
                  </div>
                </div>

                {executors.length === 0 ? (
                  <p className="text-sm text-slate-400 my-auto text-center">수집된 ThreadPool 데이터가 없습니다.</p>
                ) : (
                  <>
                    {/* 2. 슬림 & 세련된 스레드 풀별 세로 막대그래프 (상위 Top N) */}
                    <div className="h-28 w-full shrink-0 pt-1 pb-1">
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart
                          data={chartExecutors.map((e) => {
                            const isUnbounded = e.max >= UNBOUNDED_THRESHOLD
                            const shortName = e.name.length > 14 ? e.name.slice(0, 12) + '…' : e.name
                            return {
                              name: e.name,
                              label: shortName,
                              active: e.active,
                              queuedTasks: e.queuedTasks,
                              max: isUnbounded ? 9999 : e.max,
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
                                unit={chartMetric === 'active' ? '개' : '건'}
                              />
                            }
                          />
                          <Bar
                            dataKey={chartMetric === 'active' ? 'active' : 'queuedTasks'}
                            name={chartMetric === 'active' ? '활성 스레드' : '큐 대기 작업'}
                            barSize={18}
                            radius={[3, 3, 0, 0]}
                            onClick={(data: any) => {
                              if (data && data.name) setSelectedExecutorName(data.name)
                            }}
                            className="cursor-pointer"
                          >
                            {chartExecutors.map((e) => {
                              const isSelected = selected?.name === e.name
                              const activeColor = chartMetric === 'active' ? '#f59e0b' : '#f43f5e'
                              const defaultColor = chartMetric === 'active' ? '#fde68a' : '#fecdd3'
                              return (
                                <Cell
                                  key={e.name}
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

              {/* 3. 구분선 및 스레드 풀 상세 목록 테이블 */}
              <div className="mt-2 pt-2 border-t border-slate-100 flex-1 flex flex-col min-h-0">
                <div className="mb-1.5 flex items-center justify-between text-[11px] text-slate-400 font-medium shrink-0">
                  <span>스레드 풀 목록</span>
                  <span>* 행 또는 막대 클릭 시 선택</span>
                </div>
                <div className="flex-1 overflow-y-auto min-h-0">
                  <table className="w-full text-left text-sm">
                    <thead>
                      <tr className="border-b border-slate-200 text-xs text-slate-400 sticky top-0 bg-white">
                        <th className="pb-1.5 px-2 font-medium border-r border-slate-100">명칭</th>
                        <th className="pb-1.5 px-2 font-medium text-right w-[60px] whitespace-nowrap border-r border-slate-100">Active</th>
                        <th className="pb-1.5 px-2 font-medium text-right w-[50px] whitespace-nowrap border-r border-slate-100">Max</th>
                        <th className="pb-1.5 px-2 font-medium text-right w-[55px] whitespace-nowrap border-r border-slate-100">대기 큐</th>
                        <th className="pb-1.5 px-2 font-medium text-right w-[55px] whitespace-nowrap border-r border-slate-100">잔여 큐</th>
                        <th className="pb-1.5 px-2 font-medium text-center w-[48px] whitespace-nowrap">상태</th>
                      </tr>
                    </thead>
                    <tbody>
                      {executors.map((e) => {
                        const isSelected = selected?.name === e.name
                        const isUnbounded = e.max >= UNBOUNDED_THRESHOLD
                        const maxLabel = isUnbounded ? '∞' : `${e.max}`
                        const remLabel = isUnbounded ? '∞' : `${e.queueRemaining}`
                        const threadUsage = !isUnbounded && e.max > 0 ? (e.active / e.max) * 100 : 0
                        const threadTone = getTone(threadUsage, DEFAULT_THRESHOLDS.THREADPOOL_QUEUE_USAGE.warn, DEFAULT_THRESHOLDS.THREADPOOL_QUEUE_USAGE.crit)
                        const queueTone = getTone(e.queuedTasks, 10, 50)

                        return (
                          <tr
                            key={e.name}
                            onClick={() => setSelectedExecutorName(e.name)}
                            className={`cursor-pointer border-b border-slate-100 transition-colors ${
                              isSelected ? 'bg-amber-50/90 font-medium' : 'hover:bg-slate-50'
                            }`}
                          >
                            <td className="py-1.5 px-2 font-mono text-xs font-semibold text-slate-800 truncate border-r border-slate-100/80" title={e.name}>
                              {e.name}
                            </td>
                            <td className={`py-1.5 px-2 whitespace-nowrap text-xs text-right font-mono border-r border-slate-100/80 ${
                              threadTone === 'danger' ? 'text-danger-600 font-bold' : threadTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-800'
                            }`}>
                              {e.active}
                            </td>
                            <td className="py-1.5 px-2 whitespace-nowrap text-xs text-slate-700 text-right font-mono border-r border-slate-100/80">
                              {maxLabel}
                            </td>
                            <td className={`py-1.5 px-2 whitespace-nowrap text-xs text-right font-mono border-r border-slate-100/80 ${
                              queueTone === 'danger' ? 'text-danger-600 font-bold' : queueTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-600'
                            }`}>
                              {e.queuedTasks}
                            </td>
                            <td className="py-1.5 px-2 whitespace-nowrap text-xs text-slate-700 text-right font-mono border-r border-slate-100/80">
                              {remLabel}
                            </td>
                            <td className="py-1.5 px-2 whitespace-nowrap text-center">
                              <span
                                className={`rounded px-1.5 py-0.5 text-[10px] font-semibold ${
                                  queueTone === 'danger' || threadTone === 'danger'
                                    ? 'bg-danger-50 text-danger-700'
                                    : queueTone === 'warn' || threadTone === 'warn'
                                    ? 'bg-warn-50 text-warn-700'
                                    : 'bg-emerald-50 text-emerald-700'
                                }`}
                              >
                                {queueTone === 'danger' || threadTone === 'danger'
                                  ? '위험'
                                  : queueTone === 'warn' || threadTone === 'warn'
                                  ? '주의'
                                  : '정상'}
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
          </>
        )
      })()}
    </Card>
      </div>
    </div>
  )
}
