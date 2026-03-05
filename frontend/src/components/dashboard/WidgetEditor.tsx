import { useState, useEffect, useMemo } from 'react'
import { X, Palette } from 'lucide-react'
import type { WidgetConfig, ChartType, DataSourceKey, WidgetSize } from '@/types/dashboard'
import { DATA_SOURCES, CHART_TYPE_LABELS, SIZE_OPTIONS, WIDGET_COLORS } from '@/types/dashboard'
import { useDashboardStore } from '@/store/dashboardStore'

interface WidgetEditorProps {
  isOpen: boolean
  onClose: () => void
  /** If provided, editing an existing widget. Otherwise creating new. */
  existingWidget?: WidgetConfig
}

export function WidgetEditor({ isOpen, onClose, existingWidget }: WidgetEditorProps) {
  const { addWidget, updateWidget } = useDashboardStore()

  const [title, setTitle] = useState('')
  const [dataSource, setDataSource] = useState<DataSourceKey>('totalInvoices')
  const [chartType, setChartType] = useState<ChartType>('kpiCard')
  const [size, setSize] = useState<WidgetSize>('small')
  const [color, setColor] = useState('#3b82f6')
  const [subtitle, setSubtitle] = useState('')
  const [clickUrl, setClickUrl] = useState('')
  const [showColorPicker, setShowColorPicker] = useState(false)
  const [xAxisField, setXAxisField] = useState<string>('')
  const [yAxisFields, setYAxisFields] = useState<string[]>([])

  // Load existing widget values
  useEffect(() => {
    if (existingWidget) {
      setTitle(existingWidget.title)
      setDataSource(existingWidget.dataSource)
      setChartType(existingWidget.chartType)
      setSize(existingWidget.size)
      setColor(existingWidget.color || '#3b82f6')
      setSubtitle(existingWidget.subtitle || '')
      setClickUrl(existingWidget.clickUrl || '')
      setXAxisField(existingWidget.xAxisField || '')
      setYAxisFields(existingWidget.yAxisFields || [])
    } else {
      setTitle('')
      setDataSource('totalInvoices')
      setChartType('kpiCard')
      setSize('small')
      setColor('#3b82f6')
      setSubtitle('')
      setClickUrl('')
      setXAxisField('')
      setYAxisFields([])
    }
  }, [existingWidget, isOpen])

  // Get compatible chart types for selected data source
  const selectedSource = useMemo(
    () => DATA_SOURCES.find(d => d.key === dataSource),
    [dataSource]
  )

  const compatibleCharts = selectedSource?.compatibleCharts || ['kpiCard']

  // When data source changes, auto-set chart type & title
  useEffect(() => {
    if (!existingWidget && selectedSource) {
      setChartType(selectedSource.defaultChart)
      if (!title) setTitle(selectedSource.label)
      // Auto-set default axes
      if (selectedSource.defaultXAxis) setXAxisField(selectedSource.defaultXAxis)
      if (selectedSource.defaultYAxis) setYAxisFields(selectedSource.defaultYAxis)
    }
  }, [dataSource, selectedSource])

  // Ensure chart type is compatible when data source changes
  useEffect(() => {
    if (!compatibleCharts.includes(chartType)) {
      setChartType(compatibleCharts[0])
    }
  }, [compatibleCharts, chartType])

  const handleSave = () => {
    const sizeOpt = SIZE_OPTIONS.find(s => s.value === size)
    const widgetData = {
      title: title || selectedSource?.label || 'Widget',
      chartType,
      dataSource,
      size,
      colSpan: sizeOpt?.colSpan || 3,
      rowSpan: 1,
      color,
      subtitle: subtitle || undefined,
      clickUrl: clickUrl || undefined,
      visible: true,
      xAxisField: xAxisField || undefined,
      yAxisFields: yAxisFields.length > 0 ? yAxisFields : undefined,
    }

    if (existingWidget) {
      updateWidget(existingWidget.id, widgetData)
    } else {
      addWidget(widgetData)
    }
    onClose()
  }

  // Group data sources by category
  const groupedSources = useMemo(() => {
    const groups: Record<string, typeof DATA_SOURCES> = {}
    for (const src of DATA_SOURCES) {
      if (src.key === 'alertCards') continue
      const cat = src.category
      if (!groups[cat]) groups[cat] = []
      groups[cat].push(src)
    }
    return groups
  }, [])

  const categoryLabels: Record<string, string> = {
    count: 'Counts',
    amount: 'Amounts',
    rate: 'Rates',
    'time-series': 'Time Series',
    breakdown: 'Breakdowns',
    composite: 'Composite',
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="fixed inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />
      <div className="flex min-h-full items-center justify-center p-4">
        <div className="relative bg-[#1e1e2f] rounded-lg shadow-2xl w-full max-w-2xl border border-gray-600/40" onClick={e => e.stopPropagation()}>
          {/* Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-700/50">
            <h2 className="text-xl font-semibold text-gray-100">
              {existingWidget ? 'Edit Panel' : 'Add Panel'}
            </h2>
            <button onClick={onClose} className="text-gray-500 hover:text-gray-300 transition-colors">
              <X className="w-6 h-6" />
            </button>
          </div>

          {/* Content */}
          <div className="p-6 space-y-6 max-h-[70vh] overflow-y-auto">
            {/* Title */}
            <div>
              <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Panel Title</label>
              <input
                type="text"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="Enter panel title"
                className="w-full px-3 py-2 border border-gray-600/50 bg-[#111119] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm text-gray-200 placeholder-gray-500"
              />
            </div>

            {/* Data Source */}
            <div>
              <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Data Source</label>
              <select
                value={dataSource}
                onChange={e => {
                  setDataSource(e.target.value as DataSourceKey)
                  const src = DATA_SOURCES.find(d => d.key === e.target.value)
                  if (src && !title) setTitle(src.label)
                }}
                className="w-full px-3 py-2 border border-gray-600/50 bg-[#111119] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm text-gray-200"
              >
                {Object.entries(groupedSources).map(([cat, sources]) => (
                  <optgroup key={cat} label={categoryLabels[cat] || cat}>
                    {sources.map(src => (
                      <option key={src.key} value={src.key}>
                        {src.label} — {src.description}
                      </option>
                    ))}
                  </optgroup>
                ))}
              </select>
            </div>

            {/* Chart Type — Visualization */}
            <div>
              <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Visualization</label>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                {compatibleCharts.map(ct => (
                  <button
                    key={ct}
                    type="button"
                    onClick={() => setChartType(ct)}
                    className={`px-3 py-2.5 rounded-lg border text-sm font-medium transition-all ${
                      chartType === ct
                        ? 'border-blue-500 bg-blue-900/20 text-blue-400 ring-1 ring-blue-500/30'
                        : 'border-gray-600/50 text-gray-400 hover:border-gray-500 hover:bg-white/5'
                    }`}
                  >
                    {CHART_TYPE_LABELS[ct]}
                  </button>
                ))}
              </div>
            </div>

            {/* ─── Axis Configuration ─── */}
            {selectedSource?.axisFields && selectedSource.axisFields.length > 0 &&
              ['area', 'line', 'bar', 'horizontalBar', 'pie', 'donut', 'table', 'treemap'].includes(chartType) && (
              <div className="bg-[#111119] rounded-lg p-4 space-y-4 border border-gray-700/40">
                <div className="flex items-center gap-2 mb-1">
                  <svg className="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21l5-5 5 5M7 3l5 5 5-5" />
                  </svg>
                  <span className="text-sm font-semibold text-gray-300">Axis Configuration</span>
                </div>

                {/* X-Axis */}
                {['area', 'line', 'bar', 'horizontalBar'].includes(chartType) && (
                  <div>
                    <label className="block text-xs font-medium text-gray-500 mb-1">
                      {chartType === 'horizontalBar' ? 'Y-Axis (Categories)' : 'X-Axis (Categories)'}
                    </label>
                    <select
                      value={xAxisField || selectedSource.defaultXAxis || ''}
                      onChange={e => setXAxisField(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-600/50 bg-[#1e1e2f] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm text-gray-200"
                    >
                      {selectedSource.axisFields.filter(f => f.type === 'category').map(f => (
                        <option key={f.key} value={f.key}>{f.label}</option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Y-Axis */}
                {['area', 'line', 'bar', 'horizontalBar'].includes(chartType) && (
                  <div>
                    <label className="block text-xs font-medium text-gray-500 mb-1">
                      {chartType === 'horizontalBar' ? 'X-Axis (Values)' : 'Y-Axis (Values)'}
                      <span className="text-gray-600 ml-1">— select one or more</span>
                    </label>
                    <div className="flex flex-wrap gap-2">
                      {selectedSource.axisFields.filter(f => f.type === 'numeric').map(f => {
                        const active = (yAxisFields.length > 0 ? yAxisFields : selectedSource.defaultYAxis || []).includes(f.key)
                        return (
                          <button
                            key={f.key}
                            type="button"
                            onClick={() => {
                              const current = yAxisFields.length > 0 ? yAxisFields : selectedSource.defaultYAxis || []
                              if (current.includes(f.key)) {
                                if (current.length > 1) setYAxisFields(current.filter(k => k !== f.key))
                              } else {
                                setYAxisFields([...current, f.key])
                              }
                            }}
                            className={`px-3 py-1.5 rounded-md border text-xs font-medium transition-all ${
                              active
                                ? 'border-blue-500 bg-blue-900/20 text-blue-400 ring-1 ring-blue-500/30'
                                : 'border-gray-600/50 text-gray-500 hover:border-gray-500 hover:bg-white/5'
                            }`}
                          >
                            {f.label}
                          </button>
                        )
                      })}
                    </div>
                  </div>
                )}

                {/* Pie / Donut / Treemap: label + value */}
                {['pie', 'donut', 'treemap'].includes(chartType) && (
                  <>
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">Label Field</label>
                      <select
                        value={xAxisField || selectedSource.defaultXAxis || ''}
                        onChange={e => setXAxisField(e.target.value)}
                        className="w-full px-3 py-2 border border-gray-600/50 bg-[#1e1e2f] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm text-gray-200"
                      >
                        {selectedSource.axisFields.filter(f => f.type === 'category').map(f => (
                          <option key={f.key} value={f.key}>{f.label}</option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-gray-500 mb-1">Value Field</label>
                      <select
                        value={(yAxisFields.length > 0 ? yAxisFields : selectedSource.defaultYAxis || [])[0] || ''}
                        onChange={e => setYAxisFields([e.target.value])}
                        className="w-full px-3 py-2 border border-gray-600/50 bg-[#1e1e2f] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm text-gray-200"
                      >
                        {selectedSource.axisFields.filter(f => f.type === 'numeric').map(f => (
                          <option key={f.key} value={f.key}>{f.label}</option>
                        ))}
                      </select>
                    </div>
                  </>
                )}
              </div>
            )}

            {/* Size */}
            <div>
              <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Panel Size</label>
              <div className="grid grid-cols-4 gap-2">
                {SIZE_OPTIONS.map(opt => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setSize(opt.value)}
                    className={`px-3 py-2.5 rounded-lg border text-sm font-medium transition-all ${
                      size === opt.value
                        ? 'border-blue-500 bg-blue-900/20 text-blue-400 ring-1 ring-blue-500/30'
                        : 'border-gray-600/50 text-gray-400 hover:border-gray-500 hover:bg-white/5'
                    }`}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Color */}
            <div>
              <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">
                <span className="flex items-center gap-1.5">
                  <Palette className="w-4 h-4" />
                  Accent Color
                </span>
              </label>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setShowColorPicker(!showColorPicker)}
                  className="w-8 h-8 rounded-lg border-2 border-gray-600/50 shadow-sm"
                  style={{ backgroundColor: color }}
                />
                {showColorPicker && (
                  <div className="flex flex-wrap gap-1.5">
                    {WIDGET_COLORS.map(c => (
                      <button
                        key={c}
                        type="button"
                        onClick={() => { setColor(c); setShowColorPicker(false) }}
                        className={`w-7 h-7 rounded-md border-2 transition-all ${
                          color === c ? 'border-white scale-110' : 'border-transparent hover:scale-110'
                        }`}
                        style={{ backgroundColor: c }}
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Description / subtitle */}
            <div>
              <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Description (optional)</label>
              <input
                type="text"
                value={subtitle}
                onChange={e => setSubtitle(e.target.value)}
                placeholder="Brief description shown below the title"
                className="w-full px-3 py-2 border border-gray-600/50 bg-[#111119] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm text-gray-200 placeholder-gray-500"
              />
            </div>

            {/* Click URL */}
            <div>
              <label className="block text-xs font-medium text-gray-400 uppercase tracking-wider mb-1.5">Click Navigation (optional)</label>
              <input
                type="text"
                value={clickUrl}
                onChange={e => setClickUrl(e.target.value)}
                placeholder="/invoices?status=PENDING"
                className="w-full px-3 py-2 border border-gray-600/50 bg-[#111119] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 text-sm text-gray-200 placeholder-gray-500"
              />
              <p className="text-xs text-gray-500 mt-1">URL path to navigate to when panel is clicked</p>
            </div>
          </div>

          {/* Footer */}
          <div className="flex items-center justify-end gap-3 p-6 border-t border-gray-700/50 bg-[#161622] rounded-b-lg">
            <button
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-gray-400 bg-transparent border border-gray-600/50 rounded-lg hover:bg-white/5"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              className="px-5 py-2 text-sm font-medium text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition-colors"
            >
              {existingWidget ? 'Update Panel' : 'Add Panel'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
