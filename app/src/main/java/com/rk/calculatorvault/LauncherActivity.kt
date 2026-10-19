package com.rk.calculatorvault

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Lightweight optional Home screen. App filtering only applies while RK Home
 * is the default launcher; an ordinary app cannot edit Vivo's stock app drawer.
 */
class LauncherActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("rk_vault_secure", MODE_PRIVATE) }
    private val queryExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var cachedApps: List<ResolveInfo>? = null
    private var appQueryInProgress = false

    private val bgColor = Color.rgb(243, 246, 252)
    private val inkColor = Color.rgb(31, 40, 57)
    private val mutedColor = Color.rgb(112, 124, 145)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    override fun onResume() {
        super.onResume()
        renderHome()
    }

    override fun onDestroy() {
        super.onDestroy()
        queryExecutor.shutdownNow()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    private fun makeText(text: String, size: Float, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(inkColor)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_VERTICAL
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }

    private fun renderHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(12), dp(12), dp(12), dp(8))
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(makeText("RK Home", 22f, true))
        titles.addView(makeText("Beranda ringan", 12f).apply { setTextColor(mutedColor) })
        top.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))

        val settings = Button(this).apply {
            text = "Atur Beranda"
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = rounded(inkColor, 12)
            minWidth = dp(0)
            minimumWidth = dp(0)
            stateListAnimator = null
            setOnClickListener { openHomeSettings() }
        }
        top.addView(settings, LinearLayout.LayoutParams(-2, dp(40)))
        root.addView(top)
        root.addView(makeText("Aplikasi tersembunyi tidak ditampilkan di RK Home.", 12f).apply {
            setTextColor(mutedColor)
            setPadding(0, dp(6), 0, dp(10))
        })

        val allApps = cachedApps
        if (allApps == null) {
            root.addView(makeText("Memuat aplikasi…", 14f).apply {
                setTextColor(mutedColor)
                setPadding(dp(8), dp(18), dp(8), dp(18))
            }, LinearLayout.LayoutParams(-1, 0, 1f))
            root.addView(makeText("Untuk kembali ke launcher lama, buka Atur Beranda di atas.", 10f).apply {
                setTextColor(mutedColor)
                setPadding(dp(2), dp(8), dp(2), dp(2))
            })
            setContentView(root)
            requestAppList()
            return
        }

        val hidden = prefs.getStringSet("hidden_launcher_packages", emptySet())?.toSet() ?: emptySet()
        val visibleApps = allApps.filter { info ->
            val packageName = info.activityInfo?.packageName
            packageName != null && packageName !in hidden
        }
        if (visibleApps.isEmpty()) {
            root.addView(makeText("Tidak ada aplikasi untuk ditampilkan.", 14f).apply {
                setPadding(dp(12), dp(18), dp(12), dp(18))
            }, LinearLayout.LayoutParams(-1, 0, 1f))
        } else {
            val grid = GridView(this).apply {
                numColumns = 3
                horizontalSpacing = dp(6)
                verticalSpacing = dp(6)
                stretchMode = GridView.STRETCH_COLUMN_WIDTH
                setPadding(dp(1), dp(1), dp(1), dp(8))
                clipToPadding = false
                isVerticalScrollBarEnabled = false
                adapter = AppGridAdapter(visibleApps)
            }
            root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        }

        root.addView(makeText("Pilih RK Home sebagai Beranda untuk memakai penyaringan ikon.", 10f).apply {
            setTextColor(mutedColor)
            setPadding(dp(2), dp(8), dp(2), dp(2))
        })
        setContentView(root)
    }

    private fun requestAppList() {
        if (appQueryInProgress || cachedApps != null) return
        appQueryInProgress = true
        queryExecutor.execute {
            val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val found = try {
                packageManager.queryIntentActivities(query, 0)
                    .distinctBy { it.activityInfo?.packageName }
                    .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
            } catch (_: Exception) {
                emptyList()
            }
            mainHandler.post {
                appQueryInProgress = false
                cachedApps = found
                if (!isFinishing && !isDestroyed) renderHome()
            }
        }
    }

    private fun openHomeSettings() {
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "Buka Setelan > Aplikasi default > Aplikasi Beranda.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private data class TileHolder(val icon: ImageView, val label: TextView)

    private inner class AppGridAdapter(private val apps: List<ResolveInfo>) : BaseAdapter() {
        override fun getCount(): Int = apps.size
        override fun getItem(position: Int): ResolveInfo = apps[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
            val info = getItem(position)
            val tile: LinearLayout
            val holder: TileHolder
            if (convertView == null) {
                tile = LinearLayout(this@LauncherActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(dp(5), dp(8), dp(5), dp(6))
                    background = rounded(Color.WHITE, 16)
                }
                val icon = ImageView(this@LauncherActivity).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                tile.addView(icon, LinearLayout.LayoutParams(dp(38), dp(38)).apply {
                    bottomMargin = dp(5)
                })
                val label = makeText("", 11f).apply {
                    gravity = Gravity.CENTER
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    maxLines = 2
                }
                tile.addView(label, LinearLayout.LayoutParams(-1, dp(32)))
                holder = TileHolder(icon, label)
                tile.tag = holder
            } else {
                tile = convertView as LinearLayout
                holder = tile.tag as TileHolder
            }

            holder.label.text = try {
                info.loadLabel(packageManager)?.toString() ?: info.activityInfo?.packageName.orEmpty()
            } catch (_: Exception) {
                info.activityInfo?.packageName.orEmpty()
            }
            try {
                holder.icon.setImageDrawable(info.loadIcon(packageManager))
            } catch (_: Exception) {
                holder.icon.setImageDrawable(null)
            }
            tile.contentDescription = holder.label.text
            tile.setOnClickListener { launchResolvedApp(info) }
            return tile
        }
    }

    private fun launchResolvedApp(info: ResolveInfo) {
        val target = info.activityInfo ?: return
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(target.packageName, target.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Aplikasi tidak bisa dibuka.", Toast.LENGTH_SHORT).show()
        }
    }
}
