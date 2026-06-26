# Changelog

## v3.0.3

- Fix Reader Mode blank screen on second article (removed global `pauseTimers()` in WebView destroy)
- Fix Reader Mode toggle clearing translation state incorrectly
- Fix Ask HN story detail text not being translated
- **On-device translation**: ML Kit Translate, English → 50 languages
- Story list translate button with loading indicator and toggle
- Comments page translate title + all comments, toggle to remove
- Reader Mode body text immersive translation (bilingual / overlay)
- Dedicated Translation settings section
- Code block auto-skip
- Header button resize for narrow screens
- ARM-only ABIs: APK 75MB → 42MB
