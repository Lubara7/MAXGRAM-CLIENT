package io.github.lubara7.tgmax.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lubara7.tgmax.domain.AuthError
import io.github.lubara7.tgmax.localization.R
import io.github.lubara7.tgmax.ui.BrandMark

@Composable
fun AuthRoute(viewModel: AuthViewModel, demo: Boolean, onLanguage: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.challenge != null) { if (!state.busy) viewModel.editPhone() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onLanguage, enabled = !state.busy) { Text(stringResource(R.string.language_title)) }
        }
        Spacer(Modifier.height(32.dp))
        BrandMark()
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 32.dp))
        Column(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
            if (demo) {
                Text(stringResource(R.string.demo_badge), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge)
                Text(stringResource(R.string.demo_hint, "12345"),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp))
            }
            if (state.authenticated) {
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
                Text(stringResource(R.string.home_description), Modifier.padding(vertical = 16.dp))
                TextButton(onClick = viewModel::signOut, enabled = !state.busy) {
                    Text(stringResource(R.string.action_sign_out))
                }
            } else if (!state.restoring) {
                if (state.challenge == null) PhoneNumberScreen(state, viewModel::setPhone, viewModel::setRegion, viewModel::requestCode)
                else VerificationCodeScreen(state, viewModel::setCode, viewModel::verifyCode,
                    viewModel::resendCode, viewModel::editPhone)
            }
            state.error?.let {
                Text(stringResource(errorResource(it)), color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp))
            }
            if (state.challenge == null && state.retrySeconds > 0) {
                Text(pluralStringResource(R.plurals.resend_countdown, state.retrySeconds, state.retrySeconds),
                    Modifier.padding(top = 12.dp))
            }
            if (state.busy) CircularProgressIndicator(Modifier.padding(top = 24.dp).size(28.dp))
        }
    }
}

private fun errorResource(error: AuthError): Int = when (error) {
    AuthError.INVALID_PHONE -> R.string.error_invalid_phone
    AuthError.INVALID_CODE -> R.string.error_invalid_code
    AuthError.EXPIRED_CODE -> R.string.error_expired_code
    AuthError.RATE_LIMITED -> R.string.error_rate_limited
    AuthError.NETWORK -> R.string.error_network
    AuthError.INTEGRATION_UNAVAILABLE -> R.string.error_integration_unavailable
    AuthError.SESSION_EXPIRED -> R.string.error_session_expired
    AuthError.STORAGE -> R.string.error_storage
    AuthError.UNSUPPORTED_CHALLENGE -> R.string.error_unsupported_challenge
    AuthError.UNKNOWN -> R.string.error_unknown
}
