import { api } from './api'

export interface LoginResponse {
  tokenType: string
  accessToken: string
  expiresIn: number
  user: { userId: number; email: string }
}

export interface SignupResponse {
  userId: number
  email: string
  createdAt: string
}

export function login(email: string, password: string) {
  return api.post<LoginResponse>('/api/v1/auth/login', { email, password })
}

export function signup(email: string, password: string) {
  return api.post<SignupResponse>('/api/v1/auth/signup', { email, password })
}

export function logout() {
  return api.post<void>('/api/v1/auth/logout')
}

export function saveSession(res: LoginResponse) {
  localStorage.setItem('accessToken', res.accessToken)
  localStorage.setItem('userEmail', res.user.email)
}

export function clearSession() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('userEmail')
}

export function isLoggedIn() {
  return localStorage.getItem('accessToken') !== null
}