import { auth, firestore, googleProvider } from '../firebase'
import {
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  sendPasswordResetEmail,
  signInWithPopup,
  onAuthStateChanged,
  User
} from 'firebase/auth'
import { doc, setDoc, getDoc, serverTimestamp } from 'firebase/firestore'

export interface UserProfile {
  userId: string
  username: string
  displayName: string
  email: string
  avatarUrl?: string
  language: string
  createdAt?: any
  updatedAt?: any
}

class AuthService {
  private currentUser: User | null = null

  constructor() {
    onAuthStateChanged(auth, (user) => {
      this.currentUser = user
      if (user) {
        this.syncUserProfile(user)
      }
    })
  }

  getCurrentUser(): User | null {
    return this.currentUser || auth.currentUser
  }

  getCurrentUserId(): string {
    return this.getCurrentUser()?.uid || 'user_jeeva_123'
  }

  async login(email: string, pass: string) {
    const cred = await signInWithEmailAndPassword(auth, email, pass)
    await this.syncUserProfile(cred.user)
    return cred.user
  }

  async register(email: string, pass: string, displayName: string) {
    const cred = await createUserWithEmailAndPassword(auth, email, pass)
    await this.syncUserProfile(cred.user, displayName)
    return cred.user
  }

  async loginWithGoogle() {
    const cred = await signInWithPopup(auth, googleProvider)
    await this.syncUserProfile(cred.user)
    return cred.user
  }

  async logout() {
    await signOut(auth)
  }

  async resetPassword(email: string) {
    await sendPasswordResetEmail(auth, email)
  }

  async syncUserProfile(user: User, customDisplayName?: string) {
    try {
      const userRef = doc(firestore, 'profiles', user.uid)
      const snap = await getDoc(userRef)
      const name = customDisplayName || user.displayName || 'Jeeva ⚡'
      
      if (!snap.exists()) {
        await setDoc(userRef, {
          userId: user.uid,
          username: user.email?.split('@')[0] || 'jeeva',
          displayName: name,
          email: user.email || '',
          avatarUrl: user.photoURL || '',
          language: 'Tamil',
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        })
      } else {
        await setDoc(userRef, {
          displayName: name,
          email: user.email || '',
          avatarUrl: user.photoURL || snap.data()?.avatarUrl || '',
          updatedAt: serverTimestamp()
        }, { merge: true })
      }
    } catch (err) {
      console.warn('[AuthService] Profile sync fallback:', err)
    }
  }

  async getUserProfile(userId: string): Promise<UserProfile | null> {
    try {
      const userRef = doc(firestore, 'profiles', userId)
      const snap = await getDoc(userRef)
      if (snap.exists()) {
        return snap.data() as UserProfile
      }
    } catch {}
    return null
  }
}

export const authService = new AuthService()
