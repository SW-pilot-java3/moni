import { useState } from 'react'
import { Area, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts'
import Card from '../ui/Card'
import type { HttpEndpointMetric } from '../../lib/servers'
import { getTone, DEFAULT_THRESHOLDS } from '../../lib/thresholdUtils'

interface TimePoint {
  time: string
  rps: number
  latencyMs: number
}

export default function HttpApiTab({
  endpoints,
  rpsLatencyTimeline,
  totalRps,
  avgLatency,
  avgErrorRate,
}: {
  endpoints: HttpEndpointMetric[]
  rpsLatencyTimeline: TimePoint[]
  totalRps: number
  avgLatency: number
  avgErrorRate: number
}) {
  const [selectedUri, setSelectedUri] = useState<string | null>(endpoints[0]?.uri ?? null)
  const selected = endpoints.find((e) => e.uri === selectedUri) ?? endpoints[0]

  const totalAvgLatencyTone = getTone(avgLatency, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.warn, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.crit)
  const totalErrorTone = getTone(avgErrorRate, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.warn, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.crit)

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h2 className="mb-3 text-sm font-semibold text-slate-800">전체 HTTP 처리량 및 지연시간</h2>
        <div className="mb-4 grid grid-cols-3 gap-3">
          <Card className="p-4">
            <div className="text-xs text-slate-500">총 RPS (요청/초)</div>
            <div className="mt-1 text-2xl font-bold text-slate-900">{totalRps.toFixed(1)} req/s</div>
          </Card>
          <Card className="p-4">
            <div className="text-xs text-slate-500">평균 응답시간</div>
            <div className={`mt-1 text-2xl font-bold ${totalAvgLatencyTone === 'danger' ? 'text-danger-600' : totalAvgLatencyTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
              {avgLatency.toFixed(1)} ms
            </div>
          </Card>
          <Card className="p-4">
            <div className="text-xs text-slate-500">에러율</div>
            <div className={`mt-1 text-2xl font-bold ${totalErrorTone === 'danger' ? 'text-danger-600' : totalErrorTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
              {avgErrorRate.toFixed(1)}%
            </div>
          </Card>
        </div>

        <Card className="p-5">
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <ComposedChart data={rpsLatencyTimeline} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} interval="preserveStartEnd" minTickGap={45} />
                <YAxis yAxisId="rps" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={36} />
                <YAxis yAxisId="latency" orientation="right" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={40} />
                <Tooltip contentStyle={{ fontSize: 12, borderRadius: 8, borderColor: '#e2e8f0' }} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Area
                  yAxisId="rps"
                  type="monotone"
                  dataKey="rps"
                  name="RPS (req/s)"
                  stroke="#5b7fa6"
                  strokeWidth={2}
                  fill="#5b7fa6"
                  fillOpacity={0.12}
                  dot={false}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                  isAnimationActive
                  animationDuration={600}
                  animationEasing="ease-out"
                />
                <Line
                  yAxisId="latency"
                  type="monotone"
                  dataKey="latencyMs"
                  name="Latency (ms)"
                  stroke="#c8922f"
                  strokeWidth={2}
                  strokeDasharray="4 3"
                  dot={false}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                  isAnimationActive
                  animationDuration={600}
                  animationEasing="ease-out"
                />
              </ComposedChart>
            </ResponsiveContainer>
          </div>
        </Card>
      </div>

      <div className="flex flex-col gap-4">
        <Card className="p-5">
          <h3 className="mb-3 text-sm font-semibold text-slate-800">
            API 엔드포인트 목록 <span className="text-xs font-normal text-slate-400">(행 클릭 시 상세 확인)</span>
          </h3>
          {endpoints.length === 0 ? (
            <p className="text-sm text-slate-400">수집된 엔드포인트 데이터가 없습니다.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs text-slate-400">
                    <th className="pb-2 font-medium">메서드</th>
                    <th className="pb-2 font-medium">URI</th>
                    <th className="pb-2 font-medium">상태</th>
                    <th className="pb-2 font-medium">RPS</th>
                    <th className="pb-2 font-medium">평균 응답</th>
                    <th className="pb-2 font-medium">오류율</th>
                  </tr>
                </thead>
                <tbody>
                  {endpoints.map((ep) => {
                    const isSelected = selected?.uri === ep.uri
                    const latencyTone = getTone(ep.avgLatencyMs, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.warn, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.crit)
                    const errorTone = getTone(ep.errorRatePct, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.warn, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.crit)
                    return (
                      <tr
                        key={`${ep.method}-${ep.uri}-${ep.status}`}
                        onClick={() => setSelectedUri(ep.uri)}
                        className={`cursor-pointer border-b border-slate-100 ${isSelected ? 'bg-brand-50' : 'hover:bg-slate-50'}`}
                      >
                        <td className="py-2.5 pr-3 whitespace-nowrap">
                          <span
                            className={`rounded px-1.5 py-0.5 font-mono text-[11px] font-semibold ${
                              ep.method === 'GET' ? 'bg-brand-100 text-brand-700' : 'bg-emerald-100 text-emerald-700'
                            }`}
                          >
                            {ep.method}
                          </span>
                        </td>
                        <td className="py-2.5 pr-3 font-mono text-xs whitespace-nowrap text-slate-700">{ep.uri}</td>
                        <td className="py-2.5 pr-3 whitespace-nowrap text-slate-600">{ep.status}</td>
                        <td className="py-2.5 pr-3 whitespace-nowrap text-slate-700">{ep.rps.toFixed(1)} req/s</td>
                        <td className={`py-2.5 pr-3 whitespace-nowrap ${latencyTone === 'danger' ? 'text-danger-600 font-semibold' : latencyTone === 'warn' ? 'text-warn-600 font-semibold' : 'text-slate-700'}`}>
                          {ep.avgLatencyMs.toLocaleString()} ms
                        </td>
                        <td className={`py-2.5 whitespace-nowrap ${errorTone === 'danger' ? 'text-danger-600 font-semibold' : errorTone === 'warn' ? 'text-warn-600 font-semibold' : 'text-slate-700'}`}>
                          {ep.errorRatePct}%
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        {selected && (
          <Card className="flex-1 p-5">
            <h3 className="mb-3 flex items-center gap-1.5 text-sm font-semibold text-slate-800">
              선택된 API:{' '}
              <span className="rounded bg-emerald-100 px-1.5 py-0.5 font-mono text-[11px] font-semibold text-emerald-700">
                {selected.method}
              </span>
              <span className="font-mono text-xs text-slate-600">{selected.uri}</span>
            </h3>

            {(() => {
              const selLatencyTone = getTone(selected.avgLatencyMs, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.warn, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.crit)
              const selErrorTone = getTone(selected.errorRatePct, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.warn, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.crit)
              return (
                <div className="grid grid-cols-4 gap-2">
                  <div className="rounded-md bg-slate-50 px-2 py-2 text-center">
                    <div className="text-[11px] text-slate-500">RPS</div>
                    <div className="mt-0.5 text-sm font-bold text-slate-900">{selected.rps.toFixed(1)} req/s</div>
                  </div>
                  <div className={`rounded-md px-2 py-2 text-center ${selLatencyTone === 'danger' ? 'bg-danger-50' : selLatencyTone === 'warn' ? 'bg-warn-50' : 'bg-slate-50'}`}>
                    <div className="text-[11px] text-slate-500">평균 응답</div>
                    <div className={`mt-0.5 text-sm font-bold ${selLatencyTone === 'danger' ? 'text-danger-600' : selLatencyTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
                      {selected.avgLatencyMs.toFixed(1)} ms
                    </div>
                  </div>
                  <div className={`rounded-md px-2 py-2 text-center ${selErrorTone === 'danger' ? 'bg-danger-50' : selErrorTone === 'warn' ? 'bg-warn-50' : 'bg-slate-50'}`}>
                    <div className="text-[11px] text-slate-500">에러율</div>
                    <div className={`mt-0.5 text-sm font-bold ${selErrorTone === 'danger' ? 'text-danger-600' : selErrorTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
                      {selected.errorRatePct}%
                    </div>
                  </div>
                  <div className="rounded-md bg-slate-50 px-2 py-2 text-center">
                    <div className="text-[11px] text-slate-500">최대 응답</div>
                    <div className="mt-0.5 text-sm font-bold text-slate-900">{selected.maxLatencyMs.toFixed(1)} ms</div>
                  </div>
                </div>
              )
            })()}
          </Card>
        )}
      </div>
    </div>
  )
}