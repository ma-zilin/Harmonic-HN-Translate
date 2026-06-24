package com.simon.harmonichackernews.network;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.HashMap;
import java.util.Map;

public class TranslationManager {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static final Map<String, Translator> translatorCache = new HashMap<>();

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onFailure(String error);
    }

    public interface ModelDownloadCallback {
        void onProgress(long downloadedBytes, long totalBytes);
        void onComplete(boolean success);
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

        String cacheKey = sourceLang + "→" + targetLang;
        Translator translator = translatorCache.get(cacheKey);

        if (translator == null) {
            TranslatorOptions options;
            if ("auto".equals(sourceLang)) {
                options = new TranslatorOptions.Builder()
                        .setTargetLanguage(translateLanguageForCode(targetLang))
                        .build();
            } else {
                options = new TranslatorOptions.Builder()
                        .setSourceLanguage(translateLanguageForCode(sourceLang))
                        .setTargetLanguage(translateLanguageForCode(targetLang))
                        .build();
            }
            translator = Translation.getClient(options);
            translatorCache.put(cacheKey, translator);
        }

        Translator finalTranslator = translator;
        String trimmedText = text.trim();
        finalTranslator.downloadModelIfNeeded()
                .addOnSuccessListener(unused -> {
                    finalTranslator.translate(trimmedText)
                            .addOnSuccessListener(translatedText -> {
                                MAIN_HANDLER.post(() -> callback.onSuccess(translatedText));
                            })
                            .addOnFailureListener(e -> {
                                MAIN_HANDLER.post(() -> callback.onFailure(
                                        "Translation failed: " + e.getMessage()));
                            });
                })
                .addOnFailureListener(e -> {
                    MAIN_HANDLER.post(() -> callback.onFailure(
                            "Model download failed: " + e.getMessage()));
                });
    }

    public static void downloadModel(String sourceLang, String targetLang,
                                      ModelDownloadCallback callback) {
        String cacheKey = sourceLang + "→" + targetLang;
        Translator translator = translatorCache.get(cacheKey);

        if (translator == null) {
            TranslatorOptions options;
            if ("auto".equals(sourceLang)) {
                options = new TranslatorOptions.Builder()
                        .setTargetLanguage(translateLanguageForCode(targetLang))
                        .build();
            } else {
                options = new TranslatorOptions.Builder()
                        .setSourceLanguage(translateLanguageForCode(sourceLang))
                        .setTargetLanguage(translateLanguageForCode(targetLang))
                        .build();
            }
            translator = Translation.getClient(options);
            translatorCache.put(cacheKey, translator);
        }

        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        Translator finalTranslator = translator;
        finalTranslator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    MAIN_HANDLER.post(() -> callback.onComplete(true));
                })
                .addOnFailureListener(e -> {
                    MAIN_HANDLER.post(() -> callback.onComplete(false));
                });
    }

    private static String translateLanguageForCode(String code) {
        switch (code) {
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
            default: return code;
        }
    }
}
