package com.conadasoft.rosariodiario

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.opciones)) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.opciones, DemoFragment())
            .commit()
    }

    class DemoFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            var versionName: String
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.settings, rootKey)

            var mCustomAppPreference: Preference? = checkNotNull(findPreference("version"))
            versionName = ""
            try {
                val pInfo = requireActivity().packageManager.getPackageInfo(
                    requireActivity().packageName, 0
                )
                versionName = pInfo?.versionName ?: "2.0"
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }

            mCustomAppPreference!!.summary = versionName

            mCustomAppPreference = findPreference("version")
            checkNotNull(mCustomAppPreference)
            mCustomAppPreference.summary = versionName

            mCustomAppPreference = findPreference("apps")
            checkNotNull(mCustomAppPreference)
            mCustomAppPreference.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    val intent = Intent(
                        requireContext(),
                        AppsActivity::class.java
                    )
                    startActivity(intent)
                    false
                }
        }
    }
}