package com.estimaph.app

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.estimaph.app.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Polygon
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var sheet: BottomSheetBehavior<NestedScrollView>
    private var selectionRing: Polygon? = null
    private var hintGone = false

    private val CEBU = GeoPoint(10.3157, 123.8854)

    // ─────────────────────────────────────────────────────────────────────
    // Data models
    // ─────────────────────────────────────────────────────────────────────

    enum class PropType { HOUSE, CONDO, LOT }

    data class Area(
        val name: String,
        val lat: Double, val lon: Double,
        val estMid: Double,
        val estLow: Double,
        val estHigh: Double,
        val sqmLow: Int,
        val sqmHigh: Int,
        val trend: Double,
        val listings: Int,
        val houseM: Double,
        val condoM: Double,
        val lotSqm: Int,
        val heat: Int
    ) {
        fun fmtRange() = "PHP %.1fM  –  PHP %.1fM".format(estLow, estHigh)
        fun fmtSqm()   = "PHP %,d  –  PHP %,d / sqm".format(sqmLow, sqmHigh)
        fun fmtTrend() = if (trend >= 0) "+%.1f%%".format(trend) else "%.1f%%".format(trend)
    }

    data class Listing(
        val type: PropType,
        val title: String,
        val priceM: Double,
        val distKm: Double
    ) {
        fun fmtPrice() = "PHP %.1fM".format(priceM)
        fun fmtDist()  = "%.1f km".format(distKm)
        fun typeLabel() = when (type) {
            PropType.HOUSE -> "HOUSE"
            PropType.CONDO -> "CONDO"
            PropType.LOT   -> "LOT"
        }
        fun typeIcon() = when (type) {
            PropType.HOUSE -> R.drawable.ic_type_house
            PropType.CONDO -> R.drawable.ic_type_condo
            PropType.LOT   -> R.drawable.ic_type_lot
        }
    }

    // Mock area dataset — replace with real API response objects
    private val AREAS = listOf(
        Area("IT Park, Cebu City",    10.3310, 123.9053, 14.0, 10.0, 18.0, 150_000, 210_000, 3.8, 42, 14.0,  8.5, 170_000, 2),
        Area("Ayala Center, Cebu",    10.3175, 123.9054, 17.0, 12.0, 22.0, 180_000, 260_000, 4.2, 35, 18.0, 12.0, 200_000, 2),
        Area("Lahug, Cebu City",      10.3392, 123.8961,  9.0,  6.5, 12.0,  80_000, 130_000, 2.1, 31,  9.0,  5.5,  95_000, 1),
        Area("Banilad, Cebu City",    10.3456, 123.9010, 10.0,  7.0, 13.0,  90_000, 140_000, 1.9, 28, 10.0,  5.8, 100_000, 1),
        Area("Mabolo, Cebu City",     10.3260, 123.9120, 10.5,  7.5, 13.5,  95_000, 145_000, 2.5, 24, 10.5,  6.0, 110_000, 1),
        Area("Guadalupe, Cebu City",  10.3210, 123.8890,  8.0,  5.5, 10.5,  70_000, 110_000, 1.5, 22,  7.5,  4.8,  80_000, 1),
        Area("Talamban, Cebu City",   10.3620, 123.8990,  5.2,  3.5,  7.0,  45_000,  80_000, 1.2, 19,  5.2,  3.5,  55_000, 0),
        Area("Mandaue City",          10.3500, 123.9390,  6.5,  4.5,  9.0,  60_000, 100_000, 2.0, 33,  6.5,  4.2,  72_000, 1),
        Area("Lapu-Lapu / Mactan",    10.2890, 123.9600,  7.0,  4.0, 10.0,  55_000, 120_000, 3.1, 29,  6.0,  5.5,  75_000, 1),
        Area("Talisay City",          10.2450, 123.8490,  4.0,  2.5,  5.5,  35_000,  65_000, 0.8, 17,  3.8,  2.9,  42_000, 0),
        Area("Pit-os / Busay, Cebu",  10.3710, 123.8770,  4.7,  3.0,  6.5,  40_000,  75_000, 0.5, 12,  4.5,  0.0,  48_000, 0)
    )

    // Generates mock nearby listings for a given area
    private fun listingsFor(area: Area): List<Listing> {
        val loc   = area.name.split(",")[0]
        val condo = if (area.condoM > 0) area.condoM else area.houseM * 0.60
        return listOf(
            Listing(PropType.HOUSE, "$loc  ·  4-Bedroom",      area.houseM * 1.12, 0.4),
            Listing(PropType.HOUSE, "$loc  ·  3-Bedroom",      area.houseM * 0.93, 0.8),
            Listing(PropType.CONDO, "$loc  ·  2-Bedroom Unit", condo,              1.1),
            Listing(PropType.CONDO, "$loc  ·  Studio Unit",    condo * 0.58,       1.7),
            Listing(PropType.LOT,   "$loc  ·  120 sqm Lot",    area.lotSqm * 120.0 / 1_000_000, 2.3)
        )
    }

    // Backend stub — replace with Retrofit/OkHttp call
    private fun searchAreas(q: String): List<Area> {
        // TODO: ApiService.searchAreas(q, callback = ...)
        return if (q.length < 2) emptyList()
        else AREAS.filter { it.name.lowercase().contains(q.lowercase().trim()) }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@MainActivity, getSharedPreferences("osm", MODE_PRIVATE))
        }
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupSheet()
        initMap()
        drawHeatClusters()
        attachMapTap()
        setupSearch()
        setupZoom()
    }

    override fun onResume() { super.onResume(); b.mapView.onResume() }
    override fun onPause()  { super.onPause();  b.mapView.onPause()  }

    // ─────────────────────────────────────────────────────────────────────
    // Bottom sheet
    // ─────────────────────────────────────────────────────────────────────

    private fun setupSheet() {
        sheet = BottomSheetBehavior.from(b.bottomSheet)
        sheet.isHideable       = true
        sheet.skipCollapsed    = true
        sheet.isFitToContents  = false
        sheet.halfExpandedRatio = 0.56f   // sheet takes 56% of screen; map visible above
        sheet.state            = BottomSheetBehavior.STATE_HIDDEN

        sheet.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Fade the search bar out when the sheet expands to full screen
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        b.llSearch.animate().alpha(0f).translationY(-16f).setDuration(220).start()
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        b.llSearch.animate().alpha(1f).translationY(0f).setDuration(220).start()
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                        b.llSearch.animate().alpha(1f).translationY(0f).setDuration(220).start()
                        clearSelectionRing()
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })

        b.btnCloseSheet.setOnClickListener {
            sheet.state = BottomSheetBehavior.STATE_HIDDEN
        }
    }

    private fun clearSelectionRing() {
        selectionRing?.let { b.mapView.overlays.remove(it) }
        selectionRing = null
        b.mapView.invalidate()
    }

    // ─────────────────────────────────────────────────────────────────────
    // Map
    // ─────────────────────────────────────────────────────────────────────

    private fun initMap() {
        b.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            controller.setZoom(12.5)
            controller.setCenter(CEBU)
            minZoomLevel = 9.0
            maxZoomLevel = 19.0
            // Disable OSMdroid's built-in zoom buttons — the layout has custom ones
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        }
    }

    private fun drawHeatClusters() {
        AREAS.forEach { area ->
            val center = GeoPoint(area.lat, area.lon)

            val outerFill = when (area.heat) {
                2    -> Color.parseColor("#55C3110F")
                1    -> Color.parseColor("#55FF6B35")
                else -> Color.parseColor("#55FFB347")
            }
            val innerFill = when (area.heat) {
                2    -> Color.parseColor("#C0C3110F")
                1    -> Color.parseColor("#C0FF6B35")
                else -> Color.parseColor("#C0FFB347")
            }
            val strokeColor = when (area.heat) {
                2    -> Color.parseColor("#FFC3110F")
                1    -> Color.parseColor("#FFFF6B35")
                else -> Color.parseColor("#FFFFB347")
            }

            val outerRadius = when (area.heat) { 2 -> 2200.0; 1 -> 1500.0; else -> 1000.0 }
            val innerRadius = when (area.heat) { 2 -> 1100.0; 1 ->  750.0; else ->  520.0 }

            // Outer glow — purely visual, inserted at the bottom of the stack
            Polygon(b.mapView).apply {
                points = circlePoints(center, outerRadius)
                fillPaint.color = outerFill
                outlinePaint.apply {
                    color = outerFill; style = Paint.Style.STROKE; strokeWidth = 1f
                }
                b.mapView.overlays.add(0, this)
            }

            // Inner circle — visual only; taps are handled by MapEventsOverlay below
            Polygon(b.mapView).apply {
                points = circlePoints(center, innerRadius)
                fillPaint.color = innerFill
                outlinePaint.apply {
                    color       = strokeColor
                    style       = Paint.Style.STROKE
                    strokeWidth = 5f
                }
                b.mapView.overlays.add(this)
            }
        }
        b.mapView.invalidate()
    }

    // Uses haversine proximity instead of Polygon.setOnClickListener, which is unreliable.
    // MapEventsOverlay must be added AFTER all other overlays to be on top.
    private fun attachMapTap() {
        val eventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                val nearest = AREAS.minByOrNull { area ->
                    haversineM(p.latitude, p.longitude, area.lat, area.lon)
                } ?: return false

                // Accept the tap only if it lands within the inner circle's radius
                val threshold = when (nearest.heat) { 2 -> 1200.0; 1 -> 800.0; else -> 560.0 }
                if (haversineM(p.latitude, p.longitude, nearest.lat, nearest.lon) > threshold) {
                    return false
                }

                onAreaSelected(nearest)
                return true
            }

            override fun longPressHelper(p: GeoPoint) = false
        })
        b.mapView.overlays.add(eventsOverlay)
    }

    private fun onAreaSelected(area: Area) {
        dismissHint()
        val center = GeoPoint(area.lat, area.lon)

        // White ring highlights the selected cluster
        clearSelectionRing()
        val ringRadius = when (area.heat) { 2 -> 1150.0; 1 -> 790.0; else -> 550.0 }
        selectionRing = Polygon(b.mapView).apply {
            points = circlePoints(center, ringRadius)
            fillPaint.color = Color.TRANSPARENT
            outlinePaint.apply {
                color       = Color.WHITE
                style       = Paint.Style.STROKE
                strokeWidth = 6f
            }
        }
        b.mapView.overlays.add(selectionRing!!)
        b.mapView.invalidate()

        // Zoom into the area, then open the sheet after the animation settles
        b.mapView.controller.animateTo(center, 14.5, 700L)
        b.root.postDelayed({
            showPanel(area)
            sheet.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        }, 320)
    }

    private fun setupZoom() {
        b.btnZoomIn.setOnClickListener  { b.mapView.controller.zoomIn()  }
        b.btnZoomOut.setOnClickListener { b.mapView.controller.zoomOut() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Search
    // ─────────────────────────────────────────────────────────────────────

    private fun setupSearch() {
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun afterTextChanged(e: Editable?) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b2: Int, c: Int) {
                val q = s?.toString() ?: ""
                b.tvClear.visibility = if (q.isNotEmpty()) View.VISIBLE else View.GONE
                updateSuggestions(q)
            }
        })
        b.etSearch.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                searchAreas(b.etSearch.text.toString()).firstOrNull()?.let { pickArea(it) }
                hideKeyboard()
                true
            } else false
        }
        b.tvClear.setOnClickListener {
            b.etSearch.setText("")
            b.llSuggestions.visibility = View.GONE
            b.tvClear.visibility       = View.GONE
        }
    }

    private fun updateSuggestions(q: String) {
        b.llSuggestions.removeAllViews()
        if (q.length < 2) { b.llSuggestions.visibility = View.GONE; return }

        val results = searchAreas(q).take(5)
        if (results.isEmpty()) { b.llSuggestions.visibility = View.GONE; return }

        results.forEachIndexed { index, area ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setOnClickListener { pickArea(area); hideKeyboard() }
            }
            row.addView(TextView(this).apply {
                text      = area.name
                textSize  = 13f
                setTextColor(Color.parseColor("#E0E0DC"))
                setTypeface(null, Typeface.NORMAL)
                setPadding(dp(16), dp(13), dp(16), dp(13))
            })
            if (index < results.lastIndex) {
                row.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(Color.parseColor("#3A3A36"))
                })
            }
            b.llSuggestions.addView(row)
        }
        b.llSuggestions.visibility = View.VISIBLE
    }

    private fun pickArea(area: Area) {
        b.etSearch.setText(area.name)
        b.llSuggestions.visibility = View.GONE
        b.tvClear.visibility       = View.GONE
        onAreaSelected(area)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Panel
    // ─────────────────────────────────────────────────────────────────────

    private fun showPanel(area: Area) {
        b.tvAreaName.text = area.name

        b.llIdle.visibility = View.GONE
        b.llValueData.apply {
            alpha      = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(380).setInterpolator(DecelerateInterpolator()).start()
        }

        animateCountUp(0.0, area.estMid) { v ->
            b.tvEstValue.text = "PHP %.2fM".format(v)
        }
        b.tvEstRange.text = area.fmtRange()
        b.tvPriceSqm.text = area.fmtSqm()

        val trendUp = area.trend >= 0
        b.tvTrend.text = area.fmtTrend()
        b.tvTrend.setTextColor(
            if (trendUp) Color.parseColor("#4CAF50") else Color.parseColor("#C3110F"))
        b.ivTrendArrow.visibility = if (trendUp) View.VISIBLE else View.GONE

        b.tvNearbyIdle.visibility = View.GONE
        b.llNearbyList.removeAllViews()
        b.tvListingCount.text = "${area.listings} within 3 km"

        // Staggered slide-in for each listing card
        listingsFor(area).forEachIndexed { i, listing ->
            val card = buildPropertyCard(listing)
            card.alpha        = 0f
            card.translationX = 20f
            b.llNearbyList.addView(card)
            card.animate()
                .alpha(1f).translationX(0f)
                .setDuration(300).setStartDelay(i * 70L)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    // Builds a single property card entirely in code (no XML) to support dynamic stagger animation
    private fun buildPropertyCard(listing: Listing): View {
        val borderColor = Color.parseColor("#3A3A36")
        val titleColor  = Color.WHITE
        val priceColor  = Color.parseColor("#C3110F")
        val mutedColor  = Color.parseColor("#666662")
        val labelColor  = Color.parseColor("#AAAAAA")

        // Outer card
        val card = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            background   = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_property_card)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(8)) }
            setPadding(dp(14), dp(13), dp(14), dp(13))
        }

        // Top row: [icon] [type badge] [title]
        val rowTop = LinearLayout(this).apply {
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        rowTop.addView(ImageView(this).apply {
            setImageDrawable(ContextCompat.getDrawable(this@MainActivity, listing.typeIcon()))
            setColorFilter(Color.parseColor("#AAAAAA"))
            layoutParams = LinearLayout.LayoutParams(dp(15), dp(15)).apply {
                setMargins(0, 0, dp(8), 0)
            }
        })
        rowTop.addView(TextView(this).apply {
            text          = listing.typeLabel()
            textSize      = 8f
            letterSpacing = 0.10f
            setTextColor(labelColor)
            setTypeface(null, Typeface.BOLD)
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_badge)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, dp(8), 0) }
        })
        rowTop.addView(TextView(this).apply {
            text      = listing.title
            textSize  = 13f
            ellipsize = TextUtils.TruncateAt.END
            maxLines  = 1
            setTextColor(titleColor)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        card.addView(rowTop)

        card.addView(View(this).apply {
            setBackgroundColor(borderColor)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { setMargins(0, dp(9), 0, dp(9)) }
        })

        // Bottom row: price (left) | distance (right)
        val rowBottom = LinearLayout(this).apply {
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        rowBottom.addView(TextView(this).apply {
            text     = listing.fmtPrice()
            textSize = 15f
            setTextColor(priceColor)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        val distRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
        }
        distRow.addView(ImageView(this).apply {
            setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_location_dot))
            setColorFilter(mutedColor)
            layoutParams = LinearLayout.LayoutParams(dp(11), dp(11)).apply {
                setMargins(0, 0, dp(4), 0)
            }
        })
        distRow.addView(TextView(this).apply {
            text = listing.fmtDist(); textSize = 11f; setTextColor(mutedColor)
        })
        rowBottom.addView(distRow)
        card.addView(rowBottom)

        return card
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private fun animateCountUp(from: Double, to: Double, onUpdate: (Double) -> Unit) {
        ValueAnimator.ofFloat(from.toFloat(), to.toFloat()).apply {
            duration     = 700
            interpolator = DecelerateInterpolator()
            addUpdateListener { onUpdate((animatedValue as Float).toDouble()) }
            start()
        }
    }

    private fun dismissHint() {
        if (!hintGone) {
            hintGone = true
            b.tvHint.animate().alpha(0f).setDuration(400)
                .withEndAction { b.tvHint.visibility = View.GONE }.start()
        }
    }

    private fun hideKeyboard() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(b.root.windowToken, 0)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // Haversine distance in metres between two coordinates
    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R    = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                   cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // Approximates a circle as a 36-point polygon using the haversine formula
    private fun circlePoints(center: GeoPoint, radiusM: Double): List<GeoPoint> {
        val R   = 6_371_000.0
        val lat = Math.toRadians(center.latitude)
        val lon = Math.toRadians(center.longitude)
        val ang = radiusM / R
        return (0..36).map { i ->
            val brg  = Math.toRadians(i * 10.0)
            val pLat = asin(sin(lat) * cos(ang) + cos(lat) * sin(ang) * cos(brg))
            val pLon = lon + atan2(
                sin(brg) * sin(ang) * cos(lat),
                cos(ang) - sin(lat) * sin(pLat))
            GeoPoint(Math.toDegrees(pLat), Math.toDegrees(pLon))
        }
    }
}
