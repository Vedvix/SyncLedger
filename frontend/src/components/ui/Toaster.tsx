import * as React from 'react'
import { CheckCircle2, XCircle, Info, X } from 'lucide-react'

export interface ToasterProps {}

interface ToastItem {
  id: string
  title: string
  variant: 'default' | 'destructive' | 'success'
}

const ToastContext = React.createContext<{
  toasts: ToastItem[]
  addToast: (toast: Omit<ToastItem, 'id'>) => void
  removeToast: (id: string) => void
}>({
  toasts: [],
  addToast: () => {},
  removeToast: () => {},
})

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = React.useState<ToastItem[]>([])

  const addToast = React.useCallback((toast: Omit<ToastItem, 'id'>) => {
    const id = Math.random().toString(36).substring(7)
    setToasts((prev) => [...prev, { ...toast, id }])
    
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 4000)
  }, [])

  const removeToast = React.useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return (
    <ToastContext.Provider value={{ toasts, addToast, removeToast }}>
      {children}
    </ToastContext.Provider>
  )
}

const toastStyles = {
  destructive: {
    bg: 'bg-white border-red-200',
    icon: XCircle,
    iconColor: 'text-red-500',
    text: 'text-gray-900',
  },
  success: {
    bg: 'bg-white border-emerald-200',
    icon: CheckCircle2,
    iconColor: 'text-emerald-500',
    text: 'text-gray-900',
  },
  default: {
    bg: 'bg-white border-gray-200',
    icon: Info,
    iconColor: 'text-primary-500',
    text: 'text-gray-900',
  },
}

export function Toaster(_props: ToasterProps) {
  const { toasts, removeToast } = React.useContext(ToastContext)

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2.5 pointer-events-none">
      {toasts.map((toast) => {
        const style = toastStyles[toast.variant]
        const Icon = style.icon
        return (
          <div
            key={toast.id}
            className={`pointer-events-auto px-4 py-3 rounded-2xl shadow-lg border min-w-[320px] max-w-[420px] flex items-center gap-3 animate-slide-in ${style.bg}`}
          >
            <Icon className={`w-5 h-5 flex-shrink-0 ${style.iconColor}`} />
            <span className={`text-sm font-medium flex-1 ${style.text}`}>{toast.title}</span>
            <button
              onClick={() => removeToast(toast.id)}
              className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors flex-shrink-0"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )
      })}
    </div>
  )
}

export function useToast() {
  const { addToast } = React.useContext(ToastContext)

  const toast = React.useMemo(
    () => ({
      success: (title: string) => addToast({ title, variant: 'success' }),
      error: (title: string) => addToast({ title, variant: 'destructive' }),
      default: (title: string) => addToast({ title, variant: 'default' }),
    }),
    [addToast]
  )

  return { toast }
}
