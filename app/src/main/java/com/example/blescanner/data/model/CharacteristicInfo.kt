package com.example.blescanner.data.model

import java.util.UUID

data class CharacteristicInfo(
    val uuid: UUID,
    val properties: Int,
    val canRead: Boolean,
    val canNotify: Boolean,
    val canWrite: Boolean
)
