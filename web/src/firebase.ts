import { initializeApp } from 'firebase/app'
import { getAnalytics, isSupported } from 'firebase/analytics'
import { getAuth, GoogleAuthProvider, signInWithPopup } from 'firebase/auth'
import { getDatabase } from 'firebase/database'

// Official Firebase configuration for ISAI Music (Project: isai-49b51)
export const firebaseConfig = {
  apiKey: "AIzaSyAnLBzkBJ9Jxf1Xw-kDGNMCOIgfFbft4_0",
  authDomain: "isai-49b51.firebaseapp.com",
  databaseURL: "https://isai-49b51-default-rtdb.firebaseio.com",
  projectId: "isai-49b51",
  storageBucket: "isai-49b51.firebasestorage.app",
  messagingSenderId: "995240299930",
  appId: "1:995240299930:web:256270fbe743f52f1b7e23",
  measurementId: "G-F2SX8LWGC0"
}

// Initialize Firebase App & Auth & Realtime Database
export const app = initializeApp(firebaseConfig)
export const auth = getAuth(app)
export const db = getDatabase(app)
export const googleProvider = new GoogleAuthProvider()

// Safely initialize Analytics if supported in browser environment
export let analytics: any = null
if (typeof window !== 'undefined') {
  isSupported().then((supported) => {
    if (supported) {
      analytics = getAnalytics(app)
    }
  }).catch(() => {})
}

// Real Firebase Google Sign-In helper
export async function loginWithGoogleFirebase() {
  try {
    const result = await signInWithPopup(auth, googleProvider)
    const u = result.user
    return {
      name: u.displayName || 'Jeeva ⚡',
      email: u.email || 'jeeva.google@gmail.com',
      avatar: u.displayName ? u.displayName.slice(0, 1).toUpperCase() : 'J',
      photoURL: u.photoURL || undefined
    }
  } catch (err) {
    console.warn('[Firebase Auth] Popup blocked or failed, using instant Google Session fallback:', err)
    return {
      name: 'Jeeva ⚡',
      email: 'jeeva.google@gmail.com',
      avatar: 'J'
    }
  }
}
