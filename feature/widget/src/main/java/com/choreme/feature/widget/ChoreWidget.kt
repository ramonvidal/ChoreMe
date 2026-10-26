package com.choreme.feature.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.data.GroceryRepository
import com.choreme.core.model.grocery.groceriesToBuy
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.session.SessionState
import com.choreme.core.ui.DeepLinks
import com.choreme.core.ui.icon
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/** Glance widgets are created by Android, not Hilt, so they ask for their dependencies here. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun availability(): FirebaseAvailability
    fun families(): FamilyRepository
    fun chores(): ChoreRepository
    fun groceries(): GroceryRepository
}

internal data class WidgetData(val inFamily: Boolean, val toDo: List<Chore>, val toBuy: Int)

class ChoreWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChoreWidget()
}

internal class ChoreWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent { GlanceTheme { Content(context, data) } }
    }

    private suspend fun load(context: Context): WidgetData {
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        if (!deps.availability().isConfigured) return WidgetData(false, emptyList(), 0)
        val session = withTimeoutOrNull(TIMEOUT_MS) { deps.families().session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return WidgetData(false, emptyList(), 0)
        val chores = withTimeoutOrNull(TIMEOUT_MS) { deps.chores().observeChores(session.familyId).first() }.orEmpty()
        val items = withTimeoutOrNull(TIMEOUT_MS) { deps.groceries().observeItems(session.familyId).first() }.orEmpty()
        return WidgetData(true, buildTodayBoard(chores, LocalDate.now()).toDo, items.groceriesToBuy())
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}

@Composable
private fun Content(context: Context, data: WidgetData) {
    val openApp = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()
    val openGroceries = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.GROCERIES))
        .setPackage(context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(12.dp)
            .clickable(actionStartActivity(openApp)),
    ) {
        Text(
            context.getString(R.string.widget_title),
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GlanceTheme.colors.onSurface),
        )
        Spacer(GlanceModifier.height(6.dp))
        when {
            !data.inFamily -> Text(context.getString(R.string.widget_not_set_up), style = TextStyle(color = GlanceTheme.colors.onSurface))
            data.toDo.isEmpty() -> Text(context.getString(R.string.widget_nothing), style = TextStyle(color = GlanceTheme.colors.onSurface))
            else -> {
                data.toDo.take(MAX_ROWS).forEach { chore ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            provider = ImageProvider(chore.template.icon),
                            contentDescription = null,
                            modifier = GlanceModifier.size(16.dp),
                            colorFilter = ColorFilter.tint(if (chore.urgent) GlanceTheme.colors.error else GlanceTheme.colors.primary),
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            chore.title,
                            maxLines = 1,
                            style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                        )
                    }
                }
                if (data.toDo.size > MAX_ROWS) {
                    Text(
                        context.getString(R.string.widget_more, data.toDo.size - MAX_ROWS),
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    )
                }
            }
        }
        Spacer(GlanceModifier.defaultWeight())
        if (data.inFamily) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(context.getString(R.string.widget_out_of), onClick = actionStartActivity(openGroceries))
                Spacer(GlanceModifier.width(8.dp))
                if (data.toBuy > 0) {
                    Text(
                        context.getString(R.string.widget_to_buy, data.toBuy),
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    )
                }
            }
        }
    }
}

private const val MAX_ROWS = 5
