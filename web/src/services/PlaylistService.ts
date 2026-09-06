import { firestore } from '../firebase'
import { authService } from './AuthService'
import {
  collection,
  doc,
  setDoc,
  deleteDoc,
  getDocs,
  query,
  orderBy,
  limit,
  serverTimestamp
} from 'firebase/firestore'
import type { Song } from '../../../shared/models/song'

export interface PlaylistDoc {
  id: string
  name: string
  description?: string
  coverUrl?: string
  songCount?: number
  isPublic?: boolean
  createdAt?: any
}

class PlaylistService {
  private getUserId(): string {
    return authService.getCurrentUserId()
  }

  // Liked Songs
  async likeSong(song: Song): Promise<void> {
    const userId = this.getUserId()
    try {
      const songRef = doc(firestore, 'users', userId, 'likedSongs', song.videoId)
      await setDoc(songRef, {
        songId: song.videoId,
        songData: song,
        likedAt: serverTimestamp()
      })
    } catch (err) {
      console.warn('[PlaylistService] Like song fallback:', err)
    }
  }

  async unlikeSong(songId: string): Promise<void> {
    const userId = this.getUserId()
    try {
      const songRef = doc(firestore, 'users', userId, 'likedSongs', songId)
      await deleteDoc(songRef)
    } catch (err) {
      console.warn('[PlaylistService] Unlike song fallback:', err)
    }
  }

  async getLikedSongs(): Promise<Song[]> {
    const userId = this.getUserId()
    try {
      const q = query(collection(firestore, 'users', userId, 'likedSongs'), orderBy('likedAt', 'desc'))
      const snap = await getDocs(q)
      return snap.docs.map((d) => d.data().songData as Song).filter(Boolean)
    } catch {
      return []
    }
  }

  // User Playlists
  async createPlaylist(name: string, description = ''): Promise<string> {
    const userId = this.getUserId()
    const playlistId = 'pl_' + Math.random().toString(36).substring(2, 9)
    try {
      const plRef = doc(firestore, 'users', userId, 'playlists', playlistId)
      await setDoc(plRef, {
        id: playlistId,
        name,
        description,
        coverUrl: '',
        songCount: 0,
        isPublic: false,
        createdAt: serverTimestamp()
      })
    } catch {}
    return playlistId
  }

  async getUserPlaylists(): Promise<PlaylistDoc[]> {
    const userId = this.getUserId()
    try {
      const snap = await getDocs(collection(firestore, 'users', userId, 'playlists'))
      return snap.docs.map((d) => ({ id: d.id, ...d.data() } as PlaylistDoc))
    } catch {
      return []
    }
  }

  async addSongToPlaylist(playlistId: string, song: Song): Promise<void> {
    const userId = this.getUserId()
    try {
      const songRef = doc(firestore, 'users', userId, 'playlists', playlistId, 'songs', song.videoId)
      await setDoc(songRef, {
        songId: song.videoId,
        songData: song,
        addedAt: serverTimestamp()
      })
    } catch {}
  }

  // Listening History
  async recordHistory(song: Song): Promise<void> {
    const userId = this.getUserId()
    try {
      const historyRef = doc(collection(firestore, 'users', userId, 'listeningHistory'))
      await setDoc(historyRef, {
        songId: song.videoId,
        songData: song,
        playedAt: serverTimestamp()
      })
    } catch {}
  }

  async getRecentHistory(maxResults = 20): Promise<Song[]> {
    const userId = this.getUserId()
    try {
      const q = query(
        collection(firestore, 'users', userId, 'listeningHistory'),
        orderBy('playedAt', 'desc'),
        limit(maxResults)
      )
      const snap = await getDocs(q)
      return snap.docs.map((d) => d.data().songData as Song).filter(Boolean)
    } catch {
      return []
    }
  }
}

export const playlistService = new PlaylistService()
