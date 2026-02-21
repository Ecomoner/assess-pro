// Автодополнение поиска тестов
class SearchAutocomplete {
    constructor(inputSelector, resultsSelector, searchUrl) {
        this.input = document.querySelector(inputSelector);
        this.resultsContainer = document.querySelector(resultsSelector);
        this.searchUrl = searchUrl;
        this.minChars = 2;
        this.debounceTimer = null;

        this.init();
    }

    init() {
        if (!this.input) return;

        this.input.addEventListener('input', () => {
            clearTimeout(this.debounceTimer);
            this.debounceTimer = setTimeout(() => this.search(), 300);
        });

        // Закрытие при клике вне
        document.addEventListener('click', (e) => {
            if (!this.input.contains(e.target) && !this.resultsContainer?.contains(e.target)) {
                this.hideResults();
            }
        });
    }

    async search() {
        const query = this.input.value.trim();

        if (query.length < this.minChars) {
            this.hideResults();
            return;
        }

        try {
            const response = await fetch(`${this.searchUrl}?term=${encodeURIComponent(query)}&limit=5`);
            const results = await response.json();
            this.displayResults(results);
        } catch (error) {
            console.error('Search error:', error);
        }
    }

    displayResults(results) {
        if (!this.resultsContainer) return;

        if (results.length === 0) {
            this.resultsContainer.innerHTML = `
                <div class="list-group-item text-muted">
                    <i class="bi bi-info-circle me-2"></i>Ничего не найдено
                </div>
            `;
        } else {
            this.resultsContainer.innerHTML = results.map(item => `
                <a href="${item.url}" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center">
                    <div>
                        <strong>${this.highlight(item.title, this.input.value)}</strong>
                        <br>
                        <small class="text-muted">${item.categoryName || 'Без категории'}</small>
                    </div>
                    <span class="badge bg-success rounded-pill">${item.questionCount} вопросов</span>
                </a>
            `).join('');
        }

        this.resultsContainer.style.display = 'block';
    }

    highlight(text, query) {
        if (!query) return text;
        const regex = new RegExp(`(${query})`, 'gi');
        return text.replace(regex, '<mark>$1</mark>');
    }

    hideResults() {
        if (this.resultsContainer) {
            this.resultsContainer.style.display = 'none';
        }
    }
}

// Инициализация
document.addEventListener('DOMContentLoaded', function() {
    new SearchAutocomplete(
        '#searchInput',
        '#searchResults',
        '/tester/tests/search/quick'
    );
});