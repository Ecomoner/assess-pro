/* ==========================================================
   AssessPro — CSRF-обвязка для клиента

   Зачем: при включённом CSRF Spring Security отклоняет любой
   POST/PUT/DELETE/PATCH без токена (403). Формы на th:action
   токен получают автоматически (CsrfRequestDataValueProcessor),
   а вот fetch / jQuery / htmx и формы с action, собранным в JS,
   нужно покрыть вручную. Этот файл делает это централизованно,
   чтобы не править каждый вызов на каждой странице.

   Токен берётся из <meta name="_csrf">, которые отдаёт
   fragments/header.html.

   Файл безопасен и при выключенном CSRF: если мета-тегов нет,
   он ничего не делает.
   ========================================================== */
(function () {
    'use strict';

    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    var paramMeta = document.querySelector('meta[name="_csrf_parameter"]');

    var token = tokenMeta ? tokenMeta.getAttribute('content') : '';
    var headerName = (headerMeta && headerMeta.getAttribute('content')) || 'X-CSRF-TOKEN';
    var paramName = (paramMeta && paramMeta.getAttribute('content')) || '_csrf';

    // Доступно другим скриптам страницы
    window.AP_CSRF = { token: token, header: headerName, param: paramName };

    if (!token) {
        return; // CSRF выключен на сервере — вмешиваться незачем
    }

    function isUnsafe(method) {
        return !!method && !/^(GET|HEAD|OPTIONS|TRACE)$/i.test(method);
    }

    /* ---------- fetch ---------- */
    if (window.fetch) {
        var originalFetch = window.fetch;
        window.fetch = function (input, init) {
            init = init || {};

            var method = init.method ||
                (typeof input === 'object' && input && input.method) || 'GET';

            if (isUnsafe(method)) {
                var headers = new Headers(
                    init.headers ||
                    (typeof input === 'object' && input ? input.headers : undefined)
                );
                if (!headers.has(headerName)) {
                    headers.set(headerName, token);
                }
                init = Object.assign({}, init, { headers: headers });
            }

            return originalFetch.call(this, input, init);
        };
    }

    /* ---------- jQuery.ajax ---------- */
    if (window.jQuery) {
        window.jQuery(document).ajaxSend(function (event, jqxhr, settings) {
            if (isUnsafe(settings.type || settings.method)) {
                jqxhr.setRequestHeader(headerName, token);
            }
        });
    }

    /* ---------- htmx ---------- */
    document.addEventListener('htmx:configRequest', function (event) {
        if (event.detail && event.detail.headers) {
            event.detail.headers[headerName] = token;
        }
    });

    /* ---------- обычные формы ----------
       Покрывает формы, у которых action выставляется из JS
       (admin/project-list, admin/user-list), и любые будущие
       формы без th:action. Формы с th:action уже содержат поле
       от Spring — повторно не добавляем. */
    function injectIntoForms(root) {
        var forms = (root || document).querySelectorAll('form');
        Array.prototype.forEach.call(forms, function (form) {
            var method = (form.getAttribute('method') || 'get').toUpperCase();
            if (method !== 'POST') return;
            if (form.querySelector('input[name="' + paramName + '"]')) return;

            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = paramName;
            input.value = token;
            form.appendChild(input);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { injectIntoForms(document); });
    } else {
        injectIntoForms(document);
    }

    // Диалоги и формы, добавляемые динамически
    window.AP_injectCsrf = injectIntoForms;
})();
