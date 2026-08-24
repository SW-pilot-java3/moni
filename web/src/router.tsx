import { createBrowserRouter, Navigate } from 'react-router-dom'
import AppLayout from './components/layout/AppLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import InstanceRegisterPage from './pages/InstanceRegisterPage'
import AppLinkPage from './pages/AppLinkPage'
import MonitoringPage from './pages/MonitoringPage'
import HistoryPage from './pages/HistoryPage'
import ThresholdPage from './pages/ThresholdPage'

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/login" replace /> },
  { path: '/login', element: <LoginPage /> },
  {
    element: <AppLayout />,
    children: [
      { path: '/dashboard', element: <DashboardPage /> },
      { path: '/instances/new', element: <InstanceRegisterPage /> },
      { path: '/apps/link', element: <AppLinkPage /> },
      { path: '/monitoring', element: <MonitoringPage /> },
      { path: '/history', element: <HistoryPage /> },
      { path: '/manage', element: <Navigate to="/dashboard" replace /> },
      { path: '/hub', element: <Navigate to="/dashboard" replace /> },
      { path: '/thresholds/:scope?', element: <ThresholdPage /> },
    ],
  },
])