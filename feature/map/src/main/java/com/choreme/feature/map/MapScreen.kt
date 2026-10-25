package com.choreme.feature.map

import com.choreme.core.ui.R as CoreUiR
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.location.DeviceLocation
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.LocationStatus
import com.choreme.core.model.location.Place
import com.choreme.core.ui.ErrorText
import com.choreme.core.ui.theme.memberColor
import java.time.Duration

@Composable
internal fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    var focus by remember { mutableStateOf<GeoPoint?>(null) }
    var explainBackground by rememberSaveable { mutableStateOf(false) }
    var addingPlace by rememberSaveable { mutableStateOf(false) }
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

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            FamilyMap(
                members = state.members,
                places = state.places,
                me = state.me?.let { me -> state.myFix?.let { fix -> me to fix.point } },
                focus = focus,
                modifier = Modifier.fillMaxSize(),
            )
            SmallFloatingActionButton(
                onClick = {
                    viewModel.refresh()
                    state.myFix?.let { focus = it.point }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) { Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh)) }
            if (state.isParent && state.myFix != null) {
                ExtendedFloatingActionButton(
                    onClick = { addingPlace = true },
                    icon = { Text("📍") },
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
            item(key = "share") {
                SharingCard(
                    isSharing = state.isSharing,
                    needsBackground = state.isSharing && !state.hasBackgroundPermission &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                    onToggle = { on ->
                        if (on && !state.hasPermission) {
                            foregroundLauncher.launch(DeviceLocation.FOREGROUND_PERMISSIONS)
                        } else {
                            viewModel.setSharing(on)
                        }
                    },
                    onAllowBackground = { explainBackground = true },
                )
            }

            item(key = "alerts") {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.alerts_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Switch(checked = state.alertsEnabled, onCheckedChange = viewModel::setAlertsEnabled)
                        }
                        Text(stringResource(R.string.alerts_body), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item(key = "family-title") { SectionTitle(stringResource(R.string.family_title)) }
            items(state.members, key = { "member-${it.member.id}" }) { m ->
                MemberRow(m, onClick = { (m.status as? LocationStatus.Known)?.let { focus = it.location.point } })
            }

            item(key = "places-title") {
                Column {
                    SectionTitle(stringResource(R.string.places_title))
                    if (state.places.isEmpty()) Text(stringResource(R.string.places_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
            items(state.places, key = { "place-${it.id}" }) { place ->
                PlaceRow(place, canDelete = state.isParent, onClick = { focus = place.center }, onDelete = { viewModel.deletePlace(place) })
            }
            item(key = "error") { ErrorText(actionState.error) }
        }
    }

    if (explainBackground) {
        AlertDialog(
            onDismissRequest = { explainBackground = false },
            title = { Text(stringResource(R.string.background_title)) },
            text = { Text(stringResource(R.string.background_body)) },
            confirmButton = {
                TextButton(onClick = {
                    explainBackground = false
                    backgroundLauncher.launch(DeviceLocation.BACKGROUND_PERMISSION)
                }) { Text(stringResource(R.string.continue_)) }
            },
            dismissButton = { TextButton(onClick = { explainBackground = false }) { Text(stringResource(R.string.not_now)) } },
        )
    }

    if (addingPlace) {
        AddPlaceDialog(
            onAdd = { name, radius ->
                viewModel.addPlace(name, radius)
                addingPlace = false
            },
            onDismiss = { addingPlace = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SharingCard(isSharing: Boolean, needsBackground: Boolean, onToggle: (Boolean) -> Unit, onAllowBackground: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = isSharing, onCheckedChange = onToggle)
            }
            Text(
                stringResource(if (isSharing) R.string.share_body_on else R.string.share_body_off),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (needsBackground) {
                Text(stringResource(R.string.share_needs_background), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onAllowBackground) { Text(stringResource(R.string.share_allow_background)) }
            }
        }
    }
}

@Composable
private fun MemberRow(m: MemberOnMap, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(32.dp).background(memberColor(m.member.color), CircleShape), contentAlignment = Alignment.Center) {
            Text(m.member.name.take(1).uppercase(), color = MaterialTheme.colorScheme.surface)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(m.member.name, style = MaterialTheme.typography.bodyLarge)
            Text(statusText(m.status), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text("📍")
        Spacer(Modifier.width(12.dp))
        Text(place.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.radius_meters, place.radiusMeters), style = MaterialTheme.typography.bodySmall)
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_place, place.name))
            }
        }
    }
    HorizontalDivider()
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
