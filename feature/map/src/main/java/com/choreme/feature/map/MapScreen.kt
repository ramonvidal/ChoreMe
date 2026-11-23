package com.choreme.feature.map

import com.choreme.core.ui.AddButton
import androidx.compose.ui.res.pluralStringResource
import com.choreme.core.ui.HeaderLine
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import com.choreme.core.model.location.batteryVendorOf
import com.choreme.core.model.location.BatteryVendor
import com.choreme.core.location.BatteryOptimization
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.location.DeviceLocation
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.LocationStatus
import com.choreme.core.model.location.Place
import com.choreme.core.push.PushRegistration
import com.choreme.core.ui.ErrorText
import com.choreme.core.ui.SectionLabel

@Composable
internal fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onMyWay by viewModel.onMyWay.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    var explainBackground by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Whether the battery saver may put the app to sleep, checked again every time the person comes back to the app.
    var batteryRestricted by remember { mutableStateOf(BatteryOptimization.isRestricted(context)) }
    val batteryHintHidden by viewModel.batteryHintHidden.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.placeSaved.collect { result ->
            snackbar.showSnackbar(
                when (result) {
                    is PlaceSaved.Saved -> context.getString(R.string.place_saved, result.name)
                    is PlaceSaved.Pending -> context.getString(R.string.place_pending, result.name)
                    PlaceSaved.Failed -> context.getString(R.string.place_failed)
                },
            )
        }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionsChanged()
    }
    val foregroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.onPermissionsChanged()
        if (result.values.any { it }) {
            viewModel.setSharing(true)
            // Android only lets apps ask for background location after foreground location.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) explainBackground = true
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        batteryRestricted = BatteryOptimization.isRestricted(context)
        onPauseOrDispose { }
    }

    MapContent(
        state = state,
        onMyWay = onMyWay,
        error = actionState.error,
        snackbar = snackbar,
        explainBackground = explainBackground,
        onExplainBackground = { explainBackground = it },
        batteryVendor = if (batteryRestricted && !batteryHintHidden) batteryVendorOf(Build.MANUFACTURER) else null,
        onOpenBatterySettings = { context.startActivity(BatteryOptimization.settingsIntent(context)) },
        onHideBatteryHint = { viewModel.hideBatteryHint() },
        actions = MapActions(
            refresh = { viewModel.refresh() },
            setOnMyWay = { viewModel.setOnMyWay(it) },
            setSharing = { on ->
                if (on && !state.hasPermission) {
                    foregroundLauncher.launch(DeviceLocation.FOREGROUND_PERMISSIONS)
                } else {
                    viewModel.setSharing(on)
                }
            },
            allowBackground = { backgroundLauncher.launch(DeviceLocation.BACKGROUND_PERMISSION) },
            setAlertsEnabled = { viewModel.setAlertsEnabled(it) },
            installNtfy = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PushRegistration.NTFY_PLAY_STORE_URL))) },
            deletePlace = { viewModel.deletePlace(it) },
            addPlace = { name, radius -> viewModel.addPlace(name, radius) },
        ),
        map = { focus, modifier ->
            FamilyMap(
                members = state.members,
                places = state.places,
                me = state.me?.let { me -> state.myFix?.let { fix -> me to fix.point } },
                focus = focus,
                modifier = modifier,
            )
        },
    )
}

/** What the Map tab can ask for; [MapScreen] passes the view model's (and Android's permission prompts). */
internal class MapActions(
    val refresh: () -> Unit = {},
    val setOnMyWay: (Boolean) -> Unit = {},
    val setSharing: (Boolean) -> Unit = {},
    val allowBackground: () -> Unit = {},
    val setAlertsEnabled: (Boolean) -> Unit = {},
    val installNtfy: () -> Unit = {},
    val deletePlace: (Place) -> Unit = {},
    val addPlace: (String, Int) -> Unit = { _, _ -> },
)

/**
 * The whole tab, drawn from [state] alone. [map] draws the map itself (OpenStreetMap tiles in the
 * app, a stand-in in screenshots).
 */
@Composable
internal fun MapContent(
    state: MapUiState,
    onMyWay: OnMyWay,
    error: Int?,
    actions: MapActions,
    map: @Composable (focus: GeoPoint?, Modifier) -> Unit,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    explainBackground: Boolean = false,
    onExplainBackground: (Boolean) -> Unit = {},
    /** Set when the battery saver may stop the location: the maker, for the extra tips. Null hides the card. */
    batteryVendor: BatteryVendor? = null,
    onOpenBatterySettings: () -> Unit = {},
    onHideBatteryHint: () -> Unit = {},
) {
    var focus by remember { mutableStateOf<GeoPoint?>(null) }
    var addingPlace by rememberSaveable { mutableStateOf(false) }
    var ntfyHelp by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        // The sub-tab above already says "Map" (it is a sub-tab of Family): only how many share where they are.
        val sharing = state.members.count { it.status is LocationStatus.Known }
        HeaderLine(pluralStringResource(R.plurals.map_sharing_count, sharing, sharing), Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp))
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            map(focus, Modifier.fillMaxSize())
            SmallFloatingActionButton(
                onClick = {
                    actions.refresh()
                    state.myFix?.let { focus = it.point }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh)) }
            if (state.isParent && state.myFix != null) {
                // The same add button as every tab.
                AddButton(stringResource(R.string.save_place), { addingPlace = true }, Modifier.align(Alignment.BottomEnd).padding(12.dp))
            }
            SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
            Text(
                stringResource(R.string.osm_credit),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                    .padding(horizontal = 4.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "on-my-way") {
                OnMyWayCard(onMyWay, onChange = actions.setOnMyWay)
            }
            // A warning to act on stays near the top.
            if (state.isSharing && batteryVendor != null) {
                item(key = "battery") { BatteryCard(batteryVendor, onOpenBatterySettings, onHideBatteryHint) }
            }
            item(key = "family-title") { SectionLabel(stringResource(R.string.family_title)) }
            item(key = "members") {
                Card(Modifier.fillMaxWidth()) {
                    state.members.forEachIndexed { index, m ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        MemberRow(m, onClick = { (m.status as? LocationStatus.Known)?.let { focus = it.location.point } })
                    }
                }
            }

            item(key = "places-title") { SectionLabel(stringResource(R.string.places_title)) }
            item(key = "places") {
                if (state.places.isEmpty()) {
                    GreyText(stringResource(R.string.places_hint), Modifier.padding(horizontal = 4.dp))
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        state.places.forEachIndexed { index, place ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            PlaceRow(place, canDelete = state.isParent, onClick = { focus = place.center }, onDelete = { actions.deletePlace(place) })
                        }
                    }
                }
            }
            // This phone's switches, together at the end: set once, rarely touched.
            item(key = "phone-title") { SectionLabel(stringResource(R.string.this_phone_title)) }
            item(key = "phone") {
                ThisPhoneCard(
                    isSharing = state.isSharing,
                    needsBackground = state.isSharing && !state.hasBackgroundPermission &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                    onShare = actions.setSharing,
                    onAllowBackground = { onExplainBackground(true) },
                    alertsEnabled = state.alertsEnabled,
                    onAlerts = actions.setAlertsEnabled,
                    instant = state.instant,
                    onNtfyHelp = { ntfyHelp = true },
                )
            }
            item(key = "error") { ErrorText(error) }
        }
    }

    if (explainBackground) {
        AlertDialog(
            onDismissRequest = { onExplainBackground(false) },
            title = { Text(stringResource(R.string.background_title)) },
            text = { Text(stringResource(R.string.background_body)) },
            confirmButton = {
                TextButton(onClick = {
                    onExplainBackground(false)
                    actions.allowBackground()
                }) { Text(stringResource(R.string.continue_)) }
            },
            dismissButton = { TextButton(onClick = { onExplainBackground(false) }) { Text(stringResource(R.string.not_now)) } },
        )
    }

    if (ntfyHelp) {
        com.choreme.core.ui.NtfyHelpDialog(onInstall = { ntfyHelp = false; actions.installNtfy() }, onDismiss = { ntfyHelp = false })
    }

    if (addingPlace) {
        AddPlaceDialog(
            onAdd = { name, radius ->
                actions.addPlace(name, radius)
                addingPlace = false
            },
            onDismiss = { addingPlace = false },
        )
    }
}
