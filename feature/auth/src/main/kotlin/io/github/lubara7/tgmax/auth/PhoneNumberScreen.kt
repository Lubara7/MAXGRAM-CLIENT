package io.github.lubara7.tgmax.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lubara7.tgmax.localization.R
import io.github.lubara7.tgmax.localization.AppLanguage
import io.github.lubara7.tgmax.ui.PrimaryButton
import java.util.Locale

@Composable
fun PhoneNumberScreen(state: AuthUiState, onPhone: (String) -> Unit, onRegion: (String) -> Unit, onNext: () -> Unit) {
    var choosingCountry by remember { mutableStateOf(false) }
    val systemLocale = LocalConfiguration.current.locales[0]
    val displayLocale = Locale.forLanguageTag(AppLanguage.normalize(systemLocale.toLanguageTag()).ifEmpty { "en" })
    val selectedCountry = Locale.Builder().setRegion(state.region).build().getDisplayCountry(displayLocale)
    Text(stringResource(R.string.phone_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.phone_description), style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 12.dp, bottom = 28.dp))
    OutlinedButton(onClick = { choosingCountry = true }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.country_calling_code, selectedCountry, PhoneNumberNormalizer.callingCode(state.region)))
    }
    OutlinedTextField(value = state.phoneInput, onValueChange = onPhone, singleLine = true,
        enabled = !state.busy, label = { Text(stringResource(R.string.phone_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp))
    PrimaryButton(stringResource(R.string.action_continue),
        !state.busy && state.phoneInput.isNotBlank() && state.retrySeconds == 0, onNext)
    if (choosingCountry) {
        CountryPicker(displayLocale, onDismiss = { choosingCountry = false }) {
            onRegion(it)
            choosingCountry = false
        }
    }
}

@Composable
private fun CountryPicker(locale: Locale, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    val countries = remember(locale, search) {
        PhoneNumberNormalizer.regions.map { it to Locale.Builder().setRegion(it).build().getDisplayCountry(locale) }
            .filter { (region, name) -> name.contains(search, true) || region.contains(search, true) ||
                PhoneNumberNormalizer.callingCode(region).toString().contains(search) }
            .sortedBy { it.second }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.country_title)) },
        text = {
            Column {
                OutlinedTextField(search, { search = it }, singleLine = true,
                    label = { Text(stringResource(R.string.country_search)) })
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(countries, key = { it.first }) { (region, name) ->
                        Text(stringResource(R.string.country_calling_code, name, PhoneNumberNormalizer.callingCode(region)),
                            Modifier.fillMaxWidth().clickable { onSelect(region) }.padding(vertical = 16.dp))
                    }
                }
            }
        }, confirmButton = {}, dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        })
}
