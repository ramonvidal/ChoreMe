package com.choreme.feature.map

import com.choreme.core.ui.R as CoreUiR
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
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
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.theme.memberColor
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val onMyWay by viewModel.onMyWay.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    var explainBackground by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

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
        onPauseOrDispose { }
    }

    MapContent(
        state = state,
        onMyWay = onMyWay,
        error = actionState.error,
        snackbar = snackbar,
        explainBackground = explainBackground,
        onExplainBackground = { explainBackground = it },
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
) {
    var focus by remember { mutableStateOf<GeoPoint?>(null) }
    var addingPlace by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
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
                // Black, like the add button on every tab.
                ExtendedFloatingActionButton(
                    onClick = { addingPlace = true },
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = MaterialTheme.shapes.medium,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.save_place)) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                )
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
            item(key = "share") {
                SharingCard(
                    isSharing = state.isSharing,
                    needsBackground = state.isSharing && !state.hasBackgroundPermission &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                    onToggle = actions.setSharing,
                    onAllowBackground = { onExplainBackground(true) },
                )
            }

            item(key = "alerts") {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.alerts_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Switch(checked = state.alertsEnabled, onCheckedChange = actions.setAlertsEnabled)
                        }
                        Text(stringResource(R.string.alerts_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        when {
                            state.instant.connected -> GreyText(stringResource(R.string.instant_on))
                            state.instant.hasApp -> GreyText(stringResource(R.string.instant_connecting))
                            else -> {
                                GreyText(stringResource(R.string.instant_off))
                                TextButton(onClick = actions.installNtfy) { Text(stringResource(R.string.instant_install)) }
                            }
                        }
                    }
                }
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

@Composable
private fun GreyText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** "On my way home" with one tap, and who else is on the way. Works without sharing a location. */
@Composable
private fun OnMyWayCard(state: OnMyWay, onChange: (Boolean) -> Unit) {
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.others.forEach { (name, since) ->
                Text(
                    stringResource(R.string.on_my_way_other, name, since.atZone(ZoneId.systemDefault()).format(time)),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GreyText(
                    stringResource(if (state.mine) R.string.on_my_way_sent else R.string.on_my_way_hint),
                    Modifier.weight(1f).padding(end = 12.dp),
                )
                if (state.mine) {
                    OutlinedButton(
                        onClick = { onChange(false) },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) { Text(stringResource(R.string.on_my_way_home_now)) }
                } else {
                    Button(onClick = { onChange(true) }, shape = MaterialTheme.shapes.small) { Text(stringResource(R.string.on_my_way_button)) }
                }
            }
        }
    }
}

@Composable
private fun SharingCard(isSharing: Boolean, needsBackground: Boolean, onToggle: (Boolean) -> Unit, onAllowBackground: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = isSharing, onCheckedChange = onToggle)
            }
            GreyText(stringResource(if (isSharing) R.string.share_body_on else R.string.share_body_off))
            if (needsBackground) {
                GreyText(stringResource(R.string.share_needs_background))
                TextButton(onClick = onAllowBackground) { Text(stringResource(R.string.share_allow_background)) }
            }
        }
    }
}

@Composable
private fun MemberRow(m: MemberOnMap, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Their color matches their dot on the map.
        MemberAvatar(m.member, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(m.member.name, style = MaterialTheme.typography.bodyLarge)
            GreyText(statusText(m.status))
        }
    }
}

@Composable
private fun statusText(status: LocationStatus): String = when (status) {
    LocationStatus.NotSharing -> stringResource(R.string.status_not_sharing)
    is LocationStatus.Known -> {
        val ago = ageText(status.age)
        val text = status.place?.let { stringResource(R.string.status_at, it.name, ago) } ?: stringResource(R.string.status_away, ago)
        if (status.isStale) stringResource(R.string.status_stale, text) else text
    }
}

@Composable
private fun ageText(age: Duration): String = when {
    age.toMinutes() < 1 -> stringResource(R.string.just_now)
    age.toHours() < 1 -> pluralStringResource(R.plurals.minutes_ago, age.toMinutes().toInt(), age.toMinutes().toInt())
    age.toDays() < 1 -> pluralStringResource(R.plurals.hours_ago, age.toHours().toInt(), age.toHours().toInt())
    else -> pluralStringResource(R.plurals.days_ago, age.toDays().toInt(), age.toDays().toInt())
}

@Composable
private fun PlaceRow(place: Place, canDelete: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 52.dp).padding(start = 16.dp, end = if (canDelete) 4.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(CoreUiR.drawable.ic_location_on, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(place.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        GreyText(stringResource(R.string.radius_meters, place.radiusMeters))
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_place, place.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AddPlaceDialog(onAdd: (name: String, radiusMeters: Int) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var radius by rememberSaveable { mutableIntStateOf(Place.RADIUS_OPTIONS[0]) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_place_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    label = { Text(stringResource(R.string.place_name)) },
                    singleLine = true,
                )
                Text(stringResource(R.string.place_radius), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Place.RADIUS_OPTIONS.forEach { option ->
                        FilterChip(
                            selected = radius == option,
                            onClick = { radius = option },
                            label = { Text(stringResource(R.string.radius_meters, option)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, radius) }, enabled = name.isNotBlank()) { Text(stringResource(CoreUiR.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}
