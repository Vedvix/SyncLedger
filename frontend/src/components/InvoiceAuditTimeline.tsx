import { useQuery } from '@tanstack/react-query'
import { invoiceService } from '@/services/invoiceService'
import type { InvoiceAuditEventType } from '@/types'
import {
  Mail,
  Upload,
  Cpu,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Edit3,
  Send,
  CloudOff,
  UserPlus,
  Archive,
  Link2,
  FileText,
  Loader2,
  Clock,
} from 'lucide-react'

/** Icon + color mapping for each audit event type */
const EVENT_CONFIG: Record<
  InvoiceAuditEventType,
  { icon: React.ElementType; color: string; bg: string; ring: string }
> = {
  RECEIVED_VIA_EMAIL: { icon: Mail, color: 'text-blue-600', bg: 'bg-blue-100', ring: 'ring-blue-200' },
  RECEIVED_VIA_UPLOAD: { icon: Upload, color: 'text-blue-600', bg: 'bg-blue-100', ring: 'ring-blue-200' },
  EXTRACTION_STARTED: { icon: Cpu, color: 'text-indigo-600', bg: 'bg-indigo-100', ring: 'ring-indigo-200' },
  EXTRACTION_COMPLETED: { icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-100', ring: 'ring-green-200' },
  EXTRACTION_FAILED: { icon: AlertTriangle, color: 'text-red-600', bg: 'bg-red-100', ring: 'ring-red-200' },
  STATUS_CHANGED: { icon: FileText, color: 'text-gray-600', bg: 'bg-gray-100', ring: 'ring-gray-200' },
  FIELD_UPDATED: { icon: Edit3, color: 'text-yellow-600', bg: 'bg-yellow-100', ring: 'ring-yellow-200' },
  SUBMITTED_FOR_REVIEW: { icon: FileText, color: 'text-blue-600', bg: 'bg-blue-100', ring: 'ring-blue-200' },
  APPROVED: { icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-100', ring: 'ring-green-200' },
  REJECTED: { icon: XCircle, color: 'text-red-600', bg: 'bg-red-100', ring: 'ring-red-200' },
  SYNC_STARTED: { icon: Send, color: 'text-purple-600', bg: 'bg-purple-100', ring: 'ring-purple-200' },
  SYNC_COMPLETED: { icon: CheckCircle, color: 'text-purple-600', bg: 'bg-purple-100', ring: 'ring-purple-200' },
  SYNC_FAILED: { icon: CloudOff, color: 'text-orange-600', bg: 'bg-orange-100', ring: 'ring-orange-200' },
  ASSIGNED: { icon: UserPlus, color: 'text-cyan-600', bg: 'bg-cyan-100', ring: 'ring-cyan-200' },
  EXPORTED: { icon: FileText, color: 'text-gray-600', bg: 'bg-gray-100', ring: 'ring-gray-200' },
  ARCHIVED: { icon: Archive, color: 'text-gray-500', bg: 'bg-gray-100', ring: 'ring-gray-200' },
  NOTE_ADDED: { icon: Edit3, color: 'text-yellow-600', bg: 'bg-yellow-100', ring: 'ring-yellow-200' },
  VENDOR_LINKED: { icon: Link2, color: 'text-teal-600', bg: 'bg-teal-100', ring: 'ring-teal-200' },
}

const DEFAULT_CONFIG = { icon: FileText, color: 'text-gray-600', bg: 'bg-gray-100', ring: 'ring-gray-200' }

function formatTimestamp(iso: string): string {
  const d = new Date(iso)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60_000)

  if (diffMin < 1) return 'Just now'
  if (diffMin < 60) return `${diffMin}m ago`
  const diffHours = Math.floor(diffMin / 60)
  if (diffHours < 24) return `${diffHours}h ago`
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays < 7) return `${diffDays}d ago`

  return d.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: d.getFullYear() !== now.getFullYear() ? 'numeric' : undefined,
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatFullTimestamp(iso: string): string {
  return new Date(iso).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

interface InvoiceAuditTimelineProps {
  invoiceId: number
  /** compact mode hides metadata details — for use in side panel */
  compact?: boolean
}

export function InvoiceAuditTimeline({ invoiceId, compact = false }: InvoiceAuditTimelineProps) {
  const { data: events, isLoading, error } = useQuery({
    queryKey: ['invoiceAuditTrail', invoiceId],
    queryFn: () => invoiceService.getAuditTrail(invoiceId),
    enabled: !!invoiceId,
    refetchInterval: 30_000, // auto-refresh every 30s
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="w-6 h-6 animate-spin text-primary-400" />
        <span className="ml-2 text-sm text-gray-500">Loading audit trail…</span>
      </div>
    )
  }

  if (error) {
    return (
      <div className="text-center py-8 text-red-500 text-sm">
        Failed to load audit trail
      </div>
    )
  }

  if (!events || events.length === 0) {
    return (
      <div className="text-center py-12 text-gray-400">
        <Clock className="w-10 h-10 mx-auto mb-3 opacity-40" />
        <p className="text-sm">No audit events recorded yet</p>
      </div>
    )
  }

  return (
    <div className="flow-root">
      <ul className="-mb-8">
        {events.map((event, idx) => {
          const config = EVENT_CONFIG[event.eventType] ?? DEFAULT_CONFIG
          const Icon = config.icon
          const isLast = idx === events.length - 1

          return (
            <li key={event.id}>
              <div className="relative pb-8">
                {/* Connector line */}
                {!isLast && (
                  <span
                    className="absolute left-4 top-8 -ml-px h-full w-0.5 bg-gray-200"
                    aria-hidden="true"
                  />
                )}
                <div className="relative flex items-start space-x-3">
                  {/* Icon */}
                  <div
                    className={`relative flex h-8 w-8 items-center justify-center rounded-full ring-2 ${config.bg} ${config.ring}`}
                  >
                    <Icon className={`h-4 w-4 ${config.color}`} />
                  </div>

                  {/* Content */}
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-medium text-gray-900">
                        {event.eventDisplayName}
                      </p>
                      <time
                        className="text-xs text-gray-500 whitespace-nowrap ml-2"
                        title={formatFullTimestamp(event.createdAt)}
                      >
                        {formatTimestamp(event.createdAt)}
                      </time>
                    </div>

                    <p className="mt-0.5 text-sm text-gray-600">{event.description}</p>

                    {/* Performer badge */}
                    {event.performedByName && (
                      <span className="mt-1 inline-flex items-center text-xs text-gray-500">
                        <span className="inline-block w-4 h-4 rounded-full bg-gray-300 text-[10px] font-bold text-white text-center leading-4 mr-1">
                          {event.performedByName.charAt(0).toUpperCase()}
                        </span>
                        {event.performedByName}
                      </span>
                    )}

                    {/* Status transition badge */}
                    {event.fromStatus && event.toStatus && event.fromStatus !== event.toStatus && (
                      <div className="mt-1 flex items-center gap-1 text-xs">
                        <span className="px-1.5 py-0.5 rounded bg-gray-100 text-gray-600">
                          {event.fromStatus.replace('_', ' ')}
                        </span>
                        <span className="text-gray-400">→</span>
                        <span className="px-1.5 py-0.5 rounded bg-primary-100 text-primary-700 font-medium">
                          {event.toStatus.replace('_', ' ')}
                        </span>
                      </div>
                    )}

                    {/* Metadata details — only in non-compact mode */}
                    {!compact && event.metadata && Object.keys(event.metadata).length > 0 && (
                      <div className="mt-2 rounded-lg bg-gray-50 border border-gray-100 p-2 text-xs text-gray-500 space-y-0.5">
                        {Object.entries(event.metadata).map(([key, val]) => (
                          <div key={key} className="flex justify-between">
                            <span className="text-gray-400 capitalize">{key.replace(/([A-Z])/g, ' $1').trim()}</span>
                            <span className="text-gray-700 font-mono truncate max-w-[60%] text-right">
                              {typeof val === 'object' ? JSON.stringify(val) : String(val)}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
