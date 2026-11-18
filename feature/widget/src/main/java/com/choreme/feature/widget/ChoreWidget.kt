package com.choreme.feature.widget

import kotlinx.coroutines.coroutineScope
import com.choreme.core.model.chore.complete
import com.choreme.core.model.Role
import com.choreme.core.chores.ChoreChanger
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.actionParametersOf
import androidx.glance.action.ActionParameters
import com.choreme.core.ui.ChoreNames
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
import androidx.glance.material3.ColorProviders
import com.choreme.core.ui.theme.ChoreMeDarkColors
import com.choreme.core.ui.theme.ChoreMeLightColors
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
    fun changer(): ChoreChanger
}

internal data class WidgetData(val inFamily: Boolean, val toDo: List<Chore>, val toBuy: Int)

class ChoreWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChoreWidget()
}

internal class ChoreWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent {
            // The app's own colours, not the wallpaper's.
            GlanceTheme(colors = ColorProviders(light = ChoreMeLightColors, dark = ChoreMeDarkColors)) { Content(context, data) }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        if (!deps.availability().isConfigured) return WidgetData(false, emptyList(), 0)
        val session = withTimeoutOrNull(TIMEOUT_MS) { deps.families().session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return WidgetData(false, emptyList(), 0)
        val chores = withTimeoutOrNull(TIMEOUT_MS) { deps.chores().observeChores(session.familyId).first() }.orEmpty()
        val items = withTimeoutOrNull(TIMEOUT_MS) { deps.groceries().observeItems(session.familyId).first() }.orEmpty()
        // Yours first, then what's waiting for anyone.
        val board = buildTodayBoard(chores, LocalDate.now(), session.memberId)
        return WidgetData(true, board.mine + board.toDo, items.groceriesToBuy())
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
                            ChoreNames.shown(context, chore.title),
                            maxLines = 1,
                            style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurface),
                            modifier = GlanceModifier.defaultWeight(),
                        )
                        // A task that belongs to a room says so.
                        if (chore.roomId != null) {
                            Text(
                                context.getString(com.choreme.core.ui.R.string.room_tag),
                                maxLines = 1,
                                style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                            )
                        }
                        // Done from here: a parent's counts at once, a kid's goes to a parent (as in the app).
                        Spacer(GlanceModifier.width(6.dp))
                        Image(
                            provider = ImageProvider(com.choreme.core.ui.R.drawable.ic_check_circle),
                            contentDescription = context.getString(R.string.widget_done, ChoreNames.shown(context, chore.title)),
                            modifier = GlanceModifier.size(24.dp).clickable(actionRunCallback<DoneAction>(actionParametersOf(CHORE_ID to chore.id))),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
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

private val CHORE_ID = ActionParameters.Key<String>("choreId")

/** The widget's Done: the same step as the app's (see ChoreChanger), then the widget redraws. */
internal class DoneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val choreId = parameters[CHORE_ID] ?: return
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        if (!deps.availability().isConfigured) return
        val session = withTimeoutOrNull(TIMEOUT_MS) { deps.families().session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return
        val members = withTimeoutOrNull(TIMEOUT_MS) { deps.families().observeMembers(session.familyId).first() }.orEmpty()
        val me = members.firstOrNull { it.id == session.memberId } ?: return
        val chore = withTimeoutOrNull(TIMEOUT_MS) { deps.chores().observeChores(session.familyId).first() }
            ?.firstOrNull { it.id == choreId } ?: return
        val parents = members.filter { it.role == Role.PARENT }.map { it.id }.toSet()
        // Waits for the pings to the family to go out before the widget's work ends.
        coroutineScope {
            deps.changer().change(this, session.familyId, me, parents, chore) { who, today -> complete(who, today) }
        }
        ChoreWidget().update(context, glanceId)
    }

    private companion object {
        const val TIMEOUT_MS = 15_000L
    }
}
