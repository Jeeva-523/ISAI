export interface Thumbnail {
  url: string
  width?: number
  height?: number
}

export interface SongArtist {
  id?: string
  name: string
  role?: string
}

export interface Song {
  videoId: string
  title: string
  channelTitle: string
  thumbnailUrl: string
  durationFormatted: string
  durationMs: number
  viewCountFormatted: string
  album?: string
  audioUrl?: string
  isFavorite?: boolean
  language?: string
  releaseDate?: string
  releaseTimestamp?: number
  year?: string | number
  genre?: string
  playCountNumber?: number
  artists?: SongArtist[]
}

export interface TamilCategory {
  id: string
  title: string
  subtitle: string
  query: string
  gradient: [string, string]
  icon: string
}

export interface UserPlaylist {
  id: string
  name: string
  createdAt: number
  songs: Song[]
}

export interface SearchResponse {
  query: string
  totalResults: number
  items: Song[]
  error?: string
}
