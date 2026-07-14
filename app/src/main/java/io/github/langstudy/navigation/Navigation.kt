package io.github.langstudy.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object Login : NavRoute

    @Serializable
    data object VerifyEmail : NavRoute

    @Serializable
    data object Portfolio : NavRoute

    @Serializable
    data object Vocab : NavRoute

    @Serializable
    data class Flashcards(val category: String? = null, val language: String? = null) : NavRoute

    @Serializable
    data object Skills : NavRoute

    @Serializable
    data class Journal(val openEntry: Boolean = false) : NavRoute

    @Serializable
    data object Admin : NavRoute

    @Serializable
    data object Settings : NavRoute

    companion object {
        val mainRoutes = listOf(Vocab, Skills, Portfolio, Journal())

        fun fromString(route: String): NavRoute = when (route.lowercase()) {
            "vocab", "vocabulary" -> Vocab
            "flashcards", "review" -> Flashcards()
            "skills" -> Skills
            "portfolio" -> Portfolio
            "journal" -> Journal()
            else -> Vocab
        }
    }
}

val NavRoute.icon: ImageVector
    get() = when (this) {
        NavRoute.Login -> Icons.AutoMirrored.Rounded.Login
        NavRoute.VerifyEmail -> Icons.Rounded.Settings // Placeholder icon
        NavRoute.Portfolio -> Icons.Rounded.Work
        NavRoute.Vocab -> Icons.Rounded.Book
        is NavRoute.Flashcards -> Icons.Rounded.Style
        NavRoute.Skills -> Icons.Rounded.Psychology
        is NavRoute.Journal -> Icons.Rounded.Edit
        NavRoute.Admin -> Icons.Rounded.Star
        NavRoute.Settings -> Icons.Rounded.Settings
    }

val NavRoute.label: String
    get() = when (this) {
        NavRoute.Login -> "Login"
        NavRoute.VerifyEmail -> "Verify Email"
        NavRoute.Portfolio -> "Portfolio"
        NavRoute.Vocab -> "Vocab"
        is NavRoute.Flashcards -> "Flashcards"
        NavRoute.Skills -> "Skills"
        is NavRoute.Journal -> "Journal"
        NavRoute.Admin -> "Admin"
        NavRoute.Settings -> "Settings"
    }
