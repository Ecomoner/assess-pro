/* ==========================================================
   AssessPro UI — общий JS
   ========================================================== */
(function () {
    'use strict';

    function initSidebar() {
        const toggle  = document.getElementById('apMenuToggle');
        const close   = document.getElementById('apSideClose');
        const side    = document.getElementById('apSide');
        const overlay = document.getElementById('apOverlay');

        if (!side || !overlay) return;

        function open() {
            side.classList.add('ap-open');
            overlay.classList.add('ap-show');
            document.body.style.overflow = 'hidden';
            if (toggle) toggle.setAttribute('aria-expanded', 'true');
        }

        function closeMenu() {
            side.classList.remove('ap-open');
            overlay.classList.remove('ap-show');
            document.body.style.overflow = '';
            if (toggle) toggle.setAttribute('aria-expanded', 'false');
        }

        toggle?.addEventListener('click', open);
        close?.addEventListener('click', closeMenu);
        overlay?.addEventListener('click', closeMenu);

        side.querySelectorAll('.ap-nav-item').forEach(function (link) {
            link.addEventListener('click', closeMenu);
        });

        window.addEventListener('resize', function () {
            if (window.innerWidth > 900) closeMenu();
        });

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') closeMenu();
        });
    }

    function animateScoreBars() {
        document.querySelectorAll('.ap-score-bar i').forEach(function (bar) {
            const w = bar.style.width;
            bar.style.width = '0';
            setTimeout(function () { bar.style.width = w; }, 250);
        });
    }

    document.addEventListener('DOMContentLoaded', function () {
        initSidebar();
        animateScoreBars();
    });
})();