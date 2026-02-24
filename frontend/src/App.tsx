import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { Toaster, ToastProvider } from '@/components/ui/Toaster'

// Layout
import { DashboardLayout } from '@/components/layout/DashboardLayout'

// Pages
import { LoginPage } from '@/pages/LoginPage'
import { SignupPage } from '@/pages/SignupPage'
import { DashboardPage } from '@/pages/DashboardPage'
import { InvoicesPage } from '@/pages/InvoicesPage'
import { InvoiceDetailPage } from '@/pages/InvoiceDetailPage'
import { UsersPage } from '@/pages/UsersPage'
import { SettingsPage } from '@/pages/SettingsPage'
import { SubscriptionPage } from '@/pages/SubscriptionPage'
import { MicrosoftConfigPage } from '@/pages/MicrosoftConfigPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import SuperAdminPage from '@/pages/SuperAdminPage'
import { OrganizationFormPage } from '@/pages/OrganizationFormPage'
import { OrganizationDetailPage } from '@/pages/OrganizationDetailPage'
import { MappingConfigPage } from '@/pages/MappingConfigPage'
import { VendorsPage } from '@/pages/VendorsPage'
import { VendorDetailPage } from '@/pages/VendorDetailPage'
import ManagePlansPage from '@/pages/ManagePlansPage'
import ManageCouponsPage from '@/pages/ManageCouponsPage'
import { OnboardingPage } from '@/pages/OnboardingPage'

// Protected Route wrapper
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, user } = useAuthStore()
  const location = useLocation()
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  // Redirect ONBOARDING orgs to the wizard (unless already there)
  if (
    user?.organizationStatus === 'ONBOARDING' &&
    user?.role !== 'SUPER_ADMIN' &&
    location.pathname !== '/onboarding'
  ) {
    return <Navigate to="/onboarding" replace />
  }
  
  return <>{children}</>
}

// Admin Route wrapper
function AdminRoute({ children }: { children: React.ReactNode }) {
  const { user, isAuthenticated } = useAuthStore()
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  
  if (user?.role !== 'ADMIN' && user?.role !== 'SUPER_ADMIN') {
    return <Navigate to="/dashboard" replace />
  }
  
  return <>{children}</>
}

// Super Admin Route wrapper
function SuperAdminRoute({ children }: { children: React.ReactNode }) {
  const { user, isAuthenticated } = useAuthStore()
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  
  if (user?.role !== 'SUPER_ADMIN') {
    return <Navigate to="/dashboard" replace />
  }
  
  return <>{children}</>
}

function App() {
  return (
    <ToastProvider>
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        
        {/* Onboarding (protected, no sidebar) */}
        <Route
          path="/onboarding"
          element={
            <ProtectedRoute>
              <OnboardingPage />
            </ProtectedRoute>
          }
        />
        
        {/* Protected Routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="invoices" element={<InvoicesPage />} />
          <Route path="invoices/:id" element={<InvoiceDetailPage />} />
          <Route path="vendors" element={<VendorsPage />} />
          <Route path="vendors/:id" element={<VendorDetailPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route
            path="subscription"
            element={
              <AdminRoute>
                <SubscriptionPage />
              </AdminRoute>
            }
          />
          <Route
            path="microsoft-config"
            element={
              <AdminRoute>
                <MicrosoftConfigPage />
              </AdminRoute>
            }
          />
          
          {/* Admin Only Routes */}
          <Route
            path="users"
            element={
              <AdminRoute>
                <UsersPage />
              </AdminRoute>
            }
          />
          <Route
            path="mapping"
            element={
              <AdminRoute>
                <MappingConfigPage />
              </AdminRoute>
            }
          />
          
          {/* Super Admin Only Routes */}
          <Route
            path="super-admin"
            element={
              <SuperAdminRoute>
                <SuperAdminPage />
              </SuperAdminRoute>
            }
          />
          <Route
            path="super-admin/organizations/new"
            element={
              <SuperAdminRoute>
                <OrganizationFormPage />
              </SuperAdminRoute>
            }
          />
          <Route
            path="super-admin/organizations/:id"
            element={
              <SuperAdminRoute>
                <OrganizationDetailPage />
              </SuperAdminRoute>
            }
          />
          <Route
            path="super-admin/organizations/:id/edit"
            element={
              <SuperAdminRoute>
                <OrganizationFormPage />
              </SuperAdminRoute>
            }
          />
          <Route
            path="super-admin/plans"
            element={
              <SuperAdminRoute>
                <ManagePlansPage />
              </SuperAdminRoute>
            }
          />
          <Route
            path="super-admin/coupons"
            element={
              <SuperAdminRoute>
                <ManageCouponsPage />
              </SuperAdminRoute>
            }
          />
        </Route>
        
        {/* 404 */}
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
      
      <Toaster />
    </BrowserRouter>
  </ToastProvider>
  )
}

export default App
