import { useState } from 'react'
import {
  Area,
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import Card from '../ui/Card'
import CustomChartTooltip from './CustomChartTooltip'
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
  maxLatency,
  avgErrorRate,
}: {
  endpoints: HttpEndpointMetric[]
  rpsLatencyTimeline: TimePoint[]
  totalRps: number
  avgLatency: number
  maxLatency: number
  avgErrorRate: number
}) {
  const [selectedUri, setSelectedUri] = useState<string | null>(endpoints[0]?.uri ?? null)
  const [chartMetric, setChartMetric] = useState<'rps' | 'latency'>('rps')
  const selected = endpoints.find((e) => e.uri === selectedUri) ?? endpoints[0]

  const totalAvgLatencyTone = getTone(avgLatency, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.warn, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.crit)
  const totalMaxLatencyTone = getTone(maxLatency, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.warn * 2, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.crit * 2)
  const totalErrorTone = getTone(avgErrorRate, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.warn, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.crit)

  return (
    <div className="flex-1 flex flex-col min-h-0 gap-4 h-full">
      {/* 상단 4개 슬림 지표 카드 */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4 shrink-0">
        <Card className="p-3">
          <div className="text-[11px] text-slate-500">총 RPS (초당 요청 수)</div>
          <div className="mt-0.5 text-xl font-bold text-slate-900">{totalRps.toFixed(1)} <span className="text-xs font-normal text-slate-400">req/s</span></div>
          <div className="mt-0.5 text-[10px] text-slate-400">전체 API 요청 처리량</div>
        </Card>
        <Card className="p-3">
          <div className="text-[11px] text-slate-500">평균 응답시간</div>
          <div className={`mt-0.5 text-xl font-bold ${totalAvgLatencyTone === 'danger' ? 'text-danger-600' : totalAvgLatencyTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {avgLatency.toFixed(1)} <span className="text-xs font-normal text-slate-400">ms</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">전체 요청 평균 소요</div>
        </Card>
        <Card className="p-3">
          <div className="text-[11px] text-slate-500">최대 응답시간</div>
          <div className={`mt-0.5 text-xl font-bold ${totalMaxLatencyTone === 'danger' ? 'text-danger-600' : totalMaxLatencyTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {maxLatency.toFixed(0)} <span className="text-xs font-normal text-slate-400">ms</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">엔드포인트 중 최대 지연</div>
        </Card>
        <Card className="p-3">
          <div className="text-[11px] text-slate-500">전체 오류율</div>
          <div className={`mt-0.5 text-xl font-bold ${totalErrorTone === 'danger' ? 'text-danger-600' : totalErrorTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {avgErrorRate.toFixed(1)}%
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">HTTP 4xx / 5xx 비율</div>
        </Card>
      </div>

      {/* 하단 2열 스플릿 영역 (좌측 차트 7 : 우측 테이블 5) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 flex-1 min-h-0 h-full">
        {/* 좌측: 실시간 RPS & Latency 복합 차트 */}
        <Card className="lg:col-span-7 p-4 flex flex-col justify-between flex-1 min-h-0 h-full shadow-sm hover:shadow-md transition-shadow">
          <h3 className="mb-2 text-sm font-semibold text-slate-800 shrink-0">
            HTTP 처리량 &amp; 지연시간 추이 <span className="text-xs font-normal text-slate-400">(RPS &amp; Avg Latency)</span>
          </h3>
          <div className="flex-1 w-full min-h-[180px] pt-1">
            <ResponsiveContainer width="100%" height="100%">
              <ComposedChart data={rpsLatencyTimeline} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} interval="preserveStartEnd" minTickGap={45} />
                <YAxis yAxisId="rps" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={36} />
                <YAxis yAxisId="latency" orientation="right" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={40} />
                <Tooltip content={<CustomChartTooltip />} />
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
                  connectNulls={true}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
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
                  connectNulls={true}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                />
              </ComposedChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* 우측: 선택된 API 상세 + 엔드포인트 목록 테이블 */}
        <div className="lg:col-span-5 flex flex-col gap-3 flex-1 min-h-0 h-full">
          {/* 선택된 API 상세 드릴다운 카드 (상단 슬림형) */}
          {selected && (
            <Card className="p-3 shrink-0 bg-slate-50 border border-slate-200 shadow-sm">
              <div className="mb-2 flex items-center justify-between">
                <div className="flex items-center gap-1.5 truncate">
                  <span className="rounded bg-brand-100 px-1.5 py-0.5 font-mono text-[10px] font-bold text-brand-700">
                    {selected.method}
                  </span>
                  <span className="font-mono text-xs font-semibold text-slate-800 truncate" title={selected.uri}>
                    {selected.uri}
                  </span>
                </div>
                <span className="text-[11px] text-slate-400 shrink-0">선택된 API 메트릭</span>
              </div>

              {(() => {
                const selLatencyTone = getTone(selected.avgLatencyMs, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.warn, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.crit)
                const selErrorTone = getTone(selected.errorRatePct, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.warn, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.crit)
                return (
                  <div className="grid grid-cols-4 gap-2">
                    <div className="rounded bg-white border border-slate-200/80 px-2 py-1.5 text-center">
                      <div className="text-[10px] text-slate-500">RPS</div>
                      <div className="text-xs font-bold text-slate-900">{selected.rps.toFixed(1)}</div>
                    </div>
                    <div className={`rounded px-2 py-1.5 text-center border ${selLatencyTone === 'danger' ? 'bg-danger-50 border-danger-200' : selLatencyTone === 'warn' ? 'bg-warn-50 border-warn-200' : 'bg-white border-slate-200/80'}`}>
                      <div className="text-[10px] text-slate-500">평균 응답</div>
                      <div className={`text-xs font-bold ${selLatencyTone === 'danger' ? 'text-danger-600' : selLatencyTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
                        {selected.avgLatencyMs.toFixed(0)} ms
                      </div>
                    </div>
                    <div className={`rounded px-2 py-1.5 text-center border ${selErrorTone === 'danger' ? 'bg-danger-50 border-danger-200' : selErrorTone === 'warn' ? 'bg-warn-50 border-warn-200' : 'bg-white border-slate-200/80'}`}>
                      <div className="text-[10px] text-slate-500">오류율</div>
                      <div className={`text-xs font-bold ${selErrorTone === 'danger' ? 'text-danger-600' : selErrorTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
                        {selected.errorRatePct}%
                      </div>
                    </div>
                    <div className="rounded bg-white border border-slate-200/80 px-2 py-1.5 text-center">
                      <div className="text-[10px] text-slate-500">최대 응답</div>
                      <div className="text-xs font-bold text-slate-900">{selected.maxLatencyMs.toFixed(0)} ms</div>
                    </div>
                  </div>
                )
              })()}
            </Card>
          )}

          {/* 엔드포인트 비교 바 차트 & 목록 통합 카드 */}
          <Card className="flex-1 p-4 flex flex-col min-h-0 overflow-hidden shadow-sm hover:shadow-md transition-shadow">
            {/* 1. 상단: 타이틀 & 지표 선택 세그먼트 */}
            {(() => {
              const TOP_N_LIMIT = 7
              const sortedEndpoints = [...endpoints].sort((a, b) =>
                chartMetric === 'rps' ? b.rps - a.rps : b.avgLatencyMs - a.avgLatencyMs
              )
              const chartEndpoints = sortedEndpoints.slice(0, TOP_N_LIMIT)

              return (
                <>
                  <div className="mb-2 flex items-center justify-between shrink-0">
                    <div className="flex items-center gap-1.5">
                      <h3 className="text-sm font-semibold text-slate-800">
                        엔드포인트별 지표 비교
                      </h3>
                      <span className="text-xs text-slate-400 font-normal">
                        ({endpoints.length > TOP_N_LIMIT ? `상위 ${chartEndpoints.length}개 / 전체 ${endpoints.length}개` : `${endpoints.length}개 API`})
                      </span>
                    </div>

                    {/* RPS / 응답시간 토글 */}
                    <div className="flex rounded-md border border-slate-200 bg-slate-100 p-0.5 text-[11px] font-semibold">
                      <button
                        type="button"
                        onClick={() => setChartMetric('rps')}
                        className={`rounded px-2 py-0.5 transition-all ${
                          chartMetric === 'rps'
                            ? 'bg-white text-brand-700 shadow-2xs font-bold'
                            : 'text-slate-500 hover:text-slate-800'
                        }`}
                      >
                        RPS
                      </button>
                      <button
                        type="button"
                        onClick={() => setChartMetric('latency')}
                        className={`rounded px-2 py-0.5 transition-all ${
                          chartMetric === 'latency'
                            ? 'bg-white text-amber-700 shadow-2xs font-bold'
                            : 'text-slate-500 hover:text-slate-800'
                        }`}
                      >
                        응답시간
                      </button>
                    </div>
                  </div>

                  {endpoints.length === 0 ? (
                    <p className="text-sm text-slate-400 my-auto text-center">수집된 엔드포인트 데이터가 없습니다.</p>
                  ) : (
                    <>
                      {/* 2. 슬림 & 세련된 세로 막대그래프 (상위 Top N) */}
                      <div className="h-28 w-full shrink-0 pt-1 pb-1">
                        <ResponsiveContainer width="100%" height="100%">
                          <BarChart
                            data={chartEndpoints.map((ep) => {
                              const pathOnly = ep.uri.startsWith('/') ? ep.uri.slice(1) : ep.uri
                              const shortUri = pathOnly.length > 14 ? pathOnly.slice(0, 12) + '…' : pathOnly
                              return {
                                name: `${ep.method} ${ep.uri}`,
                                label: `${ep.method} /${shortUri}`,
                                uri: ep.uri,
                                method: ep.method,
                                rps: Number(ep.rps.toFixed(1)),
                                latencyMs: Number(ep.avgLatencyMs.toFixed(0)),
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
                                  unit={chartMetric === 'rps' ? 'req/s' : 'ms'}
                                />
                              }
                            />
                            <Bar
                              dataKey={chartMetric === 'rps' ? 'rps' : 'latencyMs'}
                              name={chartMetric === 'rps' ? '초당 요청 수 (RPS)' : '평균 응답시간'}
                              barSize={18}
                              radius={[3, 3, 0, 0]}
                              onClick={(data: any) => {
                                if (data && data.uri) setSelectedUri(data.uri)
                              }}
                              className="cursor-pointer"
                            >
                              {chartEndpoints.map((ep) => {
                                const isSelected = selected?.uri === ep.uri
                                const activeColor = chartMetric === 'rps' ? '#3b82f6' : '#f59e0b'
                                const defaultColor = chartMetric === 'rps' ? '#bfdbfe' : '#fef3c7'
                                return (
                                  <Cell
                                    key={ep.uri}
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

                {/* 3. 구분선 및 상세 테이블 목록 */}
                <div className="mt-2 pt-2 border-t border-slate-100 flex-1 flex flex-col min-h-0">
                  <div className="mb-1.5 flex items-center justify-between text-[11px] text-slate-400 font-medium shrink-0">
                    <span>엔드포인트 목록</span>
                    <span>* 행 또는 막대 클릭 시 선택</span>
                  </div>
                  <div className="flex-1 overflow-y-auto min-h-0">
                    <table className="w-full text-left text-sm">
                      <thead>
                        <tr className="border-b border-slate-200 text-xs text-slate-400 sticky top-0 bg-white">
                          <th className="pb-1.5 px-2 font-medium w-[54px] border-r border-slate-100">메서드</th>
                          <th className="pb-1.5 px-2 font-medium border-r border-slate-100">URI</th>
                          <th className="pb-1.5 px-2 font-medium text-right w-[60px] whitespace-nowrap border-r border-slate-100">RPS</th>
                          <th className="pb-1.5 px-2 font-medium text-right w-[72px] whitespace-nowrap border-r border-slate-100">평균 응답</th>
                          <th className="pb-1.5 px-2 font-medium text-right w-[58px] whitespace-nowrap">오류율</th>
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
                              className={`cursor-pointer border-b border-slate-100 transition-colors ${
                                isSelected ? 'bg-brand-50/90 font-medium' : 'hover:bg-slate-50'
                              }`}
                            >
                              <td className="py-1.5 px-2 whitespace-nowrap border-r border-slate-100/80">
                                <span
                                  className={`rounded px-1.5 py-0.5 font-mono text-[10px] font-bold ${
                                    ep.method === 'GET' ? 'bg-brand-100 text-brand-700' : 'bg-emerald-100 text-emerald-700'
                                  }`}
                                >
                                  {ep.method}
                                </span>
                              </td>
                              <td className="py-1.5 px-2 font-mono text-xs whitespace-nowrap text-slate-700 truncate border-r border-slate-100/80" title={ep.uri}>
                                {ep.uri}
                              </td>
                              <td className="py-1.5 px-2 whitespace-nowrap text-xs text-slate-700 text-right font-mono border-r border-slate-100/80">
                                {ep.rps.toFixed(1)}
                              </td>
                              <td className={`py-1.5 px-2 whitespace-nowrap text-xs text-right font-mono border-r border-slate-100/80 ${
                                latencyTone === 'danger' ? 'text-danger-600 font-bold' : latencyTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-700'
                              }`}>
                                {ep.avgLatencyMs.toFixed(0)} ms
                              </td>
                              <td className={`py-1.5 px-2 whitespace-nowrap text-xs text-right font-mono ${
                                errorTone === 'danger' ? 'text-danger-600 font-bold' : errorTone === 'warn' ? 'text-warn-600 font-bold' : 'text-slate-700'
                              }`}>
                                {ep.errorRatePct}%
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
    </div>
  )
}