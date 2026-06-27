package io.github.languagestudy.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    @Json(name = "tag_name") val tagName: String,
    @Json(name = "name") val name: String?,
    @Json(name = "body") val body: String?,
    @Json(name = "published_at") val publishedAt: String?
)
