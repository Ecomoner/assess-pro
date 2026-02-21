// Сортировка таблиц
class TableSorter {
    constructor(tableSelector) {
        this.table = document.querySelector(tableSelector);
        if (!this.table) return;

        this.headers = this.table.querySelectorAll('th.sortable');
        this.currentSort = {
            column: null,
            direction: 'asc'
        };

        this.init();
    }

    init() {
        this.headers.forEach((header, index) => {
            header.addEventListener('click', () => this.sort(index));
            header.setAttribute('data-index', index);
        });
    }

    sort(columnIndex) {
        const tbody = this.table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr'));

        // Обновляем направление сортировки
        if (this.currentSort.column === columnIndex) {
            this.currentSort.direction = this.currentSort.direction === 'asc' ? 'desc' : 'asc';
        } else {
            this.currentSort.column = columnIndex;
            this.currentSort.direction = 'asc';
        }

        // Обновляем классы заголовков
        this.updateHeaderClasses(columnIndex);

        // Сортируем строки
        const sortedRows = rows.sort((a, b) => {
            const aValue = this.getCellValue(a, columnIndex);
            const bValue = this.getCellValue(b, columnIndex);

            return this.compareValues(aValue, bValue);
        });

        // Перестраиваем таблицу
        tbody.innerHTML = '';
        sortedRows.forEach(row => tbody.appendChild(row));
    }

    getCellValue(row, columnIndex) {
        const cell = row.querySelectorAll('td')[columnIndex];
        if (!cell) return '';

        // Пытаемся получить числовое значение
        const text = cell.textContent.trim();
        const number = parseFloat(text.replace(/[^\d.-]/g, ''));

        return isNaN(number) ? text : number;
    }

    compareValues(a, b) {
        let result;

        if (typeof a === 'number' && typeof b === 'number') {
            result = a - b;
        } else {
            result = String(a).localeCompare(String(b), 'ru');
        }

        return this.currentSort.direction === 'asc' ? result : -result;
    }

    updateHeaderClasses(activeIndex) {
        this.headers.forEach(header => {
            header.classList.remove('active');
            const icon = header.querySelector('i');
            if (icon) {
                icon.className = 'bi bi-arrow-down-up ms-1';
            }
        });

        const activeHeader = this.headers[activeIndex];
        activeHeader.classList.add('active');

        const icon = activeHeader.querySelector('i');
        if (icon) {
            icon.className = this.currentSort.direction === 'asc'
                ? 'bi bi-arrow-up ms-1'
                : 'bi bi-arrow-down ms-1';
        }
    }
}

// Инициализация для всех таблиц с классом .sortable-table
document.addEventListener('DOMContentLoaded', function() {
    new TableSorter('#usersTable');
    new TableSorter('#testsTable');
    new TableSorter('#historyTable');
});