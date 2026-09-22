package za.co.quickfix.repairmate.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.clickable
import za.co.quickfix.repairmate.ui.PrimaryButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import za.co.quickfix.repairmate.R
import za.co.quickfix.repairmate.data.local.Booking
import za.co.quickfix.repairmate.data.local.RepairStatus
import za.co.quickfix.repairmate.data.local.SyncState
import za.co.quickfix.repairmate.ui.labelRes
import za.co.quickfix.repairmate.ui.textRes
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(vm: BookingViewModel, localId: Long, isAdmin: Boolean = false, onBack: () -> Unit) {
    val flow = remember(localId) { vm.booking(localId) }
    val booking by flow.collectAsStateWithLifecycle(initialValue = null)
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(context.getString(it.textRes())) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        val b = booking
        if (b == null) {
            Box(Modifier.padding(padding).fillMaxSize())
        } else {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${b.deviceBrand} ${b.deviceModel}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.label_booked_on, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(b.createdAt))),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (b.syncState == SyncState.PENDING) {
                        Text(stringResource(R.string.sync_pending), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
                    }
                }

                Column {
                    Text(stringResource(R.string.section_status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    StatusTimeline(b.status)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.section_issue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(b.issue, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.label_date) + ": " + b.preferredDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Admin Controls vs Client View
                if (isAdmin) {
                    var adminStatus by remember { mutableStateOf(b.status) }
                    var adminPriority by remember { mutableStateOf(b.priority) }
                    var adminNotes by remember { mutableStateOf(b.internalNotes) }

                    LaunchedEffect(b.localId) {
                        adminStatus = b.status
                        adminPriority = b.priority
                        adminNotes = b.internalNotes
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Admin Workbench", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        Text("Update Repair Status:", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RepairStatus.entries.forEach { status ->
                                val sel = adminStatus == status
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { adminStatus = status },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = status.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Text("Update Priority Level:", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Low", "Medium", "High").forEach { pr ->
                                val sel = adminPriority.lowercase() == pr.lowercase()
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { adminPriority = pr },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (sel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = pr,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = adminNotes,
                            onValueChange = { adminNotes = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.label_internal_notes)) },
                            minLines = 3
                        )

                        PrimaryButton(R.string.action_save, false) {
                            vm.updateBookingAdmin(b, adminStatus, adminPriority, adminNotes)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { vm.advance(b) },
                            enabled = b.status.next() != null && b.syncState == SyncState.SYNCED,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.action_advance)) }
                        Text(
                            stringResource(R.string.demo_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** Vertical list of the five stages; stages up to and including the current one are filled in. */
@Composable
private fun StatusTimeline(current: RepairStatus) {
    Column(Modifier.padding(top = 8.dp)) {
        RepairStatus.entries.forEach { step ->
            val reached = step.ordinal <= current.ordinal
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (reached) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(step.labelRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (step == current) FontWeight.Bold else FontWeight.Normal,
                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
