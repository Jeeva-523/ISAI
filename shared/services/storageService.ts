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
      return data ? JSON.parse(data) : {
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
    } catch (e) {
      console.error('Failed to save user profile', e)
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
    } catch (e) {
      console.error('Failed to save favorites', e)
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
    const recents = this.getRecentlyPlayed().filter(s => s.videoId !== song.videoId)
    recents.unshift(song)
    const trimmed = recents.slice(0, 20)
    try {
      localStorage.setItem('isai_recently_played', JSON.stringify(trimmed))
    } catch (e) {
      console.error('Failed to save recently played song', e)
    }
  }

  setRecentlyPlayed(songs: Song[]) {
    if (typeof window === 'undefined' || !window.localStorage) return
    const trimmed = songs.slice(0, 20)
    try {
      localStorage.setItem('isai_recently_played', JSON.stringify(trimmed))
    } catch (e) {
      console.error('Failed to set recently played songs', e)
    }
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
