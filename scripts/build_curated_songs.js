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

const songQueries = [
  { q: 'Arabic Kuthu Beast Tamil', title: 'Arabic Kuthu - Halamithi Habibo | Beast', vid: 'KUN5Uf9mObQ' },
  { q: 'Hukum Thalaivar Alappara Jailer Tamil', title: 'Hukum - Thalaivar Alappara | Jailer', vid: '1F3hm6MfR1k' },
  { q: 'Naa Ready Leo Tamil', title: 'Naa Ready | Leo | Thalapathy Vijay', vid: 'szvt1vD0Uug' },
  { q: 'Marakkuma Nenjam Vendhu Thanindhathu Kaadu Tamil', title: 'Marakkuma Nenjam | VTK | A.R. Rahman', vid: '3tmd-ClpJxA' },
  { q: 'Kaavaalaa Jailer Tamil', title: 'Kaavaalaa - Jailer | Rajinikanth | Tamannaah', vid: 'mqqft2x_Aa4' },
  { q: 'Vaseegara Minnale Tamil', title: 'Vaseegara - Minnale | Bombay Jayashri', vid: 'eN6AnYGYdVE' },
  { q: 'Rowdy Baby Maari 2 Tamil', title: 'Rowdy Baby - Maari 2 | Dhanush | Sai Pallavi', vid: 'jHNNMj5bNQw' },
  { q: 'Why This Kolaveri Di 3 Tamil', title: 'Why This Kolaveri Di - 3 | Dhanush', vid: 'x6Q7c9Ry3tk' },
  { q: 'Chilla Chilla Thunivu Tamil', title: 'Chilla Chilla - Thunivu | Ajith Kumar', vid: 'Y6wB5b89E1E' },
  { q: 'Vaathi Coming Master Tamil', title: 'Vaathi Coming - Master | Thalapathy Vijay', vid: 'vx2u5uUu3DE' },
  { q: 'Chellamma Doctor Tamil', title: 'Chellamma - Doctor | Sivakarthikeyan | Anirudh', vid: 'vB8csw_kkyU' },
  { q: 'Dippam Dappam KRK Tamil', title: 'Dippam Dappam - KRK | Vijay Sethupathi', vid: 'mSgN49e_Jyo' },
  { q: 'Badass Leo Tamil', title: 'Badass - Leo | Thalapathy Vijay | Anirudh', vid: 'I4_mN6-rC0E' },
  { q: 'Ranjithame Varisu Tamil', title: 'Ranjithame - Varisu | Thalapathy Vijay', vid: 'sUzG3h5V6bE' },
  { q: 'Thee Thalapathy Varisu Tamil', title: 'Thee Thalapathy - Varisu | Silambarasan TR', vid: 'uM5d992fL9g' },
  { q: 'Jimikki Ponnu Varisu Tamil', title: 'Jimikki Ponnu - Varisu | Anirudh', vid: '8uN1kR4-31w' },
  { q: 'Aalaporaan Thamizhan Mersal Tamil', title: 'Aalaporaan Thamizhan - Mersal | Vijay | ARR', vid: '3XmrZaWVUpE' },
  { q: 'Singappenney Bigil Tamil', title: 'Singappenney - Bigil | Vijay | Nayanthara', vid: 'dJvj2c8d20A' },
  { q: 'Verithanam Bigil Tamil', title: 'Verithanam - Bigil | Thalapathy Vijay | ARR', vid: '8kL3yJ_aH4s' },
  { q: 'Kadhaippoma Oh My Kadavule Tamil', title: 'Kadhaippoma - Oh My Kadavule | Sid Sriram', vid: 'cR77gU2g16s' },
  { q: 'En Rojaa Neeye Kushi Tamil', title: 'En Roja Neeye - Kushi | Vijay Deverakonda', vid: 'Gg4x336nN2E' },
  { q: 'Pookkal Pookkum Madrasapattinam Tamil', title: 'Pookkal Pookkum - Madrasapattinam | GV Prakash', vid: 'w3v1Q3-2qgY' },
  { q: 'Adiye Bachelor Tamil', title: 'Adiye - Bachelor | Kapil Kapilan | Dhibu Ninan Thomas', vid: 'xVj_kL3o9yE' },
  { q: 'Kannaana Kanney Viswasam Tamil', title: 'Kannaana Kanney - Viswasam | Ajith Kumar | Sid Sriram', vid: 'oRdxUFDoQe0' },
  { q: 'Munbe Vaa Sillunu Oru Kaadhal Tamil', title: 'Munbe Vaa - Sillunu Oru Kaadhal | Shreya Ghoshal | ARR', vid: 'P36eX-189fU' },
  { q: 'Hosanna Vinnaithaandi Varuvaayaa Tamil', title: 'Hosanna - Vinnaithaandi Varuvaayaa | Vijay Prakash', vid: '3r88BwZ_g60' },
  { q: 'Anbil Avan Vinnaithaandi Varuvaayaa Tamil', title: 'Anbil Avan - Vinnaithaandi Varuvaayaa | Devan', vid: 'kLp65mG9w1Y' },
  { q: 'Kanave Kanave David Tamil', title: 'Kanave Kanave - David | Anirudh Ravichander', vid: '4Gq8o0y123E' },
  { q: 'Sirikkadhey Remo Tamil', title: 'Sirikkadhey - Remo | Anirudh Ravichander', vid: '9L0sW81a33w' },
  { q: 'Neeyum Naanum Anbe Imaikkaa Nodigal Tamil', title: 'Neeyum Naanum Anbe - Imaikkaa Nodigal | Hiphop Tamizha', vid: 'e3X9827fghY' },
  { q: 'Annul Maelae Vaaranam Aayiram Tamil', title: 'Annul Maelae - Vaaranam Aayiram | Harris Jayaraj', vid: 'P6q9oK3Lw77' },
  { q: 'Mental Manadhil OK Kanmani Tamil', title: 'Mental Manadhil - OK Kanmani | Mani Ratnam | ARR', vid: '5w21q9Y68wY' },
  { q: 'Katchi Sera Sai Abhyankkar Tamil', title: 'Katchi Sera - Think Indie | Sai Abhyankkar', vid: 'VT7412hT91g' },
  { q: 'Kutti Story Master Tamil', title: 'Kutti Story | Master | Thalapathy Vijay', vid: 'gvyUuxdRdR4' },
  { q: 'Matta The Greatest Of All Time Tamil', title: 'Matta - GOAT | Thalapathy Vijay | Yuvan', vid: 'bo_efYhYU2A' }
];

async function run() {
  const songs = [];
  console.log(`Starting to resolve ${songQueries.length} curated songs...`);

  for (let i = 0; i < songQueries.length; i++) {
    const item = songQueries[i];
    const encoded = encodeURIComponent(item.q);
    const url = `https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=web6dot0&q=${encoded}&n=3`;
    
    try {
      const res = await fetch(url, { headers: { 'User-Agent': 'Mozilla/5.0' } });
      const data = await res.json();
      const results = data?.results || [];
      const tamilResults = results.filter(r => (r.language || '').toLowerCase() === 'tamil');
      const first = tamilResults[0] || results[0];
      if (!first || !first.more_info?.encrypted_media_url) {
        console.warn(`[WARN] Not found: ${item.q}`);
        continue;
      }
      const rawDecrypted = desDecrypt(first.more_info.encrypted_media_url);
      const audioUrl320 = rawDecrypted.replace(/_(?:96|160|320|48|12)/, '_320');

      // Verify HEAD
      let isLive = false;
      try {
        const head = await fetch(audioUrl320, { method: 'HEAD', headers: { 'User-Agent': 'Mozilla/5.0' } });
        isLive = (head.status === 200);
      } catch (e) {
        isLive = false;
      }

      if (!isLive) {
        console.warn(`[FAIL 404] ${item.title} -> ${audioUrl320}`);
        continue;
      }

      const durSec = parseInt(first.more_info?.duration || first.duration || '225', 10);
      const m = Math.floor(durSec / 60);
      const s = (durSec % 60).toString().padStart(2, '0');
      const durFormatted = `${m}:${s}`;
      const durMs = durSec * 1000;

      let img = (first.image || '').replace('150x150', '500x500').replace('50x50', '500x500');
      if (!img.startsWith('http')) {
        img = `https://img.youtube.com/vi/${item.vid}/hqdefault.jpg`;
      }

      const artist = (first.more_info?.artistMap?.primary_artists?.[0]?.name || first.more_info?.singers || 'Anirudh Ravichander').replace(/&amp;/g, '&');
      const album = (first.more_info?.album || '').replace(/&amp;/g, '&');
      const channelTitle = album ? `${artist} • ${album}` : artist;
      const cleanTitle = (first.title || item.title).replace(/&amp;/g, '&').replace(/&quot;/g, '"').replace(/&#039;/g, "'");

      const playCount = parseInt(first.play_count || '1000000', 10);
      const viewsFmt = playCount >= 100000000 ? `${Math.round(playCount / 10000000)}0M views` : playCount >= 1000000 ? `${Math.round(playCount / 1000000)}M views` : '10M views';

      songs.push({
        videoId: item.vid,
        title: cleanTitle,
        channelTitle: channelTitle,
        thumbnailUrl: img,
        durationFormatted: durFormatted,
        durationMs: durMs,
        viewCountFormatted: viewsFmt,
        audioUrl: audioUrl320,
        playCount: playCount,
        language: 'tamil'
      });

      console.log(`[OK ${i+1}/${songQueries.length}] ${cleanTitle} => 200 OK (${audioUrl320.slice(-25)})`);
    } catch (e) {
      console.error(`Error fetching ${item.q}:`, e.message);
    }
  }

  console.log(`Successfully verified ${songs.length} live curated songs!`);
  fs.writeFileSync('scripts/verified_curated_songs.json', JSON.stringify(songs, null, 2));
}

run().catch(console.error);
