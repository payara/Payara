/* =========================================================
 Payara UI Shared Layer
 - Theme resolution
 - Chart.js dark defaults
 ========================================================= */

(function () {
    const css = getComputedStyle(document.documentElement);

    window.PayaraUI = {
        colors: {
            accent: css.getPropertyValue("--payara-accent").trim(),
            accentSoft: css.getPropertyValue("--payara-accent-soft").trim(),
            text: css.getPropertyValue("--payara-text").trim() || "#e5e7eb",
            muted: css.getPropertyValue("--payara-muted").trim() || "#94a3b8",
            surface: "#0b2c3f",
            border: "#174a66",
            grid: "rgba(255,255,255,0.06)"
        }
    };

    /* Chart.js global defaults */
    if (window.Chart) {
        Chart.defaults.color = PayaraUI.colors.muted;
        Chart.defaults.borderColor = PayaraUI.colors.grid;

        Chart.defaults.plugins.legend.labels.color = PayaraUI.colors.muted;
        Chart.defaults.plugins.legend.labels.font = {weight: 600};

        Chart.defaults.plugins.tooltip.backgroundColor = PayaraUI.colors.surface;
        Chart.defaults.plugins.tooltip.borderColor = PayaraUI.colors.border;
        Chart.defaults.plugins.tooltip.borderWidth = 1;
        Chart.defaults.plugins.tooltip.titleColor = "#ffffff";
        Chart.defaults.plugins.tooltip.bodyColor = PayaraUI.colors.text;

        Chart.defaults.scales.linear.grid.color = PayaraUI.colors.grid;
        Chart.defaults.scales.category.grid.color = PayaraUI.colors.grid;
    }
})();
function payaraAccentShade(label) {
    let h = 0;
    for (let i = 0; i < label.length; i++) {
        h = label.charCodeAt(i) + ((h << 5) - h);
    }
    const alpha = 0.60 + (Math.abs(h) % 40) / 100; // 0.45 → 0.85
    return `rgba(240,152,27,${alpha})`;
}
/**
 * Persist and restore search input value using localStorage
 *
 * @param {string} inputId  - ID of the input element
 * @param {string} storageKey - Unique localStorage key
 * @param {Object} [options]
 * @param {boolean} [options.fireInput=true] - Trigger input event after restore
 */
function persistSearchInput(inputId, storageKey, options = {}) {
    const {fireInput = true} = options;
debugger;
    const input = document.getElementById(inputId);
    if (!input)
        return;

    // Restore
    const saved = localStorage.getItem(storageKey);
    if (saved !== null) {
        input.value = saved;
        if (fireInput) {
            input.dispatchEvent(new Event("input", {bubbles: true}));
        }
    }

    // Persist
    input.addEventListener("input", () => {
        localStorage.setItem(storageKey, input.value);
    });
}

/**
 * Optional helper to clear stored search value
 */
function clearSearchState(storageKey) {
    localStorage.removeItem(storageKey);
}
