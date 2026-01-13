/* =========================================================
 * FQCN Utilities – shared across Dev Console dashboards
 * ========================================================= */

window.FQCN = (() => {

    function isFull(toggleBtn) {
        return toggleBtn?.getAttribute("aria-pressed") === "true";
    }

    function simpleName(fqcn) {
        if (!fqcn)
            return "";

        // Handle generics: Event<jakarta.mvc.event.MvcEvent>
        const genericStart = fqcn.indexOf("<");
        if (genericStart !== -1) {
            const rawType = fqcn.substring(0, genericStart);
            const genericPart = fqcn.substring(
                    genericStart + 1,
                    fqcn.lastIndexOf(">")
                    );

            return (
                    simpleName(rawType) +
                    "&lt;" +
                    genericPart
                    .split(",")
                    .map(p => simpleName(p.trim()))
                    .join(", ") +
                    "&gt;"
                    );
        }

        // Handle inner / nested classes
        if (fqcn.includes("$")) {
            return fqcn.substring(fqcn.lastIndexOf("$") + 1);
        }
        if (fqcn.includes("#")) {
            return fqcn.substring(fqcn.lastIndexOf("#") + 1);
        }

        // Normal class
        return fqcn.substring(fqcn.lastIndexOf(".") + 1);
    }

    function smartShort(fqcn, parts = 2) {
        if (!fqcn)
            return "";
        const p = fqcn.split(".");
        return p.slice(-parts).join(".");
    }

    function display(fqcn, toggleBtn) {
        if (!fqcn)
            return "";
        return isFull(toggleBtn) ? fqcn : simpleName(fqcn);
    }

    function displayList(list, toggleBtn) {
        if (!Array.isArray(list))
            return "";
        return list.map(n => display(n, toggleBtn)).join("<br>");
    }

    function chartName(fqcn, toggleBtn) {
        if (!fqcn)
            return "";
        return isFull(toggleBtn)
                ? smartShort(fqcn, 2)
                : simpleName(fqcn);
    }

    return {
        isFull,
        simpleName,
        smartShort,
        display,
        displayList,
        chartName
    };
})();
/* =========================================================
 * Table Sort Utilities – shared across Dev Console dashboards
 * ========================================================= */

window.TableSort = (() => {

    function createState() {
        return {index: null, dir: 1};
    }

    function defaultComparator(a, b) {
        if (typeof a === "number" && typeof b === "number") {
            return a - b;
        }
        return String(a).localeCompare(String(b), undefined, {numeric: true});
    }

    function sort(items, state, getValueFn) {
        if (state.index === null)
            return items;

        return items.slice().sort((a, b) => {
            const va = getValueFn(a, state.index);
            const vb = getValueFn(b, state.index);
            return defaultComparator(va, vb) * state.dir;
        });
    }

    function bind(tableSelector, state, onChange) {
        document.querySelectorAll(`${tableSelector} th`)
                .forEach((th, i) => {
                    th.style.cursor = "pointer";
                    th.addEventListener("click", () => {
                        state.dir = (state.index === i) ? -state.dir : 1;
                        state.index = i;
                        onChange();
                    });
                });
    }

    return {
        createState,
        sort,
        bind
    };
})();
// summary-card-utils.js
window.SummaryCards = {
    render(container, cards) {
        container.innerHTML = "";
        cards.forEach(c => {
            const d = document.createElement("div");
            d.className = "stat";
            d.style.background = c.bg;
            d.innerHTML = `
        <div class="label">${c.label}</div>
        <div class="value">${c.value}</div>
      `;
            container.appendChild(d);
        });
    }
};
