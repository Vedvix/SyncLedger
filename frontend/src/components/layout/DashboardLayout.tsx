import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuthStore } from '@/store/authStore'
import { dashboardService } from '@/services/dashboardService'
import { 
  LayoutDashboard, 
  FileText, 
  Users, 
  Settings, 
  LogOut,
  Menu,
  X,
  Shield,
  GitCompareArrows,
  Building2
} from 'lucide-react'
import { useState } from 'react'

export function DashboardLayout() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  // Fetch stats for the invoice count badge
  const { data: stats } = useQuery({
    queryKey: ['dashboardStats'],
    queryFn: () => dashboardService.getStats(),
    refetchInterval: 30000,
  })
  
  const handleLogout = () => {
    logout()
    navigate('/login')
  }
  
  const isSuperAdmin = user?.role === 'SUPER_ADMIN'
  const isAdmin = user?.role === 'ADMIN' || isSuperAdmin

  const coreNavItems = [
    { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/invoices', icon: FileText, label: 'Invoices', badge: stats?.totalInvoices },
    { to: '/vendors', icon: Building2, label: 'Vendors' },
    ...(isAdmin ? [{ to: '/users', icon: Users, label: 'Users' }] : []),
  ]

  const systemNavItems = [
    ...(isAdmin ? [{ to: '/mapping', icon: GitCompareArrows, label: 'Mapping' }] : []),
    { to: '/settings', icon: Settings, label: 'Settings' },
    ...(isSuperAdmin ? [{ to: '/super-admin', icon: Shield, label: 'Platform Admin' }] : []),
  ]

  const userInitials = `${user?.firstName?.[0] || ''}${user?.lastName?.[0] || ''}`
  const userFullName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim()
  const userRoleLabel = user?.role?.replace(/_/g, ' ') || ''
  
  return (
    <div className="min-h-screen bg-gray-100">
      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div 
          className="fixed inset-0 bg-black bg-opacity-50 z-20 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}
      
      {/* Sidebar */}
      <aside className={`
        fixed top-0 left-0 z-30 h-full w-64 transform transition-transform duration-200 ease-in-out
        lg:translate-x-0
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}
        style={{ backgroundColor: '#141414' }}
      >
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center justify-between px-6 pt-6 pb-4">
            <div>
              <h1 className="text-xl font-bold">
                <span className="text-orange-400">Sync</span>
                <span className="text-white italic">Ledger</span>
              </h1>
              <p className="text-[10px] font-semibold tracking-[0.2em] text-gray-500 uppercase mt-0.5">
                Accounts Payable
              </p>
            </div>
            <button 
              className="lg:hidden text-gray-400 hover:text-white"
              onClick={() => setSidebarOpen(false)}
            >
              <X className="w-5 h-5" />
            </button>
          </div>
          
          {/* Navigation */}
          <nav className="flex-1 px-4 pt-4 overflow-y-auto">
            {/* CORE section */}
            <p className="px-3 mb-2 text-[10px] font-semibold tracking-[0.15em] text-gray-600 uppercase">
              Core
            </p>
            <div className="space-y-1 mb-6">
              {coreNavItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setSidebarOpen(false)}
                  className={({ isActive }) => `
                    group flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-medium transition-colors
                    ${isActive 
                      ? 'bg-white/10 text-orange-400' 
                      : 'text-gray-400 hover:bg-white/5 hover:text-gray-200'
                    }
                  `}
                >
                  {({ isActive }) => (
                    <>
                      <div className="flex items-center">
                        <item.icon className={`w-[18px] h-[18px] mr-3 ${isActive ? 'text-orange-400' : 'text-gray-500 group-hover:text-gray-300'}`} />
                        {item.label}
                      </div>
                      {'badge' in item && item.badge != null && item.badge > 0 && (
                        <span className={`
                          min-w-[24px] h-5 flex items-center justify-center px-1.5 rounded-md text-[11px] font-bold
                          ${isActive ? 'bg-orange-500/20 text-orange-400' : 'bg-white/10 text-gray-400'}
                        `}>
                          {item.badge}
                        </span>
                      )}
                    </>
                  )}
                </NavLink>
              ))}
            </div>

            {/* SYSTEM section */}
            <p className="px-3 mb-2 text-[10px] font-semibold tracking-[0.15em] text-gray-600 uppercase">
              System
            </p>
            <div className="space-y-1">
              {systemNavItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setSidebarOpen(false)}
                  className={({ isActive }) => `
                    group flex items-center px-3 py-2.5 rounded-lg text-sm font-medium transition-colors
                    ${isActive 
                      ? 'bg-white/10 text-orange-400' 
                      : 'text-gray-400 hover:bg-white/5 hover:text-gray-200'
                    }
                  `}
                >
                  {({ isActive }) => (
                    <div className="flex items-center">
                      <item.icon className={`w-[18px] h-[18px] mr-3 ${isActive ? 'text-orange-400' : 'text-gray-500 group-hover:text-gray-300'}`} />
                      {item.label}
                    </div>
                  )}
                </NavLink>
              ))}
            </div>
          </nav>
          
          {/* User info & Logout */}
          <div className="p-4 border-t border-white/10">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold text-white flex-shrink-0"
                style={{ backgroundColor: '#e65100' }}
              >
                {userInitials}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-200 truncate">{userFullName}</p>
                <p className="text-[10px] font-semibold tracking-wider text-gray-500 uppercase">{userRoleLabel}</p>
              </div>
              <button
                onClick={handleLogout}
                title="Logout"
                className="p-1.5 rounded-lg text-gray-500 hover:text-red-400 hover:bg-white/5 transition-colors flex-shrink-0"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </aside>
      
      {/* Main content */}
      <div className="lg:ml-64">
        {/* Top bar */}
        <header className="h-14 bg-white shadow-sm flex items-center px-6 sticky top-0 z-10">
          <button 
            className="lg:hidden mr-4"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="w-6 h-6" />
          </button>
          <div className="flex-1" />
          <div className="flex items-center gap-3">
            {user?.role !== 'SUPER_ADMIN' && user?.organizationName && (
              <span className="hidden sm:inline-flex items-center px-2.5 py-1 text-xs font-medium bg-primary-50 text-primary-700 rounded-full">
                {user.organizationName}
              </span>
            )}
            {user?.role === 'SUPER_ADMIN' && (
              <span className="hidden sm:inline-flex items-center px-2.5 py-1 text-xs font-medium bg-indigo-50 text-indigo-700 rounded-full">
                <Shield className="w-3 h-3 mr-1" />
                Platform Admin
              </span>
            )}
            <span className="text-sm text-gray-500">
              Welcome, <span className="font-medium text-gray-700">{user?.firstName}</span>
            </span>
          </div>
        </header>
        
        {/* Page content */}
        <main className="p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
