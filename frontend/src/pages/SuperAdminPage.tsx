import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { 
  Building2, 
  Users, 
  FileText, 
  Plus,
  MoreHorizontal,
  CheckCircle,
  XCircle,
  AlertCircle,
  Mail,
  RefreshCw,
  Wifi,
  WifiOff,
  Loader2
} from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import { organizationService } from '@/services/organizationService'
import { emailService } from '@/services/emailService'
import { useAuthStore } from '@/store/authStore'
import { useToast } from '@/components/ui/Toaster'
import type { Organization, PlatformStats, EmailPollingStatus } from '@/types'

export default function SuperAdminPage() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { toast } = useToast()
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [stats, setStats] = useState<PlatformStats | null>(null)
  const [emailStatus, setEmailStatus] = useState<EmailPollingStatus | null>(null)
  const [loading, setLoading] = useState(true)
  const [pollingOrgId, setPollingOrgId] = useState<number | null>(null)
  const [testingOrgId, setTestingOrgId] = useState<number | null>(null)
  const [pollingAll, setPollingAll] = useState(false)

  useEffect(() => {
    // Redirect non-super admins
    if (user?.role !== 'SUPER_ADMIN') {
      navigate('/dashboard')
      return
    }

    loadData()
  }, [user, navigate])

  const loadData = async () => {
    try {
      setLoading(true)
      const [orgsResponse, platformStats] = await Promise.all([
        organizationService.getOrganizations(),
        organizationService.getPlatformStats()
      ])
      setOrganizations(orgsResponse.content)
      setStats(platformStats)
      
      // Try to load email status (may fail if not enabled)
      try {
        const status = await emailService.getPollingStatus()
        setEmailStatus(status)
      } catch {
        // Email polling may not be enabled
        setEmailStatus(null)
      }
    } catch (error) {
      console.error('Failed to load data:', error)
      toast.error('Failed to load data')
    } finally {
      setLoading(false)
    }
  }

  const handlePollAllOrganizations = async () => {
    try {
      setPollingAll(true)
      const processed = await emailService.triggerPollAll()
      toast.success(`Email polling completed. Processed ${processed} emails.`)
      // Refresh email status
      const status = await emailService.getPollingStatus()
      setEmailStatus(status)
    } catch (error: any) {
      console.error('Failed to poll emails:', error)
      toast.error(error.response?.data?.message || 'Failed to trigger email polling')
    } finally {
      setPollingAll(false)
    }
  }

  const handlePollOrganization = async (orgId: number, orgName: string) => {
    try {
      setPollingOrgId(orgId)
      const processed = await emailService.triggerPollForOrganization(orgId)
      toast.success(`Polled emails for ${orgName}. Processed ${processed} emails.`)
    } catch (error: any) {
      console.error('Failed to poll organization emails:', error)
      toast.error(error.response?.data?.message || `Failed to poll emails for ${orgName}`)
    } finally {
      setPollingOrgId(null)
    }
  }

  const handleTestConnection = async (orgId: number, orgName: string) => {
    try {
      setTestingOrgId(orgId)
      const connected = await emailService.testEmailConnection(orgId)
      if (connected) {
        toast.success(`Email connection successful for ${orgName}`)
      } else {
        toast.error(`Email connection failed for ${orgName} - check mailbox configuration`)
      }
    } catch (error: any) {
      console.error('Failed to test connection:', error)
      toast.error(error.response?.data?.message || `Failed to test email connection for ${orgName}`)
    } finally {
      setTestingOrgId(null)
    }
  }

  const handleActivate = async (id: number) => {
    try {
      const org = await organizationService.activateOrganization(id)
      toast.success(`${org.name} activated! Email polling is now enabled for this organization.`)
      loadData()
    } catch (error: any) {
      console.error('Failed to activate organization:', error)
      toast.error(error.response?.data?.message || 'Failed to activate organization')
    }
  }

  const handleSuspend = async (id: number) => {
    try {
      const org = await organizationService.suspendOrganization(id)
      toast.success(`${org.name} suspended. Email polling disabled.`)
      loadData()
    } catch (error: any) {
      console.error('Failed to suspend organization:', error)
      toast.error(error.response?.data?.message || 'Failed to suspend organization')
    }
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return <Badge className="bg-green-100 text-green-800"><CheckCircle className="w-3 h-3 mr-1" />Active</Badge>
      case 'SUSPENDED':
        return <Badge className="bg-red-100 text-red-800"><XCircle className="w-3 h-3 mr-1" />Suspended</Badge>
      case 'ONBOARDING':
        return <Badge className="bg-yellow-100 text-yellow-800"><AlertCircle className="w-3 h-3 mr-1" />Onboarding</Badge>
      default:
        return <Badge variant="secondary">{status}</Badge>
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Super Admin Dashboard</h1>
          <p className="text-muted-foreground">
            Platform-wide management and organization overview
          </p>
        </div>
        <div className="flex gap-2">
          <Button 
            variant="outline" 
            onClick={handlePollAllOrganizations}
            disabled={pollingAll}
          >
            {pollingAll ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <RefreshCw className="w-4 h-4 mr-2" />
            )}
            Poll All Emails
          </Button>
          <Button onClick={() => navigate('/super-admin/organizations/new')}>
            <Plus className="w-4 h-4 mr-2" />
            Add Organization
          </Button>
        </div>
      </div>

      {/* Platform Stats */}
      <div className="grid gap-4 md:grid-cols-5">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Organizations</CardTitle>
            <Building2 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalOrganizations || 0}</div>
            <p className="text-xs text-muted-foreground">
              {stats?.activeOrganizations || 0} active
            </p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Users</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalUsers || 0}</div>
            <p className="text-xs text-muted-foreground">
              Across all organizations
            </p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Invoices</CardTitle>
            <FileText className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{stats?.totalInvoices || 0}</div>
            <p className="text-xs text-muted-foreground">
              Platform-wide
            </p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Email Polling</CardTitle>
            <Mail className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {emailStatus?.enabled ? (
                <span className="text-green-600">Active</span>
              ) : (
                <span className="text-gray-400">Disabled</span>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              {emailStatus?.totalEmailsProcessed || 0} emails processed
            </p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Active Rate</CardTitle>
            <Building2 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {stats?.totalOrganizations 
                ? Math.round((stats.activeOrganizations / stats.totalOrganizations) * 100) 
                : 0}%
            </div>
            <p className="text-xs text-muted-foreground">
              Organizations active
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Organizations Table */}
      <Card>
        <CardHeader>
          <CardTitle>Organizations</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Slug</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Created</TableHead>
                <TableHead className="w-12"></TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {organizations.map((org) => (
                <TableRow key={org.id}>
                  <TableCell className="font-medium">{org.name}</TableCell>
                  <TableCell>{org.slug}</TableCell>
                  <TableCell>{org.emailAddress || '-'}</TableCell>
                  <TableCell>{getStatusBadge(org.status)}</TableCell>
                  <TableCell>
                    {new Date(org.createdAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm">
                          <MoreHorizontal className="w-4 h-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem 
                          onClick={() => navigate(`/super-admin/organizations/${org.id}`)}
                        >
                          View Details
                        </DropdownMenuItem>
                        <DropdownMenuItem 
                          onClick={() => navigate(`/super-admin/organizations/${org.id}/edit`)}
                        >
                          Edit
                        </DropdownMenuItem>
                        
                        <DropdownMenuSeparator />
                        
                        {/* Email Polling Actions */}
                        {org.emailAddress && org.status === 'ACTIVE' && (
                          <>
                            <DropdownMenuItem 
                              onClick={() => handlePollOrganization(org.id, org.name)}
                              disabled={pollingOrgId === org.id}
                            >
                              {pollingOrgId === org.id ? (
                                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                              ) : (
                                <Mail className="w-4 h-4 mr-2" />
                              )}
                              Poll Emails
                            </DropdownMenuItem>
                            <DropdownMenuItem 
                              onClick={() => handleTestConnection(org.id, org.name)}
                              disabled={testingOrgId === org.id}
                            >
                              {testingOrgId === org.id ? (
                                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                              ) : (
                                <Wifi className="w-4 h-4 mr-2" />
                              )}
                              Test Email Connection
                            </DropdownMenuItem>
                          </>
                        )}
                        {!org.emailAddress && (
                          <DropdownMenuItem disabled>
                            <WifiOff className="w-4 h-4 mr-2 text-muted-foreground" />
                            <span className="text-muted-foreground">No email configured</span>
                          </DropdownMenuItem>
                        )}
                        
                        <DropdownMenuSeparator />
                        
                        {org.status !== 'ACTIVE' && (
                          <DropdownMenuItem onClick={() => handleActivate(org.id)}>
                            <CheckCircle className="w-4 h-4 mr-2" />
                            Activate
                          </DropdownMenuItem>
                        )}
                        {org.status === 'ACTIVE' && (
                          <DropdownMenuItem 
                            onClick={() => handleSuspend(org.id)}
                            className="text-red-600"
                          >
                            <XCircle className="w-4 h-4 mr-2" />
                            Suspend
                          </DropdownMenuItem>
                        )}
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))}
              {organizations.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8 text-muted-foreground">
                    No organizations yet. Click "Add Organization" to create one.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  )
}
