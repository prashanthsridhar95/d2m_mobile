package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Column
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
)

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
    // Same field chrome as every other D2MTextField, just masked.
    isPassword: Boolean = false,
) {
    Column(modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) FieldLabel(label, optional)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(D2MRadius.md),
            colors = fieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        )
        if (hint != null) FieldHint(hint)
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
        if (hint != null) FieldHint(hint)
    }
}
