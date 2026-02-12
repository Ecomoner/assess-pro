// Базовые функции валидации
const Validation = {

    // Валидация имени пользователя
    validateUsername: function(username) {
        if (!username || username.trim().length < 3) {
            return { valid: false, message: 'Имя пользователя должно быть не менее 3 символов' };
        }

        if (username.trim().length > 50) {
            return { valid: false, message: 'Имя пользователя должно быть не более 50 символов' };
        }

        const regex = /^[a-zA-Z0-9_]+$/;
        if (!regex.test(username)) {
            return { valid: false, message: 'Можно использовать только буквы, цифры и подчеркивания' };
        }

        return { valid: true, message: '' };
    },

    // Валидация пароля
    validatePassword: function(password) {
        if (!password || password.length < 6) {
            return { valid: false, message: 'Пароль должен быть не менее 6 символов' };
        }

        if (password.length > 100) {
            return { valid: false, message: 'Пароль должен быть не более 100 символов' };
        }

        // Проверка сложности
        const hasLetter = /[a-zA-Z]/.test(password);
        const hasNumber = /\d/.test(password);

        if (!hasLetter || !hasNumber) {
            return { valid: false, message: 'Пароль должен содержать буквы и цифры' };
        }

        return { valid: true, message: '' };
    },

    // Валидация совпадения паролей
    validatePasswordConfirmation: function(password, confirmPassword) {
        if (password !== confirmPassword) {
            return { valid: false, message: 'Пароли не совпадают' };
        }
        return { valid: true, message: '' };
    },

    // Валидация названия теста
    validateTestTitle: function(title) {
        if (!title || title.trim().length < 3) {
            return { valid: false, message: 'Название теста должно быть не менее 3 символов' };
        }

        if (title.trim().length > 200) {
            return { valid: false, message: 'Название теста должно быть не более 200 символов' };
        }

        return { valid: true, message: '' };
    },

    // Валидация описания
    validateDescription: function(description) {
        if (description && description.length > 1000) {
            return { valid: false, message: 'Описание не должно превышать 1000 символов' };
        }
        return { valid: true, message: '' };
    },

    // Валидация лимита времени
    validateTimeLimit: function(minutes) {
        const num = parseInt(minutes);
        if (isNaN(num) || num < 0) {
            return { valid: false, message: 'Лимит времени не может быть отрицательным' };
        }

        if (num > 300) {
            return { valid: false, message: 'Максимальный лимит времени - 300 минут' };
        }

        return { valid: true, message: '' };
    },

    // Валидация текста вопроса
    validateQuestionText: function(text) {
        if (!text || text.trim().length < 5) {
            return { valid: false, message: 'Текст вопроса должен быть не менее 5 символов' };
        }

        if (text.trim().length > 1000) {
            return { valid: false, message: 'Текст вопроса должен быть не более 1000 символов' };
        }

        return { valid: true, message: '' };
    },

    // Валидация варианта ответа
    validateAnswerOption: function(text) {
        if (!text || text.trim().length === 0) {
            return { valid: false, message: 'Текст варианта ответа не может быть пустым' };
        }

        if (text.trim().length > 500) {
            return { valid: false, message: 'Текст варианта ответа должен быть не более 500 символов' };
        }

        return { valid: true, message: '' };
    },

    // Показать сообщение об ошибке
    showError: function(element, message) {
        const feedbackElement = element.nextElementSibling;
        if (feedbackElement && feedbackElement.classList.contains('invalid-feedback')) {
            feedbackElement.textContent = message;
            element.classList.add('is-invalid');
        }
    },

    // Убрать сообщение об ошибке
    hideError: function(element) {
        element.classList.remove('is-invalid');
    },

    // Валидация всей формы регистрации
    validateRegistrationForm: function(form) {
        let isValid = true;

        const username = form.querySelector('#username').value;
        const password = form.querySelector('#password').value;
        const confirmPassword = form.querySelector('#confirmPassword').value;

        // Валидация имени пользователя
        const usernameValidation = this.validateUsername(username);
        if (!usernameValidation.valid) {
            this.showError(form.querySelector('#username'), usernameValidation.message);
            isValid = false;
        } else {
            this.hideError(form.querySelector('#username'));
        }

        // Валидация пароля
        const passwordValidation = this.validatePassword(password);
        if (!passwordValidation.valid) {
            this.showError(form.querySelector('#password'), passwordValidation.message);
            isValid = false;
        } else {
            this.hideError(form.querySelector('#password'));
        }

        // Валидация подтверждения пароля
        const confirmValidation = this.validatePasswordConfirmation(password, confirmPassword);
        if (!confirmValidation.valid) {
            this.showError(form.querySelector('#confirmPassword'), confirmValidation.message);
            isValid = false;
        } else {
            this.hideError(form.querySelector('#confirmPassword'));
        }

        return isValid;
    }
};

// Инициализация валидации на всех формах
document.addEventListener('DOMContentLoaded', function() {
    // Регистрация
    const registrationForm = document.getElementById('registrationForm');
    if (registrationForm) {
        registrationForm.addEventListener('submit', function(e) {
            if (!Validation.validateRegistrationForm(this)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    // Создание теста
    const testForm = document.getElementById('testForm');
    if (testForm) {
        testForm.addEventListener('submit', function(e) {
            let isValid = true;

            const title = this.querySelector('#title').value;
            const description = this.querySelector('#description').value;
            const timeLimit = this.querySelector('#timeLimitMinutes').value;

            // Валидация названия
            const titleValidation = Validation.validateTestTitle(title);
            if (!titleValidation.valid) {
                Validation.showError(this.querySelector('#title'), titleValidation.message);
                isValid = false;
            }

            // Валидация описания
            const descValidation = Validation.validateDescription(description);
            if (!descValidation.valid) {
                Validation.showError(this.querySelector('#description'), descValidation.message);
                isValid = false;
            }

            // Валидация лимита времени
            const timeValidation = Validation.validateTimeLimit(timeLimit);
            if (!timeValidation.valid) {
                Validation.showError(this.querySelector('#timeLimitMinutes'), timeValidation.message);
                isValid = false;
            }

            if (!isValid) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }

    // Форма вопроса
    const questionForm = document.getElementById('questionForm');
    if (questionForm) {
        questionForm.addEventListener('submit', function(e) {
            let isValid = true;

            // Проверка текста вопроса
            const questionText = this.querySelector('#text').value;
            const questionValidation = Validation.validateQuestionText(questionText);
            if (!questionValidation.valid) {
                Validation.showError(this.querySelector('#text'), questionValidation.message);
                isValid = false;
            }

            // Проверка вариантов ответов
            const answerInputs = this.querySelectorAll('input[name$="].text"]');
            let hasValidAnswers = 0;
            let hasCorrectAnswer = false;

            answerInputs.forEach((input, index) => {
                const answerValidation = Validation.validateAnswerOption(input.value);
                if (answerValidation.valid && input.value.trim() !== '') {
                    hasValidAnswers++;

                    // Проверяем, выбран ли этот вариант как правильный
                    const checkbox = this.querySelector(`input[name="answerOptions[${index}].isCorrect"]`);
                    if (checkbox && checkbox.checked) {
                        hasCorrectAnswer = true;
                    }
                }
            });

            if (hasValidAnswers < 2) {
                alert('Добавьте как минимум 2 варианта ответа');
                isValid = false;
            }

            if (!hasCorrectAnswer) {
                alert('Отметьте хотя бы один правильный вариант ответа');
                isValid = false;
            }

            if (!isValid) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    }
});