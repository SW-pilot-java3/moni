export type InstanceStatus = 'normal' | 'warning' | 'critical' | 'unreachable' | 'waiting'
export type AppStatus = 'connected' | 'warning' | 'critical' | 'waiting'

export interface AppSummary {
  name: string
  port: number | null
  status: AppStatus
  heapUsage?: number
  heapLimitGb?: number
  gcMs?: number
  hikariActive?: number
  hikariMax?: number
  hikariPending?: number
  httpRps?: number | null
  httpAvgMs?: number | null
  http5xx?: number
  topApi?: string
  registeredAt: string
  lastSeen: string | null
}

export interface InstanceSummary {
  id: string
  name: string
  status: InstanceStatus
  region: string
  type: string
  cpu: number | null
  mem: number | null
  netMbps: number | null
  netInMbps: number | null
  netOutMbps: number | null
  diskUsagePct: number | null
  diskFreeGb: number | null
  diskIoMbps: number | null
  uptimeDays: number | null
  uptimeHours: number | null
  loadAvg1m: number | null
  vcpu: number | null
  memTotalGb: number | null
  memUsedGb: number | null
  swapGb: number | null
  apiKey: string
  apiKeyIssuedAt: string
  lastSeen: string | null
  apps: AppSummary[]
}

export const instances: InstanceSummary[] = [
  {
    id: 'ins_8f21c0',
    name: 'prod-ec2-a',
    status: 'normal',
    region: 'ap-northeast-2',
    type: 't3.large',
    cpu: 42,
    mem: 61,
    netMbps: 3.2,
    netInMbps: 2.1,
    netOutMbps: 1.1,
    diskUsagePct: 58,
    diskFreeGb: 47,
    diskIoMbps: 18,
    uptimeDays: 28,
    uptimeHours: 4,
    loadAvg1m: 1.42,
    vcpu: 2,
    memTotalGb: 8,
    memUsedGb: 4.9,
    swapGb: 0,
    apiKey: 'ins_8f21c0_sk_live_a1b2c3d4',
    apiKeyIssuedAt: '2026-07-12',
    lastSeen: '3초 전',
    apps: [
      {
        name: 'order-api',
        port: 8080,
        status: 'connected',
        heapUsage: 60,
        heapLimitGb: 2,
        gcMs: 32,
        hikariActive: 7,
        hikariMax: 20,
        hikariPending: 0,
        httpRps: 128,
        httpAvgMs: 84,
        http5xx: 0,
        topApi: '/api/orders · 62/s',
        registeredAt: '07-12',
        lastSeen: '3초 전',
      },
      {
        name: 'payment-api',
        port: 8081,
        status: 'critical',
        heapUsage: 87,
        heapLimitGb: 4,
        gcMs: 210,
        hikariActive: 19,
        hikariMax: 20,
        hikariPending: 6,
        httpRps: 87,
        httpAvgMs: 340,
        http5xx: 190,
        topApi: '/api/payments · 42/s',
        registeredAt: '방금',
        lastSeen: '2초 전',
      },
      {
        name: 'batch-worker',
        port: null,
        status: 'connected',
        heapUsage: 40,
        heapLimitGb: 2,
        gcMs: 18,
        hikariActive: 3,
        hikariMax: 10,
        hikariPending: 0,
        httpRps: null,
        httpAvgMs: null,
        http5xx: 0,
        topApi: undefined,
        registeredAt: '07-14',
        lastSeen: '4초 전',
      },
    ],
  },
  {
    id: 'ins_3ad901',
    name: 'dev-ec2-b',
    status: 'unreachable',
    region: 'ap-northeast-2',
    type: 't3.medium',
    cpu: null,
    mem: null,
    netMbps: null,
    netInMbps: null,
    netOutMbps: null,
    diskUsagePct: null,
    diskFreeGb: null,
    diskIoMbps: null,
    uptimeDays: null,
    uptimeHours: null,
    loadAvg1m: null,
    vcpu: null,
    memTotalGb: null,
    memUsedGb: null,
    swapGb: null,
    apiKey: 'ins_3ad901_sk_live_e5f6g7h8',
    apiKeyIssuedAt: '2026-07-20',
    lastSeen: null,
    apps: [
      {
        name: 'admin-web',
        port: null,
        status: 'connected',
        registeredAt: '07-20',
        lastSeen: '6초 전',
      },
      {
        name: 'notify-api',
        port: null,
        status: 'waiting',
        registeredAt: '07-22',
        lastSeen: null,
      },
    ],
  },
]

export interface EndpointStat {
  method: string
  path: string
  count: number
  share: number
  avgMs: number
  p95Ms: number
  errorRate: number
}

export const paymentEndpoints: EndpointStat[] = [
  { method: 'POST', path: '/api/payments', count: 12480, share: 0.48, avgMs: 312, p95Ms: 840, errorRate: 1.8 },
  { method: 'GET', path: '/api/payments/{id}', count: 8120, share: 0.31, avgMs: 64, p95Ms: 180, errorRate: 0.1 },
  { method: 'POST', path: '/api/refunds', count: 2940, share: 0.11, avgMs: 1240, p95Ms: 3100, errorRate: 6.4 },
  { method: 'GET', path: '/api/methods', count: 1510, share: 0.06, avgMs: 28, p95Ms: 72, errorRate: 0 },
  { method: 'GET', path: '/actuator/health', count: 960, share: 0.04, avgMs: 4, p95Ms: 9, errorRate: 0 },
]

export interface ThresholdRow {
  key: string
  label: string
  unit: string
  current: number
  warn: number
  critical: number
  state: 'default' | 'changed' | 'error'
}

export const instanceThresholds: ThresholdRow[] = [
  { key: 'CPU_USAGE', label: 'CPU 사용률', unit: '%', current: 42, warn: 85, critical: 95, state: 'default' },
  { key: 'MEMORY_USAGE', label: '메모리 사용률', unit: '%', current: 61, warn: 75, critical: 90, state: 'changed' },
  { key: 'SWAP_USAGE', label: '스왑 사용률', unit: '%', current: 0, warn: 10, critical: 30, state: 'default' },
  { key: 'DISK_USAGE', label: '디스크 사용률', unit: '%', current: 67, warn: 80, critical: 90, state: 'default' },
  { key: 'DISK_IO_UTIL', label: '디스크 활용도', unit: '%', current: 18, warn: 95, critical: 80, state: 'error' },
  { key: 'LOAD_AVG_1M', label: 'Load Average (1m)', unit: '코어당', current: 0.71, warn: 1.0, critical: 2.0, state: 'default' },
]

export const appThresholds: ThresholdRow[] = [
  { key: 'HEAP_USAGE', label: '힙 사용률', unit: '%', current: 87, warn: 80, critical: 92, state: 'default' },
  { key: 'GC_OVERHEAD', label: 'GC 오버헤드', unit: '%', current: 0.4, warn: 2, critical: 5, state: 'default' },
  { key: 'RESPONSE_TIME_AVG', label: '평균 응답시간', unit: 'ms', current: 84, warn: 300, critical: 1000, state: 'changed' },
  { key: 'ERROR_RATE', label: '에러율', unit: '%', current: 0.7, warn: 1, critical: 5, state: 'default' },
  { key: 'POOL_USAGE', label: '커넥션 풀 사용률', unit: '%', current: 35, warn: 80, critical: 95, state: 'default' },
  { key: 'POOL_PENDING', label: '커넥션 대기 스레드', unit: '개', current: 0, warn: 1, critical: 5, state: 'default' },
  { key: 'WAS_THREAD_USAGE', label: 'WAS 스레드 사용률', unit: '%', current: 4, warn: 70, critical: 90, state: 'default' },
]

export interface TimePoint {
  time: string
  rps: number
  latencyMs: number
}

export const orderApiTimeline: TimePoint[] = [
  { time: '14:00', rps: 45, latencyMs: 160 },
  { time: '14:01', rps: 60, latencyMs: 220 },
  { time: '14:02', rps: 110, latencyMs: 480 },
  { time: '14:03', rps: 87, latencyMs: 340 },
  { time: '14:04 (Live)', rps: 87, latencyMs: 340 },
]

export interface HeapPoint {
  time: string
  heapMb: number
}

export const jvmHeapTimeline: HeapPoint[] = [
  { time: '14:00', heapMb: 720 },
  { time: '14:01', heapMb: 850 },
  { time: '14:02', heapMb: 995 },
  { time: '14:03 (Live)', heapMb: 675 },
]

export interface PoolPoint {
  time: string
  active: number
}

export const hikariTimeline: PoolPoint[] = [
  { time: '14:00', active: 2 },
  { time: '14:01', active: 3 },
  { time: '14:02', active: 5 },
  { time: '14:03 (Live)', active: 6 },
]

export interface ThreadPoolPoint {
  time: string
  active: number
}

export const threadPoolTimeline: ThreadPoolPoint[] = [
  { time: '14:00', active: 1 },
  { time: '14:01', active: 2 },
  { time: '14:02', active: 4 },
  { time: '14:03 (Live)', active: 4 },
]

export const overviewSummary = {
  http: { rps: 87.0, avgMs: 340, errorRate: 0.73 },
  jvm: { heapUsage: 68.4, oldGenMb: 384, gcPauseS: 0.045, gcCount: 3 },
  hikari: { active: 6, max: 10, idle: 4, pending: 0 },
  threadPool: { active: 4, max: 10, queued: 2, remaining: 98 },
}

export const httpApiSummary = {
  totalRps: 87,
  avg5minRps: 78,
  avgLatencyMs: 340,
  latencyVsNormal: '정상 대비 4배 지연',
  errorRate: 0.73,
  error5xxCount: 180,
  maxLatencyMs: 3100,
  maxLatencyEndpoint: 'POST /api/refunds',
}

export interface EndpointDetailStat {
  method: string
  path: string
  status: number
  rps: number
  avgMs: number
  errorRate: number
}

export const orderApiEndpoints: EndpointDetailStat[] = [
  { method: 'POST', path: '/api/payments', status: 201, rps: 65.0, avgMs: 312, errorRate: 1.8 },
  { method: 'GET', path: '/api/payments/{id}', status: 200, rps: 22.5, avgMs: 64, errorRate: 0.1 },
  { method: 'POST', path: '/api/refunds', status: 201, rps: 8.0, avgMs: 1240, errorRate: 6.4 },
]

export const selectedEndpointDetail = {
  method: 'POST',
  path: '/api/payments',
  rps: 65.0,
  avgMs: 312,
  errorRate: 1.8,
  maxMs: 840.0,
  timeline: [
    { time: '14:00', rps: 50, latencyMs: 160 },
    { time: '14:01', rps: 55, latencyMs: 190 },
    { time: '14:02', rps: 60, latencyMs: 260 },
    { time: '14:03', rps: 72, latencyMs: 400 },
    { time: '14:04', rps: 65, latencyMs: 300 },
    { time: '14:05', rps: 65, latencyMs: 300 },
    { time: '14:06 (Live)', rps: 65, latencyMs: 300 },
  ],
}

export interface HistoryPoint {
  date: string
  value: number
}

export interface HistoryQueryResult {
  instanceName: string
  metricLabel: string
  rangeLabel: string
  rangeStart: string
  rangeEnd: string
  points: HistoryPoint[]
  avg: number
  max: number
  maxAt: string
  min: number
}

export const cpuHistory: HistoryQueryResult = {
  instanceName: 'prod-ec2-a',
  metricLabel: 'CPU 사용률',
  rangeLabel: '7일',
  rangeStart: '2026-08-01',
  rangeEnd: '2026-08-07',
  points: [
    { date: '08-01', value: 24 },
    { date: '08-02', value: 31 },
    { date: '08-03', value: 45 },
    { date: '08-04', value: 58 },
    { date: '08-05', value: 93 },
    { date: '08-06', value: 52 },
    { date: '08-07', value: 46 },
  ],
  avg: 46,
  max: 93,
  maxAt: '08-05 14:20',
  min: 11,
}