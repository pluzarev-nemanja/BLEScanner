package com.example.blescanner.presentation.scanner.util

fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

fun ByteArray.toUtf8OrNull(): String? =
    try {
        String(this, Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }