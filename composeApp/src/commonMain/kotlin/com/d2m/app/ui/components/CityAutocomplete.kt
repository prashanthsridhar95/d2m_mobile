package com.d2m.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
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
import com.d2m.app.data.network.CitySuggestion
import com.d2m.app.data.network.GeocodingApi
import com.d2m.app.ui.strings.LocalStrings
import kotlinx.coroutines.delay

/**
 * Mirrors components/CityAutocomplete.jsx: 350ms-debounced type-ahead against
 * Open-Meteo, resolves lat/lon/timezone on selection, always mirrors raw
 * typed text up even without a selected match (so a birth-place field never
 * silently reverts if the user types something the API doesn't recognize).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityAutocomplete(
    label: String,
    value: String,
    geocodingApi: GeocodingApi,
    onSelect: (CitySuggestion) -> Unit,
    onRawTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = LocalStrings.current.sharedComponents.startTypingACity,
) {
    var query by remember(value) { mutableStateOf(value) }
    var results by remember { mutableStateOf<List<CitySuggestion>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(350)
        results = runCatching { geocodingApi.searchCities(query) }.getOrDefault(emptyList())
        expanded = results.isNotEmpty()
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onRawTextChange(it)
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (expanded) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                LazyColumn {
                    items(results) { r ->
                        DropdownMenuItem(
                            text = { Text(r.label) },
                            onClick = {
                                query = r.label
                                expanded = false
                                onSelect(r)
                            },
                        )
                    }
                }
            }
        }
    }
}
