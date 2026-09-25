@file:Suppress("MatchingDeclarationName")

package app.mashlab.autopreview.sample.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class BloomTab(
    val label: String,
    val icon: ImageVector,
) {
    Today("Today", Icons.Rounded.Today),
    Insights("Insights", Icons.Rounded.Insights),
    Profile("Profile", Icons.Rounded.Person),
}

/** Bottom bar on compact widths, navigation rail from 600dp up; [content] is told to go two-pane from 840dp. */
@Composable
fun BloomScaffold(
    selectedTab: BloomTab,
    modifier: Modifier = Modifier,
    onTabSelect: (BloomTab) -> Unit = {},
    floatingActionButton: @Composable (expanded: Boolean) -> Unit = {},
    content: @Composable (expanded: Boolean) -> Unit,
) {
    // Movable so content and FAB keep their state when the layout switches between bar and rail.
    val body = remember(content) { movableContentOf { expanded: Boolean -> content(expanded) } }
    val fab = remember(floatingActionButton) {
        movableContentOf { expanded: Boolean ->
            floatingActionButton(expanded)
        }
    }
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints {
            if (maxWidth >= 600.dp) {
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        header = {
                            BloomLogo(Modifier.padding(top = 20.dp, bottom = 12.dp))
                            fab(true)
                            Spacer(Modifier.height(12.dp))
                        },
                    ) {
                        BloomTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = tab == selectedTab,
                                onClick = { onTabSelect(tab) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) { body(this@BoxWithConstraints.maxWidth >= 840.dp) }
                }
            } else {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    floatingActionButton = { fab(false) },
                    bottomBar = {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                            BloomTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = tab == selectedTab,
                                    onClick = { onTabSelect(tab) },
                                    icon = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(tab.label) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding)) { body(false) }
                }
            }
        }
    }
}
