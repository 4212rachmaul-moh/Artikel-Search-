package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScopusSearchResponse(
    @Json(name = "search-results") val searchResults: SearchResults?
)

@JsonClass(generateAdapter = true)
data class SearchResults(
    @Json(name = "opensearch:totalResults") val totalResults: String?,
    @Json(name = "opensearch:startIndex") val startIndex: String?,
    @Json(name = "opensearch:itemsPerPage") val itemsPerPage: String?,
    @Json(name = "entry") val entry: List<ScopusEntry>?
)

@JsonClass(generateAdapter = true)
data class ScopusEntry(
    @Json(name = "dc:title") val title: String?,
    @Json(name = "dc:creator") val creator: String?,
    @Json(name = "prism:publicationName") val publicationName: String?,
    @Json(name = "prism:coverDate") val coverDate: String?,
    @Json(name = "prism:doi") val doi: String?,
    @Json(name = "prism:volume") val volume: String?,
    @Json(name = "prism:issueIdentifier") val issue: String?,
    @Json(name = "prism:pageRange") val pageRange: String?,
    @Json(name = "dc:identifier") val identifier: String?,
    @Json(name = "eid") val eid: String?,
    @Json(name = "link") val link: List<ScopusLink>?
) {
    val scopusUrl: String?
        get() = link?.firstOrNull { it.rel == "scopus" }?.href ?: link?.firstOrNull { it.rel == "self" }?.href

    val cleanDoi: String?
        get() = doi ?: identifier?.substringAfter("DOI:", "")?.ifEmpty { null }
}

@JsonClass(generateAdapter = true)
data class ScopusLink(
    @Json(name = "@href") val href: String?,
    @Json(name = "@rel") val rel: String?
)
