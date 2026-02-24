import * as React from 'react'
import { createPortal } from 'react-dom'
import { useState, useRef, useEffect, useCallback, useLayoutEffect } from 'react'

interface DropdownMenuProps {
  children: React.ReactNode
}

interface DropdownContextType {
  open: boolean
  setOpen: (open: boolean) => void
  triggerRef: React.RefObject<HTMLDivElement>
}

const DropdownContext = React.createContext<DropdownContextType>({
  open: false,
  setOpen: () => {},
  triggerRef: { current: null } as unknown as React.RefObject<HTMLDivElement>,
})

export function DropdownMenu({ children }: DropdownMenuProps) {
  const [open, setOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLDivElement>(null!)

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        containerRef.current &&
        !containerRef.current.contains(event.target as Node)
      ) {
        // also check if click is inside the portal content
        const portal = document.getElementById('dropdown-portal')
        if (portal && portal.contains(event.target as Node)) return
        setOpen(false)
      }
    }

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false)
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleEscape)
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleEscape)
    }
  }, [])

  return (
    <DropdownContext.Provider value={{ open, setOpen, triggerRef }}>
      <div ref={containerRef} className="relative inline-block">
        {children}
      </div>
    </DropdownContext.Provider>
  )
}

interface DropdownMenuTriggerProps {
  children: React.ReactNode
  asChild?: boolean
}

export function DropdownMenuTrigger({ children, asChild }: DropdownMenuTriggerProps) {
  const { open, setOpen, triggerRef } = React.useContext(DropdownContext)

  if (asChild && React.isValidElement(children)) {
    return (
      <div ref={triggerRef} className="inline-flex">
        {React.cloneElement(children as React.ReactElement<{ onClick?: () => void }>, {
          onClick: () => setOpen(!open),
        })}
      </div>
    )
  }

  return (
    <div ref={triggerRef} className="inline-flex">
      <button onClick={() => setOpen(!open)} type="button">
        {children}
      </button>
    </div>
  )
}

interface DropdownMenuContentProps {
  children: React.ReactNode
  align?: 'start' | 'end' | 'center'
  className?: string
}

export function DropdownMenuContent({ 
  children, 
  align = 'end',
  className = '' 
}: DropdownMenuContentProps) {
  const { open, setOpen, triggerRef } = React.useContext(DropdownContext)
  const contentRef = useRef<HTMLDivElement>(null)
  const [position, setPosition] = useState({ top: 0, left: 0 })

  const updatePosition = useCallback(() => {
    if (!triggerRef.current) return
    const rect = triggerRef.current.getBoundingClientRect()
    const contentEl = contentRef.current
    const contentWidth = contentEl?.offsetWidth || 180

    let left: number
    if (align === 'end') {
      left = rect.right - contentWidth
    } else if (align === 'start') {
      left = rect.left
    } else {
      left = rect.left + rect.width / 2 - contentWidth / 2
    }

    // Clamp to viewport
    left = Math.max(8, Math.min(left, window.innerWidth - contentWidth - 8))

    let top = rect.bottom + 4
    const contentHeight = contentEl?.offsetHeight || 200
    // Flip above if not enough space below
    if (top + contentHeight > window.innerHeight - 8) {
      top = rect.top - contentHeight - 4
    }

    setPosition({ top, left })
  }, [align, triggerRef])

  useLayoutEffect(() => {
    if (open) updatePosition()
  }, [open, updatePosition])

  useEffect(() => {
    if (!open) return
    window.addEventListener('scroll', updatePosition, true)
    window.addEventListener('resize', updatePosition)
    return () => {
      window.removeEventListener('scroll', updatePosition, true)
      window.removeEventListener('resize', updatePosition)
    }
  }, [open, updatePosition])

  // Close when clicking inside portal but outside content
  useEffect(() => {
    if (!open) return
    const handlePortalClick = (e: MouseEvent) => {
      if (contentRef.current && !contentRef.current.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handlePortalClick)
    return () => document.removeEventListener('mousedown', handlePortalClick)
  }, [open, setOpen])

  if (!open) return null

  return createPortal(
    <div
      id="dropdown-portal"
      ref={contentRef}
      style={{ position: 'fixed', top: position.top, left: position.left }}
      className={`z-[9999] min-w-[8rem] overflow-hidden rounded-md border bg-white p-1 shadow-lg animate-in fade-in-0 zoom-in-95 ${className}`}
    >
      {children}
    </div>,
    document.body
  )
}

interface DropdownMenuItemProps extends React.HTMLAttributes<HTMLDivElement> {
  disabled?: boolean
}

export function DropdownMenuItem({ 
  className = '', 
  disabled,
  onClick,
  ...props 
}: DropdownMenuItemProps) {
  const { setOpen } = React.useContext(DropdownContext)

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (!disabled && onClick) {
      onClick(e)
      setOpen(false)
    }
  }

  return (
    <div
      className={`relative flex cursor-pointer select-none items-center rounded-sm px-2 py-1.5 text-sm outline-none transition-colors hover:bg-gray-100 focus:bg-gray-100 ${
        disabled ? 'pointer-events-none opacity-50' : ''
      } ${className}`}
      onClick={handleClick}
      {...props}
    />
  )
}

export function DropdownMenuSeparator({ className = '' }: { className?: string }) {
  return <div className={`-mx-1 my-1 h-px bg-gray-200 ${className}`} />
}
