package com.magazines.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RejectRequest(
    val reason: String? = null,
)
