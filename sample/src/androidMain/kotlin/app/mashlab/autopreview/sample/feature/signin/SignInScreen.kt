package app.mashlab.autopreview.sample.feature.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import app.mashlab.autopreview.sample.PreviewBloom
import app.mashlab.autopreview.sample.ui.components.BloomLogo
import app.mashlab.autopreview.sample.ui.components.InfoBanner
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

data class SignInState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
)

object SignInSamples {
    val Empty = SignInState()
    val Filled = SignInState(email = "maya.lin@example.com", password = "hunter22")
    val Loading = Filled.copy(isLoading = true)
    val WrongPassword = Filled.copy(error = "That password doesn't match. Try again or reset it.")
}

@Composable
fun SignInScreen(
    state: SignInState,
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit = {},
    onCreateAccount: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(40.dp))
                BloomLogo(size = 64.dp)
                Spacer(Modifier.height(24.dp))
                Text("Welcome back", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Sign in to pick up your streaks.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
                if (state.error != null) {
                    InfoBanner(
                        icon = Icons.Rounded.ErrorOutline,
                        text = state.error,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Form(state)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onSignIn,
                    enabled = !state.isLoading && state.email.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Signing in…")
                    } else {
                        Text("Sign in")
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text(
                        text = "New to Bloom?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HorizontalDivider(Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Text("Create an account")
                }
            }
        }
    }
}

@Composable
private fun Form(state: SignInState) {
    Column {
        OutlinedTextField(
            value = state.email,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Rounded.Mail, contentDescription = null) },
            singleLine = true,
            enabled = !state.isLoading,
            shape = MaterialTheme.shapes.small,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            trailingIcon = { Icon(Icons.Rounded.Visibility, contentDescription = "Show password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !state.isLoading,
            isError = state.error != null,
            shape = MaterialTheme.shapes.small,
        )
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = {}) { Text("Forgot password?") }
        }
    }
}

@PreviewBloom(samplesFrom = SignInSamples::class, navigatesTo = ["TodayScreen"])
@SignInScreenAutoPreviews
@Composable
internal fun SignInScreenPreview(
    @PreviewParameter(SignInScreenPreviewSamplesProvider::class) state: SignInState,
) = BloomTheme { SignInScreen(state) }
