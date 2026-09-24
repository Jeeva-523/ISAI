import type { Song } from '@shared/models/song'

export interface FeaturedPlaylist {
  id: string
  title: string
  subtitle: string
  language: string
  coverUrl: string
  gradient: string
  songCount: number
  followers?: string
  description: string
  query: string
  initialSongs?: Song[]
}

export const FEATURED_PLAYLISTS: FeaturedPlaylist[] = [
  // ── TAMIL ─────────────────────────────────────────────────────────
  {
    id: 'hot-hits-tamil',
    title: 'Hot Hits Tamil 🔥',
    subtitle: 'By ISAI Editorial • 45 Songs',
    language: 'Tamil',
    coverUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
    gradient: 'linear-gradient(135deg, #E11D48 0%, #4C0519 100%)',
    songCount: 45,
    followers: '1.8M Saves',
    description: 'Catch the hottest Tamil chartbusters and blockbuster hits right now.',
    query: 'Tamil latest hits 2026',
    initialSongs: [
      {
        videoId: 'KUN5Uf9mObQ',
        title: 'Arabic Kuthu - Halamithi Habibo',
        channelTitle: 'Anirudh Ravichander • Beast',
        thumbnailUrl: 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
        durationFormatted: '4:39',
        durationMs: 279000,
        viewCountFormatted: '53M views',
        audioUrl: 'https://aac.saavncdn.com/510/9d96fc7ddd4ffadb745f25aed86f7a4e_320.mp4'
      },
      {
        videoId: '1F3hm6MfR1k',
        title: 'Hukum - Thalaivar Alappara',
        channelTitle: 'Anirudh Ravichander • Jailer',
        thumbnailUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
        durationFormatted: '3:27',
        durationMs: 207000,
        viewCountFormatted: '43M views',
        audioUrl: 'https://aac.saavncdn.com/187/0c4d0aee91a3ac81d4b645ec448a2960_320.mp4'
      },
      {
        videoId: 'szvt1vD0Uug',
        title: 'Naa Ready',
        channelTitle: 'Vijay • Leo',
        thumbnailUrl: 'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
        durationFormatted: '4:08',
        durationMs: 248000,
        viewCountFormatted: '34M views',
        audioUrl: 'https://aac.saavncdn.com/415/3789bee89b94522160f1e50b2266d2c4_320.mp4'
      },
      {
        videoId: 'mqqft2x_Aa4',
        title: 'Kaavaalaa',
        channelTitle: 'Shilpa Rao • Jailer',
        thumbnailUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
        durationFormatted: '3:10',
        durationMs: 190000,
        viewCountFormatted: '21M views',
        audioUrl: 'https://aac.saavncdn.com/187/49797372d021638077d8a6b749068bc8_320.mp4'
      }
    ]
  },
  {
    id: 'tamil-romance',
    title: 'Tamil Romance ❤️',
    subtitle: 'By ISAI Editorial • 50 Songs',
    language: 'Tamil',
    coverUrl: 'https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg',
    gradient: 'linear-gradient(135deg, #EC4899 0%, #831843 100%)',
    songCount: 50,
    followers: '2.4M Saves',
    description: 'Soulful love anthems & heartfelt romantic melodies from Kollywood.',
    query: 'Tamil romantic love songs',
    initialSongs: [
      {
        videoId: 'eN6AnYGYdVE',
        title: 'Vaseegara (From "Minnale")',
        channelTitle: 'Bombay Jayashri • Minnale',
        thumbnailUrl: 'https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg',
        durationFormatted: '4:59',
        durationMs: 299000,
        viewCountFormatted: '10M views',
        audioUrl: 'https://aac.saavncdn.com/450/4f7b9da8e887586e60b11afb602befac_320.mp4'
      },
      {
        videoId: '3tmd-ClpJxA',
        title: 'Marakkuma Nenjam',
        channelTitle: 'A.R. Rahman • Vendhu Thanindhathu Kaadu',
        thumbnailUrl: 'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
        durationFormatted: '4:18',
        durationMs: 258000,
        viewCountFormatted: '2M views',
        audioUrl: 'https://aac.saavncdn.com/494/ca8637be3d98d854e620dfc6e240bf7e_320.mp4'
      },
      {
        videoId: 'mSgN49e_Jyo',
        title: 'Dippam Dappam',
        channelTitle: 'Anirudh Ravichander • Kaathuvaakula Rendu Kaadhal',
        thumbnailUrl: 'https://c.saavncdn.com/403/Kaathuvaakula-Rendu-Kaadhal-Original-Motion-Picture-Soundtrack-Tamil-2022-20220428131043-500x500.jpg',
        durationFormatted: '3:29',
        durationMs: 209000,
        viewCountFormatted: '20M views',
        audioUrl: 'https://aac.saavncdn.com/403/f3014a0be23fa972ba7e72b4cba7fe79_320.mp4'
      }
    ]
  },
  {
    id: 'all-time-tamil-melodies',
    title: 'All-Time Melodies 🌸',
    subtitle: 'By ISAI Editorial • 60 Songs',
    language: 'Tamil',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #3B0764 100%)',
    songCount: 60,
    followers: '3.1M Saves',
    description: 'Golden era evergreen classics of Ilaiyaraaja, A.R. Rahman, Harris & Yuvan.',
    query: 'Tamil evergreen melody hits AR Rahman Ilayaraja',
    initialSongs: [
      {
        videoId: '3tmd-ClpJxA',
        title: 'Marakkuma Nenjam',
        channelTitle: 'A.R. Rahman • Vendhu Thanindhathu Kaadu',
        thumbnailUrl: 'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
        durationFormatted: '4:18',
        durationMs: 258000,
        viewCountFormatted: '2M views',
        audioUrl: 'https://aac.saavncdn.com/494/ca8637be3d98d854e620dfc6e240bf7e_320.mp4'
      },
      {
        videoId: 'eN6AnYGYdVE',
        title: 'Vaseegara',
        channelTitle: 'Bombay Jayashri • Minnale',
        thumbnailUrl: 'https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg',
        durationFormatted: '4:59',
        durationMs: 299000,
        viewCountFormatted: '10M views',
        audioUrl: 'https://aac.saavncdn.com/450/4f7b9da8e887586e60b11afb602befac_320.mp4'
      }
    ]
  },
  {
    id: 'kuthu-party-blast',
    title: 'Kuthu & Party Blast ⚡',
    subtitle: 'By ISAI Editorial • 40 Songs',
    language: 'Tamil',
    coverUrl: 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #78350F 100%)',
    songCount: 40,
    followers: '1.2M Saves',
    description: 'High-energy dance anthems and party beats for maximum hype.',
    query: 'Tamil kuthu party dance songs',
    initialSongs: [
      {
        videoId: 'KUN5Uf9mObQ',
        title: 'Arabic Kuthu',
        channelTitle: 'Anirudh Ravichander • Beast',
        thumbnailUrl: 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
        durationFormatted: '4:39',
        durationMs: 279000,
        viewCountFormatted: '53M views',
        audioUrl: 'https://aac.saavncdn.com/510/9d96fc7ddd4ffadb745f25aed86f7a4e_320.mp4'
      },
      {
        videoId: 'szvt1vD0Uug',
        title: 'Naa Ready',
        channelTitle: 'Vijay • Leo',
        thumbnailUrl: 'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
        durationFormatted: '4:08',
        durationMs: 248000,
        viewCountFormatted: '34M views',
        audioUrl: 'https://aac.saavncdn.com/415/3789bee89b94522160f1e50b2266d2c4_320.mp4'
      }
    ]
  },
  {
    id: 'tamil-chill-lofi',
    title: 'Tamil Chill & Lo-Fi 🌙',
    subtitle: 'By ISAI Editorial • 35 Songs',
    language: 'Tamil',
    coverUrl: 'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
    gradient: 'linear-gradient(135deg, #06B6D4 0%, #164E63 100%)',
    songCount: 35,
    followers: '950K Saves',
    description: 'Midnight drives, soothing acoustics & relaxing Tamil lo-fi beats.',
    query: 'Tamil lo-fi chill acoustic songs',
    initialSongs: [
      {
        videoId: '3tmd-ClpJxA',
        title: 'Marakkuma Nenjam',
        channelTitle: 'A.R. Rahman • Vendhu Thanindhathu Kaadu',
        thumbnailUrl: 'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
        durationFormatted: '4:18',
        durationMs: 258000,
        viewCountFormatted: '2M views',
        audioUrl: 'https://aac.saavncdn.com/494/ca8637be3d98d854e620dfc6e240bf7e_320.mp4'
      }
    ]
  },
  {
    id: '90s-2000s-nostalgia',
    title: '90s & 2000s Nostalgia 📻',
    subtitle: 'By ISAI Editorial • 55 Songs',
    language: 'Tamil',
    coverUrl: 'https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F97316 0%, #7C2D12 100%)',
    songCount: 55,
    followers: '1.5M Saves',
    description: 'Relive golden childhood memories with unforgettable Tamil nostalgic gems.',
    query: 'Tamil 90s 2000s hit songs nostalgic',
    initialSongs: [
      {
        videoId: 'eN6AnYGYdVE',
        title: 'Vaseegara (From "Minnale")',
        channelTitle: 'Bombay Jayashri • 2 - In - 1 Hits Of Maddy',
        thumbnailUrl: 'https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg',
        durationFormatted: '4:59',
        durationMs: 299000,
        viewCountFormatted: '10M views',
        audioUrl: 'https://aac.saavncdn.com/450/4f7b9da8e887586e60b11afb602befac_320.mp4'
      }
    ]
  },

  // ── TELUGU ─────────────────────────────────────────────────────────
  {
    id: 'hot-hits-telugu',
    title: 'Hot Hits Telugu 🔥',
    subtitle: 'By ISAI Editorial • 45 Songs',
    language: 'Telugu',
    coverUrl: 'https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg',
    gradient: 'linear-gradient(135deg, #EF4444 0%, #7F1D1D 100%)',
    songCount: 45,
    followers: '1.4M Saves',
    description: 'Catch the hottest Tollywood chartbusters and mass hits right now.',
    query: 'Telugu latest super hits 2026'
  },
  {
    id: 'telugu-romance',
    title: 'Telugu Romance ❤️',
    subtitle: 'By ISAI Editorial • 50 Songs',
    language: 'Telugu',
    coverUrl: 'https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F43F5E 0%, #881337 100%)',
    songCount: 50,
    followers: '1.9M Saves',
    description: 'Sweet romantic melodies & heart-touching love duets in Telugu.',
    query: 'Telugu romantic melodies love songs'
  },
  {
    id: 'mass-party-telugu',
    title: 'Tollywood Mass & Party ⚡',
    subtitle: 'By ISAI Editorial • 40 Songs',
    language: 'Telugu',
    coverUrl: 'https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #78350F 100%)',
    songCount: 40,
    followers: '920K Saves',
    description: 'High-voltage commercial dance numbers and mass party anthems.',
    query: 'Telugu mass commercial dance party hits'
  },

  // ── HINDI ──────────────────────────────────────────────────────────
  {
    id: 'hot-hits-hindi',
    title: 'Hot Hits Hindi 🔥',
    subtitle: 'By ISAI Editorial • 50 Songs',
    language: 'Hindi',
    coverUrl: 'https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F43F5E 0%, #4C0519 100%)',
    songCount: 50,
    followers: '4.2M Saves',
    description: "Bollywood's biggest chart-toppers and trending hits right now.",
    query: 'Hindi latest bollywood songs 2026'
  },
  {
    id: 'bollywood-romance',
    title: 'Bollywood Romance ❤️',
    subtitle: 'By ISAI Editorial • 60 Songs',
    language: 'Hindi',
    coverUrl: 'https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg',
    gradient: 'linear-gradient(135deg, #D946EF 0%, #701A75 100%)',
    songCount: 60,
    followers: '3.8M Saves',
    description: 'Heartwarming love anthems from Arijit Singh, Shreya Ghoshal & Pritam.',
    query: 'Hindi romantic love songs Arijit Shreya'
  },
  {
    id: '90s-bollywood-nostalgia',
    title: '90s Bollywood Classics 📻',
    subtitle: 'By ISAI Editorial • 50 Songs',
    language: 'Hindi',
    coverUrl: 'https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg',
    gradient: 'linear-gradient(135deg, #E11D48 0%, #881337 100%)',
    songCount: 50,
    followers: '2.1M Saves',
    description: 'Evergreen golden melodies of the 90s & 2000s Bollywood era.',
    query: '90s bollywood golden melody hits'
  },

  // ── MALAYALAM ──────────────────────────────────────────────────────
  {
    id: 'hot-hits-malayalam',
    title: 'Hot Hits Malayalam 🔥',
    subtitle: 'By ISAI Editorial • 35 Songs',
    language: 'Malayalam',
    coverUrl: 'https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg',
    gradient: 'linear-gradient(135deg, #10B981 0%, #064E3B 100%)',
    songCount: 35,
    followers: '880K Saves',
    description: 'Trending Mollywood songs & top tracks from Gods Own Country.',
    query: 'Malayalam latest hits 2026'
  },
  {
    id: 'malayalam-chill',
    title: 'Malayalam Chill & Acoustic 🌿',
    subtitle: 'By ISAI Editorial • 30 Songs',
    language: 'Malayalam',
    coverUrl: 'https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg',
    gradient: 'linear-gradient(135deg, #14B8A6 0%, #134E4A 100%)',
    songCount: 30,
    followers: '620K Saves',
    description: 'Serene acoustic melodies, calm vocals & soothing Malayalam tunes.',
    query: 'Malayalam acoustic chill melodies'
  },

  // ── KANNADA ────────────────────────────────────────────────────────
  {
    id: 'hot-hits-kannada',
    title: 'Hot Hits Kannada 🔥',
    subtitle: 'By ISAI Editorial • 35 Songs',
    language: 'Kannada',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #EAB308 0%, #713F12 100%)',
    songCount: 35,
    followers: '540K Saves',
    description: 'Top Sandalwood hits & trending melodies in Kannada.',
    query: 'Kannada latest super hits 2026'
  },

  // ── ENGLISH ────────────────────────────────────────────────────────
  {
    id: 'todays-top-hits-global',
    title: "Today's Top Hits 🌐",
    subtitle: 'By ISAI Editorial • 50 Songs',
    language: 'English',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1E3A8A 100%)',
    songCount: 50,
    followers: '5.6M Saves',
    description: 'Global pop leaders, billboard chart toppers & viral hits.',
    query: 'English pop chart hits 2026'
  },
  {
    id: 'pop-chill-vibes',
    title: 'Pop & Chill Vibes 🎧',
    subtitle: 'By ISAI Editorial • 40 Songs',
    language: 'English',
    coverUrl: 'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
    gradient: 'linear-gradient(135deg, #6366F1 0%, #312E81 100%)',
    songCount: 40,
    followers: '2.8M Saves',
    description: 'Smooth English acoustic, lo-fi beats & relaxing weekend pop.',
    query: 'English chill acoustic pop'
  }
]
