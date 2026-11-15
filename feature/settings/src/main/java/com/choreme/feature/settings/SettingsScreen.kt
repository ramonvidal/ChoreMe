package com.choreme.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.TabHeader

/**
 * The Settings tab: what the family agrees on (for parents), the regular trips the weather is told for, and the
 * choices of this phone, in that order.
 */
@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TabHeader(stringResource(R.string.settings_title))
        if (state.isParent) {
            SectionLabel(stringResource(R.string.settings_family))
            PointsSetting(state.pointsOn, viewModel::setPointsEnabled)
        }
        SectionLabel(stringResource(R.string.settings_trips))
        TripPlan()
        SectionLabel(stringResource(R.string.this_phone_title))
        PhoneSettings()
    }
}

/** For parents: whether the family plays with points. Off hides points everywhere. */
@Composable
internal fun PointsSetting(on: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = on, onValueChange = onChange, role = SemanticsRole.Switch)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.points_setting_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.points_setting_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = on, onCheckedChange = null)
        }
    }
}
