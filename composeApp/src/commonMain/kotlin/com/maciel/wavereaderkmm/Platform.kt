package com.maciel.wavereaderkmm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect object TimeUtil {
    fun systemTimeMs(): Long
}