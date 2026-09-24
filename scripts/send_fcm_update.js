import fs from 'fs';
import os from 'os';
import path from 'path';

/**
 * Sends a high-priority FCM Push Notification to all installed ISAI devices
 * targeting the 'updates' and 'all' broadcast topics.
 * Devices will receive this notification in their status bar even if the app is closed.
 */
async function sendUpdateNotification() {
  console.log('📡 Fetching latest app update config from Firebase RTDB...');
  
  let versionConfig;
  try {
    const res = await fetch('https://isai-49b51-default-rtdb.firebaseio.com/app_config/version.json');
    versionConfig = await res.json();
    console.log(`✅ Current Live Version: v${versionConfig.latestVersionName} (Code: ${versionConfig.latestVersionCode})`);
  } catch (e) {
    console.warn('⚠️ Could not fetch RTDB version, using defaults:', e.message);
    versionConfig = {
      latestVersionName: '1.3.5',
      latestVersionCode: 9,
      updateTitle: 'New Update Available! 🚀',
      updateMessage: 'A new version of ISAI is ready. Tap to update directly!',
      downloadUrl: 'https://isaihub.web.app/update'
    };
  }

  // Locate firebase-tools token
  const homeDir = os.homedir();
  const configPath = path.join(homeDir, '.config', 'configstore', 'firebase-tools.json');
  
  if (!fs.existsSync(configPath)) {
    throw new Error(`Firebase credentials not found at ${configPath}. Run 'npx firebase login'.`);
  }

  const fbConfig = JSON.parse(fs.readFileSync(configPath, 'utf8'));
  let accessToken = fbConfig.tokens?.access_token;
  const refreshToken = fbConfig.tokens?.refresh_token;

  // Refresh token if available
  if (refreshToken) {
    try {
      const tokenRes = await fetch('https://oauth2.googleapis.com/token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({
          client_id: '563584335869-fgrhgmd47bqnekij5i8b5pr03ho85qd6.apps.googleusercontent.com',
          grant_type: 'refresh_token',
          refresh_token: refreshToken
        })
      });
      const tokenData = await tokenRes.json();
      if (tokenData.access_token) {
        accessToken = tokenData.access_token;
      }
    } catch (e) {
      console.warn('⚠️ Using stored access_token:', e.message);
    }
  }

  const title = versionConfig.updateTitle || `New Update Available! 🚀`;
  const body = `ISAI v${versionConfig.latestVersionName} is ready. Tap to update directly!`;
  const downloadUrl = versionConfig.downloadUrl || 'https://isaihub.web.app/update';

  const payload = {
    message: {
      condition: "'updates' in topics || 'all' in topics",
      notification: {
        title,
        body
      },
      data: {
        title,
        body,
        downloadUrl,
        versionCode: String(versionConfig.latestVersionCode || '9'),
        versionName: String(versionConfig.latestVersionName || '1.3.5')
      },
      android: {
        priority: 'HIGH',
        notification: {
          channel_id: 'isai_fcm_channel',
          sound: 'default',
          default_sound: true,
          default_vibrate_timings: true,
          click_action: 'OPEN_UPDATE'
        }
      }
    }
  };

  console.log('🚀 Sending FCM broadcast to topics: updates & all...');
  const res = await fetch('https://fcm.googleapis.com/v1/projects/isai-49b51/messages:send', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  });

  const resJson = await res.json();
  if (resJson.name) {
    console.log(`🎉 SUCCESS! FCM Notification broadcasted to all users!`);
    console.log(`Message ID: ${resJson.name}`);
  } else {
    console.error('❌ FCM Error:', resJson);
  }
}

sendUpdateNotification().catch(console.error);
