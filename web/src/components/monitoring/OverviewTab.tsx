import MiniAreaCard from './MiniAreaCard'

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
  return (
    <div className="grid grid-cols-2 gap-4">
      <MiniAreaCard
        icon="🌐"
        title="HTTP · API 처리 성능"
        onDetail={() => onNavigateTab('http')}
        stats={[
          { label: 'RPS', value: `${http.rps.toFixed(1)} req/s` },
          { label: '평균 응답', value: `${http.avgMs.toFixed(0)} ms`, tone: 'warn' },
          { label: '오류율', value: `${http.errorRate.toFixed(1)}%`, tone: 'danger' },
        ]}
        data={rpsLatencyTimeline}
        dataKey="rps"
        color="#5b7fa6"
      />

      <MiniAreaCard
        icon="🩷"
        title="JVM 메모리 & GC 상태"
        onDetail={() => onNavigateTab('jvm')}
        stats={[
          { label: 'Heap 사용률', value: `${jvm.heapUsage}%` },
          { label: 'Old Gen', value: `${jvm.oldGenMb} MB`, tone: 'warn' },
          { label: 'GC 정지(누적)', value: `${jvm.gcPauseS.toFixed(3)}s` },
        ]}
        data={heapTimeline}
        dataKey="heapMb"
        color="#9b8ac1"
      />

      <MiniAreaCard
        icon="🗄️"
        title="HikariCP DB 커넥션 풀"
        onDetail={() => onNavigateTab('hikari')}
        stats={[
          { label: 'Active / Max', value: `${hikari.active} / ${hikari.max}` },
          { label: 'Idle 유휴', value: `${hikari.idle} 개` },
          { label: 'Pending 대기', value: `${hikari.pending} 개` },
        ]}
        data={hikariTimeline}
        dataKey="active"
        color="#4a8c6f"
      />

      <MiniAreaCard
        icon="⚡"
        title="ThreadPool 비동기 스레드"
        onDetail={() => onNavigateTab('threadpool')}
        stats={[
          { label: 'Active 스레드', value: `${threadPool.active} / ${threadPool.max}`, tone: 'warn' },
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