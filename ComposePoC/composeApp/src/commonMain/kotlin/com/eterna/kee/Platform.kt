package com.eterna.kee

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform