package com.jacob.kmp.core

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform