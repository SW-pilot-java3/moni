import { api, buildSseUrl } from './api'
import type { ServerApiStatus, ServerItem } from './instances'

export interface ServerCreateResult {
  serverId: number
  instanceId: number
  name: string
  port: number | null
  status: ServerApiStatus
  lastReceivedAt: string | null
  createdAt: string
}

export interface ApiKeyCreateResult {
  apiKeyId: number
  serverId: number
  apiKey: string
  createdAt: string
}

export interface ApiKeyStatus {
  hasApiKey: boolean
  createdAt: string | null
  revokedAt: string | null
}

export function createServer(instanceId: number, name: string, port: number) {
  return api.post<ServerCreateResult>('/api/v1/servers', { instanceId, name, port })
}

export function deleteServer(serverId: number) {
  return api.delete<void>(`/api/v1/servers/${serverId}`)
}

export function createApiKey(serverId: number) {
  return api.post<ApiKeyCreateResult>(`/api/v1/servers/${serverId}/api-keys`)
}

export function getApiKeyStatus(serverId: number) {
  return api.get<ApiKeyStatus>(`/api/v1/servers/${serverId}/api-keys/status`)
}

export type ServerMetricKey =
  | 'JVM_HEAP_USAGE'
  | 'JVM_OLD_GEN_USAGE'
  | 'GC_PAUSE_TIME'
  | 'HTTP_AVG_LATENCY'
  | 'HTTP_ERROR_RATE'
  | 'HIKARICP_POOL_USAGE'
  | 'THREADPOOL_QUEUE_USAGE'

export interface ServerThresholdItem {
  metricKey: ServerMetricKey
  warningValue: number
  criticalValue: number
  isCustomized: boolean
}

export function getServerThresholds(serverId: number) {
  return api.get<ServerThresholdItem[]>(`/api/v1/servers/${serverId}/thresholds`)
}

export function updateServerThresholds(
  serverId: number,
  thresholds: { metricKey: ServerMetricKey; warningValue: number; criticalValue: number }[],
) {
  return api.patch<{ serverId: number; updatedCount: number; updatedAt: string }>(
    `/api/v1/servers/${serverId}/thresholds`,
    { thresholds },
  )
}

export interface HttpEndpointMetric {
  uri: string
  method: string
  status: string
  requestsCount: number
  rps: number
  avgLatencyMs: number
  maxLatencyMs: number
  errorRatePct: number
}

export interface HikariCpPoolMetric {
  poolName: string
  active: number
  idle: number
  pending: number
  max: number
  timeoutsTotal: number
}

export interface ExecutorMetric {
  name: string
  active: number
  max: number
  queuedTasks: number
  queueRemaining: number
}

export interface ServerRealtimeCurrent {
  collectedAt: string
  processUptimeSeconds: number
  httpEndpoints: HttpEndpointMetric[]
  hikaricpPools: HikariCpPoolMetric[]
  executors: ExecutorMetric[]
}

export interface ServerRealtimeSeriesPoint {
  collectedAt: string
  jvmHeapUsedBytes: number
  jvmHeapMaxBytes: number
  jvmOldGenUsedBytes: number
  gcPauseSecondsSum: number
  totalRps: number
  avgLatencyMs: number
  hikaricpActiveTotal: number
  executorActiveTotal: number
}

export interface ServerRealtimeMetrics {
  current: ServerRealtimeCurrent | null
  series: ServerRealtimeSeriesPoint[]
}

export function getServerRealtimeMetrics(serverId: number, limit = 30) {
  return api.get<ServerRealtimeMetrics>(`/api/v1/servers/${serverId}/metrics/realtime?limit=${limit}`)
}

export interface ServerSseHttpMetric {
  uri: string
  method: string
  requestsCount: number
  rps: number
  avgLatencyMs: number
  maxLatencyMs: number
  errorRatePct: number
}

export interface ServerSseHikariMetric {
  poolName: string
  active: number
  idle: number
  pending: number
  max: number
}

export interface ServerSseExecutorMetric {
  name: string
  active: number
  max: number
  queuedTasks: number
  queueRemaining: number
}

export interface ServerSseStreamEvent {
  serverId: number
  collectedAt: string
  summary: {
    totalRps: number
    avgLatencyMs: number
    jvmHeapUsedBytes: number
    jvmHeapMaxBytes: number
    hikaricpActiveTotal: number
    hikaricpMaxTotal: number
    executorActiveTotal: number
    executorMaxTotal: number
  }
  jvm: {
    heapUsedBytes: number
    heapMaxBytes: number
    threadsLive: number
    threadsBlocked: number
    gcPauseSecondsSum: number
  }
  httpEndpoints: ServerSseHttpMetric[]
  hikaricpPools: ServerSseHikariMetric[]
  executors: ServerSseExecutorMetric[]
}

export function subscribeServerMetricStream(
  serverId: number,
  onMetric: (event: ServerSseStreamEvent) => void,
  onError?: () => void,
): () => void {
  const source = new EventSource(buildSseUrl(`/api/v1/servers/${serverId}/stream`))

  source.addEventListener('server_metric', (e) => {
    try {
      onMetric(JSON.parse((e as MessageEvent).data))
    } catch {
      // 잘못된 이벤트 페이로드는 무시
    }
  })

  if (onError) {
    source.onerror = onError
  }

  return () => source.close()
}

export type { ServerItem }