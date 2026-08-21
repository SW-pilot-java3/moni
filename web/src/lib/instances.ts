import { api, buildSseUrl } from './api'

export type InstanceApiStatus = 'CONNECTED' | 'DISCONNECTED'
export type ServerApiStatus = 'CONNECTED' | 'DISCONNECTED'

export interface InstanceListItem {
  instanceId: number
  name: string
  ip: string
  status: InstanceApiStatus
  serverCount: number
  lastReceivedAt: string | null
  createdAt: string
}

export interface ServerItem {
  serverId: number
  instanceId: number
  name: string
  port: number | null
  status: ServerApiStatus
  lastReceivedAt: string | null
  createdAt: string
}

export interface InstanceCreateResult {
  instanceId: number
  name: string
  ip: string
  status: InstanceApiStatus
  lastReceivedAt: string | null
  createdAt: string
}

export function getInstances() {
  return api.get<InstanceListItem[]>('/api/v1/instances')
}

export function getServers(instanceId: number) {
  return api.get<ServerItem[]>(`/api/v1/servers?instanceId=${instanceId}`)
}

export function createInstance(name: string, ip: string) {
  return api.post<InstanceCreateResult>('/api/v1/instances', { name, ip })
}

export type InstanceMetricKey = 'CPU_USAGE' | 'MEM_USAGE' | 'DISK_USAGE' | 'DISK_LATENCY' | 'NET_ERROR_RATE'

export interface InstanceThresholdItem {
  metricKey: InstanceMetricKey
  warningValue: number
  criticalValue: number
  isCustomized: boolean
}

export function getInstanceThresholds(instanceId: number) {
  return api.get<InstanceThresholdItem[]>(`/api/v1/instances/${instanceId}/thresholds`)
}

export function updateInstanceThresholds(
  instanceId: number,
  thresholds: { metricKey: InstanceMetricKey; warningValue: number; criticalValue: number }[],
) {
  return api.put<{ instanceId: number; updatedCount: number; updatedAt: string }>(
    `/api/v1/instances/${instanceId}/thresholds`,
    { thresholds },
  )
}

export interface InstanceRealtimeMetricPoint {
  collectedAt: string
  cpuUsagePct: number | null
  memAvailableBytes: number | null
}

export function getInstanceRealtimeMetrics(instanceId: number) {
  return api.get<InstanceRealtimeMetricPoint[]>(`/api/v1/instances/${instanceId}/metrics/realtime`)
}

export interface InstanceSseStreamEvent {
  instanceId: number
  collectedAt: string
  cpuUsagePct: number | null
  memAvailableBytes: number | null
}

export function subscribeInstanceMetricStream(
  instanceId: number,
  onMetric: (event: InstanceSseStreamEvent) => void,
  onError?: () => void,
): () => void {
  const source = new EventSource(buildSseUrl(`/api/v1/instances/${instanceId}/stream`))

  source.addEventListener('instance_metric', (e) => {
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
