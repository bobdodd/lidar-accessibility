package com.bobdodd.lidaraccessibility.core.memory

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Per-device, per-install personal-memory store.
 *
 * The chat controller calls [snapshot] before every request to
 * `POST /api/knowledge-chat` and [replace] on every response, matching
 * the web version's "memory travels in/out of the payload" pattern.
 *
 * See ADR 0004 (docs/decisions/0004-personal-memory-scope.md).
 */
interface MemoryStore {
    /** Observable list of memory items, freshest first. */
    val items: Flow<List<MemoryItem>>

    /** One-shot snapshot suitable for injecting into a chat request. */
    suspend fun snapshot(): List<MemoryItem>

    /** Apply the memory list returned by the server. */
    suspend fun replace(items: List<MemoryItem>)

    /** Wipe local memory (user-initiated "forget everything"). */
    suspend fun clear()
}

@Serializable
data class MemoryItem(
    val id: String,
    val name: String,
    val kind: MemoryKind,
    val lat: Double? = null,
    val lon: Double? = null,
    val notes: String? = null,
    @SerialName("created_at_ms")
    val createdAtMs: Long,
    @SerialName("updated_at_ms")
    val updatedAtMs: Long,
)

@Serializable
enum class MemoryKind {
    @SerialName("place") PLACE,
    @SerialName("note") NOTE,
    @SerialName("person") PERSON,
    @SerialName("other") OTHER,
}
