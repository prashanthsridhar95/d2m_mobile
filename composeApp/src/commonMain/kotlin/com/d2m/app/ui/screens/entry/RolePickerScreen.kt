package com.d2m.app.ui.screens.entry

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.d2m.app.ui.theme.D2MFlow
import com.d2m.app.ui.theme.D2MTheme
import com.d2m.app.ui.theme.mutedText
import com.d2m.app.ui.components.D2MCard
import com.d2m.app.ui.components.PageTitle
import com.d2m.app.ui.components.SubHeading
import com.d2m.app.ui.strings.LocalStrings

/** Mirrors screens/entry/RolePickerScreen.jsx -- "Who's this for?". The self-signup card exists in the design reference but has no backend path (see plan §5), so it's intentionally not offered here, same as web. */
@Composable
fun RolePickerScreen(onPickParent: () -> Unit, onPickChild: () -> Unit) {
    val strings = LocalStrings.current.login
    D2MTheme(flow = D2MFlow.ENTRY) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            PageTitle(strings.whoIsThisFor)

            D2MCard(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).clickable(onClick = onPickParent),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SubHeading(strings.roleParentTitle)
                    Text(strings.roleParentSubtitle, color = mutedText(0.55f), style = MaterialTheme.typography.bodyMedium)
                }
            }

            D2MCard(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onPickChild),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SubHeading(strings.roleLinkTitle)
                    Text(strings.roleLinkSubtitle, color = mutedText(0.55f), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
