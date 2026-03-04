import * as React from 'react'

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'default' | 'secondary' | 'destructive' | 'outline' | 'success' | 'warning' | 'info'
  dot?: boolean
}

export function Badge({
  className = '',
  variant = 'default',
  dot = false,
  ...props
}: BadgeProps) {
  const variantStyles = {
    default: 'bg-primary-50 text-primary-700 border border-primary-100',
    secondary: 'bg-gray-50 text-gray-700 border border-gray-100',
    destructive: 'bg-red-50 text-red-700 border border-red-100',
    outline: 'bg-transparent border border-gray-200 text-gray-600',
    success: 'bg-emerald-50 text-emerald-700 border border-emerald-100',
    warning: 'bg-amber-50 text-amber-700 border border-amber-100',
    info: 'bg-blue-50 text-blue-700 border border-blue-100',
  }
  
  const dotColors = {
    default: 'bg-primary-500',
    secondary: 'bg-gray-500',
    destructive: 'bg-red-500',
    outline: 'bg-gray-500',
    success: 'bg-emerald-500',
    warning: 'bg-amber-500',
    info: 'bg-blue-500',
  }
  
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium ${variantStyles[variant]} ${className}`}
      {...props}
    >
      {dot && <span className={`w-1.5 h-1.5 rounded-full ${dotColors[variant]}`} />}
      {props.children}
    </span>
  )
}
