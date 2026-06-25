package com.simon.harmonichackernews.settings;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.simon.harmonichackernews.R;
import com.simon.harmonichackernews.utils.SettingsUtils;

public class TranslationPreferenceFragment extends BaseSettingsFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SwitchPreferenceCompat enablePreference;
    private Preference targetLanguagePreference;
    private ListPreference displayModePreference;

    @Override
    protected String getToolbarTitle() {
        return "Translation";
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_translation, rootKey);

        enablePreference = findPreference(SettingsUtils.PREF_TRANSLATE_ENABLED);
        targetLanguagePreference = findPreference(SettingsUtils.PREF_TRANSLATE_TARGET_LANGUAGE);
        displayModePreference = findPreference(SettingsUtils.PREF_TRANSLATE_DISPLAY_MODE);

        if (targetLanguagePreference != null) {
            targetLanguagePreference.setOnPreferenceClickListener(preference -> {
                TranslationLanguageDialogFragment.show(getParentFragmentManager());
                return true;
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .registerOnSharedPreferenceChangeListener(this);
        updateTargetLanguageSummary();
        updateDisplayModeSummary();
    }

    @Override
    public void onPause() {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsUtils.PREF_TRANSLATE_TARGET_LANGUAGE.equals(key)) {
            updateTargetLanguageSummary();
        } else if (SettingsUtils.PREF_TRANSLATE_DISPLAY_MODE.equals(key)) {
            updateDisplayModeSummary();
        }
    }

    private void updateTargetLanguageSummary() {
        if (targetLanguagePreference != null && getContext() != null) {
            String code = SettingsUtils.getTranslateTargetLanguage(requireContext());
            String name = getLanguageName(code);
            targetLanguagePreference.setSummary(name);
        }
    }

    private void updateDisplayModeSummary() {
        if (displayModePreference != null && getContext() != null) {
            String mode = SettingsUtils.getTranslateDisplayMode(requireContext());
            String label = SettingsUtils.DISPLAY_MODE_OVERLAY.equals(mode) ? "Overlay" : "Bilingual";
            displayModePreference.setSummary(label);
        }
    }

    private String getLanguageName(String code) {
        if (code == null) return "System default";
        switch (code) {
            case "af": return "Afrikaans";
            case "ar": return "Arabic";
            case "be": return "Belarusian";
            case "bg": return "Bulgarian";
            case "bn": return "Bengali";
            case "ca": return "Catalan";
            case "cs": return "Czech";
            case "cy": return "Welsh";
            case "da": return "Danish";
            case "de": return "German";
            case "el": return "Greek";
            case "en": return "English";
            case "eo": return "Esperanto";
            case "es": return "Spanish";
            case "et": return "Estonian";
            case "fa": return "Persian";
            case "fi": return "Finnish";
            case "fr": return "French";
            case "ga": return "Irish";
            case "gl": return "Galician";
            case "gu": return "Gujarati";
            case "he": return "Hebrew";
            case "hi": return "Hindi";
            case "hr": return "Croatian";
            case "ht": return "Haitian Creole";
            case "hu": return "Hungarian";
            case "id": return "Indonesian";
            case "is": return "Icelandic";
            case "it": return "Italian";
            case "ja": return "Japanese";
            case "ka": return "Georgian";
            case "kn": return "Kannada";
            case "ko": return "Korean";
            case "lt": return "Lithuanian";
            case "lv": return "Latvian";
            case "mk": return "Macedonian";
            case "mr": return "Marathi";
            case "ms": return "Malay";
            case "mt": return "Maltese";
            case "nl": return "Dutch";
            case "no": return "Norwegian";
            case "pl": return "Polish";
            case "pt": return "Portuguese";
            case "ro": return "Romanian";
            case "ru": return "Russian";
            case "sk": return "Slovak";
            case "sl": return "Slovenian";
            case "sq": return "Albanian";
            case "sv": return "Swedish";
            case "sw": return "Swahili";
            case "ta": return "Tamil";
            case "te": return "Telugu";
            case "th": return "Thai";
            case "tl": return "Tagalog";
            case "tr": return "Turkish";
            case "uk": return "Ukrainian";
            case "ur": return "Urdu";
            case "vi": return "Vietnamese";
            case "zh": return "Chinese";
            default: return code;
        }
    }
}
