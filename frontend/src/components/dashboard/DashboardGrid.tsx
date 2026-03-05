import { useState, useRef, useCallback } from 'react'
import { GripVertical, Pencil, Trash2, Maximize2, Minimize2 } from 'lucide-react'
import type { WidgetConfig } from '@/types/dashboard'
import type { DashboardStats } from '@/types'
import { useDashboardStore } from '@/store/dashboardStore'
import { WidgetRenderer } from './WidgetRenderer'

interface DashboardGridProps {
  widgets: WidgetConfig[]
  stats: DashboardStats | undefined
  editMode: boolean
  onEditWidget: (widget: WidgetConfig) => void
}

// Map colSpan to tailwind-like classes (we use inline styles for the 12-col grid)
function colSpanToStyle(colSpan: number): React.CSSProperties {
  return {
    gridColumn: `span ${Math.min(colSpan, 12)} / span ${Math.min(colSpan, 12)}`,
  }
}

export function DashboardGrid({ widgets, stats, editMode, onEditWidget }: DashboardGridProps) {
  const { removeWidget, reorderWidgets, resizeWidget } = useDashboardStore()
  const [draggedId, setDraggedId] = useState<string | null>(null)
  const [dropTargetId, setDropTargetId] = useState<string | null>(null)
  const dragCounter = useRef<Map<string, number>>(new Map())

  const sortedWidgets = [...widgets]
    .filter(w => w.visible)
    .sort((a, b) => a.order - b.order)

  // ─── Drag & Drop ───
  const handleDragStart = useCallback((e: React.DragEvent, id: string) => {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', id)
    setDraggedId(id)
  }, [])

  const handleDragEnter = useCallback((e: React.DragEvent, id: string) => {
    e.preventDefault()
    const count = (dragCounter.current.get(id) || 0) + 1
    dragCounter.current.set(id, count)
    if (id !== draggedId) setDropTargetId(id)
  }, [draggedId])

  const handleDragLeave = useCallback((_e: React.DragEvent, id: string) => {
    const count = (dragCounter.current.get(id) || 0) - 1
    dragCounter.current.set(id, count)
    if (count <= 0) {
      dragCounter.current.set(id, 0)
      if (dropTargetId === id) setDropTargetId(null)
    }
  }, [dropTargetId])

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }, [])

  const handleDrop = useCallback((e: React.DragEvent, targetId: string) => {
    e.preventDefault()
    dragCounter.current.clear()
    setDropTargetId(null)
    setDraggedId(null)

    const sourceId = e.dataTransfer.getData('text/plain')
    if (!sourceId || sourceId === targetId) return

    const ids = sortedWidgets.map(w => w.id)
    const sourceIdx = ids.indexOf(sourceId)
    const targetIdx = ids.indexOf(targetId)
    if (sourceIdx < 0 || targetIdx < 0) return

    // Reorder
    ids.splice(sourceIdx, 1)
    ids.splice(targetIdx, 0, sourceId)
    reorderWidgets(ids)
  }, [sortedWidgets, reorderWidgets])

  const handleDragEnd = useCallback(() => {
    dragCounter.current.clear()
    setDraggedId(null)
    setDropTargetId(null)
  }, [])

  // Cycling size
  const cycleSizeUp = (widget: WidgetConfig) => {
    const sizeOrder: typeof widget.size[] = ['small', 'medium', 'large', 'full']
    const idx = sizeOrder.indexOf(widget.size)
    const next = sizeOrder[Math.min(idx + 1, sizeOrder.length - 1)]
    resizeWidget(widget.id, next)
  }

  const cycleSizeDown = (widget: WidgetConfig) => {
    const sizeOrder: typeof widget.size[] = ['small', 'medium', 'large', 'full']
    const idx = sizeOrder.indexOf(widget.size)
    const next = sizeOrder[Math.max(idx - 1, 0)]
    resizeWidget(widget.id, next)
  }

  if (sortedWidgets.length === 0) {
    return (
      <div className="bg-[#181824] rounded-lg border-2 border-dashed border-gray-700 p-12 text-center">
        <p className="text-gray-400 text-lg mb-2">No widgets added yet</p>
        <p className="text-gray-500 text-sm">Click "Add Widget" to start building your dashboard</p>
      </div>
    )
  }

  return (
    <div
      className="grid gap-5"
      style={{
        gridTemplateColumns: 'repeat(12, minmax(0, 1fr))',
      }}
    >
      {sortedWidgets.map(widget => (
        <div
          key={widget.id}
          style={colSpanToStyle(widget.colSpan)}
          draggable={editMode}
          onDragStart={e => handleDragStart(e, widget.id)}
          onDragEnter={e => handleDragEnter(e, widget.id)}
          onDragLeave={e => handleDragLeave(e, widget.id)}
          onDragOver={handleDragOver}
          onDrop={e => handleDrop(e, widget.id)}
          onDragEnd={handleDragEnd}
          className={`relative group transition-all ${
            editMode ? 'ring-1 ring-gray-600/50 ring-offset-1 ring-offset-[#111119] rounded-lg' : ''
          } ${
            draggedId === widget.id ? 'opacity-40 scale-95' : ''
          } ${
            dropTargetId === widget.id ? 'ring-2 ring-blue-500/60 ring-offset-2 ring-offset-[#111119]' : ''
          }`}
        >
          {/* Edit overlay controls */}
          {editMode && (
            <div className="absolute top-2 right-2 z-10 flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
              <button
                onClick={() => cycleSizeDown(widget)}
                className="p-1.5 bg-[#1e1e2f] rounded-md shadow-sm border border-gray-600/50 hover:bg-[#252538] text-gray-400"
                title="Shrink"
              >
                <Minimize2 className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => cycleSizeUp(widget)}
                className="p-1.5 bg-[#1e1e2f] rounded-md shadow-sm border border-gray-600/50 hover:bg-[#252538] text-gray-400"
                title="Expand"
              >
                <Maximize2 className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => onEditWidget(widget)}
                className="p-1.5 bg-[#1e1e2f] rounded-md shadow-sm border border-gray-600/50 hover:bg-blue-900/30 text-blue-400"
                title="Edit"
              >
                <Pencil className="w-3.5 h-3.5" />
              </button>
              <button
                onClick={() => removeWidget(widget.id)}
                className="p-1.5 bg-[#1e1e2f] rounded-md shadow-sm border border-gray-600/50 hover:bg-red-900/30 text-red-400"
                title="Remove"
              >
                <Trash2 className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Drag handle */}
          {editMode && (
            <div className="absolute top-2 left-2 z-10 opacity-0 group-hover:opacity-100 transition-opacity cursor-grab active:cursor-grabbing">
              <div className="p-1.5 bg-[#1e1e2f] rounded-md shadow-sm border border-gray-600/50 text-gray-500">
                <GripVertical className="w-3.5 h-3.5" />
              </div>
            </div>
          )}

          <WidgetRenderer widget={widget} stats={stats} editMode={editMode} />
        </div>
      ))}
    </div>
  )
}
