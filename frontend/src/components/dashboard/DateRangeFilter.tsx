import { useState } from 'react'
import { Calendar } from 'lucide-react'
import type { DateFilter, DateFilterPreset } from '@/types/dashboard'
import { DATE_FILTER_LABELS } from '@/types/dashboard'

const PRESETS: DateFilterPreset[] = ['today', 'thisWeek', 'thisMonth', 'thisYear', 'all']

interface DateRangeFilterProps {
  value: DateFilter
  onChange: (filter: DateFilter) => void
}

export function DateRangeFilter({ value, onChange }: DateRangeFilterProps) {
  const [showCustom, setShowCustom] = useState(value.preset === 'custom')

  const handlePreset = (preset: DateFilterPreset) => {
    if (preset === 'custom') {
      setShowCustom(true)
      onChange({
        preset: 'custom',
        startDate: value.startDate || new Date().toISOString().slice(0, 10),
        endDate: value.endDate || new Date().toISOString().slice(0, 10),
      })
    } else {
      setShowCustom(false)
      onChange({ preset })
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-2">
      <Calendar className="w-4 h-4 text-gray-400 hidden sm:block" />

      {/* Preset buttons */}
      <div className="flex items-center bg-gray-100 rounded-lg p-0.5">
        {PRESETS.map(preset => (
          <button
            key={preset}
            onClick={() => handlePreset(preset)}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap ${
              value.preset === preset
                ? 'bg-white text-primary-700 shadow-sm'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {DATE_FILTER_LABELS[preset]}
          </button>
        ))}
        <button
          onClick={() => handlePreset('custom')}
          className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap ${
            value.preset === 'custom'
              ? 'bg-white text-primary-700 shadow-sm'
              : 'text-gray-500 hover:text-gray-700'
          }`}
        >
          Custom
        </button>
      </div>

      {/* Custom date inputs */}
      {showCustom && (
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={value.startDate || ''}
            onChange={e =>
              onChange({ ...value, preset: 'custom', startDate: e.target.value })
            }
            className="px-2 py-1.5 text-xs border border-gray-300 rounded-md focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
          <span className="text-gray-400 text-xs">to</span>
          <input
            type="date"
            value={value.endDate || ''}
            onChange={e =>
              onChange({ ...value, preset: 'custom', endDate: e.target.value })
            }
            className="px-2 py-1.5 text-xs border border-gray-300 rounded-md focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
          />
        </div>
      )}
    </div>
  )
}
