package com.choreme.feature.map

import androidx.compose.material3.HorizontalDivider
import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.choreme.core.model.location.BatteryVendor
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.choreme.core.model.location.LocationStatus
import com.choreme.core.model.location.Place
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The battery saver may stop the location in the background: how to leave this app out of it. */
@Composable
internal fun BatteryCard(vendor: BatteryVendor, onOpen: () -> Unit, onHide: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.battery_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.battery_body), style = MaterialTheme.typography.bodyMedium)
            val tip = when (vendor) {
                BatteryVendor.XIAOMI -> R.string.battery_tip_xiaomi
                BatteryVendor.SAMSUNG -> R.string.battery_tip_samsung
                BatteryVendor.HUAWEI -> R.string.battery_tip_huawei
                BatteryVendor.OPPO -> R.string.battery_tip_oppo
                BatteryVendor.VIVO -> R.string.battery_tip_vivo
                BatteryVendor.OTHER -> null
            }
            tip?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpen) { Text(stringResource(R.string.battery_open)) }
                TextButton(onClick = onHide) { Text(stringResource(R.string.battery_hide)) }
            }
        }
    }
}

@Composable
internal fun GreyText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** "On my way home" with one tap, and who else is on the way. Works without sharing a location. */
@Composable
internal fun OnMyWayCard(state: OnMyWay, onChange: (Boolean) -> Unit) {
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

/**
 * This phone's two switches in one card, at the end of the Map: sharing where it is (and the background permission it
 * needs), and arrival alerts (and whether they come instantly through ntfy).
 */
@Composable
internal fun ThisPhoneCard(
    isSharing: Boolean,
    needsBackground: Boolean,
    onShare: (Boolean) -> Unit,
    onAllowBackground: () -> Unit,
    alertsEnabled: Boolean,
    onAlerts: (Boolean) -> Unit,
    instant: InstantAlerts,
    onNtfyHelp: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.share_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = isSharing, onCheckedChange = onShare)
            }
            GreyText(stringResource(if (isSharing) R.string.share_body_on else R.string.share_body_off))
            if (needsBackground) {
                GreyText(stringResource(R.string.share_needs_background))
                TextButton(onClick = onAllowBackground) { Text(stringResource(R.string.share_allow_background)) }
            }
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.alerts_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = alertsEnabled, onCheckedChange = onAlerts)
            }
            GreyText(stringResource(R.string.alerts_body))
            when {
                instant.connected -> GreyText(stringResource(R.string.instant_on))
                instant.hasApp -> GreyText(stringResource(R.string.instant_connecting))
                else -> {
                    GreyText(stringResource(R.string.instant_off))
                    TextButton(onClick = onNtfyHelp) { Text(stringResource(R.string.instant_install)) }
                }
            }
        }
    }
}

@Composable
internal fun MemberRow(m: MemberOnMap, onClick: () -> Unit) {
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
internal fun PlaceRow(place: Place, canDelete: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
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
internal fun AddPlaceDialog(onAdd: (name: String, radiusMeters: Int) -> Unit, onDismiss: () -> Unit) {
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
