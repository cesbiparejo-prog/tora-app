const C='tora-v3',F=['./','index.html','app.js','style.css','manifest.webmanifest','icon-192.png','icon-512.png','data/books.json','data/lexicon.json','data/translit.json','data/Gen.json','data/Exod.json','data/Lev.json','data/Num.json','data/Deut.json'];
self.oninstall=e=>e.waitUntil(caches.open(C).then(c=>c.addAll(F)).then(()=>self.skipWaiting()));
self.onactivate=e=>e.waitUntil(caches.keys().then(k=>Promise.all(k.filter(x=>x!=C).map(x=>caches.delete(x)))).then(()=>self.clients.claim()));
self.onfetch=e=>e.respondWith(caches.match(e.request,{ignoreSearch:true}).then(r=>r||fetch(e.request)));
