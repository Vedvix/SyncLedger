import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userService } from '@/services/userService'
import { organizationService } from '@/services/organizationService'
import { useAuthStore } from '@/store/authStore'
import type { User, CreateUserRequest, UpdateUserRequest, UserRole } from '@/types'
import { Modal, ConfirmDialog } from '@/components/ui/Modal'
import { 
  Search, 
  RefreshCw, 
  Edit2,
  Users as UsersIcon,
  UserPlus,
  Shield,
  Mail,
  Phone,
  Building,
  CheckCircle,
  XCircle,
  Eye,
  EyeOff
} from 'lucide-react'

export function UsersPage() {
  const [page, setPage] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [showEditModal, setShowEditModal] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const queryClient = useQueryClient()
  
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['users', page, searchQuery],
    queryFn: () => userService.getUsers(searchQuery, { page, size: 10 }),
  })
  
  const createMutation = useMutation({
    mutationFn: (data: CreateUserRequest) => userService.createUser(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setShowCreateModal(false)
    },
  })
  
  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateUserRequest }) => 
      userService.updateUser(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      setShowEditModal(false)
      setSelectedUser(null)
    },
  })
  
  const handleEdit = (user: User) => {
    setSelectedUser(user)
    setShowEditModal(true)
  }
  
  const handleToggleStatus = (user: User) => {
    setSelectedUser(user)
    setShowDeleteDialog(true)
  }
  
  const getRoleBadgeColor = (role: UserRole) => {
    switch (role) {
      case 'SUPER_ADMIN':
        return 'bg-indigo-100 text-indigo-800'
      case 'ADMIN':
        return 'bg-purple-100 text-purple-800'
      case 'APPROVER':
        return 'bg-blue-100 text-blue-800'
      case 'VIEWER':
        return 'bg-gray-100 text-gray-800'
    }
  }
  
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">User Management</h1>
          <p className="text-gray-500 text-sm mt-1">Manage system users and their permissions</p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
        >
          <UserPlus className="w-5 h-5 mr-2" />
          Add User
        </button>
      </div>
      
      {/* Search and Filters */}
      <div className="bg-white rounded-xl shadow-sm p-4">
        <div className="flex gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search users by name or email..."
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value)
                setPage(0)
              }}
              className="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <button
            onClick={() => refetch()}
            className="flex items-center px-4 py-2 border rounded-lg hover:bg-gray-50"
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Refresh
          </button>
        </div>
      </div>
      
      {/* Stats Summary */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white rounded-lg shadow-sm p-4">
          <div className="flex items-center">
            <div className="p-2 bg-blue-100 rounded-lg">
              <UsersIcon className="w-5 h-5 text-blue-600" />
            </div>
            <div className="ml-3">
              <p className="text-sm text-gray-500">Total Users</p>
              <p className="text-xl font-bold">{data?.totalElements || 0}</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow-sm p-4">
          <div className="flex items-center">
            <div className="p-2 bg-purple-100 rounded-lg">
              <Shield className="w-5 h-5 text-purple-600" />
            </div>
            <div className="ml-3">
              <p className="text-sm text-gray-500">Admins</p>
              <p className="text-xl font-bold">
                {data?.content.filter(u => u.role === 'ADMIN').length || 0}
              </p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow-sm p-4">
          <div className="flex items-center">
            <div className="p-2 bg-green-100 rounded-lg">
              <CheckCircle className="w-5 h-5 text-green-600" />
            </div>
            <div className="ml-3">
              <p className="text-sm text-gray-500">Active</p>
              <p className="text-xl font-bold">
                {data?.content.filter(u => u.isActive).length || 0}
              </p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow-sm p-4">
          <div className="flex items-center">
            <div className="p-2 bg-red-100 rounded-lg">
              <XCircle className="w-5 h-5 text-red-600" />
            </div>
            <div className="ml-3">
              <p className="text-sm text-gray-500">Inactive</p>
              <p className="text-xl font-bold">
                {data?.content.filter(u => !u.isActive).length || 0}
              </p>
            </div>
          </div>
        </div>
      </div>
      
      {/* Users Table */}
      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center h-64">
            <RefreshCw className="w-8 h-8 animate-spin text-primary-500" />
          </div>
        ) : data?.content.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-64 text-gray-500">
            <UsersIcon className="w-12 h-12 mb-4" />
            <p>No users found</p>
            <button
              onClick={() => setShowCreateModal(true)}
              className="mt-4 text-primary-600 hover:underline"
            >
              Add your first user
            </button>
          </div>
        ) : (
          <>
            <table className="w-full">
              <thead className="bg-gray-50 border-b">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    User
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Role
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Organization
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Department
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Status
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                    Last Login
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {data?.content.map((user) => (
                  <tr key={user.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div className="flex items-center">
                        <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center">
                          <span className="text-primary-600 font-medium">
                            {user.firstName[0]}{user.lastName[0]}
                          </span>
                        </div>
                        <div className="ml-4">
                          <div className="font-medium text-gray-900">
                            {user.firstName} {user.lastName}
                          </div>
                          <div className="text-sm text-gray-500">{user.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs rounded-full ${getRoleBadgeColor(user.role)}`}>
                        {user.role}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-700">
                      {user.role === 'SUPER_ADMIN' ? (
                        <div className="flex items-center">
                          <Shield className="w-4 h-4 text-indigo-500 mr-1" />
                          <span className="text-indigo-600 font-medium">SyncLedger Platform</span>
                        </div>
                      ) : user.organizationName ? (
                        <div>
                          <div className="font-medium">{user.organizationName}</div>
                          <div className="text-xs text-gray-500">{user.organizationSlug}</div>
                        </div>
                      ) : (
                        <span className="text-red-400 text-xs">No organization assigned</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-gray-500">
                      {user.department || '-'}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center px-2 py-1 text-xs rounded-full ${
                        user.isActive 
                          ? 'bg-green-100 text-green-800' 
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {user.isActive ? (
                          <><CheckCircle className="w-3 h-3 mr-1" /> Active</>
                        ) : (
                          <><XCircle className="w-3 h-3 mr-1" /> Inactive</>
                        )}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-gray-500 text-sm">
                      {user.lastLoginAt 
                        ? new Date(user.lastLoginAt).toLocaleString() 
                        : 'Never'
                      }
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button 
                        onClick={() => handleEdit(user)}
                        className="text-gray-400 hover:text-primary-600 mr-3"
                        title="Edit user"
                      >
                        <Edit2 className="w-5 h-5" />
                      </button>
                      <button 
                        onClick={() => handleToggleStatus(user)}
                        className="text-gray-400 hover:text-red-600"
                        title={user.isActive ? 'Deactivate user' : 'Activate user'}
                      >
                        {user.isActive ? <XCircle className="w-5 h-5" /> : <CheckCircle className="w-5 h-5" />}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            
            {/* Pagination */}
            {data && data.totalPages > 1 && (
              <div className="px-6 py-4 border-t flex items-center justify-between">
                <p className="text-sm text-gray-500">
                  Showing {page * 10 + 1} to {Math.min((page + 1) * 10, data.totalElements)} of {data.totalElements} users
                </p>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="px-3 py-1 border rounded hover:bg-gray-50 disabled:opacity-50"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage(p => p + 1)}
                    disabled={page >= data.totalPages - 1}
                    className="px-3 py-1 border rounded hover:bg-gray-50 disabled:opacity-50"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
      
      {/* Create User Modal */}
      <CreateUserModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={(data) => createMutation.mutate(data)}
        isLoading={createMutation.isPending}
        error={createMutation.error?.message}
      />
      
      {/* Edit User Modal */}
      {selectedUser && (
        <EditUserModal
          isOpen={showEditModal}
          onClose={() => {
            setShowEditModal(false)
            setSelectedUser(null)
          }}
          user={selectedUser}
          onSubmit={(data) => updateMutation.mutate({ id: selectedUser.id, data })}
          isLoading={updateMutation.isPending}
          error={updateMutation.error?.message}
        />
      )}
      
      {/* Status Toggle Confirmation */}
      <ConfirmDialog
        isOpen={showDeleteDialog}
        onClose={() => {
          setShowDeleteDialog(false)
          setSelectedUser(null)
        }}
        onConfirm={() => {
          if (selectedUser) {
            updateMutation.mutate({
              id: selectedUser.id,
              data: { isActive: !selectedUser.isActive }
            })
            setShowDeleteDialog(false)
            setSelectedUser(null)
          }
        }}
        title={selectedUser?.isActive ? 'Deactivate User' : 'Activate User'}
        message={
          selectedUser?.isActive
            ? `Are you sure you want to deactivate ${selectedUser?.firstName} ${selectedUser?.lastName}? They will no longer be able to access the system.`
            : `Are you sure you want to activate ${selectedUser?.firstName} ${selectedUser?.lastName}? They will regain access to the system.`
        }
        confirmText={selectedUser?.isActive ? 'Deactivate' : 'Activate'}
        variant={selectedUser?.isActive ? 'danger' : 'info'}
        isLoading={updateMutation.isPending}
      />
    </div>
  )
}

// Create User Modal Component
interface CreateUserModalProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (data: CreateUserRequest) => void
  isLoading: boolean
  error?: string
}

function CreateUserModal({ isOpen, onClose, onSubmit, isLoading, error }: CreateUserModalProps) {
  const { user: currentUser } = useAuthStore()
  const isSuperAdmin = currentUser?.role === 'SUPER_ADMIN'
  const [formData, setFormData] = useState<CreateUserRequest>({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'VIEWER',
    phone: '',
    department: '',
    jobTitle: '',
    organizationId: undefined,
  })
  const [showPassword, setShowPassword] = useState(false)
  
  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      setFormData({
        firstName: '',
        lastName: '',
        email: '',
        password: '',
        role: 'VIEWER',
        phone: '',
        department: '',
        jobTitle: '',
        organizationId: currentUser?.organizationId || undefined,
      })
    }
  }, [isOpen, currentUser])
  
  // Fetch organizations if super admin — fetch eagerly so data is ready when modal opens
  const { data: orgsData, isLoading: orgsLoading, isFetching: orgsFetching, error: orgsError } = useQuery({
    queryKey: ['organizations-list'],
    queryFn: () => organizationService.getOrganizations(0, 100),
    enabled: isSuperAdmin,
    staleTime: 30_000, // 30s - refetch if stale
  })

  const isSuperAdminRole = formData.role === 'SUPER_ADMIN'
  const activeOrgs = orgsData?.content?.filter(org => org.status === 'ACTIVE') || []
  
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    // SUPER_ADMIN users belong to SyncLedger platform - no org needed
    if (isSuperAdminRole) {
      onSubmit({ ...formData, organizationId: undefined })
      return
    }
    // All other roles MUST have an organization
    if (!formData.organizationId) {
      alert('Organization is required. Every non-Super Admin user must belong to an organization.')
      return
    }
    onSubmit(formData)
  }
  
  const handleChange = (field: keyof CreateUserRequest, value: string | number) => {
    setFormData(prev => {
      const updated = { ...prev, [field]: value }
      // When role changes to SUPER_ADMIN, clear organization
      if (field === 'role' && value === 'SUPER_ADMIN') {
        updated.organizationId = undefined
      }
      // When role changes from SUPER_ADMIN to another, pre-fill org for non-super-admin creators
      if (field === 'role' && value !== 'SUPER_ADMIN' && !isSuperAdmin) {
        updated.organizationId = currentUser?.organizationId || undefined
      }
      return updated
    })
  }
  
  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create New User" size="lg">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
            {error}
          </div>
        )}
        
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              First Name *
            </label>
            <input
              type="text"
              required
              value={formData.firstName}
              onChange={(e) => handleChange('firstName', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Last Name *
            </label>
            <input
              type="text"
              required
              value={formData.lastName}
              onChange={(e) => handleChange('lastName', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            <Mail className="w-4 h-4 inline mr-1" />
            Email Address *
          </label>
          <input
            type="email"
            required
            value={formData.email}
            onChange={(e) => handleChange('email', e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Password *
          </label>
          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              required
              minLength={8}
              value={formData.password}
              onChange={(e) => handleChange('password', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 pr-10"
              placeholder="Minimum 8 characters"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400"
            >
              {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
            </button>
          </div>
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            <Shield className="w-4 h-4 inline mr-1" />
            Role *
          </label>
          <select
            required
            value={formData.role}
            onChange={(e) => handleChange('role', e.target.value as UserRole)}
            className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
          >
            <option value="VIEWER">Viewer - Can view invoices only</option>
            <option value="APPROVER">Approver - Can approve/reject invoices</option>
            <option value="ADMIN">Admin - Organization admin access</option>
            {isSuperAdmin && (
              <option value="SUPER_ADMIN">Super Admin - SyncLedger platform team</option>
            )}
          </select>
          {isSuperAdminRole && (
            <p className="text-xs text-indigo-600 mt-1">
              Super Admins have platform-wide access and are not tied to any organization.
            </p>
          )}
        </div>

        {/* Organization field */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            <Building className="w-4 h-4 inline mr-1" />
            Organization {!isSuperAdminRole ? '*' : ''}
          </label>
          {isSuperAdminRole ? (
            <div className="px-4 py-2 border rounded-lg bg-indigo-50 text-indigo-700 text-sm">
              <Shield className="w-4 h-4 inline mr-1" />
              Super Admins belong to the <strong>SyncLedger Platform</strong> team and are not linked to any organization.
            </div>
          ) : isSuperAdmin ? (
            <>
              <select
                required
                value={formData.organizationId || ''}
                onChange={(e) => handleChange('organizationId', e.target.value ? parseInt(e.target.value) : '')}
                disabled={orgsLoading || orgsFetching}
                className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:opacity-50"
              >
                <option value="">
                  {orgsLoading || orgsFetching
                    ? 'Loading organizations...'
                    : activeOrgs.length === 0
                      ? 'No active organizations found'
                      : '-- Select an organization --'}
                </option>
                {activeOrgs.map(org => (
                  <option key={org.id} value={org.id}>{org.name}</option>
                ))}
              </select>
              {orgsError && (
                <p className="text-xs text-red-500 mt-1">
                  Failed to load organizations: {orgsError.message}
                </p>
              )}
              {!formData.organizationId && !orgsError && (
                <p className="text-xs text-red-500 mt-1">Organization is required for this role.</p>
              )}
            </>
          ) : (
            <input
              type="text"
              disabled
              value={currentUser?.organizationName || 'Your Organization'}
              className="w-full px-4 py-2 border rounded-lg bg-gray-100 text-gray-600 cursor-not-allowed"
            />
          )}
        </div>
        
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Phone className="w-4 h-4 inline mr-1" />
              Phone
            </label>
            <input
              type="tel"
              value={formData.phone}
              onChange={(e) => handleChange('phone', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Building className="w-4 h-4 inline mr-1" />
              Department
            </label>
            <input
              type="text"
              value={formData.department}
              onChange={(e) => handleChange('department', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Job Title
          </label>
          <input
            type="text"
            value={formData.jobTitle}
            onChange={(e) => handleChange('jobTitle', e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        
        <div className="flex justify-end gap-3 pt-4 border-t">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="px-4 py-2 border rounded-lg hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isLoading}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {isLoading ? 'Creating...' : 'Create User'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

// Edit User Modal Component
interface EditUserModalProps {
  isOpen: boolean
  onClose: () => void
  user: User
  onSubmit: (data: UpdateUserRequest) => void
  isLoading: boolean
  error?: string
}

function EditUserModal({ isOpen, onClose, user, onSubmit, isLoading, error }: EditUserModalProps) {
  const { user: currentUser } = useAuthStore()
  const isSuperAdmin = currentUser?.role === 'SUPER_ADMIN'
  const [formData, setFormData] = useState<UpdateUserRequest>({
    firstName: user.firstName,
    lastName: user.lastName,
    role: user.role,
    isActive: user.isActive,
    phone: user.phone,
    department: user.department,
    jobTitle: user.jobTitle,
  })
  
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSubmit(formData)
  }
  
  const handleChange = (field: keyof UpdateUserRequest, value: string | boolean) => {
    setFormData(prev => ({ ...prev, [field]: value }))
  }
  
  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Edit User" size="lg">
      <form onSubmit={handleSubmit} className="space-y-4">
        {error && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
            {error}
          </div>
        )}
        
        {/* User info header */}
        <div className="p-3 bg-gray-50 rounded-lg mb-4 space-y-1">
          <p className="text-sm text-gray-600">
            <Mail className="w-4 h-4 inline mr-1" />
            {user.email}
          </p>
          <p className="text-sm text-gray-600">
            <Building className="w-4 h-4 inline mr-1" />
            {user.role === 'SUPER_ADMIN' ? (
              <span className="text-indigo-600 font-medium">SyncLedger Platform Team</span>
            ) : user.organizationName ? (
              <span className="font-medium">{user.organizationName}</span>
            ) : (
              <span className="text-gray-400">No organization</span>
            )}
          </p>
        </div>
        
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              First Name *
            </label>
            <input
              type="text"
              required
              value={formData.firstName}
              onChange={(e) => handleChange('firstName', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Last Name *
            </label>
            <input
              type="text"
              required
              value={formData.lastName}
              onChange={(e) => handleChange('lastName', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
        
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Shield className="w-4 h-4 inline mr-1" />
              Role *
            </label>
            <select
              required
              value={formData.role}
              onChange={(e) => handleChange('role', e.target.value as UserRole)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              disabled={user.role === 'SUPER_ADMIN' && !isSuperAdmin}
            >
              <option value="VIEWER">Viewer</option>
              <option value="APPROVER">Approver</option>
              <option value="ADMIN">Admin</option>
              {isSuperAdmin && (
                <option value="SUPER_ADMIN">Super Admin</option>
              )}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Status
            </label>
            <select
              value={formData.isActive ? 'true' : 'false'}
              onChange={(e) => handleChange('isActive', e.target.value === 'true')}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            >
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </div>
        </div>
        
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Phone className="w-4 h-4 inline mr-1" />
              Phone
            </label>
            <input
              type="tel"
              value={formData.phone || ''}
              onChange={(e) => handleChange('phone', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Building className="w-4 h-4 inline mr-1" />
              Department
            </label>
            <input
              type="text"
              value={formData.department || ''}
              onChange={(e) => handleChange('department', e.target.value)}
              className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
          </div>
        </div>
        
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Job Title
          </label>
          <input
            type="text"
            value={formData.jobTitle || ''}
            onChange={(e) => handleChange('jobTitle', e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
        
        <div className="flex justify-end gap-3 pt-4 border-t">
          <button
            type="button"
            onClick={onClose}
            disabled={isLoading}
            className="px-4 py-2 border rounded-lg hover:bg-gray-50"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isLoading}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {isLoading ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
