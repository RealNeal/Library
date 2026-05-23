package com.rn.library.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun formatArgbHex(argb: Int): String =
    "#%08X".format(argb.toLong() and 0xFFFFFFFFL).uppercase()

private fun parseHexColorInput(raw: String): Int? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    val withHash = if (t.startsWith("#")) t else "#$t"
    return try {
        AndroidColor.parseColor(withHash)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * Выбор цвета: HEX-поле, HSV-ползунки и превью.
 */
@Composable
fun HsvColorPicker(
    colorArgb: Int,
    onColorArgbChange: (Int) -> Unit,
    labelHue: String,
    labelSaturation: String,
    labelValue: String,
    hexInputLabel: String,
    modifier: Modifier = Modifier
) {
    val hsvInit = remember(colorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(colorArgb, it) }
    }
    var hue by remember(colorArgb) { mutableFloatStateOf(hsvInit[0]) }
    var sat by remember(colorArgb) { mutableFloatStateOf(hsvInit[1]) }
    var value by remember(colorArgb) { mutableFloatStateOf(hsvInit[2]) }

    var hexInput by remember(colorArgb) { mutableStateOf(formatArgbHex(colorArgb)) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun emitFromHsv() {
        val c = AndroidColor.HSVToColor(floatArrayOf(hue, sat, value))
        onColorArgbChange(c)
    }

    val previewColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, value)))

    fun applyHexFromField() {
        val parsed = parseHexColorInput(hexInput) ?: return
        onColorArgbChange(parsed)
        val arr = FloatArray(3)
        AndroidColor.colorToHSV(parsed, arr)
        hue = arr[0]
        sat = arr[1]
        value = arr[2]
        hexInput = formatArgbHex(parsed)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewColor)
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            )
            OutlinedTextField(
                value = hexInput,
                onValueChange = { hexInput = it.uppercase() },
                label = { Text(hexInputLabel, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        applyHexFromField()
                        keyboardController?.hide()
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        if (!state.isFocused) {
                            val parsed = parseHexColorInput(hexInput)
                            if (parsed != null) {
                                onColorArgbChange(parsed)
                                val arr = FloatArray(3)
                                AndroidColor.colorToHSV(parsed, arr)
                                hue = arr[0]
                                sat = arr[1]
                                value = arr[2]
                                hexInput = formatArgbHex(parsed)
                            } else {
                                hexInput = formatArgbHex(colorArgb)
                            }
                        }
                    },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF494458),
                    unfocusedContainerColor = Color(0xFF494458),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.White.copy(alpha = 0.75f),
                    unfocusedIndicatorColor = Color.White.copy(alpha = 0.45f),
                    cursorColor = Color.White,
                    focusedLabelColor = Color.White.copy(alpha = 0.9f),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.75f)
                )
            )
        }
        Text(labelHue, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                emitFromHsv()
            },
            valueRange = 0f..359.99f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
        Text(labelSaturation, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        Slider(
            value = sat,
            onValueChange = {
                sat = it
                emitFromHsv()
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
        Text(labelValue, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
        Slider(
            value = value,
            onValueChange = {
                value = it
                emitFromHsv()
            },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White.copy(alpha = 0.85f),
                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
            )
        )
    }
}
