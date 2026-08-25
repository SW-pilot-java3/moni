export type Tone = 'default' | 'warn' | 'danger'

/**
 * 측정값이 임계치를 초과할 때 경고/위험 색상을 반환합니다.
 * - value >= crit : 'danger' (빨간색)
 * - value >= warn : 'warn' (주황색)
 * - value < warn  : 'default' (정상/기본 색상)
 */
export function getTone(value: number | null | undefined, warn: number, crit: number): Tone {
  if (value === null || value === undefined || isNaN(value)) return 'default'
  if (value >= crit) return 'danger'
  if (value >= warn) return 'warn'
  return 'default'
}

/**
 * 인스턴스/서버 기본 임계치 기준값
 */
export const DEFAULT_THRESHOLDS = {
  // Server Thresholds
  HTTP_AVG_LATENCY: { warn: 200, crit: 500 },     // ms
  HTTP_ERROR_RATE: { warn: 1.0, crit: 5.0 },      // %
  JVM_HEAP_USAGE: { warn: 75.0, crit: 90.0 },     // %
  JVM_OLD_GEN_USAGE: { warn: 80.0, crit: 95.0 },  // %
  GC_PAUSE_TIME: { warn: 0.5, crit: 1.0 },        // seconds
  HIKARICP_POOL_USAGE: { warn: 80.0, crit: 95.0 },// %
  THREADPOOL_QUEUE_USAGE: { warn: 70.0, crit: 90.0 }, // %

  // Instance Thresholds
  CPU_USAGE: { warn: 70.0, crit: 90.0 },          // %
  MEM_USAGE: { warn: 80.0, crit: 95.0 },          // %
  DISK_USAGE: { warn: 80.0, crit: 90.0 },         // %
  DISK_LATENCY: { warn: 20.0, crit: 50.0 },       // ms
  NET_ERROR_RATE: { warn: 1.0, crit: 5.0 },       // %
} as const

/**
 * 정수는 그대로, 소수점이 있는 평균 수치는 최대 1자리까지 깔끔하게 포맷팅합니다.
 * 예: 21.099999999999998 -> "21.1", 21 -> "21"
 */
export function formatCount(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(val)) return '—'
  return Number.isInteger(val) ? String(val) : Number(val.toFixed(1)).toString()
}


