package com.rn.library.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rn.library.R
import com.rn.library.ui.theme.MainBackgroundColor
import com.rn.library.ui.theme.TitleColorBetween

@Composable
fun AppGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = MainBackgroundColor()
    val text = TitleColorBetween()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = text)
            }
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.guide_title), color = text)
        }

        Spacer(Modifier.height(16.dp))
        Text(text = stringResource(R.string.guide_body), color = text)
    }
}
