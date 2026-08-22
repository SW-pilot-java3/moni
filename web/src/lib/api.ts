export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export interface ApiErrorBody {
  code: string
  message: string
  errors?: { field: string; value: string; reason: string }[]
}

export class ApiError extends Error {
  code: string
  status: number
  fieldErrors: { field: string; value: string; reason: string }[]

  constructor(status: number, body: ApiErrorBody) {
    super(body.message)
    this.code = body.code
    this.status = status
    this.fieldErrors = body.errors ?? []
  }
}

interface ApiSuccessEnvelope<T> {
  success: true
  data: T
}

interface ApiErrorEnvelope {
  success: false
  error: ApiErrorBody
}

// GlobalExceptionHandler가 ApiResponse로 감싸지 않고 그대로 내려주는 에러 응답 형태
interface RawErrorResponse {
  code: string
  message: string
  errors?: { field: string; value: string; reason: string }[]
}

type ApiEnvelope<T> = ApiSuccessEnvelope<T> | ApiErrorEnvelope | RawErrorResponse

function isRawErrorResponse(body: unknown): body is RawErrorResponse {
  return typeof body === 'object' && body !== null && 'code' in body && 'message' in body && !('success' in body)
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem('accessToken')

  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  })

  const body = (await res.json()) as ApiEnvelope<T>

  if (isRawErrorResponse(body)) {
    throw new ApiError(res.status, body)
  }

  if (!res.ok || !body.success) {
    const errorBody = !body.success ? body.error : { code: 'UNKNOWN', message: '알 수 없는 오류가 발생했습니다.' }
    throw new ApiError(res.status, errorBody)
  }

  return body.data
}

export const api = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  delete: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
}

// EventSource는 커스텀 헤더를 지원하지 않아 /stream 경로에 한해 쿼리 파라미터로 토큰을 싣는다.
export function buildSseUrl(path: string): string {
  const token = localStorage.getItem('accessToken')
  const url = new URL(`${API_BASE_URL}${path}`)
  if (token) {
    url.searchParams.set('token', token)
  }
  return url.toString()
}
