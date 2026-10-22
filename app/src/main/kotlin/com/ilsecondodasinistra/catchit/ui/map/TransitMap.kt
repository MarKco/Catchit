package com.ilsecondodasinistra.catchit.ui.map

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.delay
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.animation.LinearInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ilsecondodasinistra.catchit.R
import com.ilsecondodasinistra.catchit.data.LatLon
import com.ilsecondodasinistra.catchit.ui.board.clockText
import com.ilsecondodasinistra.catchit.ui.board.distanceMeters
import com.ilsecondodasinistra.catchit.ui.board.minutesUntil
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.has
import org.maplibre.android.style.expressions.Expression.toColor
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.lineCap
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineJoin
import org.maplibre.android.style.layers.PropertyFactory.lineOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.layers.PropertyFactory.rasterBrightnessMax
import org.maplibre.android.style.layers.PropertyFactory.rasterBrightnessMin
import org.maplibre.android.style.layers.PropertyFactory.rasterContrast
import org.maplibre.android.style.layers.PropertyFactory.rasterHueRotate
import org.maplibre.android.style.layers.PropertyFactory.rasterSaturation
import org.maplibre.android.style.layers.PropertyFactory.symbolSortKey
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.math.cos

/** OpenStreetMap raster tiles: no key, fine for a personal app. The attribution is drawn by [MapScreen]. */
private const val STYLE_JSON = """{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "maxzoom": 19
    }
  },
  "layers": [{ "id": "osm", "type": "raster", "source": "osm" }]
}"""

private const val SRC_ROUTE = "route"
private const val SRC_ROUTE_STOPS = "route-stops"
private const val SRC_BOARDING = "boarding"
private const val SRC_ALIGHT = "alight"
private const val SRC_VEHICLES = "vehicles"

private val VENICE = LatLng(45.4642, 12.2750)
private const val TAP_REACH_DP = 22f
private const val REVEAL_TIMEOUT_MS = 1_500L

/**
 * The map: OSM tiles with, on top, the run of the highlighted passage, the stops, and the vehicles (arrow in the heading
 * direction, line badge, bubble with the minutes to the stop). [cameraKey] changes when the camera should frame
 * [MapModel.focus] again (another leg or passage); live updates never move it.
 */
@Composable
internal fun TransitMap(
    model: MapModel,
    cameraKey: Any,
    topPadding: Dp,
    dark: Boolean,
    /** Bumped to frame [MapModel.focus] again (the recenter button). */
    recenter: Int,
    onVehicleTap: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val markers = remember { MapMarkers(context) }
    val background = MaterialTheme.colorScheme.background.toArgb()
    val mapView = remember {
        MapLibre.getInstance(context.applicationContext)
        // A TextureView, not the default SurfaceView: it is part of the view hierarchy, so it can be faded, scaled and
        // clipped with the rest of the screen (the SurfaceView flashed beige, then black, and ignored the transitions).
        MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true).foregroundLoadColor(background))
    }
    // Hidden until the camera is framed and its tiles are drawn (the first frame is the default camera, a blurry
    // zoomed tile): the screen's own background shows meanwhile. A slow network doesn't hold it back for long.
    var drawn by remember { mutableStateOf(false) }
    val reveal by animateFloatAsState(if (drawn) 1f else 0f, tween(220), label = "map reveal")
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var framedFor by remember { mutableStateOf<Any?>(null) }
    val tapped by rememberUpdatedState(onVehicleTap)

    DisposableEffect(lifecycle, mapView) {
        var destroyed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) { destroyed = true; mapView.onDestroy() }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // The screen can leave while the activity lives on: bring the view down by hand.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            // The observer may already have taken it down with the activity: destroying twice is not allowed.
            if (!destroyed) { destroyed = true; mapView.onDestroy() }
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.isAttributionEnabled = false
            m.uiSettings.isLogoEnabled = false
            m.uiSettings.isTiltGesturesEnabled = false
            m.setMinZoomPreference(10.0)
            m.setMaxZoomPreference(19.0)
            m.cameraPosition = CameraPosition.Builder().target(VENICE).zoom(12.0).build()
            m.addOnMapClickListener { position ->
                // A finger is wider than an icon: look in a box around the tap. Anything but a vehicle is "nothing".
                val at = m.projection.toScreenLocation(position)
                val reach = mapView.resources.displayMetrics.density * TAP_REACH_DP
                val hit = m.queryRenderedFeatures(RectF(at.x - reach, at.y - reach, at.x + reach, at.y + reach), "vehicles")
                    .firstNotNullOfOrNull { f -> f.getStringProperty("key")?.takeIf { it.isNotEmpty() } }
                tapped(hit)
                hit != null
            }
            m.setStyle(Style.Builder().fromJson(STYLE_JSON)) { loaded ->
                installLayers(loaded)
                style = loaded
                mapView.addOnDidFinishRenderingFrameListener { fully, _, _ -> if (fully && framedFor != null) drawn = true }
            }
        }
    }

    val added = remember(style) { HashSet<String>() }
    val glide = remember(style) { Glide() }
    DisposableEffect(glide) { onDispose { glide.cancel() } }
    LaunchedEffect(model, style) {
        val loaded = style ?: return@LaunchedEffect
        renderStatic(loaded, model, markers, added)
        // A vehicle that moved since the last fix glides there instead of jumping.
        glide.moveTo(model.vehicles) { positions -> renderVehicles(loaded, model, markers, added, context, positions) }
    }

    LaunchedEffect(style, dark) {
        val loaded = style ?: return@LaunchedEffect
        loaded.getLayerAs<RasterLayer>("osm")?.setProperties(*darkTiles(dark))
    }

    fun frame(m: MapLibreMap, animate: Boolean) {
        val margin = with(density) { 40.dp.roundToPx() }
        val top = with(density) { topPadding.roundToPx() } + margin
        val update = CameraUpdateFactory.newLatLngBounds(boundsAround(model.focus), margin, top, margin, margin)
        if (animate) m.easeCamera(update, 600) else m.moveCamera(update)
    }

    LaunchedEffect(map, style, cameraKey) {
        val m = map ?: return@LaunchedEffect
        if (style == null || framedFor == cameraKey || model.focus.isEmpty()) return@LaunchedEffect
        framedFor = cameraKey
        frame(m, animate = false)
        delay(REVEAL_TIMEOUT_MS)
        drawn = true
    }

    LaunchedEffect(recenter) {
        val m = map
        if (recenter > 0 && m != null && model.focus.isNotEmpty()) frame(m, animate = true)
    }

    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize().graphicsLayer { alpha = reveal })
}

/** The box around [points], never smaller than [MAP_MIN_HALF_SPAN_METERS] each way from its centre. */
private fun boundsAround(points: List<LatLon>): LatLngBounds {
    val south = points.minOf { it.lat }
    val north = points.maxOf { it.lat }
    val west = points.minOf { it.lon }
    val east = points.maxOf { it.lon }
    val centerLat = (south + north) / 2
    val centerLon = (west + east) / 2
    val halfLat = maxOf((north - south) / 2, MAP_MIN_HALF_SPAN_METERS / 111_000.0)
    val halfLon = maxOf((east - west) / 2, MAP_MIN_HALF_SPAN_METERS / (111_000.0 * cos(Math.toRadians(centerLat))))
    return LatLngBounds.from(centerLat + halfLat, centerLon + halfLon, centerLat - halfLat, centerLon - halfLon)
}

private fun installLayers(style: Style) {
    listOf(SRC_ROUTE, SRC_ROUTE_STOPS, SRC_BOARDING, SRC_ALIGHT, SRC_VEHICLES).forEach { style.addSource(GeoJsonSource(it)) }

    style.addLayer(
        LineLayer("route-casing", SRC_ROUTE).withProperties(
            lineColor(Color.WHITE), lineWidth(9f), lineOpacity(0.9f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    style.addLayer(
        LineLayer("route-line", SRC_ROUTE).withProperties(
            lineColor(toColor(get("color"))), lineWidth(5f), lineCap(Property.LINE_CAP_ROUND), lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    style.addLayer(
        CircleLayer("route-stops", SRC_ROUTE_STOPS).withProperties(
            circleColor(toColor(get("fill"))), circleRadius(get("radius")), circleStrokeColor(toColor(get("stroke"))), circleStrokeWidth(2.5f),
        ),
    )
    for ((id, source) in listOf("boarding" to SRC_BOARDING, "alight" to SRC_ALIGHT)) {
        style.addLayer(SymbolLayer(id, source).withProperties(iconImage(get("icon")), iconAllowOverlap(true), iconIgnorePlacement(true)))
    }
    style.addLayer(
        SymbolLayer("arrows", SRC_VEHICLES).withFilter(has("bearing")).withProperties(
            iconImage(get("arrow")), iconRotate(get("bearing")), iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
            iconAllowOverlap(true), iconIgnorePlacement(true), symbolSortKey(get("z")),
        ),
    )
    style.addLayer(
        SymbolLayer("vehicles", SRC_VEHICLES).withProperties(
            iconImage(get("icon")), iconAllowOverlap(true), iconIgnorePlacement(true), symbolSortKey(get("z")),
        ),
    )
}

private fun point(p: LatLon): Point = Point.fromLngLat(p.lon, p.lat)

private fun Int.hex(): String = String.format("#%06X", this and 0xFFFFFF)

private fun image(style: Style, added: MutableSet<String>, id: String, make: () -> android.graphics.Bitmap): String {
    if (added.add(id)) style.addImage(id, make())
    return id
}

/** The parts of the map that don't move between fixes: the run, the stops. */
private fun renderStatic(style: Style, model: MapModel, markers: MapMarkers, added: MutableSet<String>) {
    fun image(id: String, make: () -> android.graphics.Bitmap): String = image(style, added, id, make)

    val route = model.route
    style.getSourceAs<GeoJsonSource>(SRC_ROUTE)?.setGeoJson(
        if (route == null) FeatureCollection.fromFeatures(emptyList<Feature>()) else FeatureCollection.fromFeature(
            Feature.fromGeometry(LineString.fromLngLats(route.path.map(::point))).also { it.addStringProperty("color", route.colorArgb.hex()) },
        ),
    )
    style.getSourceAs<GeoJsonSource>(SRC_ROUTE_STOPS)?.setGeoJson(
        FeatureCollection.fromFeatures(
            route?.stops.orEmpty().map { stop ->
                val own = route!!.colorArgb.hex()
                Feature.fromGeometry(point(stop.position)).also {
                    when (stop.kind) {
                        RouteStopKind.Passed -> { it.addStringProperty("fill", "#9E9E9E"); it.addStringProperty("stroke", "#FFFFFF"); it.addNumberProperty("radius", 3.5f) }
                        RouteStopKind.Upcoming -> { it.addStringProperty("fill", own); it.addStringProperty("stroke", "#FFFFFF"); it.addNumberProperty("radius", 5f) }
                        RouteStopKind.Boarding, RouteStopKind.Alight -> { it.addStringProperty("fill", "#FFFFFF"); it.addStringProperty("stroke", own); it.addNumberProperty("radius", 8f) }
                    }
                }
            },
        ),
    )
    style.getSourceAs<GeoJsonSource>(SRC_BOARDING)?.setGeoJson(
        FeatureCollection.fromFeatures(
            model.boardingStops.map { stop ->
                Feature.fromGeometry(point(stop.position)).also { it.addStringProperty("icon", image("stop:${stop.name}") { markers.boardingStop(stop.name) }) }
            },
        ),
    )
    style.getSourceAs<GeoJsonSource>(SRC_ALIGHT)?.setGeoJson(
        FeatureCollection.fromFeatures(
            route?.stops.orEmpty().filter { it.kind == RouteStopKind.Alight }.map { stop ->
                Feature.fromGeometry(point(stop.position)).also {
                    it.addStringProperty("icon", image("alight:${stop.name}:${route!!.colorArgb}") { markers.alightStop(stop.name, route.colorArgb) })
                }
            },
        ),
    )
}

/** The vehicles, each at its place in [positions] (mid-glide, or where it is). */
private fun renderVehicles(
    style: Style,
    model: MapModel,
    markers: MapMarkers,
    added: MutableSet<String>,
    context: android.content.Context,
    positions: Map<String, LatLon>,
) {
    // Badge+ETA images are per line and per wording of the minutes: only those of this render are kept, or a long
    // session on the map would pile up one bitmap per minute per line.
    val used = HashSet<String>()
    fun image(id: String, make: () -> android.graphics.Bitmap): String = image(style, added, id, make).also { used += it }
    style.getSourceAs<GeoJsonSource>(SRC_VEHICLES)?.setGeoJson(
        FeatureCollection.fromFeatures(
            model.vehicles.map { vehicle ->
                Feature.fromGeometry(point(positions[vehicle.key] ?: vehicle.position)).also { feature ->
                    feature.addStringProperty("key", vehicle.key)
                    val color = vehicle.colorArgb
                    val line = vehicle.line
                    if (color == null || line == null) {
                        // Only trams are shown without a line (their label says what they are); nothing else tells a bus from the rest.
                        val kind = context.getString(R.string.board_tram)
                        feature.addStringProperty("icon", image("unknown:$kind") { markers.unknown(kind) })
                    } else {
                        val eta = vehicle.eta?.let { etaText(it, model.now, context) }
                        feature.addStringProperty("icon", image("v:$line:$color:$eta:${vehicle.highlighted}") { markers.vehicle(line, color, eta, vehicle.highlighted) })
                        feature.addStringProperty("arrow", image("a:$color:${vehicle.highlighted}") { markers.arrow(color, vehicle.highlighted) })
                    }
                    vehicle.bearing?.takeIf { color != null }?.let { feature.addNumberProperty("bearing", it) }
                    feature.addNumberProperty("z", if (vehicle.highlighted) 2 else if (color == null) 0 else 1)
                }
            },
        ),
    )
    // The source no longer names the old badges: free their bitmaps.
    val stale = added.filter { it.startsWith("v:") && it !in used }
    stale.forEach { style.removeImage(it); added.remove(it) }
}

/** "Ora", "7 min", or the clock time when it is an hour or more away. */
private fun etaText(eta: java.time.Instant, now: java.time.Instant, context: android.content.Context): String {
    val minutes = minutesUntil(eta, now)
    return when {
        minutes >= 60 -> eta.clockText()
        minutes == 0L -> context.getString(R.string.board_now)
        else -> "$minutes ${context.getString(R.string.board_min)}"
    }
}

/** The OSM tiles are bright: in the dark theme invert their lightness (and turn the hue back) so the map doesn't glare. */
private fun darkTiles(dark: Boolean) = if (dark) {
    arrayOf(rasterBrightnessMin(1f), rasterBrightnessMax(0f), rasterHueRotate(180f), rasterSaturation(-0.3f), rasterContrast(0.1f))
} else {
    arrayOf(rasterBrightnessMin(0f), rasterBrightnessMax(1f), rasterHueRotate(0f), rasterSaturation(0f), rasterContrast(0f))
}

/** Moves the vehicles from where they were drawn to their new fix over [GLIDE_MS], a straight line each. */
private class Glide {
    private val shown = HashMap<String, LatLon>()
    private var animator: ValueAnimator? = null

    fun moveTo(vehicles: List<MapVehicle>, draw: (Map<String, LatLon>) -> Unit) {
        animator?.cancel()
        val to = vehicles.associate { it.key to it.position }
        val from = to.mapValues { (key, target) -> shown[key] ?: target }
        shown.keys.retainAll(to.keys)
        if (from.all { (key, start) -> start.metersApart(to.getValue(key)) < MIN_GLIDE_METERS }) {
            shown.putAll(to)
            draw(to)
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = GLIDE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val t = animation.animatedFraction
                val now = to.mapValues { (key, target) -> from.getValue(key).let { LatLon(it.lat + (target.lat - it.lat) * t, it.lon + (target.lon - it.lon) * t) } }
                shown.putAll(now)
                draw(now)
            }
            start()
        }
    }

    fun cancel() {
        animator?.cancel()
    }

    private fun LatLon.metersApart(other: LatLon) = distanceMeters(lat, lon, other.lat, other.lon)

    private companion object {
        const val GLIDE_MS = 1_200L
        const val MIN_GLIDE_METERS = 2.0
    }
}
