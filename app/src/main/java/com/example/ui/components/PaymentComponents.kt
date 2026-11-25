package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.PriceFormatter
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.MarketZedOrangeDark
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.WantedBlue

sealed class PaymentUiStatePhase {
    data object Idle : PaymentUiStatePhase()
    data class Processing(
        val message: String = "Please wait while we confirm your payment."
    ) : PaymentUiStatePhase()
    data class Pending(
        val paymentId: String?,
        val amountZmw: Double,
        val purposeTitle: String,
        val message: String = "Your payment is being processed."
    ) : PaymentUiStatePhase()
    data class Successful(
        val paymentId: String?,
        val amountZmw: Double,
        val purposeTitle: String,
        val headline: String = "Payment confirmed.",
        val detail: String = "Your account verification is now active."
    ) : PaymentUiStatePhase()
    data class Failed(
        val paymentId: String?,
        val message: String = "We could not confirm your payment.\nPlease try again or contact support."
    ) : PaymentUiStatePhase()
    data class Cancelled(
        val paymentId: String?,
        val message: String = "Payment was cancelled."
    ) : PaymentUiStatePhase()
}

@Composable
fun HeroPaymentAmountCard(
    purposeBadge: String,
    purposeTitle: String,
    amountZmw: Double,
    shortExplanation: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_hero_amount_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(MarketZedOrange, MarketZedOrangeDark)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = purposeBadge.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = PriceFormatter.formatKwachaAmount(amountZmw),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 44.sp,
                        lineHeight = 48.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("payment_hero_amount_text")
                )

                Text(
                    text = purposeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = shortExplanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun LipilaPaymentMethodSummaryCard(
    instructions: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MarketZedOrange.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_wallet),
                            contentDescription = "Mobile Money",
                            tint = MarketZedOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Mobile Money · Lipila (MTN / Airtel / Zamtel)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Text(
                text = instructions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PaymentSecurityReassuranceBanner(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = VerifiedEmerald.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_shield_check),
                contentDescription = "Secure payment",
                tint = VerifiedEmerald,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Verified by MarketZed & Lipila. Features activate only after trusted server payment confirmation — never double-charged.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PaymentPhaseStatusCard(
    phase: PaymentUiStatePhase,
    onCheckStatus: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (phase is PaymentUiStatePhase.Idle) return

    val (iconRes, accent, title, body) = when (phase) {
        is PaymentUiStatePhase.Processing -> Quadruple(
            LucideR.drawable.lucide_ic_credit_card,
            MarketZedOrange,
            "Processing Payment",
            phase.message
        )
        is PaymentUiStatePhase.Pending -> Quadruple(
            LucideR.drawable.lucide_ic_clock,
            WantedBlue,
            "Payment Pending",
            phase.message
        )
        is PaymentUiStatePhase.Successful -> Quadruple(
            LucideR.drawable.lucide_ic_shield_check,
            VerifiedEmerald,
            phase.headline,
            phase.detail
        )
        is PaymentUiStatePhase.Failed -> Quadruple(
            LucideR.drawable.lucide_ic_circle_alert,
            MaterialTheme.colorScheme.error,
            "Payment Failed",
            phase.message
        )
        is PaymentUiStatePhase.Cancelled -> Quadruple(
            LucideR.drawable.lucide_ic_x,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Payment Cancelled",
            phase.message
        )
        PaymentUiStatePhase.Idle -> return
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("payment_phase_card"),
        shape = RoundedCornerShape(18.dp),
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (phase is PaymentUiStatePhase.Processing) {
                            CircularProgressIndicator(
                                color = accent,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = title,
                                tint = accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (phase) {
                is PaymentUiStatePhase.Pending -> {
                    if (onCheckStatus != null) {
                        OutlinedButton(
                            onClick = onCheckStatus,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("payment_check_status_button"),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Text("Check Payment Status", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                is PaymentUiStatePhase.Successful -> {
                    if (onDone != null) {
                        Button(
                            onClick = onDone,
                            colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("payment_success_done_button")
                        ) {
                            Text("Continue", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                is PaymentUiStatePhase.Failed, is PaymentUiStatePhase.Cancelled -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onRetry != null) {
                            Button(
                                onClick = onRetry,
                                colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("payment_retry_button")
                            ) {
                                Text("Try Again", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (onCheckStatus != null && phase is PaymentUiStatePhase.Failed && !phase.paymentId.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = onCheckStatus,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                            ) {
                                Text("Re-check Payment Status")
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
fun PaymentResultDialog(
    phase: PaymentUiStatePhase,
    onDismiss: () -> Unit,
    onCheckStatus: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onSuccessContinue: (() -> Unit)? = null
) {
    if (phase is PaymentUiStatePhase.Idle || phase is PaymentUiStatePhase.Processing) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            val (iconRes, tint) = when (phase) {
                is PaymentUiStatePhase.Pending -> LucideR.drawable.lucide_ic_clock to WantedBlue
                is PaymentUiStatePhase.Successful -> LucideR.drawable.lucide_ic_shield_check to VerifiedEmerald
                is PaymentUiStatePhase.Failed -> LucideR.drawable.lucide_ic_circle_alert to MaterialTheme.colorScheme.error
                is PaymentUiStatePhase.Cancelled -> LucideR.drawable.lucide_ic_x to MaterialTheme.colorScheme.onSurfaceVariant
                else -> LucideR.drawable.lucide_ic_wallet to MarketZedOrange
            }
            Surface(
                shape = CircleShape,
                color = tint.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = when (phase) {
                    is PaymentUiStatePhase.Pending -> "Pending"
                    is PaymentUiStatePhase.Successful -> "Successful"
                    is PaymentUiStatePhase.Failed -> "Failed"
                    is PaymentUiStatePhase.Cancelled -> "Cancelled"
                    else -> "Payment"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (phase) {
                    is PaymentUiStatePhase.Pending -> {
                        Text(PriceFormatter.formatKwachaAmount(phase.amountZmw), fontWeight = FontWeight.Bold, color = MarketZedOrange)
                        Text(phase.message)
                    }
                    is PaymentUiStatePhase.Successful -> {
                        Text(PriceFormatter.formatKwachaAmount(phase.amountZmw), fontWeight = FontWeight.Bold, color = VerifiedEmerald)
                        Text(phase.headline, fontWeight = FontWeight.SemiBold)
                        Text(phase.detail)
                    }
                    is PaymentUiStatePhase.Failed -> Text(phase.message)
                    is PaymentUiStatePhase.Cancelled -> Text(phase.message)
                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (phase) {
                is PaymentUiStatePhase.Successful -> {
                    Button(
                        onClick = {
                            onDismiss()
                            onSuccessContinue?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VerifiedEmerald)
                    ) { Text("Done", color = Color.White) }
                }
                is PaymentUiStatePhase.Pending -> {
                    Button(
                        onClick = {
                            onDismiss()
                            onCheckStatus?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                    ) { Text("Check Status", color = Color.White) }
                }
                is PaymentUiStatePhase.Failed, is PaymentUiStatePhase.Cancelled -> {
                    Button(
                        onClick = {
                            onDismiss()
                            onRetry?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MarketZedOrange)
                    ) { Text("Try Again", color = Color.White) }
                }
                else -> Unit
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
