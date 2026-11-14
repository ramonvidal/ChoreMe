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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.unit.sp
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

/** A small, slightly spaced title over a group of cards ("Needs doing", "Today", "Members"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.4.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
    )
}

/** One of a few choices side by side (List / Month, Groceries / Pharmacy): the chosen one yellow, the others sand. */
@Composable
fun PillTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** The small "#rooms" flag on a task that belongs to a room (cards in the corner, rows in line). */
@Composable
fun RoomTag(modifier: Modifier = Modifier, corner: Boolean = false) {
    androidx.compose.material3.Text(
        androidx.compose.ui.res.stringResource(R.string.room_tag),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                if (corner) androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 10.dp) else MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** "All / Rooms / No room" under a tab's title, to show only the chores of the rooms, or only the others. */
@Composable
fun RoomFilterRow(selected: com.choreme.core.model.chore.RoomFilter, onSelect: (com.choreme.core.model.chore.RoomFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillTab(androidx.compose.ui.res.stringResource(R.string.room_filter_all), selected == com.choreme.core.model.chore.RoomFilter.ALL, { onSelect(com.choreme.core.model.chore.RoomFilter.ALL) })
        PillTab(androidx.compose.ui.res.stringResource(R.string.room_filter_rooms), selected == com.choreme.core.model.chore.RoomFilter.ROOMS, { onSelect(com.choreme.core.model.chore.RoomFilter.ROOMS) })
        PillTab(androidx.compose.ui.res.stringResource(R.string.room_filter_no_room), selected == com.choreme.core.model.chore.RoomFilter.NO_ROOM, { onSelect(com.choreme.core.model.chore.RoomFilter.NO_ROOM) })
    }
}


/** What a sub-tab is called and drawn as. */
class SubTab(val label: String, @androidx.annotation.DrawableRes val icon: Int)

/**
 * The sub-tabs of a tab (Today / Agenda / Week, Chores / Rooms) as one bar edge to edge: a thin border, a divider
 * between the parts, the chosen one yellow and bold, each with its picture. With the phone's text turned way up the
 * picture sits above the words so nothing is cut off. A line under the bar separates it from the page.
 */
@Composable
fun SubTabs(tabs: List<SubTab>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.outline
    val stacked = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    Column(modifier.padding(top = 12.dp)) {
        Row(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, line, RoundedCornerShape(14.dp)),
        ) {
            tabs.forEachIndexed { index, tab ->
                if (index > 0) Box(Modifier.width(1.5.dp).fillMaxHeight().background(line))
                val on = index == selected
                val color = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                val content: @Composable () -> Unit = {
                    androidx.compose.material3.Icon(painterResource(tab.icon), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = color,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                val cell = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .selectable(selected = on, onClick = { onSelect(index) }, role = Role.Tab)
                    .padding(horizontal = 4.dp, vertical = 10.dp)
                if (stacked) {
                    Column(cell, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
                } else {
                    Row(cell, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) { content() }
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
