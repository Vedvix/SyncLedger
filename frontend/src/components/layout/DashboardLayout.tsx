import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
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
  Building,
  Building2
} from 'lucide-react'
import { useState } from 'react'

export function DashboardLayout() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  
  const handleLogout = () => {
    logout()
    navigate('/login')
  }
  
  const isSuperAdmin = user?.role === 'SUPER_ADMIN'
  const isAdmin = user?.role === 'ADMIN' || isSuperAdmin
  
  const navItems = [
    // Super Admin sees platform management first
    ...(isSuperAdmin ? [{ to: '/super-admin', icon: Shield, label: 'Platform Admin' }] : []),
    { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/invoices', icon: FileText, label: 'Invoices' },
    { to: '/vendors', icon: Building2, label: 'Vendors' },
    ...(isAdmin ? [{ to: '/users', icon: Users, label: 'Users' }] : []),
    ...(isAdmin ? [{ to: '/mapping', icon: GitCompareArrows, label: 'Mapping' }] : []),
    { to: '/settings', icon: Settings, label: 'Settings' },
  ]
  
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
        fixed top-0 left-0 z-30 h-full w-64 bg-white shadow-lg transform transition-transform duration-200 ease-in-out
        lg:translate-x-0
        ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
      `}>
        <div className="flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center justify-between h-16 px-6 border-b">
            <span className="text-xl font-bold text-primary-600">SyncLedger</span>
            <button 
              className="lg:hidden"
              onClick={() => setSidebarOpen(false)}
            >
              <X className="w-6 h-6" />
            </button>
          </div>
          
          {/* Navigation */}
          <nav className="flex-1 px-4 py-6 space-y-2">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) => `
                  flex items-center px-4 py-3 rounded-lg transition-colors
                  ${isActive 
                    ? 'bg-primary-50 text-primary-600' 
                    : 'text-gray-600 hover:bg-gray-50'
                  }
                `}
              >
                <item.icon className="w-5 h-5 mr-3" />
                {item.label}
              </NavLink>
            ))}
          </nav>
          
          {/* User info & Logout */}
          <div className="p-4 border-t">
            {/* Organization badge */}
            <div className="mb-3 px-3 py-2 bg-gray-50 rounded-lg">
              <div className="flex items-center text-xs text-gray-500 mb-1">
                <Building className="w-3 h-3 mr-1" />
                Organization
              </div>
              {user?.role === 'SUPER_ADMIN' ? (
                <p className="text-sm font-medium text-indigo-600">SyncLedger Platform</p>
              ) : (
                <p className="text-sm font-medium text-gray-800">{user?.organizationName || 'N/A'}</p>
              )}
            </div>
            <div className="flex items-center mb-4">
              <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center">
                <span className="text-primary-600 font-medium">
                  {user?.firstName?.[0]}{user?.lastName?.[0]}
                </span>
              </div>
              <div className="ml-3">
                <p className="text-sm font-medium text-gray-900">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-xs text-gray-500">{user?.role}</p>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="flex items-center w-full px-4 py-2 text-gray-600 rounded-lg hover:bg-gray-50"
            >
              <LogOut className="w-5 h-5 mr-3" />
              Logout
            </button>
          </div>
        </div>
      </aside>
      
      {/* Main content */}
      <div className="lg:ml-64">
        {/* Top bar */}
        <header className="h-16 bg-white shadow-sm flex items-center px-6">
          <button 
            className="lg:hidden mr-4"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="w-6 h-6" />
          </button>
          <div className="flex-1" />
          <div className="flex items-center gap-4">
            {user?.role !== 'SUPER_ADMIN' && user?.organizationName && (
              <span className="hidden sm:inline-flex items-center px-3 py-1 text-xs font-medium bg-primary-50 text-primary-700 rounded-full">
                <Building className="w-3 h-3 mr-1" />
                {user.organizationName}
              </span>
            )}
            {user?.role === 'SUPER_ADMIN' && (
              <span className="hidden sm:inline-flex items-center px-3 py-1 text-xs font-medium bg-indigo-50 text-indigo-700 rounded-full">
                <Shield className="w-3 h-3 mr-1" />
                Platform Admin
              </span>
            )}
            <span className="text-sm text-gray-500">
              Welcome, {user?.firstName}!
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
