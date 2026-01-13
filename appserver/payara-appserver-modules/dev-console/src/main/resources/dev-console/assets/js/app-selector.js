/* ==========================================================
 Payara Dev Console – Application Selector (Shared)
 ========================================================== */

(function () {
    const APP_KEY = "payara.devconsole.selectedApp";

    function getSelectedApp() {
        return localStorage.getItem(APP_KEY) || "";
    }

    function setSelectedApp(id) {
        localStorage.setItem(APP_KEY, id);
    }

    function api(path) {
        const app = getSelectedApp();
        return app ? `${path}?app=${encodeURIComponent(app)}` : `${path}`;
    }

    async function loadApplications(selectElementId, onChange) {
        const sel = document.getElementById(selectElementId);
        if (!sel)
            return;

        const apps = await fetch("../applications").then(r => r.json());

        // Populate dropdown
        sel.innerHTML = "";
        apps.forEach(a => {
            const opt = document.createElement("option");
            opt.value = a.id;
            opt.textContent = a.name;
            sel.appendChild(opt);
        });

        const saved = getSelectedApp();

        // 1. If storage has value → trust storage
        if (saved) {
            sel.value = saved;
        }

        // 2. If UI has no value → fallback to first app
        if (!sel.value && apps.length) {
            sel.value = apps[0].id;
        }

        // 3. If UI has value but storage is empty → persist UI value
        if (sel.value && !saved) {
            setSelectedApp(sel.value);
        }

        // Handle user change
        sel.onchange = () => {
            setSelectedApp(sel.value);
            if (onChange)
                onChange(sel.value);
        };
    }

    // Expose globally
    window.DevConsoleApp = {
        api,
        loadApplications,
        getSelectedApp
    };
})();
