package com.simon.harmonichackernews.network;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

public class TranslationManager {
    private static final String TAG = "TranslationManager";
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final Map<String, Translator> translatorCache = new HashMap<>();

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onFailure(String error);
    }

    public interface BatchCallback {
        void onComplete(String[] translations);
        void onError(String error);
    }

    public static boolean canAttemptLocalTranslation() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static void translate(String text, String sourceLang, String targetLang,
                                  TranslationCallback callback) {
        if (text == null || text.trim().isEmpty()) {
            MAIN_HANDLER.post(() -> callback.onSuccess(""));
            return;
        }

        if (!canAttemptLocalTranslation()) {
            MAIN_HANDLER.post(() -> callback.onFailure(
                    "On-device translation not supported on this device (requires Android 8+)"));
            return;
        }

        final String trimmedText = text.trim();

        try {
            Translator translator = getOrCreateTranslator(sourceLang, targetLang);

            translator.downloadModelIfNeeded()
                    .addOnSuccessListener(unused -> {
                        try {
                            translator.translate(trimmedText)
                                    .addOnSuccessListener(translatedText -> {
                                        MAIN_HANDLER.post(() -> callback.onSuccess(translatedText));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Translation failed", e);
                                        MAIN_HANDLER.post(() -> callback.onFailure(
                                                "Translation failed: " + e.getMessage()));
                                    });
                        } catch (Exception e) {
                            Log.e(TAG, "translate call failed", e);
                            MAIN_HANDLER.post(() -> callback.onFailure(
                                    "Translation error: " + e.getMessage()));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Model download failed", e);
                        MAIN_HANDLER.post(() -> callback.onFailure(
                                "Model download failed. Check your network connection."));
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to create translator", e);
            MAIN_HANDLER.post(() -> callback.onFailure(
                    "Translation unavailable: " + e.getMessage()));
        }
    }

    /**
     * Pre-download the translation model in background without requiring WiFi.
     * Call when the user selects a target language so the first translation is fast.
     */
    public static void preloadModel(String sourceLang, String targetLang) {
        if (!canAttemptLocalTranslation()) return;

        try {
            Translator translator = getOrCreateTranslator(sourceLang, targetLang);
            DownloadConditions conditions = new DownloadConditions.Builder()
                    .build(); // no WiFi requirement for preload
            translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener(unused ->
                            Log.d(TAG, "Model preloaded: " + sourceLang + "->" + targetLang))
                    .addOnFailureListener(e ->
                            Log.w(TAG, "Model preload failed: " + e.getMessage()));
        } catch (Exception e) {
            Log.w(TAG, "Preload failed", e);
        }
    }

    public static void translateBatch(String[] texts, String sourceLang, String targetLang,
                                       BatchCallback callback) {
        if (texts == null || texts.length == 0) {
            MAIN_HANDLER.post(() -> callback.onComplete(new String[0]));
            return;
        }

        if (!canAttemptLocalTranslation()) {
            MAIN_HANDLER.post(() -> callback.onError(
                    "On-device translation not supported (requires Android 8+)"));
            return;
        }

        final String[] results = new String[texts.length];
        final java.util.concurrent.atomic.AtomicInteger pending =
                new java.util.concurrent.atomic.AtomicInteger(texts.length);
        final boolean[] hasError = {false};

        for (int i = 0; i < texts.length; i++) {
            final int index = i;
            translate(texts[i], sourceLang, targetLang, new TranslationCallback() {
                @Override
                public void onSuccess(String translatedText) {
                    results[index] = translatedText;
                    checkDone();
                }

                @Override
                public void onFailure(String error) {
                    hasError[0] = true;
                    results[index] = texts[index]; // fallback to original
                    Log.w(TAG, "Batch translation failed for item " + index + ": " + error);
                    checkDone();
                }

                private void checkDone() {
                    if (pending.decrementAndGet() == 0) {
                        MAIN_HANDLER.post(() -> callback.onComplete(results));
                    }
                }
            });
        }
    }

    private static Translator getOrCreateTranslator(String sourceLang, String targetLang) {
        String cacheKey = sourceLang + "->" + targetLang;
        Translator translator = translatorCache.get(cacheKey);

        if (translator == null) {
            String resolvedTarget = resolveLanguageCode(targetLang);
            TranslatorOptions options;
            if ("auto".equals(sourceLang)) {
                options = new TranslatorOptions.Builder()
                        .setTargetLanguage(resolvedTarget)
                        .build();
            } else {
                options = new TranslatorOptions.Builder()
                        .setSourceLanguage(resolveLanguageCode(sourceLang))
                        .setTargetLanguage(resolvedTarget)
                        .build();
            }
            translator = Translation.getClient(options);
            translatorCache.put(cacheKey, translator);
        }
        return translator;
    }

    /**
     * Resolves language codes to valid ML Kit TranslateLanguage constants.
     * Handles old ISO 639 codes that differ from BCP-47 (e.g. "iw" -> "he").
     */
    private static String resolveLanguageCode(String code) {
        if (code == null) return TranslateLanguage.ENGLISH;

        switch (code) {
            // Old ISO 639 codes -> BCP-47
            case "iw": return TranslateLanguage.HEBREW;
            case "in": return TranslateLanguage.INDONESIAN;
            case "ji": return TranslateLanguage.ENGLISH; // Yiddish not supported, fallback

            // Standard codes
            case "af": return TranslateLanguage.AFRIKAANS;
            case "ar": return TranslateLanguage.ARABIC;
            case "be": return TranslateLanguage.BELARUSIAN;
            case "bg": return TranslateLanguage.BULGARIAN;
            case "bn": return TranslateLanguage.BENGALI;
            case "ca": return TranslateLanguage.CATALAN;
            case "cs": return TranslateLanguage.CZECH;
            case "cy": return TranslateLanguage.WELSH;
            case "da": return TranslateLanguage.DANISH;
            case "de": return TranslateLanguage.GERMAN;
            case "el": return TranslateLanguage.GREEK;
            case "en": return TranslateLanguage.ENGLISH;
            case "eo": return TranslateLanguage.ESPERANTO;
            case "es": return TranslateLanguage.SPANISH;
            case "et": return TranslateLanguage.ESTONIAN;
            case "fa": return TranslateLanguage.PERSIAN;
            case "fi": return TranslateLanguage.FINNISH;
            case "fr": return TranslateLanguage.FRENCH;
            case "ga": return TranslateLanguage.IRISH;
            case "gl": return TranslateLanguage.GALICIAN;
            case "gu": return TranslateLanguage.GUJARATI;
            case "he": return TranslateLanguage.HEBREW;
            case "hi": return TranslateLanguage.HINDI;
            case "hr": return TranslateLanguage.CROATIAN;
            case "ht": return TranslateLanguage.HAITIAN_CREOLE;
            case "hu": return TranslateLanguage.HUNGARIAN;
            case "id": return TranslateLanguage.INDONESIAN;
            case "is": return TranslateLanguage.ICELANDIC;
            case "it": return TranslateLanguage.ITALIAN;
            case "ja": return TranslateLanguage.JAPANESE;
            case "ka": return TranslateLanguage.GEORGIAN;
            case "kn": return TranslateLanguage.KANNADA;
            case "ko": return TranslateLanguage.KOREAN;
            case "lt": return TranslateLanguage.LITHUANIAN;
            case "lv": return TranslateLanguage.LATVIAN;
            case "mk": return TranslateLanguage.MACEDONIAN;
            case "mr": return TranslateLanguage.MARATHI;
            case "ms": return TranslateLanguage.MALAY;
            case "mt": return TranslateLanguage.MALTESE;
            case "nl": return TranslateLanguage.DUTCH;
            case "no": return TranslateLanguage.NORWEGIAN;
            case "pl": return TranslateLanguage.POLISH;
            case "pt": return TranslateLanguage.PORTUGUESE;
            case "ro": return TranslateLanguage.ROMANIAN;
            case "ru": return TranslateLanguage.RUSSIAN;
            case "sk": return TranslateLanguage.SLOVAK;
            case "sl": return TranslateLanguage.SLOVENIAN;
            case "sq": return TranslateLanguage.ALBANIAN;
            case "sv": return TranslateLanguage.SWEDISH;
            case "sw": return TranslateLanguage.SWAHILI;
            case "ta": return TranslateLanguage.TAMIL;
            case "te": return TranslateLanguage.TELUGU;
            case "th": return TranslateLanguage.THAI;
            case "tl": return TranslateLanguage.TAGALOG;
            case "tr": return TranslateLanguage.TURKISH;
            case "uk": return TranslateLanguage.UKRAINIAN;
            case "ur": return TranslateLanguage.URDU;
            case "vi": return TranslateLanguage.VIETNAMESE;
            case "zh": return TranslateLanguage.CHINESE;
            default:
                // Unknown code: return as-is and let ML Kit validate it
                Log.w(TAG, "Unknown language code: " + code + ", passing through to ML Kit");
                return code;
        }
    }
}
