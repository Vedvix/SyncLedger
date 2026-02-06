import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { userService } from '@/services/userService'
import type { User, CreateUserRequest, UserRole } from '@/types'
import { 
  Plus, 
  Search, 
  RefreshCw, 
  Edit2, 
  Trash2,
  Users as UsersIcon,
  X
} from 'lucide-react'

export function UsersPage() {
  const [page, setPage] = useState(0)
  const [searchQuery, setSearchQuery] = useState('')
  const [showCreateModal, setShowCreateModal] = useState(false)
  const queryClient = useQueryClient()
  
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['users', page, searchQuery],
    queryFn: () => userService.getUsers(searchQuery, { page, size: 10 }),
  })
  
  const deleteMutation = useMutation({
    mutationFn: (id: number) => userService.deleteUser(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
    },
  })
  
  const handleDelete = (user: User) => {
    if (confirm(`Are you sure you want to deactivate ${user.firstName} ${user.lastName}?`)) {
      deleteMutation.mutate(user.id)
    }
  }
  
  const getRoleBadgeColor = (role: UserRole) => {
    switch (role) {
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
        <h1 className="text-2xl font-bold text-gray-900">User Management</h1>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
        >
          <Plus className="w-5 h-5 mr-2" />
          Create User
        </button>
      </div>
      
      {/* Search */}
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
          </div>
        ) : (
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
                  <td className="px-6 py-4 text-gray-500">
                    {user.department || '-'}
                  </td>
                  <td className="px-6 py-4">
                    <span className={`px-2 py-1 text-xs rounded-full ${
                      user.isActive 
                        ? 'bg-green-100 text-green-800' 
                        : 'bg-red-100 text-red-800'
                    }`}>
                      {user.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-gray-500 text-sm">
                    {user.lastLoginAt 
                      ? new Date(user.lastLoginAt).toLocaleString() 
                      : 'Never'
                    }
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button className="text-gray-400 hover:text-primary-600 mr-3">
                      <Edit2 className="w-5 h-5" />
                    </button>
                    <button 
                      onClick={() => handleDelete(user)}
                      className="text-gray-400 hover:text-red-600"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
      
      {/* Create User Modal - Placeholder */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold">Create New User</h2>
              <button onClick={() => setShowCreateModal(false)}>
                <X className="w-6 h-6" />
              </button>
            </div>
            <p className="text-gray-500">User creation form will be implemented here.</p>
            <p className="text-sm text-gray-400 mt-2">
              Note: Users are created by Super Admin only. No self-registration.
            </p>
            <button
              onClick={() => setShowCreateModal(false)}
              className="mt-6 w-full py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
