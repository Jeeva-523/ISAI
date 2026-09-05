import type { Song, UserPlaylist } from '../models/song'

const FAVORITES_KEY = 'isai_favorites'
const PLAYLISTS_KEY = 'isai_playlists'

export class LocalMusicStorageService {
  getFavorites(): Song[] {
    if (typeof window === 'undefined' || !window.localStorage) return []
    try {
      const data = localStorage.getItem(FAVORITES_KEY)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  isFavorite(videoId: string): boolean {
    const favs = this.getFavorites()
    return favs.some(s => s.videoId === videoId)
  }

  toggleFavorite(song: Song): boolean {
    if (typeof window === 'undefined' || !window.localStorage) return false
    const favs = this.getFavorites()
    const index = favs.findIndex(s => s.videoId === song.videoId)
    let isFavNow: boolean

    if (index >= 0) {
      favs.splice(index, 1)
      isFavNow = false
    } else {
      favs.unshift(song)
      isFavNow = true
    }

    try {
      localStorage.setItem(FAVORITES_KEY, JSON.stringify(favs))
    } catch (e) {
      console.error('Failed to save favorites', e)
    }

    return isFavNow
  }

  getPlaylists(): UserPlaylist[] {
    if (typeof window === 'undefined' || !window.localStorage) return []
    try {
      const data = localStorage.getItem(PLAYLISTS_KEY)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  createPlaylist(name: string): UserPlaylist {
    const playlists = this.getPlaylists()
    const newPlaylist: UserPlaylist = {
      id: `pl_${Date.now()}`,
      name: name.trim() || 'My Tamil Playlist',
      createdAt: Date.now(),
      songs: []
    }
    playlists.push(newPlaylist)
    try {
      localStorage.setItem(PLAYLISTS_KEY, JSON.stringify(playlists))
    } catch (e) {
      console.error('Failed to save playlists', e)
    }
    return newPlaylist
  }

  addSongToPlaylist(playlistId: string, song: Song) {
    const playlists = this.getPlaylists()
    const target = playlists.find(p => p.id === playlistId)
    if (!target) return

    if (!target.songs.some(s => s.videoId === song.videoId)) {
      target.songs.push(song)
      try {
        localStorage.setItem(PLAYLISTS_KEY, JSON.stringify(playlists))
      } catch (e) {
        console.error('Failed to update playlist', e)
      }
    }
  }
}

export const storageService = new LocalMusicStorageService()
