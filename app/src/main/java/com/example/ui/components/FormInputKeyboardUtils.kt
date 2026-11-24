package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Automatically scrolls the enclosing ScrollView / LazyColumn when the TextField gains focus
 * so that the input sits cleanly above the keyboard with a tidy [extraBottom] margin (default 24.dp)
 * without creating excessive empty space or clipping dialog headers.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.formScrollPadding(extraBottom: Dp = 24.dp): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }

    this
        .bringIntoViewRequester(bringIntoViewRequester)
        .onSizeChanged { fieldSize = it }
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                coroutineScope.launch {
                    // Brief delay allows the IME (keyboard) resize animation to start
                    delay(120)
                    val extraBottomPx = with(density) { extraBottom.toPx() }
                    val widthPx = if (fieldSize.width > 0) fieldSize.width.toFloat() else 300f
                    val heightPx = if (fieldSize.height > 0) fieldSize.height.toFloat() else 140f
                    val targetRect = Rect(
                        left = 0f,
                        top = 0f,
                        right = widthPx,
                        bottom = heightPx + extraBottomPx
                    )
                    bringIntoViewRequester.bringIntoView(targetRect)
                }
            }
        }
}

/**
 * Standard KeyboardActions that handles both TextInputAction.next (move focus to next field)
 * and TextInputAction.done (clear focus and close keyboard).
 */
@Composable
fun rememberFormKeyboardActions(
    onDoneCallback: (() -> Unit)? = null,
    onNextCallback: (() -> Unit)? = null
): KeyboardActions {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    return remember(focusManager, keyboardController, onDoneCallback, onNextCallback) {
        KeyboardActions(
            onNext = {
                if (onNextCallback != null) {
                    onNextCallback()
                } else {
                    val moved = focusManager.moveFocus(FocusDirection.Next)
                    if (!moved) {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                }
            },
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onDoneCallback?.invoke()
            }
        )
    }
}

fun formNextKeyboardOptions(
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
): KeyboardOptions = KeyboardOptions(
    keyboardType = keyboardType,
    capitalization = capitalization,
    imeAction = ImeAction.Next
)

fun formDoneKeyboardOptions(
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
): KeyboardOptions = KeyboardOptions(
    keyboardType = keyboardType,
    capitalization = capitalization,
    imeAction = ImeAction.Done
)
