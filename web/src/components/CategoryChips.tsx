import React from 'react'
import { QUICK_SEARCH_QUERIES } from '@shared/constants/categories'

interface CategoryChipsProps {
  selectedQuery?: string
  onSelect: (query: string) => void
}

export const CategoryChips: React.FC<CategoryChipsProps> = ({
  selectedQuery,
  onSelect
}) => {
  return (
    <div className="chips-container">
      {QUICK_SEARCH_QUERIES.map((q) => {
        const isActive = selectedQuery?.toLowerCase() === q.toLowerCase()
        return (
          <button
            key={q}
            className={`chip-btn ${isActive ? 'active' : ''}`}
            onClick={() => onSelect(q)}
          >
            {q}
          </button>
        )
      })}
    </div>
  )
}
