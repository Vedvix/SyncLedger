import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom'
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
  Building2,
  SlidersHorizontal,
  ChevronRight,
  Bell,
  ChevronDown,
  HelpCircle,
  User,
  Zap,
  BarChart3,
  Ticket,
  Package,
} from 'lucide-react'
import { useState, useRef, useEffect } from 'react'

// ─── Breadcrumb label map ────────────────────────────────────────
const ROUTE_LABELS: Record<string, string> = {
  dashboard: 'Dashboard',
  invoices: 'Invoices',
  vendors: 'Vendors',
  users: 'Team Members',
  settings: 'Settings',
  mapping: 'Field Mapping',
  configuration: 'Configuration',
  subscription: 'Subscription',
  'microsoft-config': 'Email Configuration',
  'erp-config': 'ERP Integration',
  'super-admin': 'Platform Admin',
  onboarding: 'Setup Wizard',
}

export function DashboardLayout() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false)
  const [userMenuOpen, setUserMenuOpen] = useState(false)
  const userMenuRef = useRef<HTMLDivElement>(null)

  // Close user menu on click outside
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

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

  // ── Navigation items with role-based filtering ──
  const coreNavItems = [
    { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { to: '/invoices', icon: FileText, label: 'Invoices', badge: stats?.totalInvoices },
    { to: '/vendors', icon: Building2, label: 'Vendors' },
    ...(isAdmin ? [{ to: '/users', icon: Users, label: 'Team' }] : []),
  ]

  const configNavItems = [
    ...(isAdmin ? [{ to: '/configuration', icon: SlidersHorizontal, label: 'Configuration' }] : []),
    { to: '/settings', icon: Settings, label: 'Settings' },
  ]

  const platformNavItems = isSuperAdmin
    ? [
        { to: '/super-admin', icon: Shield, label: 'Organizations' },
        { to: '/super-admin/plans', icon: Package, label: 'Plans' },
        { to: '/super-admin/coupons', icon: Ticket, label: 'Coupons' },
        { to: '/super-admin/ai-usage', icon: BarChart3, label: 'AI Usage' },
      ]
    : []

  const userInitials = `${user?.firstName?.[0] || ''}${user?.lastName?.[0] || ''}`.toUpperCase()
  const userFullName = `${user?.firstName || ''} ${user?.lastName || ''}`.trim()
  const userRoleLabel = user?.role?.replace(/_/g, ' ') || ''

  // ── Breadcrumbs ──
  const pathSegments = location.pathname.split('/').filter(Boolean)
  const breadcrumbs = pathSegments.map((seg, idx) => ({
    label: ROUTE_LABELS[seg] || seg.charAt(0).toUpperCase() + seg.slice(1),
    path: '/' + pathSegments.slice(0, idx + 1).join('/'),
    isLast: idx === pathSegments.length - 1,
  }))

  const sidebarWidth = sidebarCollapsed ? 'w-[72px]' : 'w-64'
  const mainMargin = sidebarCollapsed ? 'lg:ml-[72px]' : 'lg:ml-64'

  // ── Nav item renderer ──
  const renderNavItem = (item: { to: string; icon: typeof LayoutDashboard; label: string; badge?: number }) => (
    <NavLink
      key={item.to}
      to={item.to}
      onClick={() => setSidebarOpen(false)}
      title={sidebarCollapsed ? item.label : undefined}
      className={({ isActive }) => `
        nav-item group relative
        ${isActive ? 'nav-item-active' : 'nav-item-inactive'}
      `}
    >
      {({ isActive }) => (
        <>
          {/* Active indicator bar */}
          {isActive && (
            <div className="absolute left-0 top-1/2 -translate-y-1/2 w-[3px] h-5 bg-primary-400 rounded-r-full" />
          )}
          <item.icon className={`w-[18px] h-[18px] flex-shrink-0 ${
            isActive ? 'text-primary-400' : 'text-slate-500 group-hover:text-slate-300'
          }`} />
          {!sidebarCollapsed && (
            <>
              <span className="flex-1">{item.label}</span>
              {'badge' in item && item.badge != null && item.badge > 0 && (
                <span className={`
                  min-w-[22px] h-5 flex items-center justify-center px-1.5 rounded-full text-[10px] font-bold
                  ${isActive ? 'bg-primary-500/20 text-primary-300' : 'bg-white/10 text-slate-400'}
                `}>
                  {item.badge > 999 ? '999+' : item.badge}
                </span>
              )}
            </>
          )}
        </>
      )}
    </NavLink>
  )

  const renderSectionLabel = (label: string) => {
    if (sidebarCollapsed) {
      return <div className="mx-4 my-3 border-t border-slate-700/50" />
    }
    return (
      <p className="px-3 mb-2 mt-6 text-[10px] font-semibold tracking-[0.18em] text-slate-500 uppercase first:mt-0">
        {label}
      </p>
    )
  }

  return (
    <div className="min-h-screen bg-[#f8fafc]">
      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/40 backdrop-blur-sm z-20 lg:hidden transition-opacity"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* ─── Sidebar ─── */}
      <aside
        className={`
          fixed top-0 left-0 z-30 h-full ${sidebarWidth} transform transition-all duration-200 ease-out
          lg:translate-x-0
          ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}
        `}
        style={{ background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)' }}
      >
        <div className="flex flex-col h-full">
          {/* Logo + collapse toggle */}
          <div className="flex items-center justify-between px-5 pt-6 pb-4">
            {!sidebarCollapsed && (
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center shadow-lg shadow-primary-500/25">
                  <Zap className="w-4 h-4 text-white" />
                </div>
                <div>
                  <h1 className="text-lg font-bold leading-tight">
                    <span className="text-primary-400">Sync</span>
                    <span className="text-white">Ledger</span>
                  </h1>
                  <p className="text-[9px] font-medium tracking-[0.2em] text-slate-500 uppercase">
                    Accounts Payable
                  </p>
                </div>
              </div>
            )}
            {sidebarCollapsed && (
              <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-400 to-primary-600 flex items-center justify-center mx-auto shadow-lg shadow-primary-500/25">
                <Zap className="w-4 h-4 text-white" />
              </div>
            )}
            <button
              className="lg:hidden text-slate-400 hover:text-white"
              onClick={() => setSidebarOpen(false)}
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Collapse button (desktop only) */}
          <button
            onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
            className="hidden lg:flex items-center justify-center mx-auto mb-2 w-6 h-6 rounded-full bg-slate-700/50 text-slate-400 hover:text-white hover:bg-slate-600 transition-colors"
            title={sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            <ChevronRight className={`w-3.5 h-3.5 transition-transform ${sidebarCollapsed ? '' : 'rotate-180'}`} />
          </button>

          {/* Navigation */}
          <nav className="flex-1 px-3 overflow-y-auto">
            {renderSectionLabel('Core')}
            <div className="space-y-0.5">
              {coreNavItems.map(renderNavItem)}
            </div>

            {configNavItems.length > 0 && (
              <>
                {renderSectionLabel('Configuration')}
                <div className="space-y-0.5">
                  {configNavItems.map(renderNavItem)}
                </div>
              </>
            )}

            {platformNavItems.length > 0 && (
              <>
                {renderSectionLabel('Platform')}
                <div className="space-y-0.5">
                  {platformNavItems.map(renderNavItem)}
                </div>
              </>
            )}
          </nav>

          {/* User info & Logout */}
          <div className="p-3 border-t border-slate-700/40">
            <div className={`flex items-center ${sidebarCollapsed ? 'justify-center' : 'gap-3'}`}>
              <div
                className="w-9 h-9 rounded-xl flex items-center justify-center text-xs font-bold text-white flex-shrink-0 bg-gradient-to-br from-primary-400 to-indigo-500 shadow-sm"
              >
                {userInitials}
              </div>
              {!sidebarCollapsed && (
                <>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-200 truncate">{userFullName}</p>
                    <p className="text-[10px] font-semibold tracking-wider text-slate-500 uppercase">
                      {userRoleLabel}
                    </p>
                  </div>
                  <button
                    onClick={handleLogout}
                    title="Sign out"
                    className="p-2 rounded-lg text-slate-500 hover:text-red-400 hover:bg-white/5 transition-colors flex-shrink-0"
                  >
                    <LogOut className="w-4 h-4" />
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      </aside>

      {/* ─── Main content ─── */}
      <div className={`${mainMargin} transition-all duration-200`}>
        {/* Top bar */}
        <header className="h-16 bg-white/80 backdrop-blur-md border-b border-gray-200/60 flex items-center px-6 sticky top-0 z-10">
          {/* Mobile menu button */}
          <button
            className="lg:hidden mr-4 p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
            onClick={() => setSidebarOpen(true)}
          >
            <Menu className="w-5 h-5 text-gray-600" />
          </button>

          {/* Breadcrumbs */}
          <nav className="hidden sm:flex items-center text-sm text-gray-400">
            {breadcrumbs.map((crumb, idx) => (
              <span key={crumb.path} className="flex items-center">
                {idx > 0 && <ChevronRight className="w-3.5 h-3.5 mx-1.5 text-gray-300" />}
                {crumb.isLast ? (
                  <span className="font-medium text-gray-700">{crumb.label}</span>
                ) : (
                  <button
                    onClick={() => navigate(crumb.path)}
                    className="hover:text-primary-600 transition-colors"
                  >
                    {crumb.label}
                  </button>
                )}
              </span>
            ))}
          </nav>

          <div className="flex-1" />

          {/* Right side actions */}
          <div className="flex items-center gap-2">
            {/* Organization badge */}
            {user?.role !== 'SUPER_ADMIN' && user?.organizationName && (
              <span className="hidden md:inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-primary-50 text-primary-700 rounded-full border border-primary-100">
                <Building2 className="w-3 h-3" />
                {user.organizationName}
              </span>
            )}
            {user?.role === 'SUPER_ADMIN' && (
              <span className="hidden md:inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium bg-gradient-to-r from-indigo-50 to-purple-50 text-indigo-700 rounded-full border border-indigo-100">
                <Shield className="w-3 h-3" />
                Platform Admin
              </span>
            )}

            {/* Notification bell */}
            <button className="relative p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors">
              <Bell className="w-5 h-5" />
            </button>

            {/* Help */}
            <button className="p-2 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors hidden md:flex">
              <HelpCircle className="w-5 h-5" />
            </button>

            {/* User dropdown */}
            <div className="relative" ref={userMenuRef}>
              <button
                onClick={() => setUserMenuOpen(!userMenuOpen)}
                className="flex items-center gap-2 p-1.5 pr-3 rounded-xl hover:bg-gray-100 transition-colors"
              >
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-primary-400 to-indigo-500 flex items-center justify-center text-[11px] font-bold text-white shadow-sm">
                  {userInitials}
                </div>
                <div className="hidden md:block text-left">
                  <p className="text-sm font-medium text-gray-700 leading-tight">{user?.firstName}</p>
                  <p className="text-[10px] text-gray-400 capitalize">{userRoleLabel.toLowerCase()}</p>
                </div>
                <ChevronDown className="w-3.5 h-3.5 text-gray-400 hidden md:block" />
              </button>

              {/* Dropdown menu */}
              {userMenuOpen && (
                <div className="absolute right-0 top-full mt-2 w-56 bg-white rounded-xl border border-gray-200 shadow-lg py-1.5 animate-scale-in z-50">
                  <div className="px-4 py-3 border-b border-gray-100">
                    <p className="text-sm font-medium text-gray-900">{userFullName}</p>
                    <p className="text-xs text-gray-500 mt-0.5">{user?.email}</p>
                  </div>
                  <div className="py-1">
                    <button
                      onClick={() => { navigate('/settings'); setUserMenuOpen(false) }}
                      className="flex items-center gap-2.5 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                    >
                      <User className="w-4 h-4 text-gray-400" />
                      My Profile
                    </button>
                    <button
                      onClick={() => { navigate('/settings'); setUserMenuOpen(false) }}
                      className="flex items-center gap-2.5 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition-colors"
                    >
                      <Settings className="w-4 h-4 text-gray-400" />
                      Settings
                    </button>
                  </div>
                  <div className="border-t border-gray-100 pt-1">
                    <button
                      onClick={() => { handleLogout(); setUserMenuOpen(false) }}
                      className="flex items-center gap-2.5 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors"
                    >
                      <LogOut className="w-4 h-4" />
                      Sign Out
                    </button>
                  </div>
                </div>
              )}
            </div>
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
