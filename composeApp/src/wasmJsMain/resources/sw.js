const CACHE = 'slides-v1';
const CORE_ASSETS = [
    './',
    'index.html',
    'styles.css',
    'manifest.webmanifest',
    'icon.svg',
];

self.addEventListener('install', (event) => {
    event.waitUntil((async () => {
        const cache = await caches.open(CACHE);
        await cache.addAll(CORE_ASSETS);
        self.skipWaiting();
    })());
});

self.addEventListener('activate', (event) => {
    event.waitUntil((async () => {
        const keys = await caches.keys();
        await Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)));
        await self.clients.claim();
    })());
});

self.addEventListener('fetch', (event) => {
    const req = event.request;
    if (req.method !== 'GET') return;
    event.respondWith((async () => {
        const cache = await caches.open(CACHE);
        const cached = await cache.match(req);
        if (cached) return cached;
        try {
            const resp = await fetch(req);
            if (resp && resp.ok && resp.type === 'basic') {
                cache.put(req, resp.clone());
            }
            return resp;
        } catch (err) {
            if (cached) return cached;
            throw err;
        }
    })());
});
