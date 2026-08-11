package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.d2m.app.data.network.GeocodingApi
import com.d2m.app.ui.theme.mutedText
import kotlinx.coroutines.delay

/**
 * Multi-city sibling of CityAutocomplete -- mirrors components/CityChipPicker.jsx.
 * value/onChange are plain string lists of city labels (same "just store the
 * label" simplification the web component's own doc comment describes),
 * used for accept_locations across onboarding/preferences screens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CityChipPicker(
    label: String,
    value: List<String>,
    onChange: (List<String>) -> Unit,
    geocodingApi: GeocodingApi,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        results = runCatching { geocodingApi.searchCities(query).map { it.label } }.getOrDefault(emptyList())
        expanded = results.isNotEmpty()
    }

    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = mutedText(0.55f))

        if (value.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                value.forEach { city ->
                    AssistChip(
                        onClick = { if (!disabled) onChange(value - city) },
                        label = { Text(city) },
                        trailingIcon = if (!disabled) {
                            {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove $city",
                                    modifier = Modifier.padding(2.dp),
                                )
                            }
                        } else null,
                        enabled = true,
                    )
                }
            }
        }

        if (!disabled) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Start typing a city…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            if (expanded) {
                Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    LazyColumn {
                        items(results) { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    if (!value.contains(r)) onChange(value + r)
                                    query = ""
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
