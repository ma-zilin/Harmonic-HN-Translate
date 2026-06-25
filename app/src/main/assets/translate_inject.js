/**
 * Harmonic Translate — immersive page translation injected into WebView.
 *
 * Communicates with native Android via HarmonicTranslation.translateBatch()
 * (@JavascriptInterface) which calls back into HarmonicTranslate.onTranslated().
 */
(function () {
    if (window.HarmonicTranslate && window.HarmonicTranslate.version === 1) return;

    var MODE_OVERLAY = 'overlay';
    var MODE_BILINGUAL = 'bilingual';

    var translatedNodes = [];
    var translatedOriginals = [];
    var activeMode = MODE_BILINGUAL;
    var active = false;

    // ── styles ──────────────────────────────────────────────────────────────
    var STYLE_ID = 'harmonic-translate-styles';

    function injectStyles() {
        if (document.getElementById(STYLE_ID)) return;
        var style = document.createElement('style');
        style.id = STYLE_ID;
        style.textContent =
            '.harmonic-translation {' +
            '  display: block;' +
            '  margin-top: 2px;' +
            '  margin-bottom: 6px;' +
            '}' +
            '.harmonic-translated-overlay {}';
        document.head.appendChild(style);
    }

    function removeStyles() {
        var el = document.getElementById(STYLE_ID);
        if (el) el.remove();
    }

    // ── paragraph collection ────────────────────────────────────────────────

    var SKIP_TAGS = {
        'script': 1, 'style': 1, 'code': 1, 'pre': 1,
        'nav': 1, 'header': 1, 'footer': 1, 'noscript': 1,
        'svg': 1, 'math': 1, 'textarea': 1, 'select': 1,
        'button': 1, 'input': 1, 'iframe': 1, 'canvas': 1
    };

    var BLOCK_TAGS = {
        'p': 1, 'h1': 1, 'h2': 1, 'h3': 1, 'h4': 1, 'h5': 1, 'h6': 1,
        'li': 1, 'td': 1, 'th': 1, 'div': 1, 'section': 1, 'article': 1,
        'blockquote': 1, 'figcaption': 1, 'dd': 1, 'dt': 1, 'summary': 1
    };

    function isElementVisible(el) {
        if (SKIP_TAGS[el.tagName.toLowerCase()]) return false;
        if (el.classList.contains('harmonic-translation')) return false;
        if (el.classList.contains('harmonic-translated-overlay')) return false;
        try {
            var style = window.getComputedStyle(el);
            if (style.display === 'none' || style.visibility === 'hidden') return false;
            if (parseFloat(style.opacity) === 0) return false;
        } catch (e) {
            return false;
        }
        return true;
    }

    function getDepth(el) {
        var d = 0;
        while (el && el !== document.body) { d++; el = el.parentElement; }
        return d;
    }

    function collectParagraphs() {
        var texts = [];
        var nodes = [];

        // Gather all candidate elements
        var all = document.body.getElementsByTagName('*');
        var candidates = [];
        for (var i = 0; i < all.length; i++) {
            var el = all[i];
            var tag = el.tagName.toLowerCase();
            if (!BLOCK_TAGS[tag]) continue;
            if (!isElementVisible(el)) continue;

            var text = el.textContent.trim();
            if (text.length < 15) continue;
            if (/^[\d\s.,:;!?\-–—()（）\[\]{}«»""''<>/\\|]+$/.test(text)) continue;

            candidates.push(el);
        }

        // Sort deepest-first so we collect leaves before branches
        candidates.sort(function(a, b) { return getDepth(b) - getDepth(a); });

        // Collect from deepest up; skip if ancestor already collected
        for (var j = 0; j < candidates.length; j++) {
            var cel = candidates[j];
            var skip = false;
            for (var k = 0; k < nodes.length; k++) {
                if (cel.contains(nodes[k])) { skip = true; break; }
            }
            if (skip) continue;

            // Skip nav-like elements (many links in a block)
            var links = cel.querySelectorAll('a');
            if (links.length > 8) {
                var linkChars = 0, totalChars = cel.textContent.length;
                for (var l = 0; l < links.length; l++) linkChars += links[l].textContent.length;
                if (linkChars / totalChars > 0.8) continue;
            }

            // Skip elements that contain code blocks
            var codeEls = cel.querySelectorAll('pre, code');
            if (codeEls.length > 0) {
                var codeChars = 0, allChars = cel.textContent.length;
                for (var c = 0; c < codeEls.length; c++) codeChars += codeEls[c].textContent.length;
                if (codeChars / allChars > 0.3) continue;
            }

            texts.push(cel.textContent.trim());
            nodes.push(cel);
        }

        return { texts: texts, nodes: nodes };
    }

    // ── injection ───────────────────────────────────────────────────────────

    function injectOverlay(translations) {
        for (var i = 0; i < translatedNodes.length; i++) {
            var node = translatedNodes[i];
            if (!node || !translations[i]) continue;
            translatedOriginals[i] = node.textContent;
            node.textContent = translations[i];
            node.classList.add('harmonic-translated-overlay');
        }
    }

    function injectBilingual(translations) {
        for (var i = 0; i < translatedNodes.length; i++) {
            var node = translatedNodes[i];
            if (!node || !translations[i]) continue;
            translatedOriginals[i] = node.textContent;
            var span = document.createElement('span');
            span.className = 'harmonic-translation';
            span.textContent = translations[i];
            try {
                var cs = window.getComputedStyle(node);
                span.style.fontFamily = cs.fontFamily;
                span.style.fontSize = cs.fontSize;
                span.style.fontWeight = cs.fontWeight;
                span.style.fontStyle = cs.fontStyle;
                span.style.lineHeight = cs.lineHeight;
                span.style.color = cs.color;
            } catch (e) {}
            node.parentNode.insertBefore(span, node.nextSibling);
        }
    }

    // ── restore ─────────────────────────────────────────────────────────────

    function restoreOverlay() {
        for (var i = 0; i < translatedNodes.length; i++) {
            var node = translatedNodes[i];
            if (!node) continue;
            if (translatedOriginals[i] !== undefined) {
                node.textContent = translatedOriginals[i];
            }
            node.classList.remove('harmonic-translated-overlay');
        }
    }

    function restoreBilingual() {
        var els = document.querySelectorAll('.harmonic-translation');
        for (var i = 0; i < els.length; i++) {
            els[i].remove();
        }
    }

    // ── public API ──────────────────────────────────────────────────────────

    function translatePage(fromLang, toLang, mode) {
        if (active) return;

        var result = collectParagraphs();
        if (result.texts.length === 0) {
            onError('No text paragraphs found on this page');
            return;
        }

        translatedNodes = result.nodes;
        translatedOriginals = new Array(result.nodes.length);
        activeMode = mode || MODE_BILINGUAL;
        active = true;

        var json = JSON.stringify(result.texts);
        try {
            HarmonicTranslation.translateBatch(json, fromLang, toLang);
        } catch (e) {
            onError('Translation bridge unavailable: ' + e.message);
            active = false;
        }
    }

    function onTranslated() {
        var translations = window.__harmonicTranslateData;
        window.__harmonicTranslateData = null;
        if (!translations || !Array.isArray(translations) || translations.length === 0) {
            onError('Translation returned no results');
            active = false;
            return;
        }

        if (activeMode === MODE_OVERLAY) {
            injectOverlay(translations);
        } else {
            injectBilingual(translations);
        }
    }

    function restorePage() {
        if (!active) return;

        if (activeMode === MODE_OVERLAY) {
            restoreOverlay();
        } else {
            restoreBilingual();
        }

        translatedNodes = [];
        translatedOriginals = [];
        active = false;
    }

    function onError(msg) {
        // Signal error back to native by setting a global flag
        window.__harmonicTranslateError = msg;
    }

    // ── init ────────────────────────────────────────────────────────────────

    injectStyles();

    window.HarmonicTranslate = {
        version: 1,
        translatePage: translatePage,
        restorePage: restorePage,
        onTranslated: onTranslated,
        onError: onError,
        isActive: function () { return active; }
    };
})();
