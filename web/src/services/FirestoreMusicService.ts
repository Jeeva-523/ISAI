import { collection, doc, getDoc, getDocs, limit, orderBy, query, where } from 'firebase/firestore'
import { musicApi } from '../../../shared/api/music-api'
import { firestore } from '../firebase'
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
        return snap.docs.map((docSnap) => {
          const d = docSnap.data()
          const songItem: Song = {
            videoId: docSnap.id,
            title: d.title || '',
            channelTitle: d.channelTitle || d.artist || '',
            thumbnailUrl: d.thumbnailUrl || d.image || '',
            durationFormatted: d.durationFormatted || '0:00',
            durationMs: d.durationMs || 0,
            viewCountFormatted: d.viewCountFormatted || '',
            album: d.album,
            audioUrl: d.audioUrl,
            language: d.language,
            artists: d.artists
          }
          return songItem
        })
      }
    } catch (error) {
      console.warn('[FirestoreMusicService] Firestore songs query fallback to MusicAPI:', error)
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
        where('titleLower', '<=', `${clean}\uF8FF`),
        limit(20)
      )
      const snap = await getDocs(q)
      if (!snap.empty) {
        return snap.docs.map((docSnap) => {
          const d = docSnap.data()
          const songItem: Song = {
            videoId: docSnap.id,
            title: d.title || '',
            channelTitle: d.channelTitle || d.artist || '',
            thumbnailUrl: d.thumbnailUrl || d.image || '',
            durationFormatted: d.durationFormatted || '0:00',
            durationMs: d.durationMs || 0,
            viewCountFormatted: d.viewCountFormatted || '',
            album: d.album,
            audioUrl: d.audioUrl,
            language: d.language,
            artists: d.artists
          }
          return songItem
        })
      }
    } catch {}

    return musicApi.searchSongs(rawQuery, 25)
  }

  async getArtistDetails(artistId: string): Promise<ArtistDoc | null> {
    try {
      const snap = await getDoc(doc(firestore, 'artists', artistId))
      if (snap.exists()) {
        const d = snap.data()
        const artist: ArtistDoc = {
          id: snap.id,
          name: d.name || '',
          imageUrl: d.imageUrl || '',
          bio: d.bio,
          language: d.language || ''
        }
        return artist
      }
    } catch {}
    return null
  }

  async getMovieDetails(movieId: string): Promise<MovieDoc | null> {
    try {
      const snap = await getDoc(doc(firestore, 'movies', movieId))
      if (snap.exists()) {
        const d = snap.data()
        const movie: MovieDoc = {
          id: snap.id,
          title: d.title || '',
          releaseYear: d.releaseYear || 0,
          posterUrl: d.posterUrl || '',
          language: d.language || ''
        }
        return movie
      }
    } catch {}
    return null
  }
}

export const firestoreMusicService = new FirestoreMusicService()
