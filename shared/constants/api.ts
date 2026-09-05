export const API_CONFIG = {
  DEFAULT_BACKEND_URL: 'http://localhost:3000',
  SEARCH_ENDPOINT: '/api/youtube/search',
  TRENDING_ENDPOINT: '/api/youtube/trending',
  CATEGORIES_ENDPOINT: '/api/youtube/categories',
  DEFAULT_MAX_RESULTS: 20
} as const

export const APP_INFO = {
  NAME: 'ISAI',
  TAGLINE: 'Tamil Music Streaming & Song Discovery',
  VERSION: '1.0.0'
} as const
