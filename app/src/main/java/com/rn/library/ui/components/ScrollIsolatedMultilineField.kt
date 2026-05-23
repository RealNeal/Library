package com.rn.library.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp

/**
 * Многострочное поле с внутренней прокруткой: не сдвигает родительский скролл при фокусе
 * и полностью изолирует скролл внутри себя, не передавая оверскролл форме.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollIsolatedMultilineField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp,
    maxHeight: Dp,
    maxLines: Int = Int.MAX_VALUE,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    placeholder: @Composable (() -> Unit)? = null,
    parentScrollState: ScrollState? = null, // Оставлен для обратной совместимости вызова
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Уведомляем форму об изменении фокуса поля
    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }

    // Изоляция Nested Scroll: перехватываем прокрутку на этапе OnPreScroll
    val fieldNestedScrollConnection = remember(isFocused) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Если поле в фокусе, полностью поглощаем вертикальный дельта-скролл,
                // не позволяя родительской Column двигаться.
                return if (isFocused) available else Offset.Zero
            }
        }
    }

    // Перехватчик BringIntoView: предотвращает прыжки экрана вверх при установке курсора
    val ignoreBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: Rect): Rect {
                // Возвращаем пустой Rect, чтобы родительский контейнер думал, что поле двигать не нужно
                return Rect.Zero
            }

            // Название метода и сигнатура приведены в соответствие с твоей версией Compose
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                // Оставляем тело пустым, тем самым блокируя автоматический скролл системы к полю
            }
        }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            // 1. Блокируем системный прыжок вверх при фокусе, передавая наш Responder в качестве аргумента
            .bringIntoViewResponder(ignoreBringIntoViewResponder)
            // 2. Блокируем передачу скролла основной форме
            .nestedScroll(fieldNestedScrollConnection),
        singleLine = false,
        maxLines = maxLines,
        interactionSource = interactionSource,
        colors = colors
    )
}

/**
 * Больше не выполняет работу, так как изоляция перенесена на уровень onPreScroll самого поля.
 * Оставлена пустой, чтобы не ломать вызовы и компиляцию в файле AddWorkScreen.kt.
 */
@Composable
fun rememberBlockParentScrollOnFocusedField(blockParentScroll: Boolean): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {}
    }
}