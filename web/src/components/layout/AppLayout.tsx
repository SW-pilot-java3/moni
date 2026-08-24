import { Outlet } from 'react-router-dom'
import Sidebar from './Sidebar'
import { DialogProvider } from '../../lib/dialog'

export default function AppLayout() {
  return (
    <DialogProvider>
      <div className="flex h-screen overflow-hidden bg-slate-100">
        <Sidebar />
        <main className="flex-1 overflow-y-auto p-6 flex flex-col min-h-0">
          <Outlet />
        </main>
      </div>
    </DialogProvider>
  )
}