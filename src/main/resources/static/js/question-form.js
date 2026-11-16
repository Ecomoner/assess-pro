// Управление формой создания/редактирования вопроса
class QuestionFormManager {
    constructor(maxAnswers = 10, minAnswers = 2) {
        this.maxAnswers = maxAnswers;
        this.minAnswers = minAnswers;
        this.answerCount = document.querySelectorAll('.answer-row').length;
        this.container = document.getElementById('answersContainer');
        this.form = document.getElementById('questionForm');

        this.init();
    }

    init() {
        this.form.addEventListener('submit', (e) => this.validateForm(e));
    }

    addAnswer() {
        if (this.answerCount >= this.maxAnswers) {
            this.showError(`Максимум ${this.maxAnswers} вариантов ответа`);
            return;
        }

        const newRow = this.createAnswerRow(this.answerCount);
        this.container.appendChild(newRow);
        this.answerCount++;
        this.updateIndexes();
    }

    createAnswerRow(index) {
        // Разметка повторяет серверную (creator/question-form.html), чтобы
        // добавленный вариант выглядел так же, как отрисованный Thymeleaf.
        const row = document.createElement('div');
        row.className = 'answer-row';
        row.setAttribute('style',
            'display:flex;align-items:flex-start;gap:12px;padding:12px 14px;' +
            'border:1px solid var(--ap-line);border-radius:14px;background:var(--ap-surface)');

        row.innerHTML = `
            <label class="ap-check" style="flex:0 0 auto;padding-top:9px" title="Правильный ответ">
                <input type="checkbox"
                       name="answerOptions[${index}].isCorrect"
                       id="correct${index}">
                <span class="ap-visually-hidden">Правильный ответ</span>
            </label>

            <div class="ap-field ap-grow">
                <input type="text"
                       class="ap-input"
                       name="answerOptions[${index}].text"
                       placeholder="Вариант ${index + 1}"
                       required>
            </div>

            <button type="button"
                    class="ap-btn ap-btn-icon ap-btn-sm remove-answer"
                    style="margin-top:4px"
                    onclick="questionManager.removeAnswer(this)"
                    title="Удалить вариант" aria-label="Удалить вариант">
                <svg viewBox="0 0 24 24" stroke-width="2" fill="none" stroke="currentColor"
                     stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6m5 0V4a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2"/>
                </svg>
            </button>
        `;

        return row;
    }

    removeAnswer(button) {
        const row = button.closest('.answer-row');

        if (this.container.children.length <= this.minAnswers) {
            this.showError(`Минимум ${this.minAnswers} варианта ответа`);
            return;
        }

        row.remove();
        this.answerCount--;
        this.updateIndexes();
    }

    updateIndexes() {
        const rows = this.container.children;
        for (let i = 0; i < rows.length; i++) {
            const checkbox = rows[i].querySelector('input[type="checkbox"]');
            const textInput = rows[i].querySelector('input[type="text"]');

            checkbox.name = `answerOptions[${i}].isCorrect`;
            checkbox.id = `correct${i}`;
            textInput.name = `answerOptions[${i}].text`;
            textInput.placeholder = `Вариант ${i + 1}`;
        }
    }

    validateForm(e) {
        // Считаем только чекбоксы внутри контейнера вариантов: иначе любой
        // другой чекбокс на странице (например, «активна») пройдёт проверку
        // «выбран хотя бы один правильный ответ».
        const checkboxes = this.container.querySelectorAll('input[type="checkbox"]:checked');
        if (checkboxes.length === 0) {
            e.preventDefault();
            this.showError('Выберите хотя бы один правильный ответ');
            return false;
        }

        // Проверка на пустые тексты
        const emptyInputs = Array.from(this.container.querySelectorAll('input[type="text"]'))
            .filter(input => !input.value.trim());

        if (emptyInputs.length > 0) {
            e.preventDefault();
            this.showError('Все варианты ответов должны быть заполнены');
            return false;
        }

        return true;
    }

    showError(message) {
        const alert = document.createElement('div');
        alert.className = 'ap-alert ap-alert-error';
        alert.setAttribute('role', 'alert');
        alert.innerHTML =
            '<svg viewBox="0 0 24 24" stroke-width="2" fill="none" stroke="currentColor" ' +
            'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
            '<path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/>' +
            '<line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>' +
            '<span></span>';
        alert.querySelector('span').textContent = message;

        this.form.insertBefore(alert, this.form.firstChild);

        setTimeout(() => alert.remove(), 5000);
    }
}

// Глобальный экземпляр
let questionManager;

document.addEventListener('DOMContentLoaded', function() {
    // Скрипт подключается только на странице вопроса, но защищаемся от
    // случаев, когда разметка изменилась или форма не отрисована.
    if (!document.getElementById('answersContainer') || !document.getElementById('questionForm')) {
        return;
    }
    questionManager = new QuestionFormManager();
});