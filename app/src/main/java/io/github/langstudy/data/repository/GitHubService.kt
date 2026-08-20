package io.github.langstudy.data.repository

import io.github.langstudy.data.model.GitHubRelease
import retrofit2.http.GET
import retrofit2.http.Header

interface GitHubService {
    @GET("repos/griggril000/LanguageStudyApp/releases")
    suspend fun getReleases(
        @Header("Authorization") token: String?
    ): List<GitHubRelease>
}
