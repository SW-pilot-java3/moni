import { Area, AreaChart, CartesianGrid, ComposedChart, Line, ResponsiveContainer, Tooltip, XAxis, YAxis, Legend } from 'recharts'
import Card from '../ui/Card'
import CustomChartTooltip from './CustomChartTooltip'
import { getTone, DEFAULT_THRESHOLDS } from '../../lib/thresholdUtils'
import type { ServerRealtimeCurrent, ServerRealtimeSeriesPoint } from '../../lib/servers'

function formatMb(bytes: number | undefined | null) {
  return bytes ? Math.round(bytes / 1024 / 1024) : 0
}

function shortTime(iso: string) {
  const d = new Date(iso)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}

export default function JvmTab({
  current,
  series,
  latest,
  threadsLive,
  threadsBlocked,
}: {
  current: ServerRealtimeCurrent | null | undefined
  series: ServerRealtimeSeriesPoint[]
  latest: ServerRealtimeSeriesPoint | null
  threadsLive: number
  threadsBlocked: number
}) {
  const heapUsedMb = formatMb(latest?.jvmHeapUsedBytes)
  const heapMaxMb = formatMb(latest?.jvmHeapMaxBytes)
  const oldGenUsedMb = formatMb(latest?.jvmOldGenUsedBytes)
  const heapUsagePct =
    latest && latest.jvmHeapMaxBytes > 0
      ? Number(((latest.jvmHeapUsedBytes / latest.jvmHeapMaxBytes) * 100).toFixed(1))
      : 0
  const gcPauseSum = latest?.gcPauseSecondsSum ?? 0
  const uptimeMinutes = current ? Math.round(current.processUptimeSeconds / 60) : 0

  const heapUsageTone = getTone(heapUsagePct, DEFAULT_THRESHOLDS.JVM_HEAP_USAGE.warn, DEFAULT_THRESHOLDS.JVM_HEAP_USAGE.crit)
  const oldGenTone = getTone(
    latest && latest.jvmHeapMaxBytes > 0 ? (latest.jvmOldGenUsedBytes / latest.jvmHeapMaxBytes) * 100 : 0,
    DEFAULT_THRESHOLDS.JVM_OLD_GEN_USAGE.warn,
    DEFAULT_THRESHOLDS.JVM_OLD_GEN_USAGE.crit,
  )
  const gcPauseTone = getTone(gcPauseSum, DEFAULT_THRESHOLDS.GC_PAUSE_TIME.warn, DEFAULT_THRESHOLDS.GC_PAUSE_TIME.crit)
  const blockedThreadsTone = threadsBlocked > 0 ? 'warn' : 'default'

  const memoryTimeline = series.map((p) => ({
    time: shortTime(p.collectedAt),
    heapMb: formatMb(p.jvmHeapUsedBytes),
    oldGenMb: formatMb(p.jvmOldGenUsedBytes),
    gcPauseS: Number((p.gcPauseSecondsSum ?? 0).toFixed(3)),
    liveThreads: p.threadsLive ?? threadsLive,
  }))

  return (
    <div className="flex-1 flex flex-col min-h-0 gap-4 h-full">
      {/* 상단 6개 슬림 메트릭 카드 */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6 shrink-0">
        <Card className="p-3">
          <div className="text-[11px] text-slate-500">Heap 사용률</div>
          <div className={`mt-0.5 text-xl font-bold ${heapUsageTone === 'danger' ? 'text-danger-600' : heapUsageTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {heapUsagePct}%
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400 truncate">
            {heapUsedMb} / {heapMaxMb} MB
          </div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">Old Gen 메모리</div>
          <div className={`mt-0.5 text-xl font-bold ${oldGenTone === 'danger' ? 'text-danger-600' : oldGenTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {oldGenUsedMb} <span className="text-xs font-normal text-slate-400">MB</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">장기 객체 영역</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">GC 정지 누적</div>
          <div className={`mt-0.5 text-xl font-bold ${gcPauseTone === 'danger' ? 'text-danger-600' : gcPauseTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {gcPauseSum.toFixed(3)} <span className="text-xs font-normal text-slate-400">s</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">STW 총합</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">활성 스레드 (Live)</div>
          <div className="mt-0.5 text-xl font-bold text-slate-900">
            {threadsLive} <span className="text-xs font-normal text-slate-400">개</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">JVM 실행 스레드</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">블록 스레드 (Blocked)</div>
          <div className={`mt-0.5 text-xl font-bold ${blockedThreadsTone === 'warn' ? 'text-warn-600' : 'text-slate-900'}`}>
            {threadsBlocked} <span className="text-xs font-normal text-slate-400">개</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">동기화 경합 상태</div>
        </Card>

        <Card className="p-3">
          <div className="text-[11px] text-slate-500">프로세스 가동 시간</div>
          <div className="mt-0.5 text-xl font-bold text-slate-900">
            {uptimeMinutes} <span className="text-xs font-normal text-slate-400">분</span>
          </div>
          <div className="mt-0.5 text-[10px] text-slate-400">애플리케이션 가동</div>
        </Card>
      </div>

      {/* 하단 듀얼 차트 영역 (세로 100% 꽉 참) */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 flex-1 min-h-0 h-full">
        {/* 차트 1: Heap & Old Gen 메모리 사용량 추이 */}
        <Card className="p-4 flex flex-col justify-between flex-1 min-h-0 h-full shadow-sm hover:shadow-md transition-shadow">
          <h3 className="mb-2 text-sm font-semibold text-slate-800 shrink-0">
            Heap &amp; Old Gen 메모리 사용량 추이 <span className="text-xs font-normal text-slate-400">(MB)</span>
          </h3>
          <div className="flex-1 w-full min-h-[180px] pt-1">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={memoryTimeline} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="grad-heap" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#9b8ac1" stopOpacity={0.25} />
                    <stop offset="100%" stopColor="#9b8ac1" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="grad-oldgen" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#c8922f" stopOpacity={0.2} />
                    <stop offset="100%" stopColor="#c8922f" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} interval="preserveStartEnd" minTickGap={45} />
                <YAxis tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={40} />
                <Tooltip content={<CustomChartTooltip />} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Area
                  type="monotone"
                  dataKey="heapMb"
                  name="Heap Used (MB)"
                  stroke="#9b8ac1"
                  strokeWidth={2}
                  fill="url(#grad-heap)"
                  dot={false}
                  connectNulls={true}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                />
                <Area
                  type="monotone"
                  dataKey="oldGenMb"
                  name="Old Gen Used (MB)"
                  stroke="#c8922f"
                  strokeWidth={2}
                  fill="url(#grad-oldgen)"
                  dot={false}
                  connectNulls={true}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </Card>

        {/* 차트 2: 스레드 및 GC 정지 시간 복합 추이 */}
        <Card className="p-4 flex flex-col justify-between flex-1 min-h-0 h-full shadow-sm hover:shadow-md transition-shadow">
          <h3 className="mb-2 text-sm font-semibold text-slate-800 shrink-0">
            활성 스레드 &amp; GC 정지 시간 추이
          </h3>
          <div className="flex-1 w-full min-h-[180px] pt-1">
            <ResponsiveContainer width="100%" height="100%">
              <ComposedChart data={memoryTimeline} margin={{ top: 4, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey="time" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} interval="preserveStartEnd" minTickGap={45} />
                <YAxis yAxisId="threads" tick={{ fontSize: 10, fill: '#94a3b8' }} allowDecimals={false} tickLine={false} axisLine={false} width={36} />
                <YAxis yAxisId="gc" orientation="right" tick={{ fontSize: 10, fill: '#94a3b8' }} tickLine={false} axisLine={false} width={40} />
                <Tooltip content={<CustomChartTooltip />} />
                <Legend wrapperStyle={{ fontSize: 11 }} />
                <Area
                  yAxisId="threads"
                  type="monotone"
                  dataKey="liveThreads"
                  name="Live Threads (개)"
                  stroke="#5b7fa6"
                  strokeWidth={2}
                  fill="#5b7fa6"
                  fillOpacity={0.12}
                  dot={false}
                  connectNulls={true}
                  activeDot={{ r: 4, stroke: '#fff', strokeWidth: 2 }}
                />
                <Line
                  yAxisId="gc"
                  type="monotone"
                  dataKey="gcPauseS"
                  name="GC Pause Sum (s)"
                  stroke="#e11d48"
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
      </div>
    </div>
  )
}
