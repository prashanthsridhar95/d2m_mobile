package com.d2m.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.saveable.rememberSaveable
import com.d2m.app.ui.theme.LocalD2MStatusPalette
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.strings.LocalStrings
import com.d2m.app.ui.theme.D2MRadius
import com.d2m.app.ui.theme.d2m

/*
 * Form fields, retuned to the Registration comp.
 *
 * Two changes worth naming.
 *
 * 1. The label moved OUT of the field. Material's OutlinedTextField
 *    floats its label into the border notch, which is a Material
 *    signature and reads nothing like the comps -- where the label is a
 *    small sentence-case line of body ink sitting above a plain box. So
 *    these render their own label above the field and pass `label = null`
 *    to Material, keeping the placeholder for the in-field hint the comps
 *    actually use ("As it should appear on the profile").
 *
 * 2. Fields sit on --field-bg (a hair off the card white) with a
 *    --border-strong rule, both sampled from the comp. That off-white fill
 *    is what makes an empty field visibly a field on a white card without
 *    needing a heavier border.
 *
 * Also note the label distinction this design draws, which the old code
 * collapsed: a FORM label ("Full name") is sentence case in near-body ink,
 * because it is an instruction. A DATA label ("GOTHRAM" over a filled-in
 * value) is uppercase and tracked in label grey, because it is a column
 * header. LabelText is the second one; FieldLabel below is the first.
 */

@Composable
fun FieldLabel(text: String, optional: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        // The comps mark the not-required fields rather than starring the
        // required ones ("Sub-sect (optional)") -- fewer marks on a long
        // form, and it puts the reassurance where the hesitation is.
        text = if (optional) "$text (optional)" else text,
        style = MaterialTheme.typography.labelMedium,
        color = d2m.textPrimary,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

/** Small muted line under a control -- "Give a mobile number rather than a landline." */
@Composable
fun FieldHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = d2m.meta,
        modifier = modifier.padding(top = 6.dp),
    )
}

/**
 * Shared colours for every field in the app, so the text field, the
 * dropdown and the multiline box can't drift apart -- which is exactly
 * what happened before (three hand-rolled call sites, two with different
 * padding).
 */
@Composable
private fun fieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = d2m.field,
    disabledContainerColor = d2m.surfaceSunken,
    focusedBorderColor = d2m.accent,
    unfocusedBorderColor = d2m.borderStrong,
    disabledBorderColor = d2m.border,
    focusedTextColor = d2m.textPrimary,
    unfocusedTextColor = d2m.textPrimary,
    disabledTextColor = d2m.faint,
    cursorColor = d2m.accent,
    focusedPlaceholderColor = d2m.label,
    unfocusedPlaceholderColor = d2m.label,
    focusedTrailingIconColor = d2m.label,
    unfocusedTrailingIconColor = d2m.label,
    errorBorderColor = LocalD2MStatusPalette.current.danger.fg,
    errorContainerColor = d2m.field,
    errorTextColor = d2m.textPrimary,
    errorCursorColor = LocalD2MStatusPalette.current.danger.fg,
    errorPlaceholderColor = d2m.label,
    errorTrailingIconColor = d2m.label,
)

/**
 * Inline field error -- the web ErrorLine: icon + text under the field, announced assertively when it appears and
 * exposed as the field's error. Say what to DO ("Enter a valid email"), never what went wrong.
 */
@Composable
fun FieldError(text: String, modifier: Modifier = Modifier) {
    val danger = LocalD2MStatusPalette.current.danger.fg
    Row(
        modifier = modifier.padding(top = 6.dp).semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Assertive
            error(text)
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = danger, modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = danger)
    }
}

/** Error and hint are mutually exclusive (web: `error ? <ErrorLine/> : hint ? <FieldHint/>`). */
@Composable
private fun FieldFootnote(error: String?, hint: String?) {
    if (error != null) FieldError(error) else if (hint != null) FieldHint(hint)
}

@Composable
fun D2MTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    hint: String? = null,
    optional: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    // Added for WedLock registration/login's password fields (LoginScreen.kt,
    // ClaimFlowScreen.kt) -- nothing in this app needed masked input before.
    // Same field chrome as every other D2MTextField, just masked, now with a
    // Show/Hide toggle (a masked field you cannot check is an error-prone field).
    isPassword: Boolean = false,
    /** Password only: true on create/registration (NewPassword autofill), false on login (Password). */
    isNewPassword: Boolean = false,
    /** Validation message authored by the screen (never server text). Replaces [hint] while set. */
    error: String? = null,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    autoCorrect: Boolean = true,
    /** Autofill semantics (Username/EmailAddress/SmsOtpCode...). Password fields set their own. */
    contentType: ContentType? = null,
) {
    var shown by rememberSaveable { mutableStateOf(false) }
    val strings = LocalStrings.current.login
    val effectiveType = when {
        isPassword -> if (isNewPassword) ContentType.NewPassword else ContentType.Password
        else -> contentType
    }
    val masked = isPassword && !shown
    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) FieldLabel(label, optional)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
            modifier = Modifier.fillMaxWidth().then(
                if (effectiveType != null) Modifier.semantics { this.contentType = effectiveType } else Modifier,
            ),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            isError = error != null,
            shape = RoundedCornerShape(D2MRadius.md),
            colors = fieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(
                capitalization = if (isPassword) KeyboardCapitalization.None else capitalization,
                autoCorrectEnabled = if (isPassword) false else autoCorrect,
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = if (onImeAction != null) {
                KeyboardActions(onNext = { onImeAction() }, onGo = { onImeAction() }, onDone = { onImeAction() }, onSend = { onImeAction() }, onSearch = { onImeAction() })
            } else {
                KeyboardActions.Default
            },
            visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    Box(
                        Modifier
                            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                            .clickable(role = Role.Button, onClickLabel = if (shown) strings.hide else strings.show) { shown = !shown }
                            .semantics { stateDescription = if (shown) strings.passwordShown else strings.passwordHidden },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (shown) strings.hide else strings.show,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = d2m.accent,
                            maxLines = 1,
                        )
                    }
                }
            } else {
                null
            },
        )
        FieldFootnote(error, hint)
    }
}

/** The comps' "A few words about them" box -- same control style, taller. */
@Composable
fun D2MTextArea(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    hint: String? = null,
    optional: Boolean = false,
    minLines: Int = 4,
    error: String? = null,
) {
    D2MTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        hint = hint,
        optional = optional,
        singleLine = false,
        minLines = minLines,
        error = error,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun D2MSelectField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    optionLabel: (String) -> String = { it },
    enabled: Boolean = true,
    hint: String? = null,
    optional: Boolean = false,
    placeholderWhenEmpty: String = LocalStrings.current.sharedComponents.notSet,
    error: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) FieldLabel(label, optional)
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = if (value.isEmpty()) placeholderWhenEmpty else optionLabel(value),
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                isError = error != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(D2MRadius.md),
                colors = fieldColors(),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text(placeholderWhenEmpty) }, onClick = { onValueChange(""); expanded = false })
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(optionLabel(opt)) }, onClick = { onValueChange(opt); expanded = false })
                }
            }
        }
        FieldFootnote(error, hint)
    }
}

/**
 * One-time code: a single wide field beats N boxes for paste, SMS autofill and screen readers (web OtpField).
 * Digits only, capped at [length]; bullets as the placeholder; IME action Done submits via [onDone].
 */
@Composable
fun D2MOtpField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    hint: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    onDone: (() -> Unit)? = null,
) {
    D2MTextField(
        label = label,
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(length)) },
        modifier = modifier,
        placeholder = "\u2022".repeat(length),
        hint = hint,
        error = error,
        enabled = enabled,
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Done,
        onImeAction = onDone,
        capitalization = KeyboardCapitalization.None,
        autoCorrect = false,
        contentType = ContentType.SmsOtpCode,
    )
}
