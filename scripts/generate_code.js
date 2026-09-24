import fs from 'fs';

const songs = JSON.parse(fs.readFileSync('scripts/verified_curated_songs.json', 'utf8'));

// 1. Generate Kotlin code for YouTubeMusicRepository.kt
let ktBlock = `        val CURATED_TAMIL_SONGS: List<YouTubeSong> = listOf(\n`;
for (const s of songs) {
  const safeTitle = s.title.replace(/"/g, '\\"');
  const safeChannel = s.channelTitle.replace(/"/g, '\\"');
  ktBlock += `            YouTubeSong(
                videoId = "${s.videoId}",
                title = "${safeTitle}",
                channelTitle = "${safeChannel}",
                thumbnailUrl = "${s.thumbnailUrl}",
                durationFormatted = "${s.durationFormatted}",
                durationMs = ${s.durationMs}L,
                viewCountFormatted = "${s.viewCountFormatted}",
                audioUrl = "${s.audioUrl}",
                playCount = ${s.playCount}L,
                language = "tamil"
            ),\n`;
}
ktBlock += `        )\n`;

fs.writeFileSync('scripts/generated_kt_curated.txt', ktBlock);
console.log('Generated Kotlin block, length:', ktBlock.length);

// 2. Generate TypeScript code for App.tsx
let tsBlock = `const INITIAL_CURATED_SONGS: Song[] = [\n`;
for (const s of songs) {
  const safeTitle = s.title.replace(/'/g, "\\'");
  const safeChannel = s.channelTitle.replace(/'/g, "\\'");
  tsBlock += `  {
    videoId: '${s.videoId}',
    title: '${safeTitle}',
    channelTitle: '${safeChannel}',
    thumbnailUrl: '${s.thumbnailUrl}',
    durationFormatted: '${s.durationFormatted}',
    durationMs: ${s.durationMs},
    viewCountFormatted: '${s.viewCountFormatted}',
    audioUrl: '${s.audioUrl}'
  },\n`;
}
tsBlock += `]\n`;

fs.writeFileSync('scripts/generated_ts_curated.txt', tsBlock);
console.log('Generated TypeScript block, length:', tsBlock.length);
