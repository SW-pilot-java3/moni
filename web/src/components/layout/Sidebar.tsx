import { NavLink } from 'react-router-dom'

const navItems = [
  { to: '/dashboard', label: '대시보드' },
  { to: '/instances/new', label: '인스턴스 등록' },
  { to: '/apps/link', label: '앱 연동' },
  { to: '/monitoring', label: '실시간 모니터링' },
  { to: '/history', label: '과거 데이터 조회' },
  { to: '/manage', label: '인스턴스 · 앱 관리' },
]

export default function Sidebar() {
  return (
    <aside className="w-60 shrink-0 border-r border-slate-200 bg-white px-4 py-6">
      <div className="mb-8 px-2 font-mono text-lg font-bold tracking-wide text-brand-500">
        MONITORING
      </div>
      <nav className="flex flex-col gap-1">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `rounded-md px-3 py-2.5 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-brand-50 text-brand-700'
                  : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
              }`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}