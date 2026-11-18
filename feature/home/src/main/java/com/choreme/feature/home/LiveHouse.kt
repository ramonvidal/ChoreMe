package com.choreme.feature.home

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.choreme.core.model.home.Gag
import com.choreme.core.model.home.SceneInput
import com.choreme.core.model.home.pickGag
import com.choreme.core.model.home.rainbowAfter
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.random.Random

/** The picture is a grid of 160 x 84 big square pixels, whatever the screen. */
internal const val W = 160
internal const val H = 84
internal const val GY = 60
internal const val HOUSE_RATIO = W.toFloat() / H

/** Frames a second: low on purpose, like an old game, and kind to the battery. */
private const val FPS = 12f
/** The moment shown when nothing moves (animations off, screenshots). */
private const val STILL_T = 4.2f

internal fun nowHour(): Float = LocalTime.now().let { it.hour + it.minute / 60f }

/** The hour of the day, refreshed every minute. */
@Composable
internal fun rememberHour(): Float {
    var hour by remember { mutableFloatStateOf(nowHour()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            hour = nowHour()
        }
    }
    return hour
}

private fun reducedMotion(context: android.content.Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

private class ActiveGag(val gag: Gag, val startedAt: Float)

/**
 * The little house, alive: a pixel-art diorama that follows the real weather and the real hour (sun and moon crossing
 * the sky, clouds, rain, snow, fog, storms, wind in the tree and the smoke), lights its windows for the people at home,
 * raises the mailbox flag for an urgent chore, dresses up for the special dates and now and then lets something happen
 * (a bird lands, the cat passes, a kite, a rainbow...), like the old castaway screensaver. It only moves while on
 * screen and in the foreground, a dozen frames a second, and stands still when [animated] is off or the phone asks for
 * no animations. [previewGag] (with the progress [previewAt], 0 to 1) shows a gag in a still picture, for screenshots.
 */
@Composable
internal fun LiveHouse(
    scene: SceneInput,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    previewGag: Gag? = null,
    previewAt: Float = 0.5f,
) {
    val context = LocalContext.current
    val moves = animated && previewGag == null && !reducedMotion(context)
    var t by remember { mutableFloatStateOf(STILL_T) }
    var gag by remember { mutableStateOf<ActiveGag?>(null) }
    val latest by rememberUpdatedState(scene)
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(moves) {
        if (!moves) return@LaunchedEffect
        val random = Random(System.nanoTime())
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = -1L
            var nextGagAt = t + 4f + random.nextFloat() * 6f
            var skyBefore = latest.sky
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                if (last < 0 || (now - last) / 1e9f >= 1f / FPS) {
                    t += if (last < 0) 1f / FPS else (now - last) / 1e9f
                    last = now
                    val current = latest
                    val active = gag
                    if (active != null && t - active.startedAt >= active.gag.seconds) {
                        gag = null
                        nextGagAt = t + 8f + random.nextFloat() * 8f
                    }
                    if (gag == null && rainbowAfter(skyBefore, current)) {
                        gag = ActiveGag(Gag.RAINBOW, t)
                    } else if (gag == null && t >= nextGagAt) {
                        pickGag(current, random)?.let { gag = ActiveGag(it, t) } ?: run { nextGagAt = t + 10f }
                    }
                    skyBefore = current.sky
                }
            }
        }
    }

    Canvas(modifier.aspectRatio(HOUSE_RATIO).clipToBounds()) {
        val px = Px(this, size.width / W)
        val time = if (moves) t else STILL_T
        drawSky(px, scene, time)
        val shown = previewGag?.let { ActiveGag(it, time - previewAt * it.seconds) } ?: gag
        val progress = shown?.let { ((time - it.startedAt) / it.gag.seconds).coerceIn(0f, 1f) } ?: 0f
        // The rainbow stands behind the house; every other event is in front.
        if (shown?.gag == Gag.RAINBOW) drawGag(px, Gag.RAINBOW, progress, scene, time)
        drawGround(px, scene, time)
        drawBackdropDecor(px, scene, time)
        drawTree(px, scene, time)
        drawHouse(px, scene, time)
        drawWeather(px, scene, time)
        if (shown != null && shown.gag != Gag.RAINBOW) drawGag(px, shown.gag, progress, scene, time)
    }
}
