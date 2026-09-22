package za.co.quickfix.repairmate.ui.booking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.quickfix.repairmate.R
import za.co.quickfix.repairmate.data.local.Booking
import za.co.quickfix.repairmate.data.local.RepairStatus
import za.co.quickfix.repairmate.ui.textRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    vm: BookingViewModel,
    onOpenBooking: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatusTab by rememberSaveable { mutableStateOf(0) } // 0 = All, 1..5 = RepairStatus entries
    var sortBy by rememberSaveable { mutableStateOf(0) } // 0 = Date, 1 = Status, 2 = Priority

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(context.getString(it.textRes())) }
    }

    // Refresh automatically with administrative scope
    LaunchedEffect(Unit) {
        vm.refresh(silent = true)
    }

    // Compute Summary Statistics based on full loaded dataset from clients
    val totalCount = bookings.size
    val pendingCount = bookings.count { it.status == RepairStatus.RECEIVED }
    val activeCount = bookings.count { it.status == RepairStatus.DIAGNOSING || it.status == RepairStatus.IN_PROGRESS }
    val completedCount = bookings.count { it.status == RepairStatus.READY || it.status == RepairStatus.COLLECTED }

    // Filter & Sort Logic
    val filteredBookings = remember(bookings, searchQuery, selectedStatusTab, sortBy) {
        var list = bookings.filter { booking ->
            val matchesSearch = booking.deviceBrand.contains(searchQuery, ignoreCase = true) ||
                    booking.deviceModel.contains(searchQuery, ignoreCase = true) ||
                    booking.issue.contains(searchQuery, ignoreCase = true)

            val matchesStatus = if (selectedStatusTab == 0) {
                true
            } else {
                booking.status.ordinal == selectedStatusTab - 1
            }

            matchesSearch && matchesStatus
        }

        when (sortBy) {
            0 -> list = list.sortedByDescending { it.createdAt }
            1 -> list = list.sortedBy { it.status.ordinal }
            2 -> list = list.sortedBy {
                when (it.priority.lowercase()) {
                    "high" -> 0
                    "medium" -> 1
                    else -> 2
                }
            }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.admin_dashboard_title), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (refreshing) LinearProgressIndicator(Modifier.fillMaxWidth())

            // Summary Statistics Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(stringResource(R.string.stat_total), totalCount.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_pending), pendingCount.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_active), activeCount.toString(), Modifier.weight(1f))
                StatCard(stringResource(R.string.stat_completed), completedCount.toString(), Modifier.weight(1f))
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.label_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Sorting Controls Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text("Sort by: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { sortBy = 0 }) {
                    Text("Date", fontWeight = if (sortBy == 0) FontWeight.Bold else FontWeight.Normal)
                }
                TextButton(onClick = { sortBy = 1 }) {
                    Text("Status", fontWeight = if (sortBy == 1) FontWeight.Bold else FontWeight.Normal)
                }
                TextButton(onClick = { sortBy = 2 }) {
                    Text("Priority", fontWeight = if (sortBy == 2) FontWeight.Bold else FontWeight.Normal)
                }
            }

            // Status Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedStatusTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                Tab(selected = selectedStatusTab == 0, onClick = { selectedStatusTab = 0 }) {
                    Text("All", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall)
                }
                RepairStatus.entries.forEachIndexed { index, status ->
                    Tab(selected = selectedStatusTab == index + 1, onClick = { selectedStatusTab = index + 1 }) {
                        Text(
                            text = status.name,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }

            // Repairs List
            if (filteredBookings.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No repairs found matching criteria", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookings, key = { it.localId }) { booking ->
                        AdminBookingCard(booking, onClick = { onOpenBooking(booking.localId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminBookingCard(booking: Booking, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.deviceBrand} ${booking.deviceModel}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                PriorityBadge(booking.priority)
            }
            Text(
                text = booking.issue,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(booking.status)
                Text(
                    text = "Client ID: ${booking.userId.take(6)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: String) {
    val color = when (priority.lowercase()) {
        "high" -> MaterialTheme.colorScheme.errorContainer
        "medium" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (priority.lowercase()) {
        "high" -> MaterialTheme.colorScheme.onErrorContainer
        "medium" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color
    ) {
        Text(
            text = priority,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
