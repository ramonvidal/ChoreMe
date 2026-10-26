package com.choreme.core.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.choreme.core.model.Member
import com.choreme.core.ui.theme.memberColor

/** A person as a circle in their own color with their initial, where telling people apart matters (Family, Map). */
@Composable
fun MemberAvatar(member: Member, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier.size(size).background(memberColor(member.color), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            member.name.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
        )
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
