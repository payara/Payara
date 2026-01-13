document.addEventListener('DOMContentLoaded', () => {
    loadSidePaneCounts();
    const last = localStorage.getItem("activeTab") || "CDIDevConsole";
    const btn = document.getElementById("side-" + last);

    if (btn) {
        btn.click();
    }

    startAppWatcher();
});

let lastAppValue = null;

function metadataUrl() {
    const app = DevConsoleApp.getSelectedApp();
    if (!app)
        return "metadata";
    return "metadata?app=" + encodeURIComponent(app);
}

function startAppWatcher() {
    lastAppValue = DevConsoleApp.getSelectedApp();
    setInterval(() => {
        const current = DevConsoleApp.getSelectedApp();
        if (current !== lastAppValue) {
            lastAppValue = current;
            loadSidePaneCounts();   // reload metadata when app changes
        }
    }, 5000); // 15 seconds
}
/* ========================= */

async function loadSidePaneCounts() {
    try {
        const res = await fetch(metadataUrl());

        if (!res.ok)
            return;
        const meta = await res.json();

        // Mapping metadata fields → span IDs
        const map = {
            scopedBeanCount: "count-ScopedBeans",
            producerCount: "count-Producers",
            interceptorCount: "count-Interceptors",
            interceptedClassesCount: "count-InterceptedClasses",
            decoratorCount: "count-Decorators",
            injectionPointsCount: "count-InjectionPoints",
            decoratedClassesCount: "count-DecoratedClasses",
            extensionCount: "count-Extensions",
            observerCount: "count-Observers",
            recentEventCount: "count-Events",
            beanCount: "count-Processed",
            seenTypeCount: "count-SeenTypes",
            restResourceCount: "count-RestResources",
            restMethodCount: "count-RestMethods",
            restExceptionMapperCount: "count-RestExceptionMappers",
            securityAnnotationCount: "count-SecurityAnnotations",
        };

        meta.seenTypeCount = meta.seenTypeCount ?? 0;

        Object.entries(map).forEach(([metaKey, spanId]) => {
            const el = document.getElementById(spanId);
            if (el && meta[metaKey] != null) {
                el.textContent = meta[metaKey];
                el.style.display = "inline-block";
        }
        });

    } catch (e) {
        console.error("Failed to load sidebar counts", e);
    }
}
let sidebarCollapsed = false;
function toggleSidebar() {
    const sidePane = document.getElementById("sidePane");
    const icon = document.querySelector("#collapseBtn i");

    const collapsed = sidePane.classList.toggle("collapsed");
    document.body.classList.toggle("sidebar-collapsed", collapsed);

    icon.classList.toggle("bi-chevron-left", !collapsed);
    icon.classList.toggle("bi-chevron-right", collapsed);
    
    const iframe = document.getElementById("dashboardIframe");
   const wasCollapsed = sidebarCollapsed;
sidebarCollapsed = !sidebarCollapsed;

if (wasCollapsed && !sidebarCollapsed) {
    refreshIframe(iframe);
}

}
function refreshIframe(iframe) {
    if (!iframe || !iframe.src) return;

    const currentSrc = iframe.src;

    // Preserve scroll position (optional)
    const scrollY = iframe.contentWindow?.scrollY || 0;

    iframe.src = currentSrc;

    iframe.onload = () => {
        iframe.contentWindow?.scrollTo(0, scrollY);
    };
}
