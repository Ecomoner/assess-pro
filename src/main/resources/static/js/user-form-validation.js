// user-form-validation.js – валидация формы пользователя (создание/редактирование)

(function() {
    'use strict';

    // Находим все формы с классом needs-validation
    const forms = document.querySelectorAll('.needs-validation');

    Array.prototype.slice.call(forms).forEach(function(form) {
        // Определяем режим (create/edit) из data-атрибута
        const action = form.dataset.action;

        // Элементы для режима создания
        const passwordCreate = document.getElementById('password-create');
        const confirmCreate = document.getElementById('password-confirm-create');

        // Элементы для режима редактирования
        const passwordEdit = document.getElementById('password-edit');
        const confirmBlock = document.getElementById('confirm-password-block');
        const confirmEdit = document.getElementById('password-confirm-edit');

        // Для режима редактирования: показываем поле подтверждения, если введён новый пароль
        if (action === 'edit' && passwordEdit) {
            passwordEdit.addEventListener('input', function() {
                if (this.value.trim() !== '') {
                    if (confirmBlock) confirmBlock.style.display = 'block';
                    if (confirmEdit) confirmEdit.required = true;
                } else {
                    if (confirmBlock) confirmBlock.style.display = 'none';
                    if (confirmEdit) {
                        confirmEdit.required = false;
                        confirmEdit.classList.remove('is-invalid');
                    }
                }
            });
        }

        // Обработка отправки формы
        form.addEventListener('submit', function(event) {
            let passwordsMatch = true;

            // Проверка совпадения паролей
            if (action === 'create') {
                if (passwordCreate && confirmCreate) {
                    if (passwordCreate.value !== confirmCreate.value) {
                        passwordsMatch = false;
                        confirmCreate.classList.add('is-invalid');
                    } else {
                        confirmCreate.classList.remove('is-invalid');
                    }
                }
            } else if (action === 'edit') {
                if (passwordEdit && passwordEdit.value.trim() !== '') {
                    if (confirmEdit && passwordEdit.value !== confirmEdit.value) {
                        passwordsMatch = false;
                        confirmEdit.classList.add('is-invalid');
                    } else if (confirmEdit) {
                        confirmEdit.classList.remove('is-invalid');
                    }
                }
            }

            // Стандартная Bootstrap-валидация
            if (!form.checkValidity() || !passwordsMatch) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });
})();