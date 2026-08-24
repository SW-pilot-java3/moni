import MiniAreaCard from './MiniAreaCard'
import { getTone, DEFAULT_THRESHOLDS } from '../../lib/thresholdUtils'

interface TimePoint {
  time: string
  rps: number
  latencyMs: number
}
interface HeapPoint {
  time: string
  heapMb: number
}
interface CountPoint {
  time: string
  active: number
}

export default function OverviewTab({
  onNavigateTab,
  rpsLatencyTimeline,
  heapTimeline,
  hikariTimeline,
  executorTimeline,
  http,
  jvm,
  hikari,
  threadPool,
}: {
  onNavigateTab: (tab: string) => void
  rpsLatencyTimeline: TimePoint[]
  heapTimeline: HeapPoint[]
  hikariTimeline: CountPoint[]
  executorTimeline: CountPoint[]
  http: { rps: number; avgMs: number; errorRate: number }
  jvm: { heapUsage: string; oldGenMb: number; gcPauseS: number }
  hikari: { active: number; max: number; idle: number; pending: number }
  threadPool: { active: number; max: number | string; queued: number; remaining: number | string }
}) {
  const heapUsageNum = parseFloat(jvm.heapUsage) || 0
  const hikariUsagePct = hikari.max > 0 ? (hikari.active / hikari.max) * 100 : 0
  const execMaxNum = typeof threadPool.max === 'number' ? threadPool.max : parseFloat(String(threadPool.max)) || 0
  const execUsagePct = execMaxNum > 0 ? (threadPool.active / execMaxNum) * 100 : 0

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 flex-1 min-h-0 lg:grid-rows-2 h-full">
      <MiniAreaCard
        title="HTTP · API 처리 성능"
        onDetail={() => onNavigateTab('http')}
        stats={[
          { label: 'RPS', value: `${http.rps.toFixed(1)} req/s` },
          {
            label: '평균 응답',
            value: `${http.avgMs.toFixed(0)} ms`,
            tone: getTone(http.avgMs, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.warn, DEFAULT_THRESHOLDS.HTTP_AVG_LATENCY.crit),
          },
          {
            label: '오류율',
            value: `${http.errorRate.toFixed(1)}%`,
            tone: getTone(http.errorRate, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.warn, DEFAULT_THRESHOLDS.HTTP_ERROR_RATE.crit),
          },
        ]}
        data={rpsLatencyTimeline}
        dataKey="rps"
        color="#5b7fa6"
      />

      <MiniAreaCard
        title="JVM 메모리 & GC 상태"
        onDetail={() => onNavigateTab('jvm')}
        stats={[
          {
            label: 'Heap 사용률',
            value: `${jvm.heapUsage}%`,
            tone: getTone(heapUsageNum, DEFAULT_THRESHOLDS.JVM_HEAP_USAGE.warn, DEFAULT_THRESHOLDS.JVM_HEAP_USAGE.crit),
          },
          { label: 'Old Gen', value: `${jvm.oldGenMb} MB` },
          {
            label: 'GC 정지(누적)',
            value: `${jvm.gcPauseS.toFixed(3)}s`,
            tone: getTone(jvm.gcPauseS, DEFAULT_THRESHOLDS.GC_PAUSE_TIME.warn, DEFAULT_THRESHOLDS.GC_PAUSE_TIME.crit),
          },
        ]}
        data={heapTimeline}
        dataKey="heapMb"
        color="#9b8ac1"
      />

      <MiniAreaCard
        title="HikariCP DB 커넥션 풀"
        onDetail={() => onNavigateTab('hikari')}
        stats={[
          {
            label: 'Active / Max',
            value: `${hikari.active} / ${hikari.max}`,
            tone: getTone(hikariUsagePct, DEFAULT_THRESHOLDS.HIKARICP_POOL_USAGE.warn, DEFAULT_THRESHOLDS.HIKARICP_POOL_USAGE.crit),
          },
          { label: 'Idle 유휴', value: `${hikari.idle} 개` },
          { label: 'Pending 대기', value: `${hikari.pending} 개` },
        ]}
        data={hikariTimeline}
        dataKey="active"
        color="#4a8c6f"
      />

      <MiniAreaCard
        title="ThreadPool 비동기 스레드"
        onDetail={() => onNavigateTab('threadpool')}
        stats={[
          {
            label: 'Active 스레드',
            value: `${threadPool.active} / ${threadPool.max}`,
            tone: getTone(execUsagePct, DEFAULT_THRESHOLDS.THREADPOOL_QUEUE_USAGE.warn, DEFAULT_THRESHOLDS.THREADPOOL_QUEUE_USAGE.crit),
          },
          { label: 'Queued 대기', value: `${threadPool.queued} 건` },
          { label: '남은 용량', value: `${threadPool.remaining} 건` },
        ]}
        data={executorTimeline}
        dataKey="active"
        color="#c8922f"
      />
    </div>
  )
}