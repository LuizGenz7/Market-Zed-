package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorRedContainerLight
import com.example.ui.theme.LocalDarkTheme
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldContainerLight
import com.example.ui.theme.WantedBlue
import com.example.ui.theme.WantedBlueContainerLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainerLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

enum class TopSnackbarType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class TopSnackbarMessage(
    val id: Long,
    val message: String,
    val type: TopSnackbarType,
    val durationMillis: Long = 3600L,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

object FriendlyMessageFormatter {
    fun format(rawMessage: String?, type: TopSnackbarType): String {
        val raw = rawMessage?.trim().orEmpty()
        if (raw.isEmpty()) {
            return when (type) {
                TopSnackbarType.SUCCESS -> "Action completed."
                TopSnackbarType.ERROR -> "Something went wrong. Please try again."
                TopSnackbarType.WARNING -> "Please review and try again."
                TopSnackbarType.INFO -> "Update available."
            }
        }
        val lower = raw.lowercase()
        return when {
            lower.contains("unknownhostexception") ||
                lower.contains("unable to resolve host") ||
                lower.contains("failed to connect") ||
                lower.contains("connectexception") ||
                lower.contains("network is unreachable") ->
                "You appear to be offline. Check your connection and try again."

            lower.contains("sockettimeoutexception") ||
                lower.contains("timed out") ||
                lower.contains("timeout") ->
                "The request took too long to respond. Please check your connection and retry."

            lower.contains("http 500") ||
                lower.contains("502") ||
                lower.contains("503") ||
                lower.contains("internal server error") ->
                "MarketZed is having a temporary hiccup. Please try again in a moment."

            lower.contains("http 401") ||
                lower.contains("unauthorized") ->
                "Your session needs a quick refresh. Please sign in again."

            lower.contains("http 403") ||
                lower.contains("forbidden") ->
                "You do not have permission to perform that action right now."

            lower.contains("http 429") ||
                lower.contains("too many requests") ->
                "Too many requests right now. Please wait a moment and try again."

            lower.contains("java.io.ioexception") ||
                lower.contains("sslhandshakeexception") ||
                lower.contains("eofexception") ->
                "Connection interrupted. Please check your internet and try again."

            lower.contains("json") && (lower.contains("exception") || lower.contains("malformed") || lower.contains("expected")) ->
                "Received an unexpected response from the server. Please try again."

            else -> raw
        }
    }
}

@Stable
class TopSnackbarController {
    private val idGenerator = AtomicLong(1L)
    private val _currentMessage = MutableStateFlow<TopSnackbarMessage?>(null)
    val currentMessage: StateFlow<TopSnackbarMessage?> = _currentMessage.asStateFlow()

    fun show(
        message: String,
        type: TopSnackbarType = TopSnackbarType.INFO,
        durationMillis: Long = when (type) {
            TopSnackbarType.ERROR -> 4500L
            TopSnackbarType.WARNING -> 4000L
            else -> 3200L
        },
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        val friendly = FriendlyMessageFormatter.format(message, type)
        val current = _currentMessage.value
        if (current != null && current.message == friendly && current.type == type) {
            return
        }
        _currentMessage.value = TopSnackbarMessage(
            id = idGenerator.getAndIncrement(),
            message = friendly,
            type = type,
            durationMillis = durationMillis,
            actionLabel = actionLabel,
            onAction = onAction
        )
    }

    fun showSuccess(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        show(message, TopSnackbarType.SUCCESS, actionLabel = actionLabel, onAction = onAction)
    }

    fun showError(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        show(message, TopSnackbarType.ERROR, actionLabel = actionLabel, onAction = onAction)
    }

    fun showWarning(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        show(message, TopSnackbarType.WARNING, actionLabel = actionLabel, onAction = onAction)
    }

    fun showInfo(message: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        show(message, TopSnackbarType.INFO, actionLabel = actionLabel, onAction = onAction)
    }

    fun dismiss() {
        _currentMessage.value = null
    }
}

val LocalTopSnackbarController = staticCompositionLocalOf<TopSnackbarController?> { null }

@Composable
fun TopSnackbarHost(
    controller: TopSnackbarController,
    modifier: Modifier = Modifier
) {
    val message by controller.currentMessage.collectAsState()

    LaunchedEffect(message?.id) {
        val active = message ?: return@LaunchedEffect
        delay(active.durationMillis)
        if (controller.currentMessage.value?.id == active.id) {
            controller.dismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(
                initialOffsetY = { -it - 40 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it - 40 }
            ) + fadeOut()
        ) {
            message?.let { item ->
                TopSnackbarCard(
                    item = item,
                    onDismiss = { controller.dismiss() }
                )
            }
        }
    }
}

internal data class TopSnackbarPalette(
    val containerColor: Color,
    val borderColor: Color,
    val accentColor: Color,
    val contentColor: Color,
    val iconRes: Int
)

internal fun resolveTopSnackbarPalette(
    type: TopSnackbarType,
    isDark: Boolean
): TopSnackbarPalette {
    return when (type) {
        TopSnackbarType.SUCCESS -> if (isDark) {
            val accent = Color(0xFF34D399)
            TopSnackbarPalette(
                containerColor = Color(0xFF0A2E23),
                borderColor = accent.copy(alpha = 0.5f),
                accentColor = accent,
                contentColor = Color(0xFFECFDF5),
                iconRes = LucideR.drawable.lucide_ic_circle_check
            )
        } else {
            TopSnackbarPalette(
                containerColor = VerifiedEmeraldContainerLight,
                borderColor = VerifiedEmerald.copy(alpha = 0.4f),
                accentColor = VerifiedEmerald,
                contentColor = Color(0xFF064E3B),
                iconRes = LucideR.drawable.lucide_ic_circle_check
            )
        }

        TopSnackbarType.ERROR -> if (isDark) {
            val accent = Color(0xFFF87171)
            TopSnackbarPalette(
                containerColor = Color(0xFF380D11),
                borderColor = accent.copy(alpha = 0.5f),
                accentColor = accent,
                contentColor = Color(0xFFFEF2F2),
                iconRes = LucideR.drawable.lucide_ic_circle_alert
            )
        } else {
            TopSnackbarPalette(
                containerColor = ErrorRedContainerLight,
                borderColor = ErrorRed.copy(alpha = 0.4f),
                accentColor = ErrorRed,
                contentColor = Color(0xFF7F1D1D),
                iconRes = LucideR.drawable.lucide_ic_circle_alert
            )
        }

        TopSnackbarType.WARNING -> if (isDark) {
            val accent = Color(0xFFFBBF24)
            TopSnackbarPalette(
                containerColor = Color(0xFF331B06),
                borderColor = accent.copy(alpha = 0.5f),
                accentColor = accent,
                contentColor = Color(0xFFFFFBEB),
                iconRes = LucideR.drawable.lucide_ic_triangle_alert
            )
        } else {
            TopSnackbarPalette(
                containerColor = WarningAmberContainerLight,
                borderColor = WarningAmber.copy(alpha = 0.45f),
                accentColor = WarningAmber,
                contentColor = Color(0xFF78350F),
                iconRes = LucideR.drawable.lucide_ic_triangle_alert
            )
        }

        TopSnackbarType.INFO -> if (isDark) {
            val accent = Color(0xFF60A5FA)
            TopSnackbarPalette(
                containerColor = Color(0xFF102247),
                borderColor = accent.copy(alpha = 0.5f),
                accentColor = accent,
                contentColor = Color(0xFFEFF6FF),
                iconRes = LucideR.drawable.lucide_ic_info
            )
        } else {
            TopSnackbarPalette(
                containerColor = WantedBlueContainerLight,
                borderColor = WantedBlue.copy(alpha = 0.4f),
                accentColor = WantedBlue,
                contentColor = Color(0xFF1E3A8A),
                iconRes = LucideR.drawable.lucide_ic_info
            )
        }
    }
}

@Composable
private fun TopSnackbarCard(
    item: TopSnackbarMessage,
    onDismiss: () -> Unit
) {
    val isDark = LocalDarkTheme.current || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val palette = resolveTopSnackbarPalette(type = item.type, isDark = isDark)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.containerColor,
        border = BorderStroke(1.dp, palette.borderColor),
        shadowElevation = if (isDark) 10.dp else 6.dp,
        tonalElevation = if (isDark) 4.dp else 2.dp,
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .testTag("top_snackbar_${item.type.name.lowercase()}")
            .clickable(onClick = onDismiss)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(palette.iconRes),
                contentDescription = item.type.name,
                tint = palette.accentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = palette.contentColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!item.actionLabel.isNullOrBlank() && item.onAction != null) {
                TextButton(
                    onClick = {
                        item.onAction.invoke()
                        onDismiss()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = item.actionLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentColor
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_snackbar_dismiss")
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_x),
                    contentDescription = "Dismiss notification",
                    tint = palette.contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
