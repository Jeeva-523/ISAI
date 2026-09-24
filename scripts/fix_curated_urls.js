import fs from 'fs';
import forge from 'node-forge';

function desDecrypt(enc) {
  if (!enc) return null;
  const key = forge.util.createBuffer('38346591', 'utf8');
  const cipher = forge.cipher.createDecipher('DES-ECB', key);
  cipher.start();
  cipher.update(forge.util.createBuffer(forge.util.decode64(enc)));
  cipher.finish();
  return cipher.output.toString('utf8');
}

async function resolveAll() {
  const content = fs.readFileSync('android-app/app/src/main/java/com/saavn/music/data/repository/YouTubeMusicRepository.kt', 'utf8');
  const songBlocks = content.split('YouTubeSong(').slice(1);
  console.log('Total song blocks:', songBlocks.length);

  const urlMap = {};

  for (let i = 0; i < songBlocks.length; i++) {
    const block = songBlocks[i];
    const titleMatch = block.match(/title\s*=\s*"([^"]+)"/);
    const artistMatch = block.match(/channelTitle\s*=\s*"([^"]+)"/);
    const idMatch = block.match(/videoId\s*=\s*"([^"]+)"/);
    const urlMatch = block.match(/audioUrl\s*=\s*"([^"]+)"/);

    const title = titleMatch ? titleMatch[1] : '';
    const artist = artistMatch ? artistMatch[1] : '';
    const vid = idMatch ? idMatch[1] : '';
    const oldUrl = urlMatch ? urlMatch[1] : '';

    let status = 0;
    try {
      const h = await fetch(oldUrl, { method: 'HEAD', headers: { 'User-Agent': 'Mozilla/5.0' } });
      status = h.status;
    } catch (e) {
      status = 500;
    }

    if (status !== 200) {
      console.log(`[DEAD] #${i}: '${title}' (${status})`);
      const cleanTitle = title.split('-')[0].split('|')[0].trim();
      const q = encodeURIComponent(cleanTitle + ' tamil');
      try {
        const sRes = await fetch(`https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&q=${q}&n=5`, {
          headers: { 'User-Agent': 'Mozilla/5.0' }
        });
        const sJson = await sRes.json();
        const results = sJson.results || [];
        let freshUrl = null;
        let matchedItem = null;
        for (const item of results) {
          if (item?.more_info?.encrypted_media_url) {
            const dec = desDecrypt(item.more_info.encrypted_media_url);
            const candUrl = dec.replace(/_(?:96|160|320|48|12)/, '_320');
            const check = await fetch(candUrl, { method: 'HEAD', headers: { 'User-Agent': 'Mozilla/5.0' } });
            if (check.status === 200) {
              freshUrl = candUrl;
              matchedItem = item;
              break;
            }
          }
        }
        if (freshUrl) {
          console.log(`  -> RESOLVED: '${matchedItem.title}' => ${freshUrl}`);
          urlMap[oldUrl] = freshUrl;
        } else {
          console.log(`  -> FAILED to resolve for: '${title}'`);
        }
      } catch (e) {
        console.error('  -> Search error:', e.message);
      }
    } else {
      console.log(`[OK] #${i}: '${title}'`);
    }
  }

  fs.writeFileSync('scripts/resolved_urls.json', JSON.stringify(urlMap, null, 2));
  console.log(`Finished resolving! Mapped ${Object.keys(urlMap).length} URLs.`);
}

resolveAll().catch(console.error);
