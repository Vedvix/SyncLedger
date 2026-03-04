import * as React from 'react'

interface TableProps extends React.HTMLAttributes<HTMLTableElement> {}

export function Table({ className = '', ...props }: TableProps) {
  return (
    <div className="relative w-full overflow-auto">
      <table className={`w-full caption-bottom text-sm ${className}`} {...props} />
    </div>
  )
}

interface TableHeaderProps extends React.HTMLAttributes<HTMLTableSectionElement> {}

export function TableHeader({ className = '', ...props }: TableHeaderProps) {
  return <thead className={`border-b border-gray-100 bg-gray-50/80 ${className}`} {...props} />
}

interface TableBodyProps extends React.HTMLAttributes<HTMLTableSectionElement> {}

export function TableBody({ className = '', ...props }: TableBodyProps) {
  return <tbody className={`divide-y divide-gray-50 ${className}`} {...props} />
}

interface TableRowProps extends React.HTMLAttributes<HTMLTableRowElement> {}

export function TableRow({ className = '', ...props }: TableRowProps) {
  return (
    <tr
      className={`border-b border-gray-50 transition-colors hover:bg-primary-50/30 ${className}`}
      {...props}
    />
  )
}

interface TableHeadProps extends React.ThHTMLAttributes<HTMLTableCellElement> {}

export function TableHead({ className = '', ...props }: TableHeadProps) {
  return (
    <th
      className={`h-11 px-4 text-left align-middle font-semibold text-gray-400 text-[11px] uppercase tracking-wider ${className}`}
      {...props}
    />
  )
}

interface TableCellProps extends React.TdHTMLAttributes<HTMLTableCellElement> {}

export function TableCell({ className = '', ...props }: TableCellProps) {
  return (
    <td className={`p-4 align-middle text-gray-700 ${className}`} {...props} />
  )
}
