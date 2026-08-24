import { useEffect, useRef, useState } from 'react'

interface DatePickerProps {
  value: string // 'YYYY-MM-DD'
  onChange: (date: string) => void
  maxDate?: string // 'YYYY-MM-DD'
  className?: string
}

function pad2(n: number) {
  return String(n).padStart(2, '0')
}

function formatDateStr(d: Date) {
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`
}

function parseDateStr(str: string) {
  const parts = str.split('-').map(Number)
  if (parts.length === 3 && !isNaN(parts[0]) && !isNaN(parts[1]) && !isNaN(parts[2])) {
    return new Date(parts[0], parts[1] - 1, parts[2])
  }
  return new Date()
}

export default function DatePicker({
  value,
  onChange,
  maxDate,
  className = '',
}: DatePickerProps) {
  const [isOpen, setIsOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)

  const selectedDate = parseDateStr(value)
  const [viewYear, setViewYear] = useState(selectedDate.getFullYear())
  const [viewMonth, setViewMonth] = useState(selectedDate.getMonth()) // 0 ~ 11

  // 값(value)이 외부에서 바뀔 때 뷰 년/월 동기화
  useEffect(() => {
    const d = parseDateStr(value)
    setViewYear(d.getFullYear())
    setViewMonth(d.getMonth())
  }, [value])

  // 바깥 클릭 시 닫기
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

  const today = new Date()
  const todayStr = formatDateStr(today)
  const effectiveMaxDateStr = maxDate || todayStr

  // 이전 달 / 다음 달 이동
  const handlePrevMonth = () => {
    if (viewMonth === 0) {
      setViewYear((y) => y - 1)
      setViewMonth(11)
    } else {
      setViewMonth((m) => m - 1)
    }
  }

  const handleNextMonth = () => {
    if (viewMonth === 11) {
      setViewYear((y) => y + 1)
      setViewMonth(0)
    } else {
      setViewMonth((m) => m + 1)
    }
  }

  // 달력 날짜 계산
  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate()
  const firstDayOfWeek = new Date(viewYear, viewMonth, 1).getDay() // 0(일) ~ 6(토)

  const days: { day: number; dateStr: string; disabled: boolean; isSelected: boolean; isToday: boolean }[] = []
  for (let i = 1; i <= daysInMonth; i++) {
    const dateStr = `${viewYear}-${pad2(viewMonth + 1)}-${pad2(i)}`
    const disabled = dateStr > effectiveMaxDateStr
    const isSelected = dateStr === value
    const isToday = dateStr === todayStr
    days.push({ day: i, dateStr, disabled, isSelected, isToday })
  }

  const weekLabels = ['일', '월', '화', '수', '목', '금', '토']

  return (
    <div ref={containerRef} className={`relative inline-block ${className}`}>
      {/* 캘린더 트리거 버튼 */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs font-semibold text-slate-800 shadow-2xs hover:border-brand-500 hover:bg-slate-50 transition-colors focus:border-brand-500 focus:outline-none"
      >
        <svg className="h-3.5 w-3.5 text-brand-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
        </svg>
        <span>{value}</span>
        <svg
          className={`h-3 w-3 text-slate-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth={2.5}
        >
          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {/* 팝오버 캘린더 모달 */}
      {isOpen && (
        <div className="absolute right-0 mt-1.5 z-50 w-64 rounded-xl border border-slate-200 bg-white p-3 shadow-xl animate-in fade-in zoom-in-95 duration-100">
          {/* 년/월 내비게이션 헤더 */}
          <div className="mb-2 flex items-center justify-between px-1">
            <button
              type="button"
              onClick={handlePrevMonth}
              className="flex h-7 w-7 items-center justify-center rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-900 transition-colors"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
            <span className="text-xs font-bold text-slate-800">
              {viewYear}년 {viewMonth + 1}월
            </span>
            <button
              type="button"
              onClick={handleNextMonth}
              className="flex h-7 w-7 items-center justify-center rounded-md text-slate-500 hover:bg-slate-100 hover:text-slate-900 transition-colors"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
              </svg>
            </button>
          </div>

          {/* 요일 헤더 */}
          <div className="grid grid-cols-7 gap-1 text-center text-[11px] font-semibold text-slate-400 mb-1">
            {weekLabels.map((lbl, idx) => (
              <span key={lbl} className={idx === 0 ? 'text-red-500/80' : idx === 6 ? 'text-blue-500/80' : ''}>
                {lbl}
              </span>
            ))}
          </div>

          {/* 일자 그리드 */}
          <div className="grid grid-cols-7 gap-1">
            {/* 첫날 전 빈칸 채우기 */}
            {Array.from({ length: firstDayOfWeek }).map((_, idx) => (
              <span key={`empty-${idx}`} />
            ))}

            {/* 일자 버튼들 */}
            {days.map(({ day, dateStr, disabled, isSelected, isToday }) => (
              <button
                key={dateStr}
                type="button"
                disabled={disabled}
                onClick={() => {
                  onChange(dateStr)
                  setIsOpen(false)
                }}
                className={`relative flex h-8 w-full items-center justify-center rounded-lg text-xs font-medium transition-all ${
                  isSelected
                    ? 'bg-brand-600 font-bold text-white shadow-sm'
                    : disabled
                      ? 'text-slate-300 cursor-not-allowed'
                      : 'text-slate-700 hover:bg-brand-50 hover:text-brand-700 font-semibold'
                }`}
              >
                <span>{day}</span>
                {/* 오늘 표시 점 */}
                {isToday && !isSelected && (
                  <span className="absolute bottom-1 h-1 w-1 rounded-full bg-brand-500" />
                )}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
