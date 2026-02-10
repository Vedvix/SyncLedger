import * as React from 'react'

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
    
    // Auto-dismiss after 3 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id))
    }, 3000)
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

export function Toaster(_props: ToasterProps) {
  const { toasts, removeToast } = React.useContext(ToastContext)

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`px-4 py-3 rounded-lg shadow-lg text-white min-w-[300px] flex items-center justify-between ${
            toast.variant === 'destructive'
              ? 'bg-red-600'
              : toast.variant === 'success'
              ? 'bg-green-600'
              : 'bg-gray-800'
          }`}
        >
          <span>{toast.title}</span>
          <button
            onClick={() => removeToast(toast.id)}
            className="ml-4 text-white/80 hover:text-white"
          >
            ×
          </button>
        </div>
      ))}
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
