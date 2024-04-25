package com.example.receiptsaver
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import java.util.Locale

class AccountFragment : Fragment() {

    private lateinit var switchTheme: Switch
    private lateinit var radioGroup: RadioGroup
    private lateinit var thresholdAmount: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        switchTheme = view.findViewById(R.id.switch_theme)
        radioGroup = view.findViewById(R.id.radioGroup)
        thresholdAmount = view.findViewById(R.id.threshold_amount)

        // Check current theme and set the switch accordingly
        switchTheme.isChecked = when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else -> false
        }
        // Dark Mode Switch Listener
        switchTheme.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        // Get references to the language checkboxes
        val englishCheckbox = view.findViewById<RadioButton>(R.id.english_checkbox)
        val vietnameseCheckbox = view.findViewById<RadioButton>(R.id.vietnamese_checkbox)
        val spanishCheckbox = view.findViewById<RadioButton>(R.id.spanish_checkbox)

        // Load the saved language preference from SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val language = sharedPreferences.getString("language", "en")

        // Check the appropriate checkbox based on the saved language preference
        when (language) {
            "en" -> englishCheckbox.isChecked = true
            "vi" -> vietnameseCheckbox.isChecked = true
            "es" -> spanishCheckbox.isChecked = true
        }
        // RadioGroup Listener for language selection
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.english_checkbox -> setLanguage("en")
                R.id.spanish_checkbox -> setLanguage("es")
                R.id.vietnamese_checkbox -> setLanguage("vi")
            }
        }
        // Load the saved threshold amount from SharedPreferences
        val savedThreshold = sharedPreferences.getString("thresholdAmount", "0.0")
        thresholdAmount.setText(savedThreshold)
        // Threshold Amount EditText Listener
        thresholdAmount.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                // Validate and save the threshold amount
                val amount = thresholdAmount.text.toString().toDoubleOrNull()
                if (amount != null) {
                    saveThreshold(amount)
                } else {
                    // Show error message for invalid input
                    thresholdAmount.error = getString(R.string.invalid_amount)
                }
            }
        }
    }
    private fun setLanguage(language: String) {
        // Save the selected language preference in SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = sharedPreferences.edit()
        editor.putString("language", language)
        editor.apply()

        // Change the app language
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)

        // Restart the activity to apply the new language
        activity?.recreate()
    }
    private fun saveThreshold(amount: Double) {
        // Save the threshold amount in SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity)
        val editor = sharedPreferences.edit()
        editor.putString("thresholdAmount", amount.toString())
        editor.apply()
    }
}
