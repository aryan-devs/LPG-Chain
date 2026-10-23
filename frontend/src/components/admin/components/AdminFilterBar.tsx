import React from 'react';
import { Search, X } from 'lucide-react';

export interface FilterOption {
  label: string;
  value: string;
}

export interface FilterDropdown {
  id: string;
  label: string;
  value: string;
  options: FilterOption[];
  onChange: (value: string) => void;
}

interface AdminFilterBarProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  searchPlaceholder?: string;
  dropdowns?: FilterDropdown[];
  onReset?: () => void;
  totalResults?: number;
  extraControls?: React.ReactNode;
}

export const AdminFilterBar: React.FC<AdminFilterBarProps> = ({
  searchQuery,
  onSearchChange,
  searchPlaceholder = 'Search records...',
  dropdowns = [],
  onReset,
  totalResults,
  extraControls,
}) => {
  const hasActiveFilters =
    searchQuery.trim().length > 0 || dropdowns.some((d) => d.value !== 'ALL' && d.value !== '');

  return (
    <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 p-3.5 bg-white rounded-2xl border border-slate-200/80 shadow-xs">
      {/* Search Input */}
      <div className="relative flex-1 min-w-[220px]">
        <Search
          size={16}
          className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none"
        />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder={searchPlaceholder}
          className="w-full pl-10 pr-9 py-2 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-1 focus:ring-rose-500 focus:bg-white transition-all"
        />
        {searchQuery && (
          <button
            onClick={() => onSearchChange('')}
            className="absolute right-2.5 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-600 rounded-md"
          >
            <X size={14} />
          </button>
        )}
      </div>

      {/* Filter Dropdowns */}
      <div className="flex flex-wrap items-center gap-2">
        {dropdowns.map((drop) => (
          <div key={drop.id} className="relative">
            <select
              value={drop.value}
              onChange={(e) => drop.onChange(e.target.value)}
              className="px-3 py-2 pr-7 rounded-xl bg-slate-50 border border-slate-200 text-xs text-slate-700 font-medium cursor-pointer focus:outline-none focus:ring-1 focus:ring-rose-500 hover:bg-slate-100 transition-colors"
            >
              {drop.options.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
        ))}

        {hasActiveFilters && onReset && (
          <button
            type="button"
            onClick={onReset}
            className="flex items-center gap-1 px-2.5 py-2 rounded-xl text-xs font-semibold text-rose-600 hover:bg-rose-50 border border-rose-200 transition-colors"
          >
            <X size={13} />
            <span>Reset</span>
          </button>
        )}

        {totalResults !== undefined && (
          <span className="hidden sm:inline-block px-2.5 py-1 text-[11px] font-semibold text-slate-500 bg-slate-100 rounded-lg">
            {totalResults} items
          </span>
        )}

        {extraControls}
      </div>
    </div>
  );
};
