
function openPane(evt, tabName, id) {
    const iframe = document.getElementById('dashboardIframe');
    const iframeSources = {
        CDIDevConsole: "components/cdi-dashboard.html",
        MetricsDashboard: "components/metrics-dashboard.html",
        BeanConsole: "components/bean-dashboard.html",
        ScopedBeans: "components/scoped-beans-dashboard.html",
        Producers: "components/producers-dashboard.html",
        Extensions: "components/extensions-dashboard.html",
        Events: "components/events-dashboard.html",
        Observers: "components/observers-dashboard.html",
        RestMethods: "components/rest-endpoints-dashboard.html",
        RestExceptionMappers: "components/rest-exception-mappers.html",
        Interceptors: "components/interceptors-dashboard.html",
        InterceptedClasses: "components/intercepted-classes-dashboard.html",
        Decorators: "components/decorators-dashboard.html",
        DecoratedClasses: "components/decorated-classes-dashboard.html",
        InjectionPoints: "components/injection-points-dashboard.html",
        SecurityAnnotations: "components/security-audit-dashboard.html"
    };
    if (tabName === 'BeanConsole') {
        iframe.src = iframeSources[tabName] + "?bean=" + encodeURIComponent(id);
    } else {
        iframe.src = iframeSources[tabName];
    }


    const tabcontent = document.getElementsByClassName('tabcontent');
    for (const tab of tabcontent) {
        tab.style.display = 'none';
        tab.setAttribute('aria-hidden', 'true');
    }

    const sideButtons = document.getElementsByClassName('sideButton');
    for (const btn of sideButtons) {
        btn.classList.remove('active');
        btn.setAttribute('aria-selected', 'false');
    }

    const activeTab = document.getElementById(tabName);
    if (activeTab) {
        activeTab.style.display = 'block';
        activeTab.setAttribute('aria-hidden', 'false');
    }

    if (evt && evt.currentTarget) {
        evt.currentTarget.classList.add('active');
        evt.currentTarget.setAttribute('aria-selected', 'true');
        evt.currentTarget.focus();
    }

    localStorage.setItem('activeTab', tabName);

}

document.getElementById('sidePane').addEventListener('keydown', e => {
    const btns = Array.from(document.querySelectorAll('.sideButton'));
    let idx = btns.findIndex(b => b.getAttribute('aria-selected') === 'true');
    if (e.key === 'ArrowDown') {
        idx = (idx + 1) % btns.length;
        btns[idx].focus();
        btns[idx].click();
        e.preventDefault();
    } else if (e.key === 'ArrowUp') {
        idx = (idx - 1 + btns.length) % btns.length;
        btns[idx].focus();
        btns[idx].click();
        e.preventDefault();
    }
});