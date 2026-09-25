package app.mashlab.autopreview.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.mashlab.autopreview.sample.feature.habit.DeleteHabitDialog
import app.mashlab.autopreview.sample.feature.habit.EditHabitSamples
import app.mashlab.autopreview.sample.feature.habit.EditHabitScreen
import app.mashlab.autopreview.sample.feature.habit.HabitDetailSamples
import app.mashlab.autopreview.sample.feature.habit.HabitDetailScreen
import app.mashlab.autopreview.sample.feature.insights.InsightsSamples
import app.mashlab.autopreview.sample.feature.insights.InsightsScreen
import app.mashlab.autopreview.sample.feature.premium.PremiumSamples
import app.mashlab.autopreview.sample.feature.premium.PremiumScreen
import app.mashlab.autopreview.sample.feature.profile.ProfileSamples
import app.mashlab.autopreview.sample.feature.profile.ProfileScreen
import app.mashlab.autopreview.sample.feature.signin.SignInSamples
import app.mashlab.autopreview.sample.feature.signin.SignInScreen
import app.mashlab.autopreview.sample.feature.today.TodaySamples
import app.mashlab.autopreview.sample.feature.today.TodayScreen
import app.mashlab.autopreview.sample.feature.welcome.WelcomePage
import app.mashlab.autopreview.sample.feature.welcome.WelcomeScreen
import app.mashlab.autopreview.sample.feature.welcome.WelcomeState
import app.mashlab.autopreview.sample.ui.components.BloomTab
import app.mashlab.autopreview.sample.ui.theme.BloomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BloomTheme { BloomApp() } }
    }
}

private sealed interface Route {
    data class Welcome(
        val page: WelcomePage,
    ) : Route

    data object SignIn : Route

    data class Tab(
        val tab: BloomTab,
    ) : Route

    data object HabitDetail : Route

    data object DeleteHabit : Route

    data object NewHabit : Route

    data object Premium : Route
}

// Screens are fed the same sample states the previews render; there is no data layer.
@Composable
private fun BloomApp() {
    var route: Route by remember { mutableStateOf(Route.Welcome(WelcomePage.Track)) }
    val toTab = { tab: BloomTab -> route = Route.Tab(tab) }
    val home = { route = Route.Tab(BloomTab.Today) }
    BackHandler(enabled = route !is Route.Welcome && route != Route.Tab(BloomTab.Today)) { home() }

    when (val current = route) {
        is Route.Welcome -> {
            WelcomeScreen(
                state = WelcomeState(current.page),
                onNext = {
                    val next = WelcomePage.entries.getOrNull(current.page.ordinal + 1)
                    route = if (next != null) Route.Welcome(next) else Route.SignIn
                },
                onSignIn = { route = Route.SignIn },
            )
        }

        Route.SignIn -> {
            SignInScreen(SignInSamples.Filled, onSignIn = home, onCreateAccount = home)
        }

        is Route.Tab -> {
            when (current.tab) {
                BloomTab.Today -> TodayScreen(
                    state = TodaySamples.InProgress,
                    onTabSelect = toTab,
                    onHabitClick = { route = Route.HabitDetail },
                    onAddHabit = { route = Route.NewHabit },
                )

                BloomTab.Insights -> InsightsScreen(InsightsSamples.Week, onTabSelect = toTab)

                BloomTab.Profile -> ProfileScreen(
                    state = ProfileSamples.Free,
                    onTabSelect = toTab,
                    onUpgrade = { route = Route.Premium },
                    onSignOut = { route = Route.Welcome(WelcomePage.Track) },
                )
            }
        }

        Route.HabitDetail -> {
            HabitDetailScreen(
                state = HabitDetailSamples.OnStreak,
                onBack = home,
                onEdit = { route = Route.NewHabit },
                onDelete = { route = Route.DeleteHabit },
            )
        }

        Route.DeleteHabit -> {
            HabitDetailScreen(HabitDetailSamples.OnStreak)
            DeleteHabitDialog(
                habit = HabitDetailSamples.OnStreak.habit,
                onConfirm = home,
                onDismiss = { route = Route.HabitDetail },
            )
        }

        Route.NewHabit -> {
            EditHabitScreen(EditHabitSamples.Filled, onClose = home, onSave = home)
        }

        Route.Premium -> {
            PremiumScreen(
                state = PremiumSamples.Yearly,
                onClose = { toTab(BloomTab.Profile) },
                onSubscribe = { toTab(BloomTab.Profile) },
            )
        }
    }
}
