import { useAuthStore } from '@/store/authStore'
import { Settings as SettingsIcon, User, Bell, Shield } from 'lucide-react'

export function SettingsPage() {
  const { user } = useAuthStore()
  
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Profile Settings */}
        <div className="lg:col-span-2 bg-white rounded-xl shadow-sm p-6">
          <h2 className="text-lg font-semibold mb-4 flex items-center">
            <User className="w-5 h-5 mr-2 text-primary-500" />
            Profile Information
          </h2>
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
            <button className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700">
              Save Changes
            </button>
          </div>
        </div>
        
        {/* Account Info */}
        <div className="space-y-6">
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <Shield className="w-5 h-5 mr-2 text-primary-500" />
              Account
            </h2>
            <div className="space-y-3">
              <div>
                <p className="text-sm text-gray-500">Role</p>
                <p className="font-medium">{user?.role}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Status</p>
                <p className="font-medium text-green-600">
                  {user?.isActive ? 'Active' : 'Inactive'}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Last Login</p>
                <p className="font-medium">
                  {user?.lastLoginAt 
                    ? new Date(user.lastLoginAt).toLocaleString() 
                    : 'N/A'
                  }
                </p>
              </div>
            </div>
          </div>
          
          <div className="bg-white rounded-xl shadow-sm p-6">
            <h2 className="text-lg font-semibold mb-4 flex items-center">
              <Bell className="w-5 h-5 mr-2 text-primary-500" />
              Notifications
            </h2>
            <div className="space-y-3">
              <label className="flex items-center">
                <input type="checkbox" defaultChecked className="mr-3" />
                <span className="text-sm">Email notifications</span>
              </label>
              <label className="flex items-center">
                <input type="checkbox" defaultChecked className="mr-3" />
                <span className="text-sm">Invoice approval alerts</span>
              </label>
              <label className="flex items-center">
                <input type="checkbox" className="mr-3" />
                <span className="text-sm">Daily summary email</span>
              </label>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
