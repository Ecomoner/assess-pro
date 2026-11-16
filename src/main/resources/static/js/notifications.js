/* ==========================================================
   AssessPro UI — уведомления (новый shell)
   Работает с fragments/appbar.html:
     #apNotif (details) / #apNotifCount / #apNotifList /
     #apNotifClear / #apToastStack

   Старые страницы (Bootstrap dropdown) обслуживает
   inline-скрипт из fragments/footer.html — они не пересекаются.
   ========================================================== */
(function () {
    'use strict';

    var root = document.getElementById('apNotif');
    if (!root) return;

    var badge = document.getElementById('apNotifCount');
    var list  = document.getElementById('apNotifList');
    var clear = document.getElementById('apNotifClear');
    var stack = document.getElementById('apToastStack');

    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;')
            .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function setBadge(n) {
        if (!badge) return;
        if (n > 0) {
            badge.textContent = n > 99 ? '99+' : String(n);
            badge.hidden = false;
        } else {
            badge.hidden = true;
        }
    }

    function render(items) {
        if (!list) return;
        if (!items || items.length === 0) {
            list.innerHTML = '<div class="ap-pop-empty">Нет новых уведомлений</div>';
            setBadge(0);
            return;
        }
        list.innerHTML = items.map(function (n) {
            return '<a class="ap-notif" href="' + esc(n.targetUrl || '#') + '" data-id="' + esc(n.id) + '">' +
                   '<span class="ap-notif-dot"></span>' +
                   '<span class="ap-notif-body">' +
                     '<span class="ap-notif-text">' + esc(n.message) + '</span>' +
                   '</span></a>';
        }).join('');
        setBadge(items.length);
    }

    function load() {
        fetch('/api/notifications/unread', { headers: { 'Accept': 'application/json' } })
            .then(function (r) { if (!r.ok) throw new Error('load'); return r.json(); })
            .then(render)
            .catch(function () { /* тихо: колокол не критичен для работы страницы */ });
    }

    function markRead(id) {
        return fetch('/api/notifications/' + id + '/read', { method: 'POST' });
    }

    // Клик по уведомлению: отметить прочитанным и перейти
    if (list) {
        list.addEventListener('click', function (e) {
            var item = e.target.closest('.ap-notif');
            if (!item) return;
            var id = item.getAttribute('data-id');
            var href = item.getAttribute('href');
            e.preventDefault();
            var go = function () { if (href && href !== '#') window.location.href = href; };
            if (id) { markRead(id).then(go).catch(go); } else { go(); }
        });
    }

    if (clear) {
        clear.addEventListener('click', function (e) {
            e.preventDefault();
            fetch('/api/notifications/read/clear', { method: 'DELETE' })
                .then(load)
                .catch(function () { });
        });
    }

    // Закрытие панели по клику вне и по Escape
    document.addEventListener('click', function (e) {
        if (root.open && !root.contains(e.target)) root.open = false;
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') root.open = false;
    });

    // --- SSE ---
    function toast(message, url) {
        if (!stack) return;
        var el = document.createElement('div');
        el.className = 'ap-toast';
        el.innerHTML =
            '<svg viewBox="0 0 24 24" stroke-width="2" fill="none" stroke="currentColor" ' +
            'stroke-linecap="round" stroke-linejoin="round">' +
            '<path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>' +
            '<path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>' +
            '<span>' + esc(message) + '</span>';
        if (url && url !== '#') {
            el.style.cursor = 'pointer';
            el.addEventListener('click', function () { window.location.href = url; });
        }
        stack.appendChild(el);
        setTimeout(function () {
            el.style.transition = 'opacity .3s, transform .3s';
            el.style.opacity = '0';
            el.style.transform = 'translateY(6px)';
            setTimeout(function () { el.remove(); }, 320);
        }, 6000);
    }

    var es = null;
    try {
        es = new EventSource('/sse/subscribe');
        es.onmessage = function (e) {
            var n;
            try { n = JSON.parse(e.data); } catch (err) { return; }
            toast(n.message, n.targetUrl);
            load();
        };
        es.onerror = function () { /* браузер переподключится автоматически */ };
    } catch (err) { /* SSE недоступен — не критично */ }

    window.addEventListener('beforeunload', function () {
        if (es) { es.close(); es = null; }
    });

    load();
})();
