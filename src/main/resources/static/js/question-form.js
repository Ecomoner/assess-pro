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
        const row = document.createElement('div');
        row.className = 'answer-row mb-3';
        row.innerHTML = `
            <div class="row g-2 align-items-center">
                <div class="col-auto">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" 
                               name="answerOptions[${index}].isCorrect" 
                               id="correct${index}">
                    </div>
                </div>
                <div class="col">
                    <input type="text" class="form-control" 
                           name="answerOptions[${index}].text" 
                           placeholder="Вариант ${index + 1}" required>
                </div>
                <div class="col-auto">
                    <button type="button" class="btn btn-outline-danger btn-sm remove-answer" 
                            onclick="questionManager.removeAnswer(this)">
                        <i class="bi bi-trash"></i>
                    </button>
                </div>
            </div>
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
        const checkboxes = document.querySelectorAll('input[type="checkbox"]:checked');
        if (checkboxes.length === 0) {
            e.preventDefault();
            this.showError('Выберите хотя бы один правильный ответ');
            return false;
        }

        // Проверка на пустые тексты
        const emptyInputs = Array.from(document.querySelectorAll('input[type="text"]'))
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
        alert.className = 'alert alert-danger alert-dismissible fade show';
        alert.innerHTML = `
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;

        this.form.insertBefore(alert, this.form.firstChild);

        setTimeout(() => alert.remove(), 5000);
    }
}

// Глобальный экземпляр
let questionManager;

document.addEventListener('DOMContentLoaded', function() {
    questionManager = new QuestionFormManager();
});