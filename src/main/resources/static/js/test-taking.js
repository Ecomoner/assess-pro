// Автосохранение ответов при прохождении теста
class TestTakingManager {
    constructor(attemptId, totalQuestions) {
        this.attemptId = attemptId;
        this.totalQuestions = totalQuestions;
        this.saveStatus = document.getElementById('saveStatus');
        this.initAutoSave();
    }

    initAutoSave() {
        // Сохранение при изменении ответа
        document.querySelectorAll('input[type="radio"], input[type="checkbox"]').forEach(input => {
            input.addEventListener('change', (e) => {
                this.saveAnswer(e.target);
            });
        });
    }

    saveAnswer(input) {
        const questionId = input.name.split('_')[1];
        const answerId = input.value;

        this.updateStatus('saving');

        fetch(`/tester/attempt/${this.attemptId}/answer`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                attemptId: this.attemptId,
                questionId: questionId,
                answerOptionId: answerId
            })
        })
            .then(response => response.json())
            .then(data => {
                if (data.status === 'success') {
                    this.updateStatus('saved');
                } else {
                    this.updateStatus('error');
                }
            })
            .catch(error => {
                console.error('Error saving answer:', error);
                this.updateStatus('error');
            });
    }

    updateStatus(status) {
        if (!this.saveStatus) return;

        switch(status) {
            case 'saving':
                this.saveStatus.innerHTML = '<i class="bi bi-arrow-repeat me-1"></i>Сохранение...';
                break;
            case 'saved':
                this.saveStatus.innerHTML = '<i class="bi bi-check-circle text-success me-1"></i>Сохранено';
                setTimeout(() => {
                    this.saveStatus.innerHTML = '<i class="bi bi-save me-1"></i>Автосохранение...';
                }, 2000);
                break;
            case 'error':
                this.saveStatus.innerHTML = '<i class="bi bi-exclamation-triangle text-danger me-1"></i>Ошибка';
                setTimeout(() => {
                    this.saveStatus.innerHTML = '<i class="bi bi-save me-1"></i>Автосохранение...';
                }, 3000);
                break;
        }
    }

    finishTest() {
        const finishModal = new bootstrap.Modal(document.getElementById('finishModal'));
        finishModal.show();
    }
}

// Таймер
class TestTimer {
    constructor(minutes, seconds, attemptId) {
        this.totalSeconds = minutes * 60 + seconds;
        this.attemptId = attemptId;
        this.timerDisplay = document.getElementById('timerDisplay');
        this.interval = null;
        this.start();
    }

    start() {
        this.interval = setInterval(() => this.tick(), 1000);
    }

    tick() {
        if (this.totalSeconds <= 0) {
            this.stop();
            this.autoFinish();
            return;
        }

        this.totalSeconds--;
        this.updateDisplay();

        // Предупреждения
        if (this.totalSeconds === 300) { // 5 минут
            this.showWarning('Осталось 5 минут!');
        } else if (this.totalSeconds === 60) { // 1 минута
            this.showWarning('Осталась 1 минута!');
        }
    }

    updateDisplay() {
        const mins = Math.floor(this.totalSeconds / 60);
        const secs = this.totalSeconds % 60;
        this.timerDisplay.textContent = `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }

    showWarning(message) {
        const alert = document.createElement('div');
        alert.className = 'alert alert-warning alert-dismissible fade show position-fixed top-0 start-50 translate-middle-x mt-3';
        alert.style.zIndex = '9999';
        alert.innerHTML = `
            <i class="bi bi-exclamation-triangle-fill me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        document.body.appendChild(alert);

        setTimeout(() => alert.remove(), 5000);
    }

    stop() {
        if (this.interval) {
            clearInterval(this.interval);
            this.interval = null;
        }
    }

    autoFinish() {
        this.showWarning('Время вышло! Тест будет завершен автоматически.');
        setTimeout(() => {
            document.querySelector(`form[action="/tester/attempt/${this.attemptId}/finish"]`).submit();
        }, 2000);
    }
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    const attemptId = document.querySelector('[data-attempt-id]')?.dataset.attemptId;
    const totalQuestions = parseInt(document.querySelector('[data-total-questions]')?.dataset.totalQuestions || '0');

    if (attemptId) {
        new TestTakingManager(attemptId, totalQuestions);
    }

    const timerElement = document.getElementById('timer');
    if (timerElement) {
        const minutes = parseInt(timerElement.dataset.minutes || '0');
        const seconds = parseInt(timerElement.dataset.seconds || '0');
        const attemptId = timerElement.dataset.attemptId;

        if (minutes > 0 || seconds > 0) {
            new TestTimer(minutes, seconds, attemptId);
        }
    }
});