package com.example.blescanner.presentation.scanner.util

fun String.toByteArrayFromInput(): ByteArray = if (this.matches(Regex("([0-9a-fA-F]{2}\\s*)+"))) {
    this.trim()
        .split("\\s+".toRegex())
        .map { it.toInt(16).toByte() }
        .toByteArray()
} else this.toByteArray(Charsets.UTF_8)