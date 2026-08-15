package com.bobdodd.lidaraccessibility.core.api

import com.bobdodd.lidaraccessibility.core.memory.MemoryItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed client for the a11ybob Next.js backend.
 *
 * Endpoints match the reading-pass API shapes; see the project wiki
 * `concepts/mobile-stack` for the notes. Base URL is a constructor
 * parameter so tests can point at a mock server.
 *
 * See ADR 0001 (backend reuse) and ADR 0002 (Knowledge Map port
 * strategy).
 */
interface A11yBobApi {
    suspend fun chat(request: ChatRequest): ChatResponse

    suspend fun mapSearch(
        query: String,
        accessibilityTag: String? = null,
        near: LatLon? = null,
        limit: Int = 20,
    ): List<MapSearchResult>

    suspend fun mapNearby(
        near: LatLon,
        categoriesOff: List<String> = emptyList(),
        categoriesOn: List<String> = emptyList(),
    ): List<MapSearchResult>

    suspend fun placeKnowledgeByPoint(near: LatLon): PlaceKnowledge?
    suspend fun placeKnowledgeByQuery(query: String): PlaceKnowledge?

    /** Reserved for a v2 cloud-STT opt-in. Not used in v1 (ADR 0003). */
    suspend fun mintSttToken(): String?
}

@Serializable
data class LatLon(val lat: Double, val lon: Double)

@Serializable
data class LocationHint(
    val lat: Double,
    val lon: Double,
    val heading: Double? = null,
)

@Serializable
data class ChatRequest(
    val message: String,
    val location: LocationHint? = null,
    val history: List<ChatTurn> = emptyList(),
    val memory: List<MemoryItem> = emptyList(),
    @SerialName("can_show_map")
    val canShowMap: Boolean = false,
    val modality: String = "voice",
)

@Serializable
data class ChatTurn(val role: String, val content: String)

@Serializable
data class ChatResponse(
    val reply: String,
    val memory: List<MemoryItem>? = null,
    @SerialName("map_action")
    val mapAction: MapAction? = null,
    val error: String? = null,
)

@Serializable
data class MapAction(
    val kind: String,
    val payload: Map<String, String> = emptyMap(),
)

@Serializable
data class MapSearchResult(
    val id: String,
    val display: String,
    val category: String? = null,
    val subtype: String? = null,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val access: String? = null,
    val parent: String? = null,
    @SerialName("on_street")
    val onStreet: String? = null,
)

@Serializable
data class PlaceKnowledge(
    val title: String,
    val summary: String,
    val source: String,
)
