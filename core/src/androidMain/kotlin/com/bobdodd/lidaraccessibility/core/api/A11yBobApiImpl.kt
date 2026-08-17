package com.bobdodd.lidaraccessibility.core.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Wrapper for endpoints that return { "results": [...] } */
@Serializable
private data class ResultsResponse(
    @SerialName("results") val results: List<MapSearchResultDto> = emptyList(),
)

/**
 * DTO that mirrors [MapSearchResult] but accepts the address field as a
 * JSON object (the a11ybob API returns { "unit": "...", "city": "..." })
 * rather than a plain string. Mapped to [MapSearchResult] before returning.
 */
@Serializable
private data class MapSearchResultDto(
    val id: String,
    val display: String,
    val category: String? = null,
    val subtype: String? = null,
    val lat: Double,
    val lng: Double,
    val address: JsonElement? = null,
    val access: String? = null,
    val parent: String? = null,
    @SerialName("on_street")
    val onStreet: String? = null,
) {
    fun toMapSearchResult(): MapSearchResult = MapSearchResult(
        id = id,
        display = display,
        category = category,
        subtype = subtype,
        lat = lat,
        lng = lng,
        address = formatAddress(),
        access = access,
        parent = parent,
        onStreet = onStreet,
    )

    private fun formatAddress(): String? {
        val obj = address as? JsonObject ?: return address?.toString()
        val parts = listOfNotNull(
            obj["unit"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
            obj["number"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
            obj["street"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
            obj["city"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
            obj["province"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
            obj["postcode"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() },
        )
        return parts.joinToString(", ").ifEmpty { address?.toString() }
    }
}

/**
 * Ktor-backed implementation of [A11yBobApi].
 *
 * Uses the OkHttp engine (from `ktor-client-okhttp` in `androidMain`
 * dependencies) and ContentNegotiation with kotlinx.serialization.
 * Base URL is a constructor parameter so tests can point at a mock.
 */
class A11yBobApiImpl(
    private val baseUrl: String = "https://a11ybob.com",
) : A11yBobApi {

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@A11yBobApiImpl.json)
        }
    }

    override suspend fun chat(request: ChatRequest): ChatResponse {
        return client.post("$baseUrl/api/knowledge-chat") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ChatResponse>()
    }

    override suspend fun mapSearch(
        query: String,
        accessibilityTag: String?,
        near: LatLon?,
        limit: Int,
    ): List<MapSearchResult> {
        return client.get("$baseUrl/api/map-search") {
            parameter("q", query)
            accessibilityTag?.let { parameter("access", it) }
            near?.let {
                parameter("lat", it.lat)
                parameter("lng", it.lon)
            }
            parameter("limit", limit)
        }.body<ResultsResponse>().results.map { it.toMapSearchResult() }
    }

    override suspend fun mapNearby(
        near: LatLon,
        categoriesOff: List<String>,
        categoriesOn: List<String>,
    ): List<MapSearchResult> {
        return client.get("$baseUrl/api/map-nearby") {
            parameter("lat", near.lat)
            parameter("lng", near.lon)
            if (categoriesOff.isNotEmpty()) parameter("off", categoriesOff.joinToString(","))
            if (categoriesOn.isNotEmpty()) parameter("on", categoriesOn.joinToString(","))
        }.body<ResultsResponse>().results.map { it.toMapSearchResult() }
    }

    override suspend fun placeKnowledgeByPoint(near: LatLon): PlaceKnowledge? {
        return client.get("$baseUrl/api/place-knowledge") {
            parameter("lat", near.lat)
            parameter("lng", near.lon)
        }.body<PlaceKnowledge?>()
    }

    override suspend fun placeKnowledgeByQuery(query: String): PlaceKnowledge? {
        return client.get("$baseUrl/api/place-knowledge") {
            parameter("q", query)
        }.body<PlaceKnowledge?>()
    }

    override suspend fun mintSttToken(): String? {
        return client.post("$baseUrl/api/context-stt-token").body<String?>()
    }
}
