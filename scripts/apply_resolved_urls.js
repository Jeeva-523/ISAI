import fs from 'fs';

const urlMap = JSON.parse(fs.readFileSync('scripts/resolved_urls.json', 'utf8'));
delete urlMap[''];

console.log('Total URLs to replace:', Object.keys(urlMap).length);

// 1. Update YouTubeMusicRepository.kt
let ytContent = fs.readFileSync('android-app/app/src/main/java/com/saavn/music/data/repository/YouTubeMusicRepository.kt', 'utf8');
console.log('Sample oldUrl from map:', Object.keys(urlMap)[0]);
const sample = Object.keys(urlMap)[0];
console.log('Index of sample in ytContent:', ytContent.indexOf(sample));
if (ytContent.indexOf(sample) === -1) {
  const sampleClean = sample.trim();
  console.log('Index after trim:', ytContent.indexOf(sampleClean));
}
let ytReplaced = 0;
for (const [oldUrl, newUrl] of Object.entries(urlMap)) {
  const trimmed = oldUrl.trim();
  if (trimmed && trimmed.length > 10 && ytContent.includes(trimmed)) {
    ytContent = ytContent.replaceAll(trimmed, newUrl.trim());
    ytReplaced++;
  }
}
fs.writeFileSync('android-app/app/src/main/java/com/saavn/music/data/repository/YouTubeMusicRepository.kt', ytContent, 'utf8');
console.log(`Updated YouTubeMusicRepository.kt (${ytReplaced} URLs replaced)`);

// 2. Update MusicRepository.kt
let mrContent = fs.readFileSync('android-app/app/src/main/java/com/saavn/music/data/repository/MusicRepository.kt', 'utf8');
let mrReplaced = 0;
for (const [oldUrl, newUrl] of Object.entries(urlMap)) {
  if (oldUrl && oldUrl.length > 10 && mrContent.includes(oldUrl)) {
    mrContent = mrContent.replaceAll(oldUrl, newUrl);
    mrReplaced++;
  }
}
fs.writeFileSync('android-app/app/src/main/java/com/saavn/music/data/repository/MusicRepository.kt', mrContent, 'utf8');
console.log(`Updated MusicRepository.kt (${mrReplaced} URLs replaced)`);

// 3. Update web/src/App.tsx
let webContent = fs.readFileSync('web/src/App.tsx', 'utf8');
let webReplaced = 0;
for (const [oldUrl, newUrl] of Object.entries(urlMap)) {
  if (oldUrl && oldUrl.length > 10 && webContent.includes(oldUrl)) {
    webContent = webContent.replaceAll(oldUrl, newUrl);
    webReplaced++;
  }
}
fs.writeFileSync('web/src/App.tsx', webContent, 'utf8');
console.log(`Updated web/src/App.tsx (${webReplaced} URLs replaced)`);
