package com.torusage.model

/**
 * A response by the Onionoo API service of the Torproject
 * @see [Onionoo API Documentation](https://metrics.torproject.org/onionoo.html)
 */
data class OnionooResponse<R, B>(
    val version: String,
    val build_revision: String?,
    val relays_published: String,
    val relays: List<R>,
    val bridges_published: String,
    val bridges: List<B>
)
