import type { Song } from '@shared/models/song'

export type PlaylistCategory =
  | 'trending'
  | 'kuthu'
  | 'romance'
  | 'chill'
  | 'mass'
  | '90s'
  | '2k'
  | '2010s'
  | '2020s'
  | 'retro'

export interface FeaturedPlaylist {
  id: string
  title: string
  subtitle: string
  language: string
  category: PlaylistCategory
  coverUrl: string
  gradient: string
  songCount: number
  followers?: string
  description: string
  query: string
  subqueries?: string[]
  isAutoTrending?: boolean
  fromDate?: string // e.g. '2026-07-01'
  initialSongs?: Song[]
  isFixedSongs?: boolean
}

import { KADHAL_VIBES_SONGS } from './kadhalVibesSongs'
import { IDHAYAM_PESUTHEY_SONGS } from './idhayamPesutheySongs'
import { SEMMA_KUTHU_SONGS } from './semmaKuthuSongs'
import { MASS_MODE_SONGS } from './massModeSongs'
import { GAANA_PETTAI_SONGS } from './gaanaPettaiSongs'
import { JANNAL_ORA_PAYANAM_SONGS } from './jannalOraPayanamSongs'
import { IRAVU_MELODIES_SONGS } from './iravuMelodiesSongs'
import { PUDHU_UDHAYAM_SONGS } from './pudhuUdhayamSongs'
import { IRAI_ISAI_SONGS } from './iraiIsaiSongs'
import { SCHOOL_DAYS_MEMORIES_SONGS } from './schoolDaysMemoriesSongs'

export const FEATURED_PLAYLISTS: FeaturedPlaylist[] = [
  // ══════════════════════════════════════════════════════════════════════
  // ── TAMIL ─────────────────────────────────────────────────────────────
  // ══════════════════════════════════════════════════════════════════════
  {
    id: 'kadhal-vibes',
    title: 'Kadhal Vibes ❤️',
    subtitle: '50 Pure Romantic Classics',
    language: 'Tamil',
    category: 'romance',
    coverUrl: 'https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg',
    gradient: 'linear-gradient(135deg, #EC4899 0%, #831843 100%)',
    songCount: 50,
    followers: '4.8M Saves',
    description: 'The ultimate 50 Tamil love melodies of all time. Curated & fixed.',
    query: 'Kadhal Vibes',
    initialSongs: KADHAL_VIBES_SONGS,
    isFixedSongs: true
  },
  {
    id: 'idhayam-pesuthey',
    title: 'Idhayam Pesuthey 💔',
    subtitle: '50 Soul-Stirring Heartfelt Melodies',
    language: 'Tamil',
    category: 'romance',
    coverUrl: 'https://c.saavncdn.com/134/Mudhal-Kanave-Tamil-2021-20211207181143-500x500.jpg',
    gradient: 'linear-gradient(135deg, #4F46E5 0%, #1E1B4B 100%)',
    songCount: 50,
    followers: '3.9M Saves',
    description: 'Deep emotions, poignant love, separation and soulful Tamil heartbeats. Curated & fixed.',
    query: 'Idhayam Pesuthey',
    initialSongs: IDHAYAM_PESUTHEY_SONGS,
    isFixedSongs: true
  },
  {
    id: 'semma-kuthu',
    title: 'Semma Kuthu ⚡',
    subtitle: '50 High-Voltage Kuthu Hits',
    language: 'Tamil',
    category: 'kuthu',
    coverUrl: 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
    gradient: 'linear-gradient(135deg, #EA580C 0%, #7C2D12 100%)',
    songCount: 50,
    followers: '5.2M Saves',
    description: 'The ultimate 50 Tamil high-energy mass & kuthu party anthems. Curated & fixed.',
    query: 'Semma Kuthu',
    initialSongs: SEMMA_KUTHU_SONGS,
    isFixedSongs: true
  },
  {
    id: 'mass-mode',
    title: 'Mass Mode 💥',
    subtitle: '50 Pure Mass & Adrenaline Hits',
    language: 'Tamil',
    category: 'mass',
    coverUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
    gradient: 'linear-gradient(135deg, #DC2626 0%, #450A0A 100%)',
    songCount: 50,
    followers: '6.1M Saves',
    description: 'Ultimate 50 Tamil mass, workout, hero entry & motivation anthems. Curated & fixed.',
    query: 'Mass Mode',
    initialSongs: MASS_MODE_SONGS,
    isFixedSongs: true
  },
  {
    id: 'gaana-pettai',
    title: 'Gaana Pettai 🥁',
    subtitle: '50 Chennai Gaana & Folk Beats',
    language: 'Tamil',
    category: 'kuthu',
    coverUrl: 'https://c.saavncdn.com/274/Anegan-Tamil-2014-20190822152158-500x500.jpg',
    gradient: 'linear-gradient(135deg, #D97706 0%, #78350F 100%)',
    songCount: 50,
    followers: '4.5M Saves',
    description: 'Chennai gaana, local beats, and authentic village folk anthems. Curated & fixed.',
    query: 'Gaana Pettai',
    initialSongs: GAANA_PETTAI_SONGS,
    isFixedSongs: true
  },
  {
    id: 'jannal-ora-payanam',
    title: 'Jannal Ora Payanam 🌿',
    subtitle: '50 Breezy Road Trip & Journey Songs',
    language: 'Tamil',
    category: 'chill',
    coverUrl: 'https://c.saavncdn.com/137/96-Original-Motion-Picture-Soundtrack-Tamil-2018-20250905072505-500x500.jpg',
    gradient: 'linear-gradient(135deg, #059669 0%, #064E3B 100%)',
    songCount: 50,
    followers: '5.7M Saves',
    description: 'Road trips, train journeys, breezy winds and soul-soothing travel melodies. Curated & fixed.',
    query: 'Jannal Ora Payanam',
    initialSongs: JANNAL_ORA_PAYANAM_SONGS,
    isFixedSongs: true
  },
  {
    id: 'iravu-melodies',
    title: 'Iravu Melodies 🌙',
    subtitle: '50 Quiet Nights & Gentle Melodies',
    language: 'Tamil',
    category: 'chill',
    coverUrl: 'https://c.saavncdn.com/203/Indira-Tamil-1993-20251014143719-500x500.jpg',
    gradient: 'linear-gradient(135deg, #0F172A 0%, #1E1B4B 100%)',
    songCount: 50,
    followers: '6.4M Saves',
    description: 'Quiet nights, midnight breezes, gentle acoustic strums and relaxing bedtime melodies. Curated & fixed.',
    query: 'Iravu Melodies',
    initialSongs: IRAVU_MELODIES_SONGS,
    isFixedSongs: true
  },
  {
    id: 'pudhu-udhayam',
    title: 'Pudhu Udhayam 💪',
    subtitle: '50 Motivation, Resilience & Hope Songs',
    language: 'Tamil',
    category: 'mass',
    coverUrl: 'https://c.saavncdn.com/002/Autograph-Tamil-2004-20180531-500x500.jpg',
    gradient: 'linear-gradient(135deg, #2563EB 0%, #1E3A8A 100%)',
    songCount: 50,
    followers: '4.9M Saves',
    description: 'Powerful motivational anthems, resilience, self-confidence and inspiring songs. Curated & fixed.',
    query: 'Pudhu Udhayam',
    initialSongs: PUDHU_UDHAYAM_SONGS,
    isFixedSongs: true
  },
  {
    id: 'irai-isai',
    title: 'Irai Isai 🙏',
    subtitle: '50 Divine Tamil Prayers & Hymns',
    language: 'Tamil',
    category: 'chill',
    coverUrl: 'https://c.saavncdn.com/274/Bhakthi-Sangamam-Tamil-2003-20201009144033-500x500.jpg',
    gradient: 'linear-gradient(135deg, #D97706 0%, #78350F 100%)',
    songCount: 50,
    followers: '7.2M Saves',
    description: 'Timeless Tamil devotional songs, sacred hymns, Thevaram, Thiruppavai and divine calm. Curated & fixed.',
    query: 'Irai Isai',
    initialSongs: IRAI_ISAI_SONGS,
    isFixedSongs: true
  },
  {
    id: 'school-days-memories',
    title: 'School Days Memories 📻',
    subtitle: '50 Late 90s & 2000s Nostalgia Hits',
    language: 'Tamil',
    category: '2k',
    coverUrl: 'https://c.saavncdn.com/752/Kadhal-Desam-Tamil-1996-20260923110001-500x500.jpg',
    gradient: 'linear-gradient(135deg, #0284C7 0%, #0369A1 100%)',
    songCount: 50,
    followers: '8.1M Saves',
    description: 'Late 1990s & 2000s school days nostalgia, friendship, cassette tape and radio favourites. Curated & fixed.',
    query: 'School Days Memories',
    initialSongs: SCHOOL_DAYS_MEMORIES_SONGS,
    isFixedSongs: true
  },
  {
    id: 'trending-tamil',
    title: 'Trending Hits 2026 🔥',
    subtitle: 'By ISAI Editorial • Jan - Sep 2026+',
    language: 'Tamil',
    category: 'trending',
    coverUrl: 'https://c.saavncdn.com/187/Jailer-Tamil-2023-20230728081443-500x500.jpg',
    gradient: 'linear-gradient(135deg, #E11D48 0%, #4C0519 100%)',
    songCount: 55,
    followers: '2.5M Saves',
    description:
      'Hottest Tamil tracks released from January 2026 to present. Automatically updates with every new release!',
    query: 'Tamil latest songs 2026',
    subqueries: [
      'Tamil new songs 2026',
      'Tamil trending hits 2026',
      'Tamil latest chartbusters 2026',
      'Tamil latest releases 2026',
      'Tamil viral songs 2026'
    ],
    isAutoTrending: true,
    fromDate: '2026-01-01'
  },
  {
    id: 'kuthu-tamil',
    title: 'Kuthu & Party Blast ⚡',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: 'kuthu',
    coverUrl: 'https://c.saavncdn.com/510/Beast-Tamil-2022-20220504184736-500x500.jpg',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #78350F 100%)',
    songCount: 60,
    followers: '1.9M Saves',
    description: 'High-voltage Tamil Dappankuthu, party beats & fast-paced dance anthems for pure energy.',
    query: 'Tamil kuthu dance party songs',
    subqueries: [
      'Tamil dappankuthu fast beat songs',
      'Anirudh kuthu dance hits',
      'Vijay kuthu songs collection',
      'Marana mass gana kuthu songs'
    ]
  },
  {
    id: 'romance-tamil',
    title: 'Tamil Romance ❤️',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: 'romance',
    coverUrl: 'https://c.saavncdn.com/450/2-In-1-Hits-Of-Maddy-Tamil-2001-20190515150512-500x500.jpg',
    gradient: 'linear-gradient(135deg, #EC4899 0%, #831843 100%)',
    songCount: 65,
    followers: '3.1M Saves',
    description: 'Soulful love anthems, magical duets & heart-melting romantic melodies from Kollywood.',
    query: 'Tamil romantic love melodies',
    subqueries: [
      'Tamil love duet songs',
      'AR Rahman romantic melodies',
      'Harris Jayaraj love hits',
      'Sid Sriram Tamil romantic songs'
    ]
  },
  {
    id: 'chill-tamil',
    title: 'Midnight Chill & Lo-Fi 🌙',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: 'chill',
    coverUrl:
      'https://c.saavncdn.com/420/Vendhu-Thanindhathu-Kaadu-Original-Motion-Picture-Soundtrack-Tamil-2022-20250905072731-500x500.jpg',
    gradient: 'linear-gradient(135deg, #06B6D4 0%, #164E63 100%)',
    songCount: 50,
    followers: '1.4M Saves',
    description: 'Midnight drives, soothing acoustic rhythms & relaxing Tamil lo-fi peaceful beats.',
    query: 'Tamil acoustic lofi chill songs',
    subqueries: [
      'Tamil midnight drive songs',
      'Tamil soothing rain lo-fi',
      'Pradeep Kumar chill acoustic hits',
      'Sean Roldan acoustic melodies'
    ]
  },
  {
    id: 'mass-tamil',
    title: 'Mass & Gym Motivation 💥',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: 'mass',
    coverUrl:
      'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #3B0764 100%)',
    songCount: 55,
    followers: '1.7M Saves',
    description: 'Power-packed theme tracks, adrenaline pumping mass anthems & intense gym motivation.',
    query: 'Tamil mass gym motivation songs',
    subqueries: [
      'Tamil workout mass BGM songs',
      'Anirudh mass bgm workout anthems',
      'Thalapathy Thala mass entry songs',
      'Tamil motivation adrenaline songs'
    ]
  },
  {
    id: '90s-tamil',
    title: '90s Golden Era 📻 (1990 - 1999)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: '90s',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F97316 0%, #7C2D12 100%)',
    songCount: 65,
    followers: '2.8M Saves',
    description: 'Unforgettable 90s golden classics of Ilaiyaraaja, early A.R. Rahman, Deva & SPB.',
    query: 'Tamil 90s golden super hits',
    subqueries: [
      'Ilayaraja 90s melody hits',
      'AR Rahman 90s classic songs',
      'SPB 90s Tamil super hit songs',
      'Deva 90s gaana melody hits'
    ]
  },
  {
    id: '2k-tamil',
    title: '2K Evergreen Hits ✨ (2000 - 2009)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: '2k',
    coverUrl: 'https://c.saavncdn.com/artists/Harris_Jayaraj_002_20230718071330_500x500.jpg',
    gradient: 'linear-gradient(135deg, #10B981 0%, #064E3B 100%)',
    songCount: 70,
    followers: '3.4M Saves',
    description: 'Golden childhood nostalgia from Harris Jayaraj, Yuvan Shankar Raja, Vidyasagar & early Anirudh.',
    query: 'Tamil 2000s evergreen super hit songs',
    subqueries: [
      'Harris Jayaraj 2000s blockbuster hits',
      'Yuvan Shankar Raja 2000s nostalgia songs',
      'Vidyasagar evergreen melody hits',
      'Minnale Ghilli Pokkiri Anniyan songs'
    ]
  },
  {
    id: '2010s-tamil',
    title: '2010s Blockbusters 🏆 (2010 - 2019)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: '2010s',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1E3A8A 100%)',
    songCount: 60,
    followers: '2.9M Saves',
    description: 'The revolutionary decade of 3, Kaththi, Thupakki, Mersal, Petta & Master.',
    query: 'Tamil 2010 to 2019 blockbuster hits',
    subqueries: [
      'Anirudh 2010s super hits',
      'Santhosh Narayanan blockbuster songs',
      'Hip Hop Tamizha 2010s hits',
      'Kaththi Mersal Thupakki songs'
    ]
  },
  {
    id: '2020s-tamil',
    title: '2020 - 2025 Modern Hits 🌟',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: '2020s',
    coverUrl:
      'https://c.saavncdn.com/403/Kaathuvaakula-Rendu-Kaadhal-Original-Motion-Picture-Soundtrack-Tamil-2022-20220428131043-500x500.jpg',
    gradient: 'linear-gradient(135deg, #A855F7 0%, #581C87 100%)',
    songCount: 55,
    followers: '2.1M Saves',
    description: 'Sensational hits of the 2020s: Vikram, Jailer, Leo, Amaran, GOAT, Viduthalai & Raayan.',
    query: 'Tamil 2020 to 2025 super hit songs',
    subqueries: [
      'Vikram Jailer Leo hits',
      'Amaran GOAT latest songs',
      'Tamil viral modern songs 2021 2024',
      'Kaathuvaakula Rendu Kaadhal hits'
    ]
  },
  {
    id: 'retro-tamil',
    title: 'Retro Vintage Classics 🎙️ (80s & Earlier)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Tamil',
    category: 'retro',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #78716C 0%, #1C1917 100%)',
    songCount: 55,
    followers: '1.2M Saves',
    description: 'Pristine vintage recordings of MSV, TMS, P. Susheela, S. Janaki & 80s Ilaiyaraaja magic.',
    query: 'Tamil retro 80s 70s vintage classic songs',
    subqueries: [
      'Ilayaraja 80s vintage super hits',
      'TMS MSV old Tamil classic songs',
      'SPB Janaki 80s evergreen duets',
      'Tamil golden vintage melody classics'
    ]
  },

  // ══════════════════════════════════════════════════════════════════════
  // ── TELUGU ────────────────────────────────────────────────────────────
  // ══════════════════════════════════════════════════════════════════════
  {
    id: 'trending-telugu',
    title: 'Trending Hits 2026 🔥',
    subtitle: 'By ISAI Editorial • Jan - Sep 2026+',
    language: 'Telugu',
    category: 'trending',
    coverUrl: 'https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg',
    gradient: 'linear-gradient(135deg, #EF4444 0%, #7F1D1D 100%)',
    songCount: 50,
    followers: '1.8M Saves',
    description: 'Tollywood chartbusters released from January 2026 to present. Automatically updated!',
    query: 'Telugu latest songs 2026',
    subqueries: [
      'Telugu new songs 2026',
      'Telugu latest chartbusters 2026',
      'Telugu viral songs 2026',
      'Tollywood latest hits 2026'
    ],
    isAutoTrending: true,
    fromDate: '2026-01-01'
  },
  {
    id: 'kuthu-telugu',
    title: 'Tollywood Mass & Party ⚡',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: 'kuthu',
    coverUrl: 'https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #78350F 100%)',
    songCount: 55,
    followers: '1.5M Saves',
    description: 'High-voltage commercial party tracks, energetic dance beats & mass numbers.',
    query: 'Telugu mass commercial dance party hits',
    subqueries: [
      'Thaman S mass party songs',
      'DSP mass dance numbers',
      'Allu Arjun Mahesh Babu mass dance hits',
      'Telugu DJ party songs'
    ]
  },
  {
    id: 'romance-telugu',
    title: 'Telugu Romance ❤️',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: 'romance',
    coverUrl: 'https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F43F5E 0%, #881337 100%)',
    songCount: 60,
    followers: '2.2M Saves',
    description: 'Heart-melting romantic duets & soothing melodies sung by Sid Sriram, Anurag Kulkarni & more.',
    query: 'Telugu romantic melodies love songs',
    subqueries: [
      'Sid Sriram Telugu love songs',
      'Telugu evergreen romantic melodies',
      'Geetha Govindam Ala Vaikunthapurramuloo love songs',
      'Telugu melody love duets'
    ]
  },
  {
    id: 'chill-telugu',
    title: 'Telugu Acoustic & Chill 🌙',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: 'chill',
    coverUrl: 'https://c.saavncdn.com/artists/Sid_Sriram_005_20240425180600_500x500.jpg',
    gradient: 'linear-gradient(135deg, #06B6D4 0%, #164E63 100%)',
    songCount: 50,
    followers: '980K Saves',
    description: 'Relaxing Telugu acoustic rhythms, lo-fi vibes & peaceful evening melodies.',
    query: 'Telugu acoustic lofi chill songs',
    subqueries: ['Telugu soothing midnight songs', 'Telugu calm acoustic melodies', 'Telugu relaxing playlist']
  },
  {
    id: 'mass-telugu',
    title: 'Tollywood Power & Workout 💥',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: 'mass',
    coverUrl: 'https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg',
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #3B0764 100%)',
    songCount: 50,
    followers: '1.2M Saves',
    description: 'Adrenaline pumping Telugu gym motivation songs & intense movie theme tracks.',
    query: 'Telugu workout gym motivation mass BGM',
    subqueries: ['Telugu mass action theme songs', 'Pushpa RRR mass BGM songs', 'Telugu high energy workout']
  },
  {
    id: '90s-telugu',
    title: '90s Tollywood Classics 📻 (1990 - 1999)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: '90s',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F97316 0%, #7C2D12 100%)',
    songCount: 55,
    followers: '1.6M Saves',
    description: 'Golden 90s Tollywood classics of SPB, Chitra, Keeravani & Koti.',
    query: 'Telugu 90s super hit songs',
    subqueries: ['Keeravani 90s Telugu hits', 'SPB Telugu 90s classic songs', 'Chiranjeevi Nagarjuna 90s hits']
  },
  {
    id: '2k-telugu',
    title: '2K Golden Hits ✨ (2000 - 2009)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: '2k',
    coverUrl: 'https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg',
    gradient: 'linear-gradient(135deg, #10B981 0%, #064E3B 100%)',
    songCount: 60,
    followers: '2.4M Saves',
    description: 'Iconic 2000s Telugu anthems from DSP, Mani Sharma & Harris Jayaraj.',
    query: 'Telugu 2000s evergreen super hit songs',
    subqueries: [
      'DSP 2000s blockbuster hits',
      'Mani Sharma Telugu melody hits',
      'Pokiri Arya Bommarillu songs'
    ]
  },
  {
    id: '2010s-telugu',
    title: '2010s Blockbusters 🏆 (2010 - 2019)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: '2010s',
    coverUrl: 'https://c.saavncdn.com/artists/Thaman_S__007_20231106094011_500x500.jpg',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1E3A8A 100%)',
    songCount: 55,
    followers: '2.1M Saves',
    description: 'Baahubali, Ala Vaikunthapurramuloo, Sarileru Neekevvaru & Rangasthalam era.',
    query: 'Telugu 2010 to 2019 blockbuster hits',
    subqueries: ['Baahubali Rangasthalam songs', 'Thaman 2010s hits', 'Telugu viral hits 2015 2019']
  },
  {
    id: '2020s-telugu',
    title: '2020 - 2025 Modern Hits 🌟',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: '2020s',
    coverUrl: 'https://c.saavncdn.com/artists/Devi_Sri_Prasad_008_20250619062824_500x500.jpg',
    gradient: 'linear-gradient(135deg, #A855F7 0%, #581C87 100%)',
    songCount: 50,
    followers: '1.9M Saves',
    description: 'Global viral sensations from Pushpa, RRR, Devara, Kalki 2898 AD & Guntur Kaaram.',
    query: 'Telugu 2020 to 2025 super hit songs',
    subqueries: ['Pushpa RRR Devara Kalki songs', 'Telugu modern viral hits', 'Guntur Kaaram hits']
  },
  {
    id: 'retro-telugu',
    title: 'Retro Vintage Classics 🎙️ (80s & Earlier)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Telugu',
    category: 'retro',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #78716C 0%, #1C1917 100%)',
    songCount: 50,
    followers: '900K Saves',
    description: 'Golden vintage melodies of Ilaiyaraaja, Ghantasala, K.V. Mahadevan & SPB in Telugu.',
    query: 'Telugu retro 80s 70s vintage classic songs',
    subqueries: ['Ilayaraja Telugu 80s vintage hits', 'Ghantasala Telugu old classics', 'SPB Susheela old duets']
  },

  // ══════════════════════════════════════════════════════════════════════
  // ── HINDI ─────────────────────────────────────────────────────────────
  // ══════════════════════════════════════════════════════════════════════
  {
    id: 'trending-hindi',
    title: 'Trending Hits 2026 🔥',
    subtitle: 'By ISAI Editorial • Jan - Sep 2026+',
    language: 'Hindi',
    category: 'trending',
    coverUrl: 'https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F43F5E 0%, #4C0519 100%)',
    songCount: 55,
    followers: '4.5M Saves',
    description: 'Bollywood chart-toppers released from January 2026 to present. Automatically updated!',
    query: 'Hindi latest bollywood songs 2026',
    subqueries: [
      'Hindi new songs 2026',
      'Bollywood new releases 2026',
      'Hindi latest chartbusters 2026',
      'Bollywood viral songs 2026'
    ],
    isAutoTrending: true,
    fromDate: '2026-01-01'
  },
  {
    id: 'kuthu-hindi',
    title: 'Bollywood Party & Dance ⚡',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: 'kuthu',
    coverUrl: 'https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #78350F 100%)',
    songCount: 55,
    followers: '3.1M Saves',
    description: 'Floor-burning club bangers, wedding dance anthems & high-energy Bollywood party tracks.',
    query: 'Hindi bollywood dance party songs',
    subqueries: [
      'Badshah Honey Singh party hits',
      'Bollywood club dance numbers',
      'Hindi high energy dance songs',
      'Bollywood wedding party songs'
    ]
  },
  {
    id: 'romance-hindi',
    title: 'Bollywood Romance ❤️',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: 'romance',
    coverUrl: 'https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg',
    gradient: 'linear-gradient(135deg, #D946EF 0%, #701A75 100%)',
    songCount: 65,
    followers: '5.2M Saves',
    description: 'Soulful love ballads & romantic duets from Arijit Singh, Shreya Ghoshal & Pritam.',
    query: 'Hindi romantic love songs Arijit Shreya',
    subqueries: [
      'Arijit Singh romantic love songs',
      'Shreya Ghoshal romantic melodies',
      'Bollywood love duets',
      'Atif Aslam romantic hits'
    ]
  },
  {
    id: 'chill-hindi',
    title: 'Hindi Midnight Chill & Lo-Fi 🌙',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: 'chill',
    coverUrl: 'https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg',
    gradient: 'linear-gradient(135deg, #06B6D4 0%, #164E63 100%)',
    songCount: 50,
    followers: '1.9M Saves',
    description: 'Peaceful acoustic sessions, soothing Hindi lo-fi & late-night relaxing melodies.',
    query: 'Hindi acoustic lofi chill songs',
    subqueries: [
      'Bollywood lo-fi chill songs',
      'Hindi midnight relaxing songs',
      'Prateek Kuhad Anuv Jain chill acoustic'
    ]
  },
  {
    id: 'mass-hindi',
    title: 'Bollywood Power & Workout 💥',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: 'mass',
    coverUrl: 'https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg',
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #3B0764 100%)',
    songCount: 50,
    followers: '1.5M Saves',
    description: 'Heavy basslines, motivating gym tracks & powerful Bollywood action themes.',
    query: 'Hindi gym workout motivation songs',
    subqueries: [
      'Bollywood workout mass energy songs',
      'Hindi action movie theme songs',
      'Sultan Dangal motivation songs'
    ]
  },
  {
    id: '90s-hindi',
    title: '90s Bollywood Classics 📻 (1990 - 1999)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: '90s',
    coverUrl: 'https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F97316 0%, #7C2D12 100%)',
    songCount: 65,
    followers: '3.6M Saves',
    description: 'Unforgettable 90s melodies of Kumar Sanu, Udit Narayan, Alka Yagnik & Nadeem-Shravan.',
    query: '90s bollywood golden melody hits',
    subqueries: [
      'Kumar Sanu 90s super hits',
      'Udit Narayan Alka Yagnik 90s duets',
      'Nadeem Shravan 90s classic songs'
    ]
  },
  {
    id: '2k-hindi',
    title: '2K Evergreen Bollywood ✨ (2000 - 2009)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: '2k',
    coverUrl: 'https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg',
    gradient: 'linear-gradient(135deg, #10B981 0%, #064E3B 100%)',
    songCount: 65,
    followers: '4.1M Saves',
    description: 'Memorable 2000s classics of Sonu Nigam, KK, Shaan, Mohit Chauhan & Shankar-Ehsaan-Loy.',
    query: 'Hindi 2000s evergreen bollywood hits',
    subqueries: [
      'KK 2000s super hit songs',
      'Sonu Nigam 2000s romantic songs',
      'Kal Ho Naa Ho Jab We Met Dil Chahta Hai songs'
    ]
  },
  {
    id: '2010s-hindi',
    title: '2010s Blockbusters 🏆 (2010 - 2019)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: '2010s',
    coverUrl: 'https://c.saavncdn.com/artists/Shreya_Ghoshal_007_20241101074144_500x500.jpg',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1E3A8A 100%)',
    songCount: 60,
    followers: '3.8M Saves',
    description: 'The golden decade of Aashiqui 2, Yeh Jawaani Hai Deewani, Kabir Singh & Ae Dil Hai Mushkil.',
    query: 'Hindi 2010 to 2019 blockbuster hits',
    subqueries: [
      'Aashiqui 2 Kabir Singh songs',
      'Arijit Singh 2010s blockbuster hits',
      'Yeh Jawaani Hai Deewani songs'
    ]
  },
  {
    id: '2020s-hindi',
    title: '2020 - 2025 Modern Hits 🌟',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: '2020s',
    coverUrl: 'https://c.saavncdn.com/artists/Arijit_Singh_004_20241118063717_500x500.jpg',
    gradient: 'linear-gradient(135deg, #A855F7 0%, #581C87 100%)',
    songCount: 55,
    followers: '3.2M Saves',
    description: 'Sensational hits of the 2020s: Animal, Jawan, Pathaan, Bramhastra, Stree 2 & Dunki.',
    query: 'Hindi 2020 to 2025 super hit songs',
    subqueries: ['Animal Jawan Pathaan songs', 'Stree 2 Brahmastra hits', 'Hindi viral songs 2021 2024']
  },
  {
    id: 'retro-hindi',
    title: 'Retro Vintage Classics 🎙️ (80s & Earlier)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Hindi',
    category: 'retro',
    coverUrl: 'https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg',
    gradient: 'linear-gradient(135deg, #78716C 0%, #1C1917 100%)',
    songCount: 50,
    followers: '1.8M Saves',
    description: 'Timeless vintage gems of Kishore Kumar, Mohammed Rafi, Lata Mangeshkar & RD Burman.',
    query: 'Hindi retro 70s 80s vintage classic songs',
    subqueries: [
      'Kishore Kumar old romantic songs',
      'RD Burman classic hits',
      'Lata Mangeshkar Rafi old evergreen duets'
    ]
  },

  // ══════════════════════════════════════════════════════════════════════
  // ── MALAYALAM ─────────────────────────────────────────────────────────
  // ══════════════════════════════════════════════════════════════════════
  {
    id: 'trending-malayalam',
    title: 'Trending Hits 2026 🔥',
    subtitle: 'By ISAI Editorial • Jan - Sep 2026+',
    language: 'Malayalam',
    category: 'trending',
    coverUrl: 'https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg',
    gradient: 'linear-gradient(135deg, #10B981 0%, #064E3B 100%)',
    songCount: 50,
    followers: '1.2M Saves',
    description: 'Trending Mollywood songs released from January 2026 to present. Automatically updated!',
    query: 'Malayalam latest songs 2026',
    subqueries: ['Malayalam new songs 2026', 'Mollywood latest chartbusters 2026', 'Malayalam viral songs 2026'],
    isAutoTrending: true,
    fromDate: '2026-01-01'
  },
  {
    id: 'kuthu-malayalam',
    title: 'Mollywood Party & Beats ⚡',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: 'kuthu',
    coverUrl: 'https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #78350F 100%)',
    songCount: 50,
    followers: '940K Saves',
    description: 'High energy Malayalam dance numbers, Aavesham vibes & fast party tracks.',
    query: 'Malayalam fast dance party songs',
    subqueries: ['Aavesham party songs Sushin Shyam', 'Malayalam fast beat songs', 'Malayalam DJ party hits']
  },
  {
    id: 'romance-malayalam',
    title: 'Malayalam Romance ❤️',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: 'romance',
    coverUrl: 'https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg',
    gradient: 'linear-gradient(135deg, #EC4899 0%, #831843 100%)',
    songCount: 55,
    followers: '1.4M Saves',
    description: 'Pure romantic poetry, serene love melodies & soulful Malayalam duets.',
    query: 'Malayalam romantic love songs',
    subqueries: ['Hridayam Premalu love songs', 'Shaan Rahman romantic melodies', 'Malayalam love duets']
  },
  {
    id: 'chill-malayalam',
    title: 'Malayalam Chill & Acoustic 🌿',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: 'chill',
    coverUrl: 'https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg',
    gradient: 'linear-gradient(135deg, #14B8A6 0%, #134E4A 100%)',
    songCount: 50,
    followers: '850K Saves',
    description: 'Serene acoustic melodies, calm vocals & soothing Malayalam tunes.',
    query: 'Malayalam acoustic chill melodies',
    subqueries: ['Malayalam soothing rain acoustic', 'Hesham Abdul Wahab chill songs', 'Malayalam lofi relaxation']
  },
  {
    id: 'mass-malayalam',
    title: 'Mollywood Mass & Motivation 💥',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: 'mass',
    coverUrl: 'https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg',
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #3B0764 100%)',
    songCount: 50,
    followers: '780K Saves',
    description: 'Lucifer, Minnal Murali, ARM, Aavesham & powerful Malayalam mass theme tracks.',
    query: 'Malayalam mass workout theme BGM songs',
    subqueries: ['Lucifer Aavesham BGM songs', 'Malayalam gym workout songs', 'Malayalam mass action songs']
  },
  {
    id: '90s-malayalam',
    title: '90s Golden Nostalgia 📻 (1990 - 1999)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: '90s',
    coverUrl: 'https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F97316 0%, #7C2D12 100%)',
    songCount: 50,
    followers: '1.1M Saves',
    description: 'Golden era songs of Yesudas, Chithra, Johnson Master & Raveendran Master.',
    query: 'Malayalam 90s golden melody hits',
    subqueries: ['Yesudas 90s super hits', 'Johnson Master Malayalam classics', 'Malayalam 90s evergreen duets']
  },
  {
    id: '2k-malayalam',
    title: '2K Evergreen Hits ✨ (2000 - 2009)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: '2k',
    coverUrl: 'https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg',
    gradient: 'linear-gradient(135deg, #10B981 0%, #064E3B 100%)',
    songCount: 50,
    followers: '1.3M Saves',
    description: 'Memorable 2000s Malayalam melodies from Vidyasagar, Deepak Dev & M. Jayachandran.',
    query: 'Malayalam 2000s evergreen super hit songs',
    subqueries: ['Vidyasagar Malayalam super hits', 'Deepak Dev 2000s hits', 'Malayalam 2000s romantic songs']
  },
  {
    id: '2010s-malayalam',
    title: '2010s Blockbusters 🏆 (2010 - 2019)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: '2010s',
    coverUrl: 'https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1E3A8A 100%)',
    songCount: 50,
    followers: '1.5M Saves',
    description: 'Premam, Bangalore Days, Charlie, Ennu Ninte Moideen & Kumbalangi Nights hits.',
    query: 'Malayalam 2010 to 2019 blockbuster hits',
    subqueries: ['Premam Bangalore Days Charlie songs', 'Kumbalangi Nights songs', 'Malayalam 2010s super hits']
  },
  {
    id: '2020s-malayalam',
    title: '2020 - 2025 Modern Hits 🌟',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: '2020s',
    coverUrl: 'https://c.saavncdn.com/artists/Sushin_Shyam_002_20250707125538_500x500.jpg',
    gradient: 'linear-gradient(135deg, #A855F7 0%, #581C87 100%)',
    songCount: 50,
    followers: '1.7M Saves',
    description: 'Manjummel Boys, Premalu, Aavesham, ARM, Bramayugam & Minnal Murali hits.',
    query: 'Malayalam 2020 to 2025 super hit songs',
    subqueries: ['Manjummel Boys Premalu Aavesham songs', 'ARM Bramayugam songs', 'Malayalam viral modern songs']
  },
  {
    id: 'retro-malayalam',
    title: 'Retro Vintage Classics 🎙️ (80s & Earlier)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Malayalam',
    category: 'retro',
    coverUrl: 'https://c.saavncdn.com/artists/Shaan_Rahman_500x500.jpg',
    gradient: 'linear-gradient(135deg, #78716C 0%, #1C1917 100%)',
    songCount: 50,
    followers: '680K Saves',
    description: 'Evergreen nostalgic masterworks of Yesudas, Jayachandran, S. Janaki & Baburaj.',
    query: 'Malayalam retro 80s 70s vintage classic songs',
    subqueries: ['Yesudas old Malayalam classics', 'Devarajan Master old songs', 'Malayalam 80s vintage melodies']
  },

  // ══════════════════════════════════════════════════════════════════════
  // ── KANNADA ───────────────────────────────────────────────────────────
  // ══════════════════════════════════════════════════════════════════════
  {
    id: 'trending-kannada',
    title: 'Trending Hits 2026 🔥',
    subtitle: 'By ISAI Editorial • Jan - Sep 2026+',
    language: 'Kannada',
    category: 'trending',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #EAB308 0%, #713F12 100%)',
    songCount: 50,
    followers: '920K Saves',
    description: 'Top Sandalwood hits released from January 2026 to present. Automatically updated!',
    query: 'Kannada latest songs 2026',
    subqueries: ['Kannada new songs 2026', 'Sandalwood latest chartbusters 2026', 'Kannada viral songs 2026'],
    isAutoTrending: true,
    fromDate: '2026-01-01'
  },
  {
    id: 'kuthu-kannada',
    title: 'Sandalwood Dance & Beats ⚡',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: 'kuthu',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #78350F 100%)',
    songCount: 50,
    followers: '760K Saves',
    description: 'Fast-paced Sandalwood dance tracks, DJ party beats & high energy songs.',
    query: 'Kannada mass dance party songs',
    subqueries: ['Kantara KGF dance beats', 'Kannada fast beat songs', 'Sandalwood DJ party hits']
  },
  {
    id: 'romance-kannada',
    title: 'Kannada Romance ❤️',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: 'romance',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #EC4899 0%, #831843 100%)',
    songCount: 50,
    followers: '1.1M Saves',
    description: 'Heartwarming Kannada romantic love duets, melodies & acoustic love tracks.',
    query: 'Kannada romantic love songs',
    subqueries: [
      'Sonu Nigam Kannada romantic songs',
      'Sanjith Hegde romantic hits',
      'Kannada evergreen love melodies'
    ]
  },
  {
    id: 'chill-kannada',
    title: 'Kannada Acoustic & Chill 🌙',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: 'chill',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #06B6D4 0%, #164E63 100%)',
    songCount: 50,
    followers: '620K Saves',
    description: 'Calm evening rhythms, soothing acoustics & peaceful Kannada songs.',
    query: 'Kannada acoustic lofi chill songs',
    subqueries: ['Kannada midnight peaceful melodies', 'Kannada relaxing acoustic playlist']
  },
  {
    id: 'mass-kannada',
    title: 'Sandalwood Mass & Workout 💥',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: 'mass',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #3B0764 100%)',
    songCount: 50,
    followers: '980K Saves',
    description: 'KGF, Kantara, Vikrant Rona, Yuva & high adrenaline Sandalwood mass anthems.',
    query: 'Kannada mass gym motivation BGM songs',
    subqueries: ['KGF theme BGM songs', 'Kantara mass BGM', 'Kannada gym motivation workout']
  },
  {
    id: '90s-kannada',
    title: '90s Sandalwood Nostalgia 📻 (1990 - 1999)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: '90s',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F97316 0%, #7C2D12 100%)',
    songCount: 50,
    followers: '850K Saves',
    description: '90s golden hits of Hamsalekha, SPB, Chithra, Dr. Rajkumar & Vishnuvardhan.',
    query: 'Kannada 90s golden melody hits',
    subqueries: ['Hamsalekha 90s super hits', 'SPB Kannada 90s classics', 'Dr Rajkumar 90s hits']
  },
  {
    id: '2k-kannada',
    title: '2K Evergreen Hits ✨ (2000 - 2009)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: '2k',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #10B981 0%, #064E3B 100%)',
    songCount: 50,
    followers: '1.2M Saves',
    description: 'Mungaru Male, Milana, Jothe Jotheyali & iconic 2000s Sandalwood melodies.',
    query: 'Kannada 2000s evergreen super hit songs',
    subqueries: ['Mungaru Male Milana songs', 'Mano Murthy 2000s hits', 'Sonu Nigam 2000s Kannada hits']
  },
  {
    id: '2010s-kannada',
    title: '2010s Blockbusters 🏆 (2010 - 2019)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: '2010s',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1E3A8A 100%)',
    songCount: 50,
    followers: '1.5M Saves',
    description: 'KGF Chapter 1, Kirik Party, Googly, Tagaru & Ulidavaru Kandanthe era.',
    query: 'Kannada 2010 to 2019 blockbuster hits',
    subqueries: ['KGF Kirik Party songs', 'Tagaru Googly blockbuster hits', 'Ajaneesh Loknath 2010s hits']
  },
  {
    id: '2020s-kannada',
    title: '2020 - 2025 Modern Hits 🌟',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: '2020s',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #A855F7 0%, #581C87 100%)',
    songCount: 50,
    followers: '1.6M Saves',
    description: 'Kantara, KGF Chapter 2, 777 Charlie, Vikrant Rona, Yuva & modern hits.',
    query: 'Kannada 2020 to 2025 super hit songs',
    subqueries: ['Kantara KGF 2 Charlie 777 songs', 'Vikrant Rona Yuva songs', 'Kannada modern viral hits']
  },
  {
    id: 'retro-kannada',
    title: 'Retro Vintage Classics 🎙️ (80s & Earlier)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'Kannada',
    category: 'retro',
    coverUrl: 'https://c.saavncdn.com/artists/AR_Rahman_002_20210120084455_500x500.jpg',
    gradient: 'linear-gradient(135deg, #78716C 0%, #1C1917 100%)',
    songCount: 50,
    followers: '710K Saves',
    description: 'Priceless vintage masterpieces of Dr. Rajkumar, P.B. Sreenivas & S. Janaki.',
    query: 'Kannada retro 80s 70s vintage classic songs',
    subqueries: ['Dr Rajkumar evergreen old songs', 'PB Sreenivas Kannada classics', 'Kannada 70s 80s vintage duets']
  },

  // ══════════════════════════════════════════════════════════════════════
  // ── ENGLISH ───────────────────────────────────────────────────────────
  // ══════════════════════════════════════════════════════════════════════
  {
    id: 'trending-english',
    title: 'Trending Global Hits 2026 🔥',
    subtitle: 'By ISAI Editorial • Jan - Sep 2026+',
    language: 'English',
    category: 'trending',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1E3A8A 100%)',
    songCount: 50,
    followers: '5.2M Saves',
    description: 'Worldwide Billboard & streaming hits released from January 2026 to present.',
    query: 'English pop chart hits 2026',
    subqueries: ['English new songs 2026', 'Billboard top 50 2026', 'Global viral pop hits 2026'],
    isAutoTrending: true,
    fromDate: '2026-01-01'
  },
  {
    id: 'kuthu-english',
    title: 'Global Dance & EDM Blast ⚡',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: 'kuthu',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F59E0B 0%, #78350F 100%)',
    songCount: 50,
    followers: '3.8M Saves',
    description: 'High-energy electronic dance music, festival anthems & party pop.',
    query: 'English edm festival dance party songs',
    subqueries: ['Calvin Harris David Guetta EDM hits', 'English club bangers dance songs', 'Upbeat workout dance pop']
  },
  {
    id: 'romance-english',
    title: 'Pop Love & Romance ❤️',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: 'romance',
    coverUrl:
      'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
    gradient: 'linear-gradient(135deg, #EC4899 0%, #831843 100%)',
    songCount: 55,
    followers: '4.6M Saves',
    description: 'Heartfelt love ballads from Ed Sheeran, Taylor Swift, Bruno Mars & Adele.',
    query: 'English romantic love pop songs',
    subqueries: ['Ed Sheeran Taylor Swift love songs', 'English acoustic romantic duets', 'Billboard romantic ballads']
  },
  {
    id: 'chill-english',
    title: 'Pop & Chill Vibes 🎧',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: 'chill',
    coverUrl:
      'https://c.saavncdn.com/415/Leo-Original-Motion-Picture-Soundtrack-English-2023-20231019170311-500x500.jpg',
    gradient: 'linear-gradient(135deg, #6366F1 0%, #312E81 100%)',
    songCount: 50,
    followers: '3.1M Saves',
    description: 'Smooth English acoustic, lo-fi beats & relaxing weekend pop.',
    query: 'English chill acoustic pop',
    subqueries: ['English lofi acoustic chill songs', 'Billie Eilish relaxing pop', 'Calm weekend morning pop']
  },
  {
    id: 'mass-english',
    title: 'Workout & Hype Energy 💥',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: 'mass',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #8B5CF6 0%, #3B0764 100%)',
    songCount: 50,
    followers: '3.5M Saves',
    description: 'Adrenaline pumping gym anthems, trap beats & ultimate workout motivation.',
    query: 'English gym workout motivation songs',
    subqueries: ['Eminem workout gym motivation', 'Imagine Dragons hype anthems', 'Heavy bass workout trap']
  },
  {
    id: '90s-english',
    title: '90s Pop & Rock Nostalgia 📻 (1990 - 1999)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: '90s',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #F97316 0%, #7C2D12 100%)',
    songCount: 50,
    followers: '2.7M Saves',
    description: 'Backstreet Boys, Britney Spears, Nirvana, Oasis, Madonna & 90s billboard legends.',
    query: 'English 90s pop rock super hits',
    subqueries: ['Backstreet Boys Britney 90s pop', '90s rock alternative classics', '90s billboard top 100']
  },
  {
    id: '2k-english',
    title: '2000s Pop Classics ✨ (2000 - 2009)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: '2k',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #10B981 0%, #064E3B 100%)',
    songCount: 55,
    followers: '3.9M Saves',
    description: 'Rihanna, Eminem, Linkin Park, Beyonce, Black Eyed Peas & 2000s golden pop.',
    query: 'English 2000s pop super hit songs',
    subqueries: ['2000s pop billboard hits', 'Eminem Rihanna 2000s hits', 'Linkin Park 2000s classics']
  },
  {
    id: '2010s-english',
    title: '2010s Blockbusters 🏆 (2010 - 2019)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: '2010s',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #3B82F6 0%, #1E3A8A 100%)',
    songCount: 55,
    followers: '4.2M Saves',
    description: 'Avicii, The Weeknd, Drake, Taylor Swift, Ed Sheeran & Bruno Mars 2010s hits.',
    query: 'English 2010 to 2019 blockbuster hits',
    subqueries: ['Avicii The Weeknd 2010s pop', 'Shape of You Blinding Lights era', '2010s billboard top hits']
  },
  {
    id: '2020s-english',
    title: '2020 - 2025 Modern Hits 🌟',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: '2020s',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #A855F7 0%, #581C87 100%)',
    songCount: 50,
    followers: '3.6M Saves',
    description: 'Dua Lipa, Olivia Rodrigo, Billie Eilish, Sabrina Carpenter & modern pop leaders.',
    query: 'English 2020 to 2025 super hit songs',
    subqueries: ['Olivia Rodrigo Dua Lipa modern hits', 'Sabrina Carpenter viral songs', '2020s billboard chart hits']
  },
  {
    id: 'retro-english',
    title: 'Retro Classic Legends 🎙️ (80s & Earlier)',
    subtitle: 'By ISAI Editorial • 50+ Songs',
    language: 'English',
    category: 'retro',
    coverUrl: 'https://c.saavncdn.com/artists/Anirudh_Ravichander_003_20260121134149_500x500.jpg',
    gradient: 'linear-gradient(135deg, #78716C 0%, #1C1917 100%)',
    songCount: 50,
    followers: '2.3M Saves',
    description: 'Michael Jackson, Queen, The Beatles, ABBA, Elvis & timeless retro legends.',
    query: 'English retro 70s 80s classic legends songs',
    subqueries: ['Michael Jackson 80s hits', 'Queen The Beatles classic rock', '80s pop retro legends']
  }
]
