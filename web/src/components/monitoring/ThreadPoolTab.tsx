import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts'
import Card from '../ui/Card'
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
                <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={40} />
                <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#e2e8f0' }} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Area
                  type="monotone"
                  dataKey="active"
                  name="Active Threads"
                  stroke="#c8922f"
                  strokeWidth={2}
                  fill="url(#grad-executor)"
                  dot={false}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* 우측: 스레드 풀 목록 테이블 */}
        <Card className="lg:col-span-5 p-4 flex flex-col flex-1 min-h-0 h-full overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          <h3 className="mb-3 text-sm font-semibold text-slate-800 shrink-0">
            ThreadPool 목록 <span className="text-xs font-normal text-slate-400">({executors.length}개 등록됨)</span>
          </h3>
          {executors.length === 0 ? (
            <p className="text-sm text-slate-400 my-auto text-center">수집된 ThreadPool 데이터가 없습니다.</p>
          ) : (
            <div className="flex-1 overflow-y-auto min-h-0">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs text-slate-400 sticky top-0 bg-white">
                    <th className="pb-2 font-medium">명칭</th>
                    <th className="pb-2 font-medium">Active</th>
                    <th className="pb-2 font-medium">Max</th>
                    <th className="pb-2 font-medium">대기 큐</th>
                    <th className="pb-2 font-medium">잔여 큐</th>
                    <th className="pb-2 font-medium">상태</th>
                  </tr>
                </thead>
                <tbody>
                  {executors.map((e) => {
                    const isUnbounded = e.max >= UNBOUNDED_THRESHOLD
                    const maxLabel = isUnbounded ? '∞' : `${e.max}`
                    const remLabel = isUnbounded ? '∞' : `${e.queueRemaining}`
                    const threadUsage = !isUnbounded && e.max > 0 ? (e.active / e.max) * 100 : 0
                    const threadTone = getTone(threadUsage, DEFAULT_THRESHOLDS.THREADPOOL_QUEUE_USAGE.warn, DEFAULT_THRESHOLDS.THREADPOOL_QUEUE_USAGE.crit)
                    const queueTone = getTone(e.queuedTasks, 10, 50)

                    return (
                      <tr key={e.name} className="border-b border-slate-100">
                        <td className="py-2.5 pr-2 font-mono text-xs font-semibold text-slate-800 truncate max-w-[120px]" title={e.name}>
                          {e.name}
                        </td>
                        <td className={`py-2.5 pr-2 whitespace-nowrap text-xs ${threadTone === 'danger' ? 'text-danger-600 font-bold' : threadTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-800'}`}>
                          {e.active}
                        </td>
                        <td className="py-2.5 pr-2 whitespace-nowrap text-xs text-slate-700">{maxLabel}</td>
                        <td className={`py-2.5 pr-2 whitespace-nowrap text-xs ${queueTone === 'danger' ? 'text-danger-600 font-bold' : queueTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-600'}`}>
                          {e.queuedTasks}
                        </td>
                        <td className="py-2.5 pr-2 whitespace-nowrap text-xs text-slate-700">{remLabel}</td>
                        <td className="py-2.5 whitespace-nowrap">
                          <span
                            className={`rounded px-1.5 py-0.5 text-[10px] font-medium ${
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
          )}
        </Card>
      </div>
    </div>
  )
}
