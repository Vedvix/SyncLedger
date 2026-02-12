/**
 * TypeScript types for SyncLedger frontend
 * 
 * @author vedvix
 */

// ==================== Organization Types (Multi-Tenant) ====================

export type OrganizationStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'ONBOARDING'

export interface Organization {
  id: number
  name: string
  slug: string
  emailAddress?: string
  status: OrganizationStatus
  sageApiEndpoint?: string
  s3FolderPath?: string
  sqsQueueName?: string
  createdAt: string
  updatedAt: string
}

export interface CreateOrganizationRequest {
  name: string
  slug: string
  emailAddress?: string
  sageApiEndpoint?: string
  sageApiKey?: string
}

export interface UpdateOrganizationRequest {
  name?: string
  emailAddress?: string
  status?: OrganizationStatus
  sageApiEndpoint?: string
  sageApiKey?: string
}

export interface OrganizationStats {
  organizationId: number
  organizationName: string
  totalUsers: number
  activeUsers: number
  totalInvoices: number
  status: string
  createdAt: string
}

export interface PlatformStats {
  totalOrganizations: number
  activeOrganizations: number
  totalUsers: number
  totalInvoices: number
}

// ==================== User Types ====================

export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'APPROVER' | 'VIEWER'

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  role: UserRole
  isActive: boolean
  profilePictureUrl?: string
  phone?: string
  department?: string
  jobTitle?: string
  lastLoginAt?: string
  createdAt: string
  // Organization context
  organizationId?: number
  organizationSlug?: string
  organizationName?: string
}

export interface CreateUserRequest {
  firstName: string
  lastName: string
  email: string
  password: string
  role: UserRole
  organizationId?: number
  phone?: string
  department?: string
  jobTitle?: string
}

export interface UpdateUserRequest {
  firstName?: string
  lastName?: string
  role?: UserRole
  isActive?: boolean
  phone?: string
  department?: string
  jobTitle?: string
}

// ==================== Invoice Types ====================

export type InvoiceStatus = 
  | 'PENDING'
  | 'UNDER_REVIEW'
  | 'APPROVED'
  | 'REJECTED'
  | 'SYNCED'
  | 'SYNC_FAILED'
  | 'ARCHIVED'

export type SyncStatus = 
  | 'PENDING'
  | 'IN_PROGRESS'
  | 'SUCCESS'
  | 'FAILED'
  | 'RETRYING'

export interface InvoiceLineItem {
  id?: number
  lineNumber: number
  description?: string
  itemCode?: string
  unit?: string
  quantity?: number
  unitPrice?: number
  taxRate?: number
  taxAmount?: number
  discountAmount?: number
  lineTotal: number
  glAccountCode?: string
  costCenter?: string
}

export interface Invoice {
  id: number
  organizationId: number
  invoiceNumber: string
  poNumber?: string
  
  // Vendor
  vendorId?: number
  vendorName: string
  vendorAddress?: string
  vendorEmail?: string
  vendorPhone?: string
  vendorTaxId?: string
  
  // Financial
  subtotal: number
  taxAmount: number
  discountAmount: number
  shippingAmount: number
  totalAmount: number
  currency: string
  
  // Dates
  invoiceDate: string
  dueDate?: string
  receivedDate?: string
  
  // Status
  status: InvoiceStatus
  confidenceScore?: number
  requiresManualReview: boolean
  reviewNotes?: string
  
  // File
  originalFileName: string
  s3Url?: string
  fileSizeBytes?: number
  pageCount?: number
  
  // Email source
  sourceEmailFrom?: string
  sourceEmailSubject?: string
  sourceEmailReceivedAt?: string
  
  // Extraction
  extractionMethod?: string
  extractedAt?: string
  
  // Sage
  sageInvoiceId?: string
  syncStatus?: SyncStatus
  lastSyncAttempt?: string
  syncErrorMessage?: string
  
  // Line items
  lineItems: InvoiceLineItem[]
  
  // Mapping Fields
  glAccount?: string
  project?: string
  itemCategory?: string
  location?: string
  costCenter?: string
  mappingProfileId?: string
  fieldMappings?: string  // JSON string of mapping trace
  
  // Assignment
  assignedToId?: number
  assignedToName?: string
  
  // Audit
  createdAt: string
  updatedAt: string
  
  // Computed
  daysUntilDue?: number
  isOverdue?: boolean
  isEditable?: boolean
}

export interface UpdateInvoiceRequest {
  invoiceNumber?: string
  poNumber?: string
  vendorName?: string
  vendorAddress?: string
  vendorEmail?: string
  vendorPhone?: string
  subtotal?: number
  taxAmount?: number
  discountAmount?: number
  shippingAmount?: number
  totalAmount?: number
  invoiceDate?: string
  dueDate?: string
  reviewNotes?: string
  lineItems?: InvoiceLineItem[]
  glAccount?: string
  project?: string
  itemCategory?: string
  location?: string
  costCenter?: string
}

// ==================== Approval Types ====================

export type ApprovalAction = 'APPROVED' | 'REJECTED' | 'ESCALATED' | 'RETURNED_FOR_REVIEW'

export interface Approval {
  id: number
  invoiceId: number
  invoiceNumber: string
  approverId: number
  approverName: string
  approverEmail: string
  action: ApprovalAction
  comments?: string
  rejectionReason?: string
  approvalLevel?: number
  createdAt: string
}

export interface ApprovalRequest {
  action: ApprovalAction
  comments?: string
  rejectionReason?: string
}

// ==================== Dashboard Types ====================

export interface MonthlyStats {
  year: number
  month: number
  monthName: string
  invoiceCount: number
  totalAmount: number
}

export interface VendorStats {
  vendorName: string
  invoiceCount: number
  totalAmount: number
}

export interface DashboardStats {
  // Counts
  totalInvoices: number
  pendingInvoices: number
  underReviewInvoices: number
  approvedInvoices: number
  rejectedInvoices: number
  syncedInvoices: number
  overdueInvoices: number
  
  // Amounts
  totalAmount: number
  pendingAmount: number
  approvedAmount: number
  syncedAmount: number
  
  // Processing
  invoicesProcessedToday: number
  invoicesProcessedThisWeek: number
  invoicesProcessedThisMonth: number
  averageProcessingTimeMs: number
  
  // Email
  emailsProcessedToday: number
  unprocessedEmails: number
  emailsWithErrors: number
  
  // Sync
  pendingSyncs: number
  failedSyncs: number
  successfulSyncsToday: number
  syncSuccessRate: number
  
  // Users
  activeUsers: number
  totalUsers: number
  
  // Charts
  monthlyTrends: MonthlyStats[]
  topVendors: VendorStats[]
  invoicesByStatus: Record<string, number>
}

// ==================== API Response Types ====================

export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
  errors?: string[]
  timestamp: string
  path?: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext: boolean
  hasPrevious: boolean
}

// ==================== Auth Types ====================

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

// ==================== Filter Types ====================

export interface InvoiceFilters {
  search?: string
  status?: InvoiceStatus[]
  vendorName?: string
  dateFrom?: string
  dateTo?: string
  minAmount?: number
  maxAmount?: number
  assignedToId?: number
  requiresManualReview?: boolean
}

export interface PaginationParams {
  page: number
  size: number
  sort?: string
  direction?: 'asc' | 'desc'
}

// ==================== Email Polling Types ====================

export interface EmailPollingStatus {
  enabled: boolean
  lastPollTime?: string
  totalEmailsProcessed: number
  activeOrganizations: number
  pollingIntervalMinutes: number
}

export interface EmailLogDTO {
  id: number
  messageId: string
  subject: string
  fromEmail: string
  receivedAt: string
  isProcessed: boolean
  hasError: boolean
  errorMessage?: string
  attachmentCount: number
  organizationId?: number
  organizationName?: string
}

export interface EmailStatsDTO {
  totalEmails: number
  processedEmails: number
  failedEmails: number
  pendingEmails: number
}

// ==================== Mapping Profile Types ====================

export type DateTransform = 
  | 'NONE'
  | 'NEXT_FRIDAY'
  | 'NEXT_MONDAY'
  | 'NEXT_BUSINESS_DAY'
  | 'END_OF_MONTH'
  | 'ADD_30_DAYS'
  | 'ADD_60_DAYS'
  | 'ADD_90_DAYS'
  | 'NET_30'
  | 'NET_60'

export interface FieldMappingRule {
  source: string
  target: string
  fallbackSource?: string
  defaultValue?: string
  dateTransform?: DateTransform
  description?: string
}

export interface MappingProfile {
  id: string
  name: string
  description?: string
  vendorPattern?: string
  organizationId?: number
  isDefault: boolean
  rules: FieldMappingRule[]
  createdAt?: string
  updatedAt?: string
}

export interface MappingProfileCreateRequest {
  id?: string
  name: string
  description?: string
  vendorPattern?: string
  organizationId?: number
  isDefault?: boolean
  rules: FieldMappingRule[]
}

export interface MappingProfileUpdateRequest {
  name?: string
  description?: string
  vendorPattern?: string
  isDefault?: boolean
  rules?: FieldMappingRule[]
}

export interface MappingFieldInfo {
  sourceFields: string[]
  targetFields: string[]
  dateTransforms: string[]
}

// ==================== Vendor Types ====================

export type VendorStatus = 'ACTIVE' | 'INACTIVE' | 'BLOCKED' | 'PENDING_REVIEW'

export interface Vendor {
  id: number
  organizationId: number
  organizationName?: string
  name: string
  code?: string
  address?: string
  email?: string
  phone?: string
  contactPerson?: string
  website?: string
  taxId?: string
  paymentTerms?: string
  currency: string
  status: VendorStatus
  notes?: string
  createdAt: string
  updatedAt: string
  analytics?: VendorAnalytics
}

export interface VendorAnalytics {
  totalInvoices: number
  pendingInvoices: number
  approvedInvoices: number
  rejectedInvoices: number
  syncedInvoices: number
  totalAmount: number
  averageInvoiceAmount: number
  minInvoiceAmount: number
  maxInvoiceAmount: number
  totalTaxAmount: number
  averageConfidenceScore: number
  invoicesRequiringReview: number
  firstInvoiceDate?: string
  lastInvoiceDate?: string
  monthlyTotals: Record<string, number>
}

export interface VendorSummary {
  totalVendors: number
  activeVendors: number
  totalInvoicesAcrossVendors: number
  totalAmountAcrossVendors: number
  averageAmountPerVendor: number
  topVendorsByCount: TopVendor[]
  topVendorsByAmount: TopVendor[]
}

export interface TopVendor {
  vendorId: number
  vendorName: string
  invoiceCount: number
  totalAmount: number
  averageAmount: number
}

export interface VendorRequest {
  name: string
  code?: string
  address?: string
  email?: string
  phone?: string
  contactPerson?: string
  website?: string
  taxId?: string
  paymentTerms?: string
  currency?: string
  status?: string
  notes?: string
}
