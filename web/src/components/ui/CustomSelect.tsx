import { useEffect, useRef, useState } from 'react'

export interface SelectOption<T extends string | number = string | number> {
  value: T
  label: string
  subLabel?: string
  disabled?: boolean
}

interface CustomSelectProps<T extends string | number = string | number> {
  options: SelectOption<T>[]
  value: T
  onChange: (value: T) => void
  placeholder?: string
  disabled?: boolean
  className?: string
}

export default function CustomSelect<T extends string | number = string | number>({
  options,
  value,
  onChange,
  placeholder = '선택하세요',
  disabled = false,
  className = '',
}: CustomSelectProps<T>) {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  const selectedOption = options.find((opt) => opt.value === value)

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen])

  return (
    <div ref={containerRef} className={`relative w-full ${className}`}>
      {/* 셀렉트 트리거 버튼 */}
      <button
        type="button"
        disabled={disabled}
        onClick={() => setIsOpen(!isOpen)}
        className={`flex w-full items-center justify-between rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-800 shadow-2xs transition-all ${
          disabled
            ? 'cursor-not-allowed bg-slate-100 text-slate-400 opacity-70'
            : 'hover:border-brand-400 focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500'
        } ${isOpen ? 'border-brand-500 ring-1 ring-brand-500' : ''}`}
      >
        <span className="truncate">
          {selectedOption ? (
            <span className="flex items-center gap-1.5">
              <span className="font-semibold text-slate-900">{selectedOption.label}</span>
              {selectedOption.subLabel && (
                <span className="text-xs text-slate-400 font-mono font-normal">({selectedOption.subLabel})</span>
              )}
            </span>
          ) : (
            <span className="text-slate-400 font-normal">{placeholder}</span>
          )}
        </span>

        <svg
          className={`h-4 w-4 shrink-0 text-slate-400 transition-transform duration-150 ${isOpen ? 'rotate-180 text-brand-600' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* 커스텀 옵션 드롭다운 메뉴 */}
      {isOpen && !disabled && (
        <div className="absolute left-0 right-0 mt-1 z-50 max-h-60 overflow-y-auto rounded-lg border border-slate-200 bg-white p-1 shadow-lg animate-in fade-in zoom-in-95 duration-100">
          {options.length === 0 ? (
            <div className="py-3 text-center text-xs text-slate-400">선택 가능한 항목이 없습니다.</div>
          ) : (
            options.map((opt) => {
              const isSelected = opt.value === value
              return (
                <button
                  key={String(opt.value)}
                  type="button"
                  disabled={opt.disabled}
                  onClick={() => {
                    onChange(opt.value)
                    setIsOpen(false)
                  }}
                  className={`flex w-full items-center justify-between rounded-md px-2.5 py-2 text-left text-xs transition-colors ${
                    opt.disabled
                      ? 'cursor-not-allowed text-slate-300'
                      : isSelected
                        ? 'bg-brand-50 font-bold text-brand-700'
                        : 'text-slate-700 hover:bg-slate-50 hover:text-slate-900'
                  }`}
                >
                  <span className="truncate flex items-center gap-1.5">
                    <span className={isSelected ? 'font-bold text-brand-800' : 'font-medium'}>{opt.label}</span>
                    {opt.subLabel && (
                      <span className={`text-[11px] font-mono ${isSelected ? 'text-brand-600/80' : 'text-slate-400'}`}>
                        ({opt.subLabel})
                      </span>
                    )}
                  </span>

                  {isSelected && (
                    <svg className="h-3.5 w-3.5 shrink-0 text-brand-600 ml-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  )}
                </button>
              )
            })
          )}
        </div>
      )}
    </div>
  )
}
