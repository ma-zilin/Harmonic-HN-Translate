package com.simon.harmonichackernews.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.simon.harmonichackernews.R;
import com.simon.harmonichackernews.network.TranslationManager;
import com.simon.harmonichackernews.utils.SettingsUtils;
import com.simon.harmonichackernews.utils.Utils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TranslationLanguageDialogFragment extends AppCompatDialogFragment {

    public static final String TAG = "tag_translate_language_dialog";

    private static final LinkedHashMap<String, String> LANGUAGES = new LinkedHashMap<>();

    static {
        LANGUAGES.put("af", "Afrikaans");
        LANGUAGES.put("ar", "Arabic");
        LANGUAGES.put("be", "Belarusian");
        LANGUAGES.put("bg", "Bulgarian");
        LANGUAGES.put("bn", "Bengali");
        LANGUAGES.put("ca", "Catalan");
        LANGUAGES.put("cs", "Czech");
        LANGUAGES.put("da", "Danish");
        LANGUAGES.put("de", "German");
        LANGUAGES.put("el", "Greek");
        LANGUAGES.put("en", "English");
        LANGUAGES.put("es", "Spanish");
        LANGUAGES.put("et", "Estonian");
        LANGUAGES.put("fa", "Persian");
        LANGUAGES.put("fi", "Finnish");
        LANGUAGES.put("fr", "French");
        LANGUAGES.put("ga", "Irish");
        LANGUAGES.put("hi", "Hindi");
        LANGUAGES.put("hr", "Croatian");
        LANGUAGES.put("hu", "Hungarian");
        LANGUAGES.put("id", "Indonesian");
        LANGUAGES.put("is", "Icelandic");
        LANGUAGES.put("it", "Italian");
        LANGUAGES.put("ja", "Japanese");
        LANGUAGES.put("ko", "Korean");
        LANGUAGES.put("lt", "Lithuanian");
        LANGUAGES.put("lv", "Latvian");
        LANGUAGES.put("mk", "Macedonian");
        LANGUAGES.put("mr", "Marathi");
        LANGUAGES.put("ms", "Malay");
        LANGUAGES.put("mt", "Maltese");
        LANGUAGES.put("nl", "Dutch");
        LANGUAGES.put("no", "Norwegian");
        LANGUAGES.put("pl", "Polish");
        LANGUAGES.put("pt", "Portuguese");
        LANGUAGES.put("ro", "Romanian");
        LANGUAGES.put("ru", "Russian");
        LANGUAGES.put("sk", "Slovak");
        LANGUAGES.put("sl", "Slovenian");
        LANGUAGES.put("sq", "Albanian");
        LANGUAGES.put("sv", "Swedish");
        LANGUAGES.put("sw", "Swahili");
        LANGUAGES.put("ta", "Tamil");
        LANGUAGES.put("te", "Telugu");
        LANGUAGES.put("th", "Thai");
        LANGUAGES.put("tl", "Tagalog");
        LANGUAGES.put("tr", "Turkish");
        LANGUAGES.put("uk", "Ukrainian");
        LANGUAGES.put("ur", "Urdu");
        LANGUAGES.put("vi", "Vietnamese");
        LANGUAGES.put("cy", "Welsh");
        LANGUAGES.put("zh", "Chinese");
    }

    private final Map<String, MaterialRadioButton> radioButtons = new HashMap<>();
    private String selectedLanguage;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Context ctx = requireContext();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ctx);

        selectedLanguage = SettingsUtils.getTranslateTargetLanguage(ctx);

        int textColor = MaterialColors.getColor(ctx, R.attr.storyColorNormal, "TranslateLanguageDialog");

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);

        for (Map.Entry<String, String> entry : LANGUAGES.entrySet()) {
            String code = entry.getKey();
            String name = entry.getValue();
            container.addView(createLanguageRow(ctx, code, name, textColor));
        }

        ScrollView scrollView = new ScrollView(ctx);
        scrollView.addView(container);

        builder.setTitle("Target language");
        builder.setView(scrollView);
        builder.setNegativeButton("Cancel", null);

        updateSelection();
        return builder.create();
    }

    public static void show(FragmentManager fm) {
        new TranslationLanguageDialogFragment().show(fm, TAG);
    }

    private LinearLayout createLanguageRow(Context ctx, String code, String name, int textColor) {
        int horizontalPadding = Utils.pxFromDpInt(getResources(), 24);
        int verticalPadding = Utils.pxFromDpInt(getResources(), 4);
        int minHeight = Utils.pxFromDpInt(getResources(), 56);
        int radioMarginEnd = Utils.pxFromDpInt(getResources(), 4);

        LinearLayout row = new LinearLayout(ctx);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(minHeight);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        row.setClickable(true);
        row.setFocusable(true);
        TypedValue selectableItemBackground = new TypedValue();
        ctx.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, selectableItemBackground, true);
        row.setBackgroundResource(selectableItemBackground.resourceId);

        MaterialRadioButton radioButton = new MaterialRadioButton(ctx);
        radioButton.setClickable(false);
        LinearLayout.LayoutParams radioParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        radioParams.setMarginEnd(radioMarginEnd);
        row.addView(radioButton, radioParams);
        radioButtons.put(code, radioButton);

        TextView title = new TextView(ctx);
        title.setText(name);
        title.setTextColor(textColor);
        title.setSingleLine(true);
        row.addView(title, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1));

        row.setOnClickListener(view -> {
            selectedLanguage = code;
            saveSelection();
            updateSelection();
            dismiss();
        });
        return row;
    }

    private void saveSelection() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.edit().putString(SettingsUtils.PREF_TRANSLATE_TARGET_LANGUAGE, selectedLanguage).apply();

        if (!"en".equals(selectedLanguage)) {
            TranslationManager.preloadModel("en", selectedLanguage);
            if (!prefs.getBoolean("pref_translate_model_reminder_shown", false)) {
                prefs.edit().putBoolean("pref_translate_model_reminder_shown", true).apply();
                Toast.makeText(getContext(),
                        "Translation model (~30–50 MB) will download in background",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateSelection() {
        for (Map.Entry<String, MaterialRadioButton> entry : radioButtons.entrySet()) {
            entry.getValue().setChecked(entry.getKey().equals(selectedLanguage));
        }
    }

}
