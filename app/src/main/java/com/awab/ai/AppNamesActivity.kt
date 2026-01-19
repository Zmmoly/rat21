package com.awab.ai

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class AppNamesActivity : AppCompatActivity() {

    private lateinit var searchField: EditText
    private lateinit var appsContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private val customNames = mutableMapOf<String, MutableList<String>>() // packageName -> list of custom names

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        loadCustomNames()
        setupUI()
        loadInstalledApps()
    }

    private fun setupUI() {
        // Root layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF5F5F5.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF075E54.toInt())
            setPadding(20, 40, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Back button
        val backButton = Button(this).apply {
            text = "←"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            background = null
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { finish() }
        }

        // Title
        val title = TextView(this).apply {
            text = "أسماء التطبيقات المخصصة"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = 20
            }
        }

        header.addView(backButton)
        header.addView(title)

        // Search field
        searchField = EditText(this).apply {
            hint = "🔍 ابحث عن تطبيق..."
            textSize = 16f
            setPadding(20, 16, 20, 16)
            background = createRoundedBackground(0xFFFFFFFF.toInt(), 12f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 20, 20, 10)
            }
            
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterApps(s.toString())
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }

        // ScrollView for apps
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        appsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 10, 20, 20)
        }

        scrollView.addView(appsContainer)

        rootLayout.addView(header)
        rootLayout.addView(searchField)
        rootLayout.addView(scrollView)

        setContentView(rootLayout)
    }

    private fun loadInstalledApps() {
        appsContainer.removeAllViews()
        
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || 
                     it.packageName.contains("whatsapp") ||
                     it.packageName.contains("youtube") ||
                     it.packageName.contains("camera") }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        for (app in apps) {
            val appCard = createAppCard(app, pm)
            appsContainer.addView(appCard)
        }
    }

    private fun createAppCard(app: ApplicationInfo, pm: PackageManager): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createRoundedBackground(0xFFFFFFFF.toInt(), 12f)
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 12)
            }
            tag = app.loadLabel(pm).toString() // For filtering
        }

        // App info row
        val infoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // App icon
        val icon = ImageView(this).apply {
            setImageDrawable(app.loadIcon(pm))
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                marginEnd = 12
            }
        }

        // App name
        val appName = TextView(this).apply {
            text = app.loadLabel(pm).toString()
            textSize = 16f
            setTextColor(0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Add button
        val addButton = Button(this).apply {
            text = "+ إضافة اسم"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            background = createRoundedBackground(0xFF075E54.toInt(), 8f)
            setPadding(20, 12, 20, 12)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                showAddNameDialog(app.packageName, app.loadLabel(pm).toString())
            }
        }

        infoRow.addView(icon)
        infoRow.addView(appName)
        infoRow.addView(addButton)

        card.addView(infoRow)

        // Custom names container
        val namesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12, 0, 0)
        }

        // Load existing custom names
        val names = customNames[app.packageName] ?: emptyList()
        for (name in names) {
            val nameChip = createNameChip(name, app.packageName)
            namesContainer.addView(nameChip)
        }

        if (names.isNotEmpty()) {
            card.addView(namesContainer)
        }

        return card
    }

    private fun createNameChip(name: String, packageName: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createRoundedBackground(0xFFE8F5E9.toInt(), 16f)
            setPadding(12, 8, 12, 8)
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 8, 4)
            }

            val nameText = TextView(this@AppNamesActivity).apply {
                text = name
                textSize = 14f
                setTextColor(0xFF2E7D32.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 8
                }
            }

            val deleteButton = TextView(this@AppNamesActivity).apply {
                text = "×"
                textSize = 20f
                setTextColor(0xFF2E7D32.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    removeCustomName(packageName, name)
                }
            }

            addView(nameText)
            addView(deleteButton)
        }
    }

    private fun showAddNameDialog(packageName: String, appName: String) {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val title = TextView(this).apply {
            text = "إضافة اسم لـ: $appName"
            textSize = 18f
            setTextColor(0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val nameInput = EditText(this).apply {
            hint = "مثال: واتس، واتساب، whats"
            textSize = 16f
            setPadding(16, 16, 16, 16)
            background = createRoundedBackground(0xFFF5F5F5.toInt(), 8f)
        }

        val hint = TextView(this).apply {
            text = "يمكنك إضافة عدة أسماء مفصولة بفاصلة\nمثال: واتس، واتساب، whatsapp"
            textSize = 12f
            setTextColor(0xFF666666.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
        }

        dialogView.addView(title)
        dialogView.addView(nameInput)
        dialogView.addView(hint)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("إضافة") { _, _ ->
                val names = nameInput.text.toString()
                    .split(",", "،")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                
                for (name in names) {
                    addCustomName(packageName, name)
                }
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun addCustomName(packageName: String, name: String) {
        if (!customNames.containsKey(packageName)) {
            customNames[packageName] = mutableListOf()
        }
        
        if (!customNames[packageName]!!.contains(name)) {
            customNames[packageName]!!.add(name)
            saveCustomNames()
            loadInstalledApps() // Refresh
            Toast.makeText(this, "✅ تم إضافة: $name", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ الاسم موجود مسبقاً", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeCustomName(packageName: String, name: String) {
        customNames[packageName]?.remove(name)
        if (customNames[packageName]?.isEmpty() == true) {
            customNames.remove(packageName)
        }
        saveCustomNames()
        loadInstalledApps() // Refresh
        Toast.makeText(this, "🗑️ تم الحذف: $name", Toast.LENGTH_SHORT).show()
    }

    private fun saveCustomNames() {
        val prefs = getSharedPreferences("app_names", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Save as JSON-like string
        val jsonString = customNames.entries.joinToString(";") { (pkg, names) ->
            "$pkg:${names.joinToString(",")}"
        }
        
        editor.putString("custom_names", jsonString)
        editor.apply()
    }

    private fun loadCustomNames() {
        val prefs = getSharedPreferences("app_names", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("custom_names", "") ?: ""
        
        customNames.clear()
        
        if (jsonString.isNotBlank()) {
            jsonString.split(";").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val packageName = parts[0]
                    val names = parts[1].split(",").toMutableList()
                    customNames[packageName] = names
                }
            }
        }
    }

    private fun filterApps(query: String) {
        for (i in 0 until appsContainer.childCount) {
            val card = appsContainer.getChildAt(i) as? LinearLayout
            val appName = card?.tag as? String ?: ""
            
            card?.visibility = if (appName.contains(query, ignoreCase = true)) {
                LinearLayout.VISIBLE
            } else {
                LinearLayout.GONE
            }
        }
    }

    private fun createRoundedBackground(color: Int, radius: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    companion object {
        fun getCustomNames(context: Context): Map<String, List<String>> {
            val prefs = context.getSharedPreferences("app_names", Context.MODE_PRIVATE)
            val jsonString = prefs.getString("custom_names", "") ?: ""
            
            val customNames = mutableMapOf<String, List<String>>()
            
            if (jsonString.isNotBlank()) {
                jsonString.split(";").forEach { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        val packageName = parts[0]
                        val names = parts[1].split(",")
                        customNames[packageName] = names
                    }
                }
            }
            
            return customNames
        }
    }
}
