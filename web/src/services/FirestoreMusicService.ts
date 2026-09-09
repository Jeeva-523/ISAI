import { firestore } from '../firebase'
import { collection, query, where, getDocs, doc, getDoc, limit, orderBy } from 'firebase/firestore'
import { musicApi } from '../../../shared/api/music-api'
import type { Song } from '../../../shared/models/song'

export interface ArtistDoc {
  id: string
  name: string
  imageUrl: string
  bio?: string
  language: string
}

export interface AlbumDoc {
  id: string
  title: string
  artistId: string
  artworkUrl: string
  releaseDate?: string
}

export interface MovieDoc {
  id: string
  title: string
  releaseYear: number
  posterUrl: string
  language: string
}

class FirestoreMusicService {
  async getTrendingSongs(preferredLanguages?: string[], maxResults = 30): Promise<Song[]> {
    try {
      const q = query(collection(firestore, 'songs'), orderBy('releaseDate', 'desc'), limit(maxResults))
      const snap = await getDocs(q)
      if (!snap.empty) {
        return snap.docs.map((docSnap) => ({ videoId: docSnap.id, ...docSnap.data() } as Song))
      }
    } catch (err) {
      console.warn('[FirestoreMusicService] Firestore songs query fallback to MusicAPI:', err)
    }
    return musicApi.getTrending(preferredLanguages, maxResults)
  }

  async searchCatalog(rawQuery: string): Promise<Song[]> {
    const clean = rawQuery.trim().toLowerCase()
    if (!clean) return []

    try {
      const q = query(
        collection(firestore, 'songs'),
        where('titleLower', '>=', clean),
        where('titleLower', '<=', clean + '\uf8ff'),
        limit(20)
      )
      const snap = await getDocs(q)
      if (!snap.empty) {
        return snap.docs.map((docSnap) => ({ videoId: docSnap.id, ...docSnap.data() } as Song))
      }
    } catch {}

    return musicApi.searchSongs(rawQuery, 25)
  }

  async getArtistDetails(artistId: string): Promise<ArtistDoc | null> {
    try {
      const snap = await getDoc(doc(firestore, 'artists', artistId))
      if (snap.exists()) {
        return { id: snap.id, ...snap.data() } as ArtistDoc
      }
    } catch {}
    return null
  }

  async getMovieDetails(movieId: string): Promise<MovieDoc | null> {
    try {
      const snap = await getDoc(doc(firestore, 'movies', movieId))
      if (snap.exists()) {
        return { id: snap.id, ...snap.data() } as MovieDoc
      }
    } catch {}
    return null
  }
}

export const firestoreMusicService = new FirestoreMusicService()
