package com.bobdodd.lidaraccessibility.core.memory

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple in-memory implementation of [MemoryStore].
 *
 * This is a stand-in until the Room-backed implementation lands in
 * `androidMain`. It lives in `commonMain` so it works on both
 * platforms and in unit tests. The chat controller interacts only
 * with the [MemoryStore] interface, so swapping this for Room later
 * requires no changes to the controller.
 */
class InMemoryMemoryStore : MemoryStore {

    private val _items = MutableStateFlow<List<MemoryItem>>(emptyList())
    override val items = _items.asStateFlow()

    override suspend fun snapshot(): List<MemoryItem> = _items.value

    override suspend fun replace(items: List<MemoryItem>) {
        _items.value = items
    }

    override suspend fun clear() {
        _items.value = emptyList()
    }
}
