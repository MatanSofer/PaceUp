package com.example.paceup

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform