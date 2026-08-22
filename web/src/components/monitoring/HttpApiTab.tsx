import { useState } from 'react'
import { Area, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts'
import Card from '../ui/Card'
import type { HttpEndpointMetric } from '../../lib/servers'

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
  const maxLatency = endpoints.reduce((max, e) => (e.maxLatencyMs > max.value ? { value: e.maxLatencyMs, uri: e.uri } : max), {
    value: 0,
    uri: '—',
  })

  return (
    <div className="grid grid-cols-2 gap-4">
      <div className="flex flex-col gap-4">
        <div className="grid grid-cols-2 gap-4">
          <Card className="p-4">
            <div className="text-xs text-slate-500">서버 전체 RPS</div>
            <div className="mt-1 text-2xl font-bold text-slate-900">
              {totalRps.toFixed(1)} <span className="text-sm font-normal text-slate-400">req/s</span>
            </div>
          </Card>
          <Card className="border-warn-500/30 bg-warn-50/60 p-4">
            <div className="text-xs text-slate-500">전체 평균 응답시간</div>
            <div className="mt-1 text-2xl font-bold text-warn-600">
              {avgLatency.toFixed(0)} <span className="text-sm font-normal text-warn-600/70">ms</span>
            </div>
          </Card>
          <Card className="border-danger-500/30 bg-danger-50/60 p-4">
            <div className="text-xs text-slate-500">전체 오류율</div>
            <div className="mt-1 text-2xl font-bold text-danger-600">{avgErrorRate.toFixed(1)}%</div>
          </Card>
          <Card className="p-4">
            <div className="text-xs text-slate-500">전체 최대 응답시간</div>
            <div className="mt-1 text-2xl font-bold text-slate-900">
              {maxLatency.value.toLocaleString()} <span className="text-sm font-normal text-slate-400">ms</span>
            </div>
            <div className="mt-1 text-xs text-slate-400 truncate">{maxLatency.uri}</div>
          </Card>
        </div>

        <Card className="flex-1 p-5">
          <h3 className="mb-4 text-sm font-semibold text-slate-800">
            [서버 전체] RPS &amp; 응답시간 <span className="ml-1 text-xs font-normal text-brand-500">● 실시간(SSE)</span>
          </h3>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <ComposedChart data={rpsLatencyTimeline} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} />
                <YAxis yAxisId="rps" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={30} />
                <YAxis yAxisId="latency" orientation="right" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={36} />
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
                  dot={{ r: 3 }}
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
            <p className="py-6 text-center text-sm text-slate-400">수집된 엔드포인트 데이터가 없습니다.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[560px] border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-left text-slate-500">
                    <th className="py-2 pr-3 font-medium whitespace-nowrap">Method</th>
                    <th className="py-2 pr-3 font-medium whitespace-nowrap">URI</th>
                    <th className="py-2 pr-3 font-medium whitespace-nowrap">Status</th>
                    <th className="py-2 pr-3 font-medium whitespace-nowrap">RPS</th>
                    <th className="py-2 pr-3 font-medium whitespace-nowrap">평균 응답</th>
                    <th className="py-2 font-medium whitespace-nowrap">에러율</th>
                  </tr>
                </thead>
                <tbody>
                  {endpoints.map((ep) => {
                    const isSelected = ep.uri === selectedUri
                    const isSlow = ep.errorRatePct > 5
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
                        <td className={`py-2.5 pr-3 whitespace-nowrap ${isSlow ? 'text-danger-600' : 'text-warn-600'}`}>
                          {ep.avgLatencyMs.toLocaleString()} ms
                        </td>
                        <td className={`py-2.5 whitespace-nowrap ${isSlow ? 'text-danger-600' : 'text-slate-700'}`}>{ep.errorRatePct}%</td>
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
              🔍 선택된 API:{' '}
              <span className="rounded bg-emerald-100 px-1.5 py-0.5 font-mono text-[11px] font-semibold text-emerald-700">
                {selected.method}
              </span>
              <span className="font-mono text-xs text-slate-600">{selected.uri}</span>
            </h3>

            <div className="grid grid-cols-4 gap-2">
              <div className="rounded-md bg-slate-50 px-2 py-2 text-center">
                <div className="text-[11px] text-slate-500">RPS</div>
                <div className="mt-0.5 text-sm font-bold text-slate-900">{selected.rps.toFixed(1)} req/s</div>
              </div>
              <div className="rounded-md bg-warn-50 px-2 py-2 text-center">
                <div className="text-[11px] text-slate-500">평균 응답</div>
                <div className="mt-0.5 text-sm font-bold text-warn-600">{selected.avgLatencyMs.toFixed(1)} ms</div>
              </div>
              <div className="rounded-md bg-warn-50 px-2 py-2 text-center">
                <div className="text-[11px] text-slate-500">에러율</div>
                <div className="mt-0.5 text-sm font-bold text-warn-600">{selected.errorRatePct}%</div>
              </div>
              <div className="rounded-md bg-slate-50 px-2 py-2 text-center">
                <div className="text-[11px] text-slate-500">최대 응답</div>
                <div className="mt-0.5 text-sm font-bold text-slate-900">{selected.maxLatencyMs.toFixed(1)} ms</div>
              </div>
            </div>
          </Card>
        )}
      </div>
    </div>
  )
}