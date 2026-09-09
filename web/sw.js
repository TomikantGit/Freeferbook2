const BUILD_ID = "__FREEFERBOOK_BUILD__";
const SHELL_CACHE = `freeferbook-shell-${BUILD_ID}`;
const RUNTIME_CACHE = `freeferbook-runtime-${BUILD_ID}`;

const CORE_ASSETS = [
    "./",
    "./index.html",
    "./styles.css",
    "./manifest.webmanifest",
    "./js/app.js",
    "./js/db.js",
    "./js/archive.js",
    "./js/markdown.js",
    "./js/formatting.js",
    "./js/revision.js",
    "./js/diff.js"
];

self.addEventListener("install", event => {
    event.waitUntil(
        caches.open(SHELL_CACHE)
            .then(cache => cache.addAll(CORE_ASSETS))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener("activate", event => {
    event.waitUntil((async () => {
        const names = await caches.keys();
        await Promise.all(names
            .filter(name => name.startsWith("freeferbook-") && ![SHELL_CACHE, RUNTIME_CACHE].includes(name))
            .map(name => caches.delete(name)));
        await self.clients.claim();
    })());
});

async function networkFirst(request, fallbackUrl = null) {
    const runtime = await caches.open(RUNTIME_CACHE);
    try {
        const response = await fetch(request);
        if (response.ok && request.method === "GET") await runtime.put(request, response.clone());
        return response;
    } catch (error) {
        const cached = await runtime.match(request);
        if (cached) return cached;
        const shell = await caches.open(SHELL_CACHE);
        const shellCached = await shell.match(request);
        if (shellCached) return shellCached;
        if (fallbackUrl) {
            const fallback = await shell.match(fallbackUrl);
            if (fallback) return fallback;
        }
        throw error;
    }
}

self.addEventListener("fetch", event => {
    const request = event.request;
    if (request.method !== "GET") return;

    const url = new URL(request.url);
    if (url.origin !== self.location.origin) return;

    if (url.pathname.endsWith("/freeferbook-test.apk")) {
        event.respondWith(fetch(request));
        return;
    }

    if (url.pathname.endsWith("/update.json") || url.pathname.endsWith("/web-version.json")) {
        event.respondWith(networkFirst(request));
        return;
    }

    if (request.mode === "navigate") {
        event.respondWith(networkFirst(request, "./index.html"));
        return;
    }

    event.respondWith(networkFirst(request));
});
