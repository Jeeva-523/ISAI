import fs from 'fs';
import readline from 'readline';

const rl = readline.createInterface({
  input: fs.createReadStream('C:/Users/Nithramatrimony40/.gemini/antigravity-ide/brain/69b9f9a2-2492-4d1c-827f-137aa6503d00/.system_generated/logs/transcript_full.jsonl'),
  crlfDelay: Infinity
});

rl.on('line', (line) => {
  if (line.includes('YouTubeMusicRepository.kt')) {
    try {
      const obj = JSON.parse(line);
      const call = obj.tool_calls?.[0];
      if (call) {
        console.log(`Step ${obj.step_index} tool: ${call.name} action: ${call.args?.toolAction || call.args?.CommandLine || ''}`);
      }
    } catch(e) {}
  }
});
