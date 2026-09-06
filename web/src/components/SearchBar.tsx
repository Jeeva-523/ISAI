import React, { useState, useRef, useEffect } from 'react'
import { Search as SearchIcon, X, Clock, TrendingUp } from 'lucide-react'

interface SearchBarProps {
  value: string
  onChange: (query: string) => void
  onClear: () => void
  placeholder?: string
}

const SEARCH_SUGGESTIONS = [
  'Anirudh Ravichander hits',
  'Leo Naa Ready song',
  'A R Rahman melodies',
  'Jailer Hukum bgm',
  'Beast Arabic Kuthu',
  'Sid Sriram romantic hits',
  'Harris Jayaraj classics',
  'Yuvan Shankar Raja bgm'
]

export const SearchBar: React.FC<SearchBarProps> = ({
  value,
  onChange,
  onClear,
  placeholder = 'Search songs, albums, artists, podcasts...'
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  return (
    <div className="search-bar-wrap" ref={dropdownRef}>
      <SearchIcon size={18} className="search-icon-left" />

      <input
        type="text"
        className="search-input-box"
        placeholder={placeholder}
        value={value}
        onChange={(e) => {
          onChange(e.target.value)
          setIsOpen(true)
        }}
        onFocus={() => setIsOpen(true)}
      />

      {value && (
        <button className="search-clear-btn" onClick={onClear} title="Clear search">
          <X size={16} />
        </button>
      )}

      {isOpen && (
        <div className="search-suggestions-dropdown">
          <div style={{ padding: '12px 18px 6px', fontSize: '11px', fontWeight: 800, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em' }}>
            {value ? 'Suggestions' : 'Trending Searches'}
          </div>

          {(value
            ? SEARCH_SUGGESTIONS.filter(s => s.toLowerCase().includes(value.toLowerCase()))
            : SEARCH_SUGGESTIONS.slice(0, 5)
          ).map((sugg) => (
            <div
              key={sugg}
              className="search-suggestion-item"
              onClick={() => {
                onChange(sugg)
                setIsOpen(false)
              }}
            >
              {value ? <Clock size={16} className="text-muted" /> : <TrendingUp size={16} color="var(--isai-purple-light)" />}
              <span>{sugg}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
