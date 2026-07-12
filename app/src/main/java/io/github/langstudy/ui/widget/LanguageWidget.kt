package io.github.langstudy.ui.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import io.github.langstudy.LanguageStudyApplication

class LanguageWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as LanguageStudyApplication
        val vocabCountFlow = app.vocabRepository.vocabCount
        val skillCountFlow = app.skillRepository.skillCount

        provideContent {
            val vocabCount by vocabCountFlow.collectAsState(initial = 0)
            val skillCount by skillCountFlow.collectAsState(initial = 0)

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "Language Study",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.padding(vertical = 4.dp))
                    Row {
                        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                            Text(
                                text = vocabCount.toString(),
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Vocab",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                        Spacer(GlanceModifier.width(24.dp))
                        Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                            Text(
                                text = skillCount.toString(),
                                style = TextStyle(
                                    color = GlanceTheme.colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Skills",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        }
    }
}
