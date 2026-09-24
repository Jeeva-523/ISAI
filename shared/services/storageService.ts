import type { Song, UserPlaylist } from '../models/song'

const FAVORITES_KEY = 'isai_favorites'
const PLAYLISTS_KEY = 'isai_playlists'
const USER_KEY = 'isai_user_profile'

export interface UserProfileData {
  isLoggedIn: boolean
  name: string
  email: string
  avatar: string
  isPremium: boolean
  selectedPlan?: string
  preferredLanguages?: string[]
}

export class LocalMusicStorageService {
  getUserProfile(): UserProfileData {
    if (typeof window === 'undefined' || !window.localStorage) {
      return {
        isLoggedIn: false,
        name: 'ISAI Listener',
        email: '',
        avatar: 'I',
        isPremium: true
      }
    }
    try {
      const data = localStorage.getItem(USER_KEY)
      if (data) {
        const parsed = JSON.parse(data)
        const name = (parsed.name || '').trim().toLowerCase()
        if (name === 'jeeva ⚡' || (name === 'jeeva' && parsed.email?.includes('jeeva'))) {
          localStorage.removeItem(USER_KEY)
          return {
            isLoggedIn: false,
            name: 'ISAI Listener',
            email: '',
            avatar: 'I',
            isPremium: true
          }
        }
        return parsed
      }
      return {
        isLoggedIn: false,
        name: 'ISAI Listener',
        email: '',
        avatar: 'I',
        isPremium: true
      }
    } catch {
      return {
        isLoggedIn: false,
        name: 'ISAI Listener',
        email: '',
        avatar: 'I',
        isPremium: true
      }
    }
  }

  setUserProfile(user: UserProfileData) {
    if (typeof window === 'undefined' || !window.localStorage) return
    try {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } catch (error) {
      console.error('Failed to save user profile', error)
    }
  }

  getFavorites(): Song[] {
    if (typeof window === 'undefined' || !window.localStorage) return []
    try {
      const data = localStorage.getItem(FAVORITES_KEY)
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  setFavorites(songs: Song[]) {
    if (typeof window === 'undefined' || !window.localStorage) return
    try {
      localStorage.setItem(FAVORITES_KEY, JSON.stringify(songs))
    } catch (error) {
      console.error('Failed to save favorites', error)
    }
  }

  isFavorite(videoId: string): boolean {
    const favs = this.getFavorites()
    return favs.some((s) => s.videoId === videoId)
  }

  toggleFavorite(song: Song): boolean {
    if (typeof window === 'undefined' || !window.localStorage) return false
    const favs = this.getFavorites()
    const index = favs.findIndex((s) => s.videoId === song.videoId)
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
    } catch (error) {
      console.error('Failed to save favorites', error)
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
    } catch (error) {
      console.error('Failed to save playlists', error)
    }
    return newPlaylist
  }

  getRecentlyPlayed(): Song[] {
    if (typeof window === 'undefined' || !window.localStorage) return []
    try {
      const data = localStorage.getItem('isai_recently_played')
      return data ? JSON.parse(data) : []
    } catch {
      return []
    }
  }

  addRecentlyPlayed(song: Song) {
    if (typeof window === 'undefined' || !window.localStorage) return
    const recents = this.getRecentlyPlayed().filter((s) => s.videoId !== song.videoId)
    recents.unshift(song)
    const trimmed = recents.slice(0, 20)
    try {
      localStorage.setItem('isai_recently_played', JSON.stringify(trimmed))
    } catch (error) {
      console.error('Failed to save recently played song', error)
    }
  }

  setRecentlyPlayed(songs: Song[]) {
    if (typeof window === 'undefined' || !window.localStorage) return
    const trimmed = songs.slice(0, 20)
    try {
      localStorage.setItem('isai_recently_played', JSON.stringify(trimmed))
    } catch (error) {
      console.error('Failed to set recently played songs', error)
    }
  }

  clearRecentlyPlayed() {
    if (typeof window === 'undefined' || !window.localStorage) return
    try {
      localStorage.removeItem('isai_recently_played')
    } catch (error) {
      console.error('Failed to clear recently played songs', error)
    }
  }

  addSongToPlaylist(playlistId: string, song: Song) {
    const playlists = this.getPlaylists()
    const target = playlists.find((p) => p.id === playlistId)
    if (!target) return

    if (!target.songs.some((s) => s.videoId === song.videoId)) {
      target.songs.push(song)
      try {
        localStorage.setItem(PLAYLISTS_KEY, JSON.stringify(playlists))
      } catch (error) {
        console.error('Failed to update playlist', error)
      }
    }
  }

  isMultiDevicePlaybackSeparate(): boolean {
    if (typeof window === 'undefined' || !window.localStorage) return false
    const val = localStorage.getItem('isai_multi_device_separate')
    return val === 'true'
  }

  setMultiDevicePlaybackSeparate(enabled: boolean) {
    if (typeof window === 'undefined' || !window.localStorage) return
    localStorage.setItem('isai_multi_device_separate', String(enabled))
  }

  getAudioQuality(): '320kbps' | '160kbps' | '96kbps' {
    if (typeof window === 'undefined' || !window.localStorage) return '320kbps'
    const q = localStorage.getItem('isai_audio_quality')
    if (q === '160kbps' || q === '96kbps' || q === '320kbps') return q
    return '320kbps'
  }

  setAudioQuality(quality: '320kbps' | '160kbps' | '96kbps') {
    if (typeof window === 'undefined' || !window.localStorage) return
    localStorage.setItem('isai_audio_quality', quality)
  }

  getLastPlaybackSession(): LastPlaybackSession | null {
    if (typeof window === 'undefined' || !window.localStorage) return null
    try {
      const raw = localStorage.getItem('isai_last_playback_session')
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  }

  setLastPlaybackSession(session: LastPlaybackSession) {
    if (typeof window === 'undefined' || !window.localStorage) return
    try {
      localStorage.setItem('isai_last_playback_session', JSON.stringify(session))
    } catch (error) {
      console.error('Failed to save playback session', error)
    }
  }
}

export interface LastPlaybackSession {
  song: Song
  queue?: Song[]
  queueIndex?: number
  positionSec?: number
  wasPlaying?: boolean
  timestamp?: number
}

export const storageService = new LocalMusicStorageService()
