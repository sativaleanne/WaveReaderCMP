package com.maciel.wavereaderkmm

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual object TimeUtil {
    actual fun systemTimeMs(): Long = System.currentTimeMillis()
}