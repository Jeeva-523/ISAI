import { initializeApp } from 'firebase/app'
import { getAnalytics, isSupported } from 'firebase/analytics'
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  sendEmailVerification,
  applyActionCode,
  updateProfile,
  signOut
} from 'firebase/auth'
import { getFirestore, doc, setDoc } from 'firebase/firestore'
import { getDatabase } from 'firebase/database'
import { getStorage } from 'firebase/storage'

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

// Initialize Firebase App & Core Services
export const app = initializeApp(firebaseConfig)
export const auth = getAuth(app)
export const firestore = getFirestore(app)
export const db = firestore
export const rtdb = getDatabase(app)
export const storage = getStorage(app)
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

export function sanitizeEmailKey(email: string): string {
  if (!email) return 'user_jeeva_default'
  return email.toLowerCase().trim().replace(/[.#$\[\]]/g, '_')
}

// Native Firebase Registration
export async function registerWithEmailPassword(email: string, password: string, displayName?: string) {
  const cleanEmail = email.trim().toLowerCase()
  const name = displayName?.trim() || cleanEmail.split('@')[0] || 'ISAI Listener'

  try {
    const cred = await createUserWithEmailAndPassword(auth, cleanEmail, password)
    const user = cred.user

    // Update Firebase Auth display name safely
    try {
      await updateProfile(user, { displayName: name })
    } catch (e) {
      console.warn('[Firebase Auth] updateProfile warning:', e)
    }

    // Immediately send native Firebase verification email
    try {
      await sendEmailVerification(user)
    } catch (e) {
      console.warn('[Firebase Auth] sendEmailVerification warning:', e)
    }

    // Sync profile metadata to Firestore (Non-blocking so registration never hangs)
    setDoc(doc(firestore, "profiles", user.uid), {
      userId: user.uid,
      displayName: name,
      email: cleanEmail,
      createdAt: Date.now()
    }, { merge: true }).catch((err) => {
      console.warn('[Firestore] Non-critical profile sync warning:', err)
    })

    return {
      uid: user.uid,
      name: name,
      email: cleanEmail,
      avatar: name.slice(0, 1).toUpperCase(),
      isLoggedIn: true,
      isPremium: true,
      emailVerified: false
    }
  } catch (err: any) {
    throw new Error(mapFirebaseError(err))
  }
}

// Native Firebase Login
export async function loginWithEmailPassword(email: string, password: string) {
  const cleanEmail = email.trim().toLowerCase()

  try {
    const cred = await signInWithEmailAndPassword(auth, cleanEmail, password)
    const user = cred.user

    // Reload user instance from Firebase to get latest isEmailVerified flag
    await user.reload()

    const name = user.displayName || cleanEmail.split('@')[0] || 'ISAI Listener'

    return {
      uid: user.uid,
      name: name,
      email: cleanEmail,
      avatar: name.slice(0, 1).toUpperCase(),
      isLoggedIn: true,
      isPremium: true,
      emailVerified: user.emailVerified
    }
  } catch (err: any) {
    throw new Error(mapFirebaseError(err))
  }
}

// Verify action code from custom verification link
export async function verifyEmailActionCode(oobCode: string): Promise<boolean> {
  try {
    await applyActionCode(auth, oobCode)
    if (auth.currentUser) {
      await auth.currentUser.reload()
    }
    return true
  } catch (err: any) {
    console.warn('[Firebase Auth] verifyEmailActionCode error:', err)
    throw new Error(mapFirebaseError(err))
  }
}

// Reload current user & check email verification state
export async function checkEmailVerificationStatus(): Promise<boolean> {
  const user = auth.currentUser
  if (!user) return false
  try {
    await user.reload()
    return user.emailVerified
  } catch (err) {
    return user.emailVerified
  }
}

// Resend native Firebase verification email
export async function resendVerificationEmail(): Promise<boolean> {
  const user = auth.currentUser
  if (!user) {
    throw new Error("No active authentication session found. Please sign in again.")
  }
  try {
    await sendEmailVerification(user)
    return true
  } catch (err: any) {
    throw new Error(mapFirebaseError(err))
  }
}

export async function logoutFirebaseUser() {
  await signOut(auth)
}

// Real Firebase Google Sign-In helper
export async function loginWithGoogleFirebase() {
  try {
    const result = await signInWithPopup(auth, googleProvider)
    const u = result.user
    return {
      uid: u.uid,
      name: u.displayName || 'ISAI Listener',
      email: u.email || 'user@isaimusic.com',
      avatar: u.displayName ? u.displayName.slice(0, 1).toUpperCase() : 'J',
      photoURL: u.photoURL || undefined,
      emailVerified: u.emailVerified
    }
  } catch (err) {
    console.warn('[Firebase Auth] Popup blocked or failed:', err)
    return null
  }
}

function mapFirebaseError(err: any): string {
  const code = err?.code || ''
  switch (code) {
    case 'auth/email-already-in-use':
      return 'An account already exists with this email address. Please sign in.'
    case 'auth/invalid-email':
      return 'Invalid email address format. Please check and try again.'
    case 'auth/weak-password':
      return 'Password is too weak. Please enter at least 6 characters.'
    case 'auth/wrong-password':
    case 'auth/invalid-credential':
      return 'Incorrect email or password. Please check and try again.'
    case 'auth/user-not-found':
      return 'No account found for this email address.'
    case 'auth/too-many-requests':
      return 'Too many attempts. Please wait a few minutes and try again.'
    default:
      return err?.message || 'Authentication error occurred. Please try again.'
  }
}

