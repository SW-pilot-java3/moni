interface TooltipPayloadItem {
  name: string
  value: number | string
  color?: string
  fill?: string
  stroke?: string
  unit?: string
  dataKey?: string
}

interface CustomChartTooltipProps {
  active?: boolean
  payload?: TooltipPayloadItem[]
  label?: string
  unit?: string
  valueFormatter?: (val: any) => string
}

// 데이터 키별 한글 레이블 및 기본 단위 맵
const KEY_LABEL_MAP: Record<string, { label: string; unit: string }> = {
  cpuPct: { label: 'CPU 사용률', unit: '%' },
  memAvailGb: { label: '가용 메모리', unit: 'GB' },
  utilPct: { label: '디스크 점유율', unit: '%' },
  rxMBps: { label: '네트워크 수신', unit: 'MB/s' },
  txMBps: { label: '네트워크 송신', unit: 'MB/s' },
  rps: { label: '초당 요청 수 (RPS)', unit: 'req/s' },
  avgMs: { label: '평균 응답시간', unit: 'ms' },
  latencyMs: { label: '지연시간', unit: 'ms' },
  errorRate: { label: '오류율', unit: '%' },
  heapMb: { label: 'Heap 사용량', unit: 'MB' },
  oldGenMb: { label: 'Old Gen 사용량', unit: 'MB' },
  gcPauseSec: { label: 'GC 정지 시간', unit: 's' },
  liveThreads: { label: 'Live 스레드', unit: '개' },
  active: { label: '활성 커넥션', unit: '개' },
  idle: { label: '유휴 커넥션', unit: '개' },
  pending: { label: '대기 커넥션', unit: '개' },
  max: { label: '최대 풀 크기', unit: '개' },
  activeThreads: { label: '활성 스레드', unit: '개' },
  queueTasks: { label: '큐 대기 작업', unit: '건' },
  poolSize: { label: '풀 크기', unit: '개' },
}

export default function CustomChartTooltip({
  active,
  payload,
  label,
  unit,
  valueFormatter,
}: CustomChartTooltipProps) {
  if (!active || !payload || payload.length === 0) {
    return null
  }

  return (
    <div className="rounded-xl border border-slate-200/90 bg-white/95 p-2.5 text-slate-800 shadow-xl backdrop-blur-md min-w-[140px] ring-1 ring-black/5 animate-in fade-in zoom-in-95 duration-75">
      {/* 타임스탬프 헤더 */}
      {label && (
        <div className="mb-2 flex items-center justify-between border-b border-slate-100 pb-1.5 text-[11px] font-mono text-slate-400">
          <span className="flex items-center gap-1">
            <svg className="h-3 w-3 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{label}</span>
          </span>
        </div>
      )}

      {/* 지표 리스트 */}
      <div className="flex flex-col gap-1.5">
        {payload.map((item, idx) => {
          const color = item.color || item.stroke || item.fill || '#5b7fa6'
          const meta = item.dataKey ? KEY_LABEL_MAP[item.dataKey] : undefined
          const name = meta?.label || item.name || item.dataKey || '값'
          const itemUnit = unit || meta?.unit || item.unit || ''

          let displayVal = item.value
          if (valueFormatter) {
            displayVal = valueFormatter(item.value)
          } else if (typeof item.value === 'number') {
            displayVal = Number.isInteger(item.value) ? item.value : item.value.toFixed(1)
          }

          return (
            <div key={`${item.dataKey || item.name}-${idx}`} className="flex items-center justify-between gap-4 text-xs">
              {/* 좌측: 컬러 인디케이터 점 + 지표 이름 */}
              <div className="flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full shrink-0 shadow-2xs" style={{ backgroundColor: color }} />
                <span className="text-slate-600 font-medium">{name}</span>
              </div>

              {/* 우측: 수치 + 단위 */}
              <div className="font-mono font-bold text-slate-900 flex items-baseline gap-0.5">
                <span>{displayVal}</span>
                {itemUnit && <span className="text-[10px] font-normal text-slate-400">{itemUnit}</span>}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}
