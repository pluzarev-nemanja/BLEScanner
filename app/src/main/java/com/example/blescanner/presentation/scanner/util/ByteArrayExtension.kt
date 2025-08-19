package com.example.blescanner.presentation.scanner.util

fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }