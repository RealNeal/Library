package com.rn.library.ui.screens

import com.rn.library.data.Work

/**
 * Единое состояние оверлея «Добавить / редактировать произведение».
 * Заменяет пару showAddWorkScreen + editingWork, которые могли расходиться.
 */
internal sealed interface WorkEditorOverlay {
    data object Hidden : WorkEditorOverlay

    data object Create : WorkEditorOverlay

    /** [returnToDetailOnDismiss] — вернуться к экрану просмотра по «Назад» без сохранения. */
    data class Edit(
        val work: Work,
        val returnToDetailOnDismiss: Boolean,
    ) : WorkEditorOverlay
}

internal val WorkEditorOverlay.isVisible: Boolean
    get() = this !is WorkEditorOverlay.Hidden

internal val WorkEditorOverlay.editingWorkOrNull: Work?
    get() = (this as? WorkEditorOverlay.Edit)?.work
