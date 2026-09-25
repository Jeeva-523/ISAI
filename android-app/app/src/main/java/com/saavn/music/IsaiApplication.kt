package com.saavn.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class IsaiApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        try {
            com.google.android.gms.security.ProviderInstaller.installIfNeededAsync(this, object : com.google.android.gms.security.ProviderInstaller.ProviderInstallListener {
                override fun onProviderInstalled() {
                    android.util.Log.i("ISAI_APP", "Security Provider installed successfully.")
                }
                override fun onProviderInstallFailed(errorCode: Int, recoveryIntent: android.content.Intent?) {
                    android.util.Log.w("ISAI_APP", "Security Provider install failed: $errorCode")
                }
            })
        } catch (e: Throwable) {
            android.util.Log.w("ISAI_APP", "ProviderInstaller error: ${e.message}")
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
