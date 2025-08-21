package com.example.blescanner.data.model

import java.util.UUID

data class ServiceInfo(
    val uuid: UUID,
    val characteristics: List<UUID>
)