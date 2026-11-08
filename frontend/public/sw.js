/* Service worker minimal : sert uniquement à afficher les notifications d'alerte de stock
   (les navigateurs mobiles n'autorisent pas new Notification(), il faut passer par un service worker). */
self.addEventListener('install', () => self.skipWaiting());
self.addEventListener('activate', (event) => event.waitUntil(self.clients.claim()));
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((fenetres) => {
      for (const f of fenetres) {
        if ('focus' in f) return f.focus();
      }
      return self.clients.openWindow('/produits?vue=alertes');
    }),
  );
});
