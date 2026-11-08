package io.github.lubara7.tgmax.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.lubara7.tgmax.localization.R
import io.github.lubara7.tgmax.ui.PrimaryButton

@Composable
fun VerificationCodeScreen(state: AuthUiState, onCode: (String) -> Unit, onVerify: () -> Unit,
    onResend: () -> Unit, onEditPhone: () -> Unit) {
    val challenge = state.challenge ?: return
    Text(stringResource(R.string.code_title), style = MaterialTheme.typography.headlineMedium)
    Text(stringResource(R.string.code_description, challenge.phone.e164),
        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp))
    OutlinedTextField(value = state.codeInput, onValueChange = onCode, enabled = !state.busy,
        singleLine = true, label = { Text(stringResource(R.string.code_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onVerify() }),
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))
    PrimaryButton(stringResource(R.string.action_sign_in),
        !state.busy && state.codeInput.length == challenge.codeLength, onVerify)
    TextButton(onClick = onResend, enabled = !state.busy && state.retrySeconds == 0,
        modifier = Modifier.fillMaxWidth()) {
        Text(if (state.retrySeconds > 0) pluralStringResource(R.plurals.resend_countdown,
            state.retrySeconds, state.retrySeconds) else stringResource(R.string.action_resend))
    }
    TextButton(onClick = onEditPhone, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.action_edit_phone))
    }
}
