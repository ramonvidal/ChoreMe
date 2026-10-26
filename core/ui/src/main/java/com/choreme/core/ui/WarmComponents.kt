package com.choreme.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choreme.core.model.Member
import com.choreme.core.ui.theme.memberColor

/** A person as a circle in their own color with their initial: who's on it, who noticed, who's away. */
@Composable
fun MemberAvatar(member: Member, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier.size(size).background(memberColor(member.color), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            member.name.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** A picture in a soft rounded square, the way chores and items show their icon. */
@Composable
fun IconBubble(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 48.dp,
) {
    Box(
        modifier = modifier.size(size).background(container, RoundedCornerShape(size / 3)),
        contentAlignment = Alignment.Center,
    ) {
        ItemIcon(icon, Modifier.size(size * 0.54f), tint = tint)
    }
}

/** A small rounded label, e.g. "3 need doing". */
@Composable
fun SummaryChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = modifier.background(container, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * The top of each tab: a small line above (a date, a count), the tab's big title, and chips
 * under it. Same on every tab so the app feels like one place.
 */
@Composable
fun TabHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    chips: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (eyebrow != null) {
            Text(
                eyebrow.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.6.sp,
            )
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp), content = chips)
    }
}

/** The family as a row of circles with names under them. */
@Composable
fun FamilyRow(members: List<Member>, meId: String?, youLabel: String, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        members.forEach { member ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MemberAvatar(member)
                Text(
                    if (member.id == meId) youLabel else member.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
