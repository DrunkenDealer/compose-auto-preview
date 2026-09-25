package app.mashlab.autopreview.kmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.mashlab.autopreview.annotations.AutoPreview
import app.mashlab.autopreview.annotations.Device
import app.mashlab.autopreview.annotations.Theme
import app.mashlab.autopreview.kmp.about.AboutSamples
import app.mashlab.autopreview.kmp.about.AboutScreen
import app.mashlab.autopreview.kmp.about.AboutState
import app.mashlab.autopreview.kmp.profile.ProfileSamples
import app.mashlab.autopreview.kmp.profile.ProfileScreen
import app.mashlab.autopreview.kmp.profile.ProfileState

@AutoPreview(
    samplesFrom = ProfileSamples::class,
    devices = [Device.Phone, Device.Tablet],
    themes = [Theme.Light, Theme.Dark],
    entryPoint = true,
    navigatesTo = ["AboutScreen"],
)
@ProfileScreenAutoPreviews
@Composable
internal fun ProfileScreenPreview(
    @PreviewParameter(ProfileScreenPreviewSamplesProvider::class) state: ProfileState,
) = ProfileScreen(state)

@AutoPreview(samplesFrom = AboutSamples::class)
@AboutScreenAutoPreviews
@Composable
internal fun AboutScreenPreview(
    @PreviewParameter(AboutScreenPreviewSamplesProvider::class) state: AboutState,
) = AboutScreen(state)
