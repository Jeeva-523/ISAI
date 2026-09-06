import type { TamilCategory } from '../models/song'

export const TAMIL_CATEGORIES: TamilCategory[] = [
  {
    id: 'trending',
    title: 'Trending',
    subtitle: 'Top Chartbusters',
    query: 'Latest Tamil Movie Songs 2025 2026',
    gradient: ['#00F0FF', '#8B5CF6'],
    icon: '🔥'
  },
  {
    id: 'tamil_songs',
    title: 'Tamil Songs',
    subtitle: 'All-Time Favorites',
    query: 'Tamil all time hit songs',
    gradient: ['#FF2E93', '#8B5CF6'],
    icon: '🎵'
  },
  {
    id: 'melody',
    title: 'Melody',
    subtitle: 'Soulful & Peaceful',
    query: 'Tamil melody songs all time hits',
    gradient: ['#00F0FF', '#0099FF'],
    icon: '🌙'
  },
  {
    id: 'love',
    title: 'Love Songs',
    subtitle: 'Romantic Classics',
    query: 'Tamil love romantic songs',
    gradient: ['#FF2E93', '#FF9E00'],
    icon: '💖'
  },
  {
    id: 'folk',
    title: 'Folk',
    subtitle: 'Gramiya Paadalgal',
    query: 'Tamil folk songs gramiya paadalgal',
    gradient: ['#10B981', '#059669'],
    icon: '🌿'
  },
  {
    id: 'devotional',
    title: 'Devotional',
    subtitle: 'Bakthi Paadalgal',
    query: 'Tamil devotional bakthi songs',
    gradient: ['#F59E0B', '#D97706'],
    icon: '✨'
  },
  {
    id: 'gaana',
    title: 'Gaana',
    subtitle: 'High Energy Kuthu',
    query: 'Tamil gaana songs kuthu',
    gradient: ['#EC4899', '#EF4444'],
    icon: '💃'
  },
  {
    id: 'classical',
    title: 'Classical',
    subtitle: 'Carnatic Masters',
    query: 'Tamil classical carnatic songs',
    gradient: ['#6366F1', '#4F46E5'],
    icon: '🎻'
  },
  {
    id: 'new_releases',
    title: 'New Releases',
    subtitle: 'Latest Tracks',
    query: 'Latest Tamil songs new releases',
    gradient: ['#06B6D4', '#3B82F6'],
    icon: '⚡'
  }
]

export const QUICK_SEARCH_QUERIES: string[] = [
  'Tamil songs',
  'Tamil melody songs',
  'Anirudh Tamil songs',
  'AR Rahman Tamil songs',
  'Yuvan Tamil songs',
  'Harris Jayaraj songs',
  'Sid Sriram Tamil',
  'Ilaiyaraaja hits'
]
