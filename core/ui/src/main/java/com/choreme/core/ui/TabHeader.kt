package com.choreme.core.ui

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.choreme.core.model.Member
import com.choreme.core.ui.theme.memberColor

/** A person as a circle in their own color with their initial, where telling people apart matters (Family, Map). */
@Composable
fun MemberAvatar(member: Member, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier.size(size).background(memberColor(member.color), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // Sized to the circle (dp, not the phone's text size), so the initial always fits.
        val letter = with(LocalDensity.current) { (size * 0.42f).toSp() }
        Text(
            member.name.take(1).uppercase(),
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = letter,
                lineHeight = letter,
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
            ),
            maxLines = 1,
        )
    }
}

/**
 * Several people as overlapping avatars ("Leo and Mia are on it" at a glance), each ringed in
 * [ring], the color behind them, so they stay apart. [description] is read out instead.
 */
@Composable
fun AvatarStack(members: List<Member>, description: String, ring: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(-(size * 0.3f)),
    ) {
        members.forEach { member ->
            MemberAvatar(member, Modifier.border(1.5.dp, ring, CircleShape), size = size)
        }
    }
}

/** The top of each tab: its title and one grey line under it ("3 need doing"). */
@Composable
fun TabHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: @Composable (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        subtitle?.invoke()
    }
}

/** The grey line under a [TabHeader]. */
@Composable
fun HeaderLine(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** A small grey title over a group of cards ("Needs doing", "Today", "Members"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** One of a few choices side by side (List / Month, Groceries / Pharmacy): the chosen one black, the others white. */
@Composable
fun PillTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceContainerLowest)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
