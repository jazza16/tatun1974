package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Kontainer Modal Card Dialog Standar SIPAGAR KITA dengan jarak atas & bawah layar yang seimbang
 * (simetris) dan aman terhadap Status Bar, Navigation Bar / Gesture Pill, serta Keyboard (IME).
 *
 * Keunggulan:
 * - Menggunakan `decorFitsSystemWindows = false` + pengukuran aktual `statusBars`, `navigationBars`,
 *   dan `ime` sehingga bagian bawah Card (sudut melengkung & tombol aksi footer) tidak pernah terpotong.
 * - Saat keyboard tertutup: jarak dari tepi atas layar ke atas Card == jarak dari tepi bawah layar
 *   ke bawah Card (`maxOf(statusTop, navBottom) + extraVerticalMargin`), sehingga tampil utuh dan simetris.
 * - Saat keyboard terbuka: Card otomatis menyesuaikan tepat di atas keyboard tanpa celah kosong berlebih.
 */
@Composable
fun BalancedModalDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    horizontalMargin: Dp = 14.dp,
    extraVerticalMargin: Dp = 14.dp,
    maxWidth: Dp = 560.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Fallback pembacaan insets langsung dari Activity rootView untuk kompatibilitas perangkat Android 15+
    val (activityStatusTop, activityNavBottom) = remember(context, density) {
        var c: Context? = context
        while (c is ContextWrapper && c !is Activity) {
            c = c.baseContext
        }
        val decorView = (c as? Activity)?.window?.decorView
        val rootInsets = decorView?.let { ViewCompat.getRootWindowInsets(it) }
        val statusPx = rootInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        val navPx = rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
        with(density) { statusPx.toDp() to navPx.toDp() }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val composeStatusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val composeNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val composeImeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

        val safeTopInset = maxOf(composeStatusTop, activityStatusTop, 28.dp)
        val safeBottomInset = maxOf(composeNavBottom, activityNavBottom, 24.dp)
        val isKeyboardVisible = composeImeBottom > safeBottomInset

        // Jarak seimbang (simetris) antara tepi atas layar dan tepi bawah layar saat keyboard tidak tampil
        val symmetricEdgePadding = maxOf(safeTopInset, safeBottomInset) + extraVerticalMargin
        val topPadding = if (isKeyboardVisible) safeTopInset + 10.dp else symmetricEdgePadding
        val bottomPadding = if (isKeyboardVisible) composeImeBottom + 10.dp else symmetricEdgePadding

        val backdropInteraction = remember { MutableInteractionSource() }
        val cardInteraction = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (dismissOnClickOutside) {
                        Modifier.clickable(
                            interactionSource = backdropInteraction,
                            indication = null,
                            onClick = onDismissRequest
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    start = horizontalMargin,
                    end = horizontalMargin,
                    top = topPadding,
                    bottom = bottomPadding
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = {}
                    ),
                content = content
            )
        }
    }
}

data class AdaptiveSystemBarInsets(
    val topBarInset: Dp,
    val bottomNavInset: Dp,
    val imeBottomInset: Dp,
    val effectiveBottomInset: Dp,
    val isThreeButtonNavigation: Boolean
)

/**
 * Helper WindowInsets Adaptif untuk memastikan 100% kompatibilitas pada:
 * 1. Mode Navigasi 3 Tombol (Three-Button Navigation Bar: Back, Home, Recents ~48dp-56dp)
 * 2. Mode Navigasi 2 Tombol (Pill Navigation)
 * 3. Mode Navigasi Penuh / Usap (Gesture Navigation ~16dp-24dp)
 *
 * Membaca insets langsung dari Activity `decorView` sekaligus `WindowInsets` Compose,
 * karena di dalam full-screen `Dialog(decorFitsSystemWindows = false)` WindowInsets Compose
 * terkadang bernilai 0.dp untuk bilah navigasi 3 tombol.
 */
@Composable
fun rememberAdaptiveSystemBarInsets(): AdaptiveSystemBarInsets {
    val context = LocalContext.current
    val density = LocalDensity.current

    val (activityStatusTop, activityNavBottom, isThreeButtonMode) = remember(context, density) {
        var c: Context? = context
        while (c is ContextWrapper && c !is Activity) {
            c = c.baseContext
        }
        val decorView = (c as? Activity)?.window?.decorView
        val rootInsets = decorView?.let { ViewCompat.getRootWindowInsets(it) }
        val statusPx = rootInsets?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars())?.top
            ?: rootInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top
            ?: 0
        val navPx = rootInsets?.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars())?.bottom
            ?: rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom
            ?: 0

        // Deteksi mode navigasi Android (0 = 3-button, 1 = 2-button, 2 = gesture)
        val navModeInt = runCatching {
            val resId = context.resources.getIdentifier("config_navBarInteractionMode", "integer", "android")
            if (resId > 0) context.resources.getInteger(resId) else -1
        }.getOrDefault(-1)

        val navDp = with(density) { navPx.toDp() }
        val statusDp = with(density) { statusPx.toDp() }
        val detectedThreeButton = (navModeInt == 0 || navModeInt == 1) || navDp >= 40.dp
        val guaranteedNavBottom = if (detectedThreeButton) maxOf(navDp, 52.dp) else maxOf(navDp, 24.dp)

        Triple(statusDp, guaranteedNavBottom, detectedThreeButton)
    }

    val composeStatusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val composeNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val composeImeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

    val finalTop = maxOf(composeStatusTop, activityStatusTop)
    val finalNavBottom = maxOf(composeNavBottom, activityNavBottom)
    val effectiveBottom = maxOf(finalNavBottom, composeImeBottom)

    return AdaptiveSystemBarInsets(
        topBarInset = finalTop,
        bottomNavInset = finalNavBottom,
        imeBottomInset = composeImeBottom,
        effectiveBottomInset = effectiveBottom,
        isThreeButtonNavigation = isThreeButtonMode || finalNavBottom >= 40.dp
    )
}

