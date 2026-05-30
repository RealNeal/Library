package com.rn.library.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Многострочное поле с внутренней прокруткой: изолирует свайпы внутри себя,
 * не передавая оверскролл родительской форме.
 */
@Composable
fun ScrollIsolatedMultilineField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    placeholder: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value))
    }
    if (textFieldValue.text != value) {
        textFieldValue = textFieldValue.copy(text = value)
    }
    // Изоляция ручного скролла: поглощаем весь "остаточный" скролл,
    // чтобы он не дергал родительский ScrollState (экрана), когда текст кончается.
    val fieldNestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Возвращаем available, говоря системе "мы сами обработали этот остаток"
                return available
            }
        }
    }

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
        modifier = modifier
            .fillMaxWidth()
            .nestedScroll(fieldNestedScrollConnection), // Оставляем только эту изоляцию
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        interactionSource = interactionSource,
        colors = colors,
    )
}

/**
 * Заглушка, оставленная чтобы не ломать вызовы в других файлах (например, AddWorkScreen.kt).
 */
@Composable
fun rememberBlockParentScrollOnFocusedField(blockParentScroll: Boolean): NestedScrollConnection {
    return remember {
        object : NestedScrollConnection {}
    }
}