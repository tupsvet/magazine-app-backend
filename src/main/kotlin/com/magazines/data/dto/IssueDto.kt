package com.magazines.data.dto

import java.time.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private object IsoInstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}

@Serializable
data class IssueDto(
    val id: String,
    val magazineId: String,
    val issueNumber: String,
    val publicationDate: String?,
    val pdfUrl: String?,
    val pagesCount: Int?,
    @Serializable(with = IsoInstantSerializer::class)
    val createdAt: Instant,
)

@Serializable
data class IssueCreateRequest(
    val issueNumber: String,
    val publicationDate: String? = null,
    val pagesCount: Int? = null,
)
