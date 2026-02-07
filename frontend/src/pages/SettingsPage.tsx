import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useAuthStore } from '@/store/authStore'
import { authService } from '@/services/authService'
import { 
  User, 
  Bell, 
  Shield, 
  Lock, 
  Eye, 
  EyeOff,
  CheckCircle,
  AlertCircle,
  Save
} from 'lucide-react'

export function SettingsPage() {
  const { user } = useAuthStore()
  const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'notifications'>('profile')
  
  // Password change state
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false,
  })
  const [passwordSuccess, setPasswordSuccess] = useState(false)
  const [passwordErrors, setPasswordErrors] = useState<string[]>([])
  
  const passwordMutation = useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
      authService.changePassword(currentPassword, newPassword),
    onSuccess: () => {
      setPasswordSuccess(true)
      setPasswordErrors([])
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setTimeout(() => setPasswordSuccess(false), 5000)
    },
    onError: (error: any) => {
      setPasswordErrors([error.message || 'Failed to change password'])
      setPasswordSuccess(false)
    },
  })
  
  const validatePassword = (password: string): string[] => {
    const errors: string[] = []
    if (password.length < 8) errors.push('Password must be at least 8 characters')
    if (!/[A-Z]/.test(password)) errors.push('Must contain uppercase letter')
    if (!/[a-z]/.test(password)) errors.push('Must contain lowercase letter')
    if (!/[0-9]/.test(password)) errors.push('Must contain a number')
    return errors
  }
  
  const handlePasswordChange = (e: React.FormEvent) => {
    e.preventDefault()
    setPasswordErrors([])
    
    // Validation
    const errors: string[] = []
    if (!passwordData.currentPassword) errors.push('Current password is required')
    
    const newPasswordErrors = validatePassword(passwordData.newPassword)
    errors.push(...newPasswordErrors)
    
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      errors.push('New passwords do not match')
    }
    
    if (passwordData.currentPassword === passwordData.newPassword) {
      errors.push('New password must be different from current password')
    }
    
    if (errors.length > 0) {
      setPasswordErrors(errors)
      return
    }
    
    passwordMutation.mutate({
      currentPassword: passwordData.currentPassword,
      newPassword: passwordData.newPassword,
    })
  }
  
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-500 text-sm mt-1">Manage your account settings and preferences</p>
      </div>
      
      {/* Tabs */}
      <div className="bg-white rounded-xl shadow-sm">
        <div className="border-b">
          <nav className="flex -mb-px">
            <button
              onClick={() => setActiveTab('profile')}
              className={`px-6 py-3 border-b-2 text-sm font-medium transition-colors ${
                activeTab === 'profile'
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <User className="w-4 h-4 inline mr-2" />
              Profile
            </button>
            <button
              onClick={() => setActiveTab('security')}
              className={`px-6 py-3 border-b-2 text-sm font-medium transition-colors ${
                activeTab === 'security'
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <Lock className="w-4 h-4 inline mr-2" />
              Security
            </button>
            <button
              onClick={() => setActiveTab('notifications')}
              className={`px-6 py-3 border-b-2 text-sm font-medium transition-colors ${
                activeTab === 'notifications'
                  ? 'border-primary-500 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              <Bell className="w-4 h-4 inline mr-2" />
              Notifications
            </button>
          </nav>
        </div>
        
        <div className="p-6">
          {/* Profile Tab */}
          {activeTab === 'profile' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div className="lg:col-span-2 space-y-6">
                <div>
                  <h2 className="text-lg font-semibold mb-4">Profile Information</h2>
                  <div className="space-y-4">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          First Name
                        </label>
                        <input
                          type="text"
                          defaultValue={user?.firstName}
                          className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Last Name
                        </label>
                        <input
                          type="text"
                          defaultValue={user?.lastName}
                          className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Email Address
                      </label>
                      <input
                        type="email"
                        defaultValue={user?.email}
                        disabled
                        className="w-full px-4 py-2 border rounded-lg bg-gray-50 text-gray-500"
                      />
                      <p className="mt-1 text-sm text-gray-500">
                        Contact your administrator to change your email.
                      </p>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Department
                        </label>
                        <input
                          type="text"
                          defaultValue={user?.department}
                          className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Job Title
                        </label>
                        <input
                          type="text"
                          defaultValue={user?.jobTitle}
                          className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Phone
                      </label>
                      <input
                        type="tel"
                        defaultValue={user?.phone}
                        className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
                      />
                    </div>
                    <button className="flex items-center px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700">
                      <Save className="w-4 h-4 mr-2" />
                      Save Changes
                    </button>
                  </div>
                </div>
              </div>
              
              {/* Account Info Sidebar */}
              <div className="space-y-6">
                <div className="bg-gray-50 rounded-lg p-6">
                  <h3 className="text-sm font-semibold text-gray-900 mb-4 flex items-center">
                    <Shield className="w-4 h-4 mr-2 text-primary-500" />
                    Account Details
                  </h3>
                  <div className="space-y-3 text-sm">
                    <div className="flex justify-between">
                      <span className="text-gray-500">Role</span>
                      <span className={`px-2 py-0.5 rounded-full text-xs ${
                        user?.role === 'ADMIN' 
                          ? 'bg-purple-100 text-purple-800'
                          : user?.role === 'APPROVER'
                          ? 'bg-blue-100 text-blue-800'
                          : 'bg-gray-100 text-gray-800'
                      }`}>
                        {user?.role}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Status</span>
                      <span className={`font-medium ${user?.isActive ? 'text-green-600' : 'text-red-600'}`}>
                        {user?.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-500">Last Login</span>
                      <span className="text-gray-900">
                        {user?.lastLoginAt 
                          ? new Date(user.lastLoginAt).toLocaleDateString() 
                          : 'N/A'
                        }
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
          
          {/* Security Tab */}
          {activeTab === 'security' && (
            <div className="max-w-xl">
              <h2 className="text-lg font-semibold mb-2">Change Password</h2>
              <p className="text-gray-500 text-sm mb-6">
                Update your password to keep your account secure.
              </p>
              
              {passwordSuccess && (
                <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-lg flex items-start">
                  <CheckCircle className="w-5 h-5 text-green-500 mr-3 mt-0.5" />
                  <div>
                    <p className="text-green-800 font-medium">Password changed successfully!</p>
                    <p className="text-green-600 text-sm">Your password has been updated.</p>
                  </div>
                </div>
              )}
              
              {passwordErrors.length > 0 && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
                  <div className="flex items-start">
                    <AlertCircle className="w-5 h-5 text-red-500 mr-3 mt-0.5" />
                    <div>
                      <p className="text-red-800 font-medium">Please fix the following errors:</p>
                      <ul className="mt-2 text-sm text-red-600 list-disc list-inside">
                        {passwordErrors.map((error, index) => (
                          <li key={index}>{error}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
              )}
              
              <form onSubmit={handlePasswordChange} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Current Password
                  </label>
                  <div className="relative">
                    <input
                      type={showPasswords.current ? 'text' : 'password'}
                      value={passwordData.currentPassword}
                      onChange={(e) => setPasswordData(prev => ({ ...prev, currentPassword: e.target.value }))}
                      className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPasswords(prev => ({ ...prev, current: !prev.current }))}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    >
                      {showPasswords.current ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    New Password
                  </label>
                  <div className="relative">
                    <input
                      type={showPasswords.new ? 'text' : 'password'}
                      value={passwordData.newPassword}
                      onChange={(e) => setPasswordData(prev => ({ ...prev, newPassword: e.target.value }))}
                      className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPasswords(prev => ({ ...prev, new: !prev.new }))}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    >
                      {showPasswords.new ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>
                  <p className="mt-1 text-xs text-gray-500">
                    Must be at least 8 characters with uppercase, lowercase, and numbers.
                  </p>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Confirm New Password
                  </label>
                  <div className="relative">
                    <input
                      type={showPasswords.confirm ? 'text' : 'password'}
                      value={passwordData.confirmPassword}
                      onChange={(e) => setPasswordData(prev => ({ ...prev, confirmPassword: e.target.value }))}
                      className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPasswords(prev => ({ ...prev, confirm: !prev.confirm }))}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
                    >
                      {showPasswords.confirm ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                    </button>
                  </div>
                </div>
                
                <div className="pt-4">
                  <button
                    type="submit"
                    disabled={passwordMutation.isPending}
                    className="flex items-center px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
                  >
                    <Lock className="w-4 h-4 mr-2" />
                    {passwordMutation.isPending ? 'Changing...' : 'Change Password'}
                  </button>
                </div>
              </form>
              
              <div className="mt-8 p-4 bg-amber-50 border border-amber-200 rounded-lg">
                <h3 className="text-sm font-medium text-amber-800 mb-2">Password Requirements</h3>
                <ul className="text-sm text-amber-700 list-disc list-inside space-y-1">
                  <li>At least 8 characters long</li>
                  <li>Contains at least one uppercase letter</li>
                  <li>Contains at least one lowercase letter</li>
                  <li>Contains at least one number</li>
                </ul>
              </div>
            </div>
          )}
          
          {/* Notifications Tab */}
          {activeTab === 'notifications' && (
            <div className="max-w-xl">
              <h2 className="text-lg font-semibold mb-2">Notification Preferences</h2>
              <p className="text-gray-500 text-sm mb-6">
                Choose how and when you want to receive notifications.
              </p>
              
              <div className="space-y-6">
                <div className="space-y-4">
                  <h3 className="text-sm font-medium text-gray-900">Email Notifications</h3>
                  
                  <label className="flex items-start p-4 border rounded-lg hover:bg-gray-50 cursor-pointer">
                    <input type="checkbox" defaultChecked className="mt-1 mr-4" />
                    <div>
                      <p className="font-medium text-gray-900">Invoice Approval Alerts</p>
                      <p className="text-sm text-gray-500">Get notified when invoices need your approval</p>
                    </div>
                  </label>
                  
                  <label className="flex items-start p-4 border rounded-lg hover:bg-gray-50 cursor-pointer">
                    <input type="checkbox" defaultChecked className="mt-1 mr-4" />
                    <div>
                      <p className="font-medium text-gray-900">Processing Updates</p>
                      <p className="text-sm text-gray-500">Receive updates when invoice processing is complete</p>
                    </div>
                  </label>
                  
                  <label className="flex items-start p-4 border rounded-lg hover:bg-gray-50 cursor-pointer">
                    <input type="checkbox" className="mt-1 mr-4" />
                    <div>
                      <p className="font-medium text-gray-900">Daily Summary</p>
                      <p className="text-sm text-gray-500">Receive a daily email summary of all activity</p>
                    </div>
                  </label>
                  
                  <label className="flex items-start p-4 border rounded-lg hover:bg-gray-50 cursor-pointer">
                    <input type="checkbox" defaultChecked className="mt-1 mr-4" />
                    <div>
                      <p className="font-medium text-gray-900">Sync Failures</p>
                      <p className="text-sm text-gray-500">Get alerted when Sage sync fails</p>
                    </div>
                  </label>
                </div>
                
                <div className="pt-4">
                  <button className="flex items-center px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700">
                    <Save className="w-4 h-4 mr-2" />
                    Save Preferences
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
