import { useState } from 'react'
import {
  Plus, Settings, Copy, Trash2, RotateCcw, ChevronDown, Check, LayoutDashboard,
  RefreshCw, Save,
} from 'lucide-react'
import { useDashboardStore } from '@/store/dashboardStore'

interface DashboardToolbarProps {
  onAddWidget: () => void
  onRefresh: () => void
}

export function DashboardToolbar({ onAddWidget, onRefresh }: DashboardToolbarProps) {
  const {
    editMode, toggleEditMode,
    getActiveDashboard, getDashboardList, setActiveDashboard,
    createDashboard, duplicateDashboard, deleteDashboard, renameDashboard,
    resetToDefault,
  } = useDashboardStore()

  const [showDashboardMenu, setShowDashboardMenu] = useState(false)
  const [showNewDialog, setShowNewDialog] = useState(false)
  const [newName, setNewName] = useState('')
  const [isRenaming, setIsRenaming] = useState(false)
  const [renameValue, setRenameValue] = useState('')

  const activeDashboard = getActiveDashboard()
  const dashboardList = getDashboardList()

  const handleCreate = () => {
    if (newName.trim()) {
      createDashboard(newName.trim())
      setNewName('')
      setShowNewDialog(false)
    }
  }

  const handleRename = () => {
    if (renameValue.trim()) {
      renameDashboard(activeDashboard.id, renameValue.trim())
      setIsRenaming(false)
    }
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3">
      {/* Left: Dashboard selector + title */}
      <div className="flex items-center gap-3">
        <div className="relative">
          <button
            onClick={() => setShowDashboardMenu(!showDashboardMenu)}
            className="flex items-center gap-2 px-3 py-2 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 text-sm font-medium text-gray-700 shadow-sm"
          >
            <LayoutDashboard className="w-4 h-4 text-primary-500" />
            {isRenaming ? (
              <input
                value={renameValue}
                onChange={e => setRenameValue(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') handleRename(); if (e.key === 'Escape') setIsRenaming(false) }}
                onBlur={handleRename}
                autoFocus
                className="w-32 border-b border-primary-400 bg-transparent focus:outline-none text-sm"
                onClick={e => e.stopPropagation()}
              />
            ) : (
              <span>{activeDashboard.name}</span>
            )}
            <ChevronDown className="w-4 h-4 text-gray-400" />
          </button>

          {showDashboardMenu && (
            <>
              <div className="fixed inset-0 z-30" onClick={() => setShowDashboardMenu(false)} />
              <div className="absolute left-0 top-full mt-1 z-40 bg-white border border-gray-200 rounded-lg shadow-lg min-w-[240px] py-1">
                <div className="px-3 py-2 text-xs font-semibold text-gray-400 uppercase tracking-wider">
                  Your Dashboards
                </div>
                {dashboardList.map(d => (
                  <button
                    key={d.id}
                    onClick={() => { setActiveDashboard(d.id); setShowDashboardMenu(false) }}
                    className={`w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-gray-50 ${
                      d.id === activeDashboard.id ? 'text-primary-600 font-medium' : 'text-gray-700'
                    }`}
                  >
                    {d.id === activeDashboard.id && <Check className="w-4 h-4" />}
                    {d.id !== activeDashboard.id && <div className="w-4" />}
                    {d.name}
                  </button>
                ))}
                <div className="border-t border-gray-100 mt-1 pt-1">
                  <button
                    onClick={() => { setShowNewDialog(true); setShowDashboardMenu(false) }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-primary-600 hover:bg-primary-50"
                  >
                    <Plus className="w-4 h-4" /> New Dashboard
                  </button>
                  <button
                    onClick={() => { duplicateDashboard(activeDashboard.id); setShowDashboardMenu(false) }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-600 hover:bg-gray-50"
                  >
                    <Copy className="w-4 h-4" /> Duplicate Current
                  </button>
                  <button
                    onClick={() => { setIsRenaming(true); setRenameValue(activeDashboard.name); setShowDashboardMenu(false) }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-600 hover:bg-gray-50"
                  >
                    <Settings className="w-4 h-4" /> Rename
                  </button>
                  {dashboardList.length > 1 && (
                    <button
                      onClick={() => {
                        if (confirm('Delete this dashboard?')) {
                          deleteDashboard(activeDashboard.id)
                        }
                        setShowDashboardMenu(false)
                      }}
                      className="w-full flex items-center gap-2 px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                    >
                      <Trash2 className="w-4 h-4" /> Delete
                    </button>
                  )}
                </div>
              </div>
            </>
          )}
        </div>

        <p className="text-gray-400 text-sm hidden sm:block">
          {activeDashboard.widgets.length} widget{activeDashboard.widgets.length !== 1 ? 's' : ''}
        </p>
      </div>

      {/* Right: Actions */}
      <div className="flex items-center gap-2">
        <span className="text-xs text-gray-400 hidden md:inline">
          Last updated: {new Date().toLocaleTimeString()}
        </span>
        <button
          onClick={onRefresh}
          className="flex items-center px-3 py-2 text-sm border border-gray-200 rounded-lg hover:bg-gray-50 text-gray-600 shadow-sm"
        >
          <RefreshCw className="w-4 h-4 mr-1.5" /> Refresh
        </button>

        {editMode && (
          <>
            <button
              onClick={onAddWidget}
              className="flex items-center px-3 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 shadow-sm transition-colors"
            >
              <Plus className="w-4 h-4 mr-1.5" /> Add Widget
            </button>
            <button
              onClick={() => { if (confirm('Reset dashboard to defaults?')) resetToDefault() }}
              className="flex items-center px-3 py-2 text-sm border border-gray-200 rounded-lg hover:bg-gray-50 text-gray-600 shadow-sm"
              title="Reset to defaults"
            >
              <RotateCcw className="w-4 h-4" />
            </button>
          </>
        )}

        <button
          onClick={() => toggleEditMode()}
          className={`flex items-center px-3 py-2 text-sm rounded-lg shadow-sm transition-colors ${
            editMode
              ? 'bg-green-600 text-white hover:bg-green-700'
              : 'border border-gray-200 text-gray-600 hover:bg-gray-50'
          }`}
        >
          {editMode ? (
            <><Save className="w-4 h-4 mr-1.5" /> Done Editing</>
          ) : (
            <><Settings className="w-4 h-4 mr-1.5" /> Customize</>
          )}
        </button>
      </div>

      {/* New Dashboard Dialog */}
      {showNewDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="fixed inset-0 bg-black bg-opacity-50" onClick={() => setShowNewDialog(false)} />
          <div className="relative bg-white rounded-xl shadow-xl p-6 w-full max-w-sm" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Create New Dashboard</h3>
            <input
              type="text"
              value={newName}
              onChange={e => setNewName(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter') handleCreate() }}
              placeholder="Dashboard name"
              autoFocus
              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-sm mb-4"
            />
            <div className="flex justify-end gap-2">
              <button onClick={() => setShowNewDialog(false)} className="px-4 py-2 text-sm text-gray-600 border rounded-lg hover:bg-gray-50">
                Cancel
              </button>
              <button onClick={handleCreate} className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700">
                Create
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
