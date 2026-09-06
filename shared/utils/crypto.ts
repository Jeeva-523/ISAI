// @ts-ignore
import CryptoJS from 'crypto-js'

const DES_KEY = '38346591'

/**
 * Decrypts JioSaavn encrypted_media_url into direct 320kbps AAC/MP3 stream URL
 */
export function decryptMediaUrl(encryptedUrl?: string | null): string | null {
  if (!encryptedUrl || typeof encryptedUrl !== 'string') return null
  try {
    const keyHex = CryptoJS.enc.Utf8.parse(DES_KEY)
    const decrypted = CryptoJS.DES.decrypt(
      { ciphertext: CryptoJS.enc.Base64.parse(encryptedUrl) },
      keyHex,
      { mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7 }
    )
    const decryptedStr = decrypted.toString(CryptoJS.enc.Utf8).trim()
    if (!decryptedStr) return null
    return decryptedStr.replace(/_(?:96|160|320|48|12)/, '_320')
  } catch (e) {
    return null
  }
}
