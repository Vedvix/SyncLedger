import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ArrowLeft,
  Cpu,
  DollarSign,
  Zap,
  TrendingUp,
  Building2,
  ChevronRight,
  Calendar,
  BarChart3,
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
import { Badge } from '@/components/ui/badge'
import { aiUsageService } from '@/services/aiUsageService'
import { useAuthStore } from '@/store/authStore'
import { useToast } from '@/components/ui/Toaster'
import type { PlatformAiUsageSummary, OrgAiUsageDetail } from '@/types'
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer
} from 'recharts'

type DatePreset = 'all' | '7d' | '30d' | '90d' | 'custom'

function getDateRange(preset: DatePreset): { startDate?: string; endDate?: string } {
  if (preset === 'all') return {}
  const end = new Date()
  const start = new Date()
  if (preset === '7d') start.setDate(end.getDate() - 7)
  else if (preset === '30d') start.setDate(end.getDate() - 30)
  else if (preset === '90d') start.setDate(end.getDate() - 90)
  return {
    startDate: start.toISOString().split('T')[0],
    endDate: end.toISOString().split('T')[0]
  }
}

function formatTokens(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(2)}M`
  if (count >= 1_000) return `${(count / 1_000).toFixed(1)}K`
  return count.toString()
}

function formatCost(cost: number): string {
  return `$${cost.toFixed(4)}`
}

export default function AiUsagePage() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { toast } = useToast()
  const [loading, setLoading] = useState(true)
  const [platformData, setPlatformData] = useState<PlatformAiUsageSummary | null>(null)
  const [selectedOrg, setSelectedOrg] = useState<number | null>(null)
  const [orgDetail, setOrgDetail] = useState<OrgAiUsageDetail | null>(null)
  const [orgDetailLoading, setOrgDetailLoading] = useState(false)
  const [datePreset, setDatePreset] = useState<DatePreset>('all')

  useEffect(() => {
    if (user?.role !== 'SUPER_ADMIN') {
      navigate('/dashboard')
      return
    }
    loadPlatformData()
  }, [user, navigate, datePreset])

  const loadPlatformData = async () => {
    try {
      setLoading(true)
      const { startDate, endDate } = getDateRange(datePreset)
      const data = await aiUsageService.getPlatformSummary(startDate, endDate)
      setPlatformData(data)
    } catch (error) {
      console.error('Failed to load AI usage data:', error)
      toast.error('Failed to load AI usage data')
    } finally {
      setLoading(false)
    }
  }

  const loadOrgDetail = async (orgId: number) => {
    try {
      setOrgDetailLoading(true)
      setSelectedOrg(orgId)
      const { startDate, endDate } = getDateRange(datePreset === 'all' ? '30d' : datePreset)
      const data = await aiUsageService.getOrganizationUsage(orgId, startDate, endDate)
      setOrgDetail(data)
    } catch (error) {
      console.error('Failed to load org detail:', error)
      toast.error('Failed to load organization detail')
    } finally {
      setOrgDetailLoading(false)
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
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="outline" size="icon" onClick={() => navigate('/super-admin')}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">AI Usage & Billing</h1>
            <p className="text-muted-foreground">
              Track AI token consumption and costs per organization
            </p>
          </div>
        </div>
        <div className="flex gap-2">
          {(['all', '7d', '30d', '90d'] as DatePreset[]).map(preset => (
            <Button
              key={preset}
              variant={datePreset === preset ? 'default' : 'outline'}
              size="sm"
              onClick={() => { setDatePreset(preset); setSelectedOrg(null); setOrgDetail(null) }}
            >
              {preset === 'all' ? 'All Time' : preset === '7d' ? '7 Days' : preset === '30d' ? '30 Days' : '90 Days'}
            </Button>
          ))}
        </div>
      </div>

      {/* Platform Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Extractions</CardTitle>
            <Cpu className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{platformData?.totalExtractions || 0}</div>
            <p className="text-xs text-muted-foreground">AI-powered invoice extractions</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Tokens Used</CardTitle>
            <Zap className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatTokens(platformData?.totalTokens || 0)}</div>
            <p className="text-xs text-muted-foreground">
              {formatTokens(platformData?.totalInputTokens || 0)} input / {formatTokens(platformData?.totalOutputTokens || 0)} output
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total AI Cost</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatCost(platformData?.totalCostUsd || 0)}</div>
            <p className="text-xs text-muted-foreground">OpenAI API charges</p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Cost / Extraction</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {platformData && platformData.totalExtractions > 0
                ? formatCost(platformData.totalCostUsd / platformData.totalExtractions)
                : '$0.00'}
            </div>
            <p className="text-xs text-muted-foreground">Per invoice processed</p>
          </CardContent>
        </Card>
      </div>

      {/* Per-Organization Usage Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Building2 className="h-5 w-5" />
            Usage by Organization
          </CardTitle>
        </CardHeader>
        <CardContent>
          {platformData?.organizationUsage && platformData.organizationUsage.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Organization</TableHead>
                  <TableHead className="text-right">Extractions</TableHead>
                  <TableHead className="text-right">Input Tokens</TableHead>
                  <TableHead className="text-right">Output Tokens</TableHead>
                  <TableHead className="text-right">Total Tokens</TableHead>
                  <TableHead className="text-right">Cost (USD)</TableHead>
                  <TableHead className="text-right">Avg Cost</TableHead>
                  <TableHead></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {platformData.organizationUsage.map((org) => (
                  <TableRow
                    key={org.organizationId}
                    className={`cursor-pointer hover:bg-muted/50 ${selectedOrg === org.organizationId ? 'bg-muted' : ''}`}
                    onClick={() => loadOrgDetail(org.organizationId)}
                  >
                    <TableCell className="font-medium">{org.organizationName}</TableCell>
                    <TableCell className="text-right">{org.totalExtractions}</TableCell>
                    <TableCell className="text-right">{formatTokens(org.totalInputTokens)}</TableCell>
                    <TableCell className="text-right">{formatTokens(org.totalOutputTokens)}</TableCell>
                    <TableCell className="text-right">
                      <Badge variant="secondary">{formatTokens(org.totalTokens)}</Badge>
                    </TableCell>
                    <TableCell className="text-right font-mono">{formatCost(org.totalCostUsd)}</TableCell>
                    <TableCell className="text-right font-mono text-muted-foreground">
                      {org.totalExtractions > 0 ? formatCost(org.totalCostUsd / org.totalExtractions) : '-'}
                    </TableCell>
                    <TableCell>
                      <ChevronRight className="h-4 w-4 text-muted-foreground" />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <div className="text-center py-12 text-muted-foreground">
              <Cpu className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p className="text-lg font-medium">No AI usage data yet</p>
              <p className="text-sm">Usage will appear here as invoices are processed with AI extraction</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Organization Detail Panel */}
      {selectedOrg && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="flex items-center gap-2">
                <BarChart3 className="h-5 w-5" />
                {orgDetailLoading ? 'Loading...' : `${orgDetail?.organizationName || 'Organization'} — Usage Detail`}
              </CardTitle>
              <Button variant="ghost" size="sm" onClick={() => { setSelectedOrg(null); setOrgDetail(null) }}>
                Close
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {orgDetailLoading ? (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
              </div>
            ) : orgDetail ? (
              <div className="space-y-6">
                {/* Summary Row */}
                <div className="grid gap-4 md:grid-cols-4">
                  <div className="rounded-lg border p-3">
                    <p className="text-sm text-muted-foreground">Extractions</p>
                    <p className="text-xl font-bold">{orgDetail.summary.totalExtractions}</p>
                  </div>
                  <div className="rounded-lg border p-3">
                    <p className="text-sm text-muted-foreground">Total Tokens</p>
                    <p className="text-xl font-bold">{formatTokens(orgDetail.summary.totalTokens)}</p>
                  </div>
                  <div className="rounded-lg border p-3">
                    <p className="text-sm text-muted-foreground">Total Cost</p>
                    <p className="text-xl font-bold">{formatCost(orgDetail.summary.totalCostUsd)}</p>
                  </div>
                  <div className="rounded-lg border p-3">
                    <p className="text-sm text-muted-foreground">Avg Cost / Extraction</p>
                    <p className="text-xl font-bold">
                      {orgDetail.summary.totalExtractions > 0
                        ? formatCost(orgDetail.summary.totalCostUsd / orgDetail.summary.totalExtractions)
                        : '$0.00'}
                    </p>
                  </div>
                </div>

                {/* Daily Usage Chart */}
                {orgDetail.dailyUsage.length > 0 && (
                  <div>
                    <h3 className="text-sm font-medium mb-3 flex items-center gap-2">
                      <Calendar className="h-4 w-4" />
                      Daily Usage
                    </h3>
                    <div className="h-64">
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={orgDetail.dailyUsage}>
                          <CartesianGrid strokeDasharray="3 3" />
                          <XAxis
                            dataKey="date"
                            tickFormatter={(v: string) => {
                              const d = new Date(v)
                              return `${d.getMonth() + 1}/${d.getDate()}`
                            }}
                            fontSize={12}
                          />
                          <YAxis fontSize={12} />
                          <Tooltip
                            labelFormatter={(label: string) => new Date(label).toLocaleDateString()}
                            formatter={(value: number, name: string) => [
                              name === 'totalTokens' ? formatTokens(value) : name === 'costUsd' ? formatCost(value) : value,
                              name === 'totalTokens' ? 'Tokens' : name === 'costUsd' ? 'Cost' : 'Extractions'
                            ]}
                          />
                          <Bar dataKey="extractions" fill="#6366f1" name="extractions" radius={[4, 4, 0, 0]} />
                          <Bar dataKey="totalTokens" fill="#22c55e" name="totalTokens" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                  </div>
                )}

                {/* Tier Breakdown */}
                {orgDetail.tierBreakdown.length > 0 && (
                  <div>
                    <h3 className="text-sm font-medium mb-3">AI Tier Breakdown</h3>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Tier</TableHead>
                          <TableHead className="text-right">Extractions</TableHead>
                          <TableHead className="text-right">Tokens</TableHead>
                          <TableHead className="text-right">Cost</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {orgDetail.tierBreakdown.map((tier) => (
                          <TableRow key={tier.tier}>
                            <TableCell>
                              <Badge variant={tier.tier === 'vision' ? 'default' : 'secondary'}>
                                {tier.tier}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-right">{tier.extractions}</TableCell>
                            <TableCell className="text-right">{formatTokens(tier.totalTokens)}</TableCell>
                            <TableCell className="text-right font-mono">{formatCost(tier.costUsd)}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                )}
              </div>
            ) : null}
          </CardContent>
        </Card>
      )}
    </div>
  )
}
