package com.rn.library.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
<<<<<<< Updated upstream
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
=======
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
>>>>>>> Stashed changes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
<<<<<<< Updated upstream
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
=======
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
>>>>>>> Stashed changes

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
<<<<<<< Updated upstream
    parentScrollState: ScrollState? = null, // Оставлен для обратной совместимости вызова
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
=======
    parentScrollState: ScrollState? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isFocused by remember { mutableStateOf(false) }
    var frozenScroll by remember { mutableIntStateOf(0) }

    if (parentScrollState != null) {
        LaunchedEffect(parentScrollState, isFocused) {
            if (!isFocused) return@LaunchedEffect
            snapshotFlow { parentScrollState.value }
                .collect { current ->
                    if (current != frozenScroll) {
                        parentScrollState.scrollTo(frozenScroll)
                    }
                }
        }
    }
>>>>>>> Stashed changes

    // Уведомляем форму об изменении фокуса поля
    LaunchedEffect(isFocused) {
        onFocusChanged(isFocused)
    }
<<<<<<< Updated upstream

    // Изоляция Nested Scroll: перехватываем прокрутку на этапе OnPreScroll
    val fieldNestedScrollConnection = remember(isFocused) {
=======
    SideEffect {
        if (textFieldValue.text != value) {
            val selection = textFieldValue.selection
            val clampedSelection = if (selection.start <= value.length && selection.end <= value.length) {
                selection
            } else {
                TextRange(value.length)
            }
            textFieldValue = TextFieldValue(text = value, selection = clampedSelection)
        }
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    // Изоляция ручного скролла: поглощаем весь "остаточный" скролл,
    // чтобы он не дергал родительский ScrollState (экрана), когда текст кончается.
    val fieldNestedScrollConnection = remember {
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream

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
=======
    val bringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override suspend fun bringChildIntoView(localRect: () -> Rect?) {
                // Поглощаем запрос bringIntoView при фокусе, чтобы родительский verticalScroll не дёргался
            }

            override fun calculateRectForParent(localRect: Rect): Rect {
                // Курсор в длинном тексте может иметь localRect далеко за пределами видимой области
                // (координаты всего содержимого). Передаём родителю только границы контейнера поля,
                // чтобы форма не пыталась прокрутиться к строке внутри поля — это делает сам TextField.
                val size = containerSize
                return if (size != IntSize.Zero) {
                    Rect(Offset.Zero, Size(size.width.toFloat(), size.height.toFloat()))
                } else {
                    localRect
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerSize = it.size }
            .bringIntoViewResponder(bringIntoViewResponder)
            .then(
                if (parentScrollState != null) {
                    Modifier.pointerInput(parentScrollState) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            frozenScroll = parentScrollState.value
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        OutlinedTextField(
            value = textFieldValue, // Передаем объект с памятью о курсоре
            onValueChange = { newValue ->
                textFieldValue = newValue // Сохраняем курсор локально
                // Отправляем наверх только текст, как и ожидает родительский экран
                if (newValue.text != value) {
                    onValueChange(newValue.text)
                }
            },
            label = label,
            placeholder = placeholder,
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(fieldNestedScrollConnection)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        frozenScroll = parentScrollState?.value ?: frozenScroll
                        isFocused = true
                    } else {
                        isFocused = false
                    }
                },
            singleLine = false,
            minLines = minLines,
            maxLines = maxLines,
            interactionSource = interactionSource,
            colors = colors,
        )
    }
>>>>>>> Stashed changes
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