// Страница событий (creator/event-list)
if (document.getElementById('deleteModal') && document.getElementById('deleteEventName')) {
    initDeleteModal({
        modalId: 'deleteModal',
        confirmBtnId: 'confirmDeleteBtn',
        nameElementId: 'deleteEventName',
        buttonsSelector: '.delete-event-btn',
        nameDataAttr: 'name',
        formPrefix: 'deleteForm'
    });
}