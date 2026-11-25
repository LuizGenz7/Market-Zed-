package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.example.core.util.PhoneValidator
import com.example.data.model.ContactPreferences
import com.example.data.model.Listing
import com.example.ui.theme.MarketZedOrange
import com.example.ui.theme.VerifiedEmerald

/**
 * Returns the context-sensitive contact CTA label required by MarketZed guidelines.
 */
fun contextSensitiveContactLabel(
    categoryId: String?,
    sellerAccountType: String? = null
): String {
    return when (categoryId?.trim()?.lowercase()) {
        "property" -> {
            if (sellerAccountType?.trim()?.lowercase() in setOf("business", "agent", "agency")) {
                "Contact Agent"
            } else {
                "Contact Landlord"
            }
        }
        "services" -> "Contact Provider"
        "jobs" -> "Contact Employer"
        "wanted" -> "Contact Requester"
        else -> "Contact Seller"
    }
}

/**
 * Strictly vertical stacked contact buttons layout (never horizontal rows).
 * Automatically hides any missing contact method so zero nonfunctional buttons appear.
 */
@Composable
fun VerticalContactActionsColumn(
    contactPreferences: ContactPreferences?,
    listingTitle: String,
    categoryId: String?,
    sellerAccountType: String? = null,
    modifier: Modifier = Modifier,
    emptyMessage: String = "No direct contact details were published on this listing."
) {
    val context = LocalContext.current
    val snackbar = LocalTopSnackbarController.current
    val phone = contactPreferences?.phone?.trim()?.takeIf { it.isNotEmpty() }
    val whatsapp = contactPreferences?.whatsapp?.trim()?.takeIf { it.isNotEmpty() }
    val email = contactPreferences?.email?.trim()?.takeIf { it.isNotEmpty() }
    val primaryLabel = contextSensitiveContactLabel(categoryId, sellerAccountType)

    if (phone == null && whatsapp == null && email == null) {
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.testTag("contact_actions_empty")
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("vertical_contact_actions_column"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (phone != null) {
            Button(
                onClick = {
                    val uri = PhoneValidator.buildTelUri(phone)
                    if (uri != null) {
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, uri)) }
                            .onFailure { copyContactValue(context, "Phone", phone, snackbar) }
                    } else {
                        copyContactValue(context, "Phone", phone, snackbar)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MarketZedOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("contact_button_phone")
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_phone),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = primaryLabel,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (whatsapp != null) {
            OutlinedButton(
                onClick = {
                    val uri = PhoneValidator.buildWhatsAppUri(whatsapp, listingTitle)
                    if (uri != null) {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                            .onFailure { copyContactValue(context, "WhatsApp", whatsapp, snackbar) }
                    } else {
                        copyContactValue(context, "WhatsApp", whatsapp, snackbar)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VerifiedEmerald
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("contact_button_whatsapp")
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_message_circle),
                    contentDescription = null,
                    tint = VerifiedEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "WhatsApp",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (email != null) {
            OutlinedButton(
                onClick = {
                    val uri = PhoneValidator.buildMailtoUri(email, listingTitle)
                    if (uri != null) {
                        runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
                            .onFailure { copyContactValue(context, "Email", email, snackbar) }
                    } else {
                        copyContactValue(context, "Email", email, snackbar)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("contact_button_email")
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_mail),
                    contentDescription = null,
                    tint = MarketZedOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Message",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VerticalContactActionsColumn(
    listing: Listing,
    modifier: Modifier = Modifier
) {
    VerticalContactActionsColumn(
        contactPreferences = listing.contactPreferences,
        listingTitle = listing.title,
        categoryId = listing.categoryId,
        sellerAccountType = listing.sellerAccountType,
        modifier = modifier
    )
}

internal fun copyContactValue(
    context: Context,
    label: String,
    value: String,
    snackbar: TopSnackbarController?
) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, value))
    snackbar?.showSuccess("$label copied to clipboard")
}
