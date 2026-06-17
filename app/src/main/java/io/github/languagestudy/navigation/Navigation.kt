package io.github.languagestudy.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object Login : NavRoute
    @Serializable
    data object Portfolio : NavRoute
    @Serializable
    data object Vocab : NavRoute
    @Serializable
    data object Skills : NavRoute
    @Serializable
    data object Journal : NavRoute
    @Serializable
    data object Admin : NavRoute
    @Serializable
    data object Settings : NavRoute

    companion object {
        val mainRoutes = listOf(Vocab, Skills, Portfolio, Journal)

        fun fromString(route: String): NavRoute = when (route.lowercase()) {
            "vocab" -> Vocab
            "skills" -> Skills
            "portfolio" -> Portfolio
            "journal" -> Journal
            else -> Vocab
        }
    }
}

val NavRoute.icon: ImageVector
    get() = when (this) {
        NavRoute.Login -> Icons.AutoMirrored.Rounded.Login
        NavRoute.Portfolio -> Icons.Rounded.Work
        NavRoute.Vocab -> Icons.Rounded.Book
        NavRoute.Skills -> Icons.Rounded.Psychology
        NavRoute.Journal -> Icons.Rounded.Edit
        NavRoute.Admin -> Icons.Rounded.Star
        NavRoute.Settings -> Icons.Rounded.Settings
    }

val NavRoute.label: String
    get() = when (this) {
        NavRoute.Login -> "Login"
        NavRoute.Portfolio -> "Portfolio"
        NavRoute.Vocab -> "Vocab"
        NavRoute.Skills -> "Skills"
        NavRoute.Journal -> "Journal"
        NavRoute.Admin -> "Admin"
        NavRoute.Settings -> "Settings"
    }
