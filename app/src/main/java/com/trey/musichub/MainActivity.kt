package com.trey.musichub

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    /**
     * A streaming service the app can hand off to.
     *
     * We never play audio ourselves — playback happens inside each official app
     * (or the browser as a fallback), which keeps everything within each
     * service's terms of use.
     */
    private data class Service(
        val id: String,
        val name: String,
        val subtitle: String,
        val colorHex: String,
        val pkg: String,
        val homeUrl: String,
        val searchUrl: (String) -> String,
        val nativeUri: ((String) -> String)? = null
    )

    private val services: List<Service> by lazy {
        listOf(
            Service(
                id = "spotify",
                name = "Spotify",
                subtitle = "Titres, albums, playlists",
                colorHex = "#1DB954",
                pkg = "com.spotify.music",
                homeUrl = "https://open.spotify.com",
                searchUrl = { q -> "https://open.spotify.com/search/${enc(q)}" },
                nativeUri = { q -> "spotify:search:${enc(q)}" }
            ),
            Service(
                id = "deezer",
                name = "Deezer",
                subtitle = "Titres, albums, playlists",
                colorHex = "#A238FF",
                pkg = "deezer.android.app",
                homeUrl = "https://www.deezer.com",
                searchUrl = { q -> "https://www.deezer.com/search/${enc(q)}" }
            ),
            Service(
                id = "youtube",
                name = "YouTube",
                subtitle = "Clips, lives, reprises",
                colorHex = "#FF0000",
                pkg = "com.google.android.youtube",
                homeUrl = "https://www.youtube.com",
                searchUrl = { q -> "https://www.youtube.com/results?search_query=${enc(q)}" }
            ),
            Service(
                id = "soundcloud",
                name = "SoundCloud",
                subtitle = "Sons, remixes, artistes",
                colorHex = "#FF5500",
                pkg = "com.soundcloud.android",
                homeUrl = "https://soundcloud.com",
                searchUrl = { q -> "https://soundcloud.com/search?q=${enc(q)}" }
            )
        )
    }

    private lateinit var searchInput: TextInputEditText
    private lateinit var recentChips: ChipGroup
    private lateinit var recentLabel: TextView
    private lateinit var servicesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        searchInput = findViewById(R.id.searchInput)
        recentChips = findViewById(R.id.recentChips)
        recentLabel = findViewById(R.id.recentLabel)
        servicesContainer = findViewById(R.id.servicesContainer)

        buildServiceCards()
        renderRecent()
    }

    override fun onResume() {
        super.onResume()
        renderRecent()
    }

    private fun buildServiceCards() {
        servicesContainer.removeAllViews()
        for (service in services) {
            servicesContainer.addView(createServiceCard(service))
        }
    }

    private fun createServiceCard(service: Service): View {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10) }
            radius = dp(18).toFloat()
            cardElevation = dp(1).toFloat()
            isClickable = true
            isFocusable = true
            setContentPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Coloured circle with the service initial
        val badge = TextView(this).apply {
            text = service.name.substring(0, 1)
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(service.colorHex))
            }
        }

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ).apply { marginStart = dp(14) }
        }
        val title = TextView(this).apply {
            text = service.name
            textSize = 17f
            setTextColor(themeTextColor())
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val subtitle = TextView(this).apply {
            text = service.subtitle
            textSize = 13f
            alpha = 0.7f
            setTextColor(themeTextColor())
        }
        texts.addView(title)
        texts.addView(subtitle)

        val chevron = TextView(this).apply {
            text = "›"          // ›
            textSize = 24f
            alpha = 0.5f
            setTextColor(themeTextColor())
        }

        row.addView(badge)
        row.addView(texts)
        row.addView(chevron)
        card.addView(row)

        card.setOnClickListener { launch(service) }
        return card
    }

    private fun launch(service: Service) {
        val q = searchInput.text?.toString()?.trim().orEmpty()
        hideKeyboard()

        val intents = ArrayList<Intent>()
        val native = service.nativeUri
        if (q.isNotEmpty() && native != null) {
            intents.add(
                Intent(Intent.ACTION_VIEW, Uri.parse(native(q)))
                    .setPackage(service.pkg)
            )
        }
        val webUrl = if (q.isEmpty()) service.homeUrl else service.searchUrl(q)
        // Plain web intent: Android routes to the installed app (verified links) or the browser.
        intents.add(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))

        for (intent in intents) {
            try {
                startActivity(intent)
                if (q.isNotEmpty()) addRecent(q)
                return
            } catch (e: ActivityNotFoundException) {
                // try the next fallback
            }
        }
        Toast.makeText(this, getString(R.string.no_target_app), Toast.LENGTH_SHORT).show()
    }

    // ---- Recent searches (stored locally) --------------------------------

    private fun addRecent(query: String) {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = LinkedHashSet<String>()
        current.add(query)
        current.addAll(loadRecent())
        val trimmed = current.take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT, trimmed.joinToString("\n")).apply()
        renderRecent()
    }

    private fun loadRecent(): List<String> {
        val raw = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_RECENT, "") ?: ""
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun renderRecent() {
        val items = loadRecent()
        recentChips.removeAllViews()
        val show = items.isNotEmpty()
        recentLabel.visibility = if (show) View.VISIBLE else View.GONE
        recentChips.visibility = if (show) View.VISIBLE else View.GONE
        for (item in items) {
            val chip = Chip(this).apply {
                text = item
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    searchInput.setText(item)
                    searchInput.setSelection(item.length)
                }
            }
            recentChips.addView(chip)
        }
    }

    // ---- Helpers ---------------------------------------------------------

    private fun enc(q: String): String =
        URLEncoder.encode(q, "UTF-8").replace("+", "%20")

    private fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
    ).toInt()

    private fun themeTextColor(): Int {
        val tv = TypedValue()
        theme.resolveAttribute(android.R.attr.textColorPrimary, tv, true)
        return if (tv.resourceId != 0) getColor(tv.resourceId) else tv.data
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    companion object {
        private const val PREFS = "musichub_prefs"
        private const val KEY_RECENT = "recent_searches"
        private const val MAX_RECENT = 6
    }
}
