package com.rn.library.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.rn.library.ui.LocalStrings
import com.rn.library.ui.screens.AppTheme
import com.rn.library.ui.theme.IconTextColor
import com.rn.library.ui.theme.SearchBarColor

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String? = null,
    onSearchQueryChange: (String) -> Unit = {},
    currentTheme: AppTheme = AppTheme.DARK,
    iconOnly: Boolean = false,
    expanded: Boolean? = null,
    onExpandedChange: (Boolean) -> Unit = {}
) {
    val strings = LocalStrings.current
    val iconTextColor = IconTextColor()
    val baseSearchBarColor = SearchBarColor()
    
    // Adjust colors based on theme
    val searchBarColor = if (currentTheme == AppTheme.LIGHT) {
        // Light theme: slightly lighter background
        Color(
            red = kotlin.math.min(1f, baseSearchBarColor.red + 0.05f),
            green = kotlin.math.min(1f, baseSearchBarColor.green + 0.05f),
            blue = kotlin.math.min(1f, baseSearchBarColor.blue + 0.05f)
        )
    } else {
        // Dark theme: slightly lighter to stand out
        Color(
            red = kotlin.math.min(1f, baseSearchBarColor.red * 1.15f),
            green = kotlin.math.min(1f, baseSearchBarColor.green * 1.15f),
            blue = kotlin.math.min(1f, baseSearchBarColor.blue * 1.15f)
        )
    }
    
    val borderColor = if (currentTheme == AppTheme.DARK) {
        Color(0xFF232323) // Темная тема: #232323
    } else {
        Color(0xFFCCCCCC) // Светлая тема: серая окантовка
    }
    
    var internalQuery by remember { mutableStateOf("") }
    val searchQuery = query ?: internalQuery

    var internalExpanded by remember { mutableStateOf(false) }
    val isSearchExpanded = expanded ?: internalExpanded

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Request focus when search is expanded
    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded && iconOnly) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        } else if (!isSearchExpanded) {
            keyboardController?.hide()
        }
    }

    if (iconOnly && !isSearchExpanded) {
        // Icon-only mode
        IconButton(
            onClick = { 
                if (expanded != null) onExpandedChange(true) else internalExpanded = true
            },
            modifier = modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = strings.search,
                tint = if (currentTheme == AppTheme.DARK) Color.White else Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        // Full search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { newValue ->
                if (query == null) internalQuery = newValue
                onSearchQueryChange(newValue)
            },
            modifier = modifier
                .height(56.dp)
                .focusRequester(focusRequester)
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(28.dp)
                ),
            placeholder = {
                Text(
                    text = strings.search,
                    color = iconTextColor.copy(alpha = 0.6f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = strings.search,
                    tint = if (currentTheme == AppTheme.DARK) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingIcon = {
                if (iconOnly && isSearchExpanded) {
                    // Close button when in icon-only mode
                    IconButton(
                        onClick = {
                            if (expanded != null) onExpandedChange(false) else internalExpanded = false
                            if (query == null) internalQuery = ""
                            onSearchQueryChange("")
                            keyboardController?.hide()
                        }
                    ) {
                        Text(
                            text = "×",
                            color = iconTextColor.copy(alpha = 0.6f),
                            fontSize = 20.sp
                        )
                    }
                } else if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            if (query == null) internalQuery = ""
                            onSearchQueryChange("")
                        }
                    ) {
                        Text(
                            text = "×",
                            color = iconTextColor.copy(alpha = 0.6f),
                            fontSize = 20.sp
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = searchBarColor,
                unfocusedContainerColor = searchBarColor,
                disabledContainerColor = searchBarColor,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                cursorColor = iconTextColor,
                focusedTextColor = iconTextColor,
                unfocusedTextColor = iconTextColor,
                disabledTextColor = iconTextColor,
                focusedPlaceholderColor = iconTextColor.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = iconTextColor.copy(alpha = 0.6f),
                disabledPlaceholderColor = iconTextColor.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(28.dp),
            singleLine = true
        )
    }
}
