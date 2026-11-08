package io.github.lubara7.tgmax.localization

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun LanguagePickerScreen(currentTag: String, onSelect: (String) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        Text(stringResource(R.string.language_title), style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp))
        LazyColumn {
            item { LanguageRow(stringResource(R.string.language_system), currentTag.isEmpty()) { onSelect("") } }
            items(AppLanguage.entries, key = { it.tag }) { language ->
                LanguageRow(language.nativeName, currentTag == language.tag) { onSelect(language.tag) }
            }
        }
    }
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.RadioButton, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(12.dp))
    }
}
