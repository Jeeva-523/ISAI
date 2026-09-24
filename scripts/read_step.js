import fs from 'fs';
import readline from 'readline';

const rl = readline.createInterface({
  input: fs.createReadStream('C:/Users/Nithramatrimony40/.gemini/antigravity-ide/brain/69b9f9a2-2492-4d1c-827f-137aa6503d00/.system_generated/logs/transcript_full.jsonl'),
  crlfDelay: Infinity
});

rl.on('line', (line) => {
  if (line.includes('"step_index":4943') || line.includes('"step_index":4944')) {
    try {
      const obj = JSON.parse(line);
      console.log('--- Step', obj.step_index, '---');
      if (obj.tool_calls) {
        console.log('Tool call args:', JSON.stringify(obj.tool_calls[0].args));
      }
      if (obj.content) {
        fs.writeFileSync('scripts/step_4944_content.txt', obj.content);
        console.log('Content written to scripts/step_4944_content.txt, length:', obj.content.length);
      }
    } catch(e) {}
  }
});
