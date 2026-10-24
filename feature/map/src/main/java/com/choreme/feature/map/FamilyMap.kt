package com.choreme.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.LocationStatus
import com.choreme.core.model.location.Place
import com.choreme.core.ui.theme.memberColor
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.util.GeoPoint as OsmPoint

/**
 * OpenStreetMap with a dot per member who shares and a circle per saved place.
 * [focus] moves the map, e.g. when a member is tapped in the list.
 */
@Composable
internal fun FamilyMap(
    members: List<MemberOnMap>,
    places: List<Place>,
    focus: GeoPoint?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        // osmdroid needs an identifying user agent for the OpenStreetMap tile servers.
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(DEFAULT_ZOOM)
        }
    }
    MapLifecycle(mapView)

    // Plain holders, not state: changing them must not trigger another redraw.
    val fittedOnce = remember { booleanArrayOf(false) }
    val lastFocus = remember { arrayOfNulls<GeoPoint>(1) }
    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()
            places.forEach { place ->
                map.overlays += Polygon(map).apply {
                    points = Polygon.pointsAsCircle(place.center.toOsm(), place.radiusMeters.toDouble())
                    fillPaint.color = PLACE_FILL
                    outlinePaint.color = PLACE_OUTLINE
                    outlinePaint.strokeWidth = 3f
                    title = place.name
                }
            }
            val shown = members.mapNotNull { m -> (m.status as? LocationStatus.Known)?.let { m to it } }
            shown.forEach { (m, status) ->
                map.overlays += Marker(map).apply {
                    position = status.location.point.toOsm()
                    title = m.member.name
                    icon = memberIcon(context, memberColor(m.member.color).toArgb(), m.member.name.take(1).uppercase(), status.isStale)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
            }
            when {
                focus != null && focus != lastFocus[0] -> {
                    lastFocus[0] = focus
                    map.controller.animateTo(focus.toOsm(), FOCUS_ZOOM, null)
                }
                // The first time there's something to show, fit everyone on screen.
                !fittedOnce[0] && shown.isNotEmpty() -> {
                    fittedOnce[0] = true
                    val points = shown.map { it.second.location.point.toOsm() }
                    if (points.size == 1) {
                        map.controller.setCenter(points.single())
                    } else {
                        map.post { map.zoomToBoundingBox(BoundingBox.fromGeoPoints(points).increaseByScale(1.4f), false) }
                    }
                }
            }
            map.invalidate()
        },
    )
}

@Composable
private fun MapLifecycle(mapView: MapView) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
}

/** A colored circle with the member's initial; faded when the location is old. */
private fun memberIcon(context: Context, color: Int, initial: String, stale: Boolean): BitmapDrawable {
    val size = (40 * context.resources.displayMetrics.density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = color
    paint.alpha = if (stale) 110 else 255
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - size / 12f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = size / 2.2f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText(initial, size / 2f, size / 2f - (paint.descent() + paint.ascent()) / 2, paint)
    return BitmapDrawable(context.resources, bitmap)
}

private fun GeoPoint.toOsm() = OsmPoint(latitude, longitude)

private const val DEFAULT_ZOOM = 13.0
private const val FOCUS_ZOOM = 16.0
private const val PLACE_FILL = 0x332E7D6B
private const val PLACE_OUTLINE = 0xFF2E7D6B.toInt()
