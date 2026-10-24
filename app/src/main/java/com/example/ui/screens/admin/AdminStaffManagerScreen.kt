package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthManager
import com.example.data.firestore.StaffMemberDto
import com.example.data.firestore.UserDto
import com.example.ui.components.HighContrastCard
import com.example.ui.theme.FireOrange
import com.example.ui.theme.FireOrangeDark
import com.example.ui.theme.HighContrastDark
import com.example.ui.theme.LiveRed
import com.example.ui.theme.SlateCardBg
import com.example.ui.theme.SlateLightBg
import com.example.ui.theme.SlateMutedBorder
import com.example.ui.theme.SlateSubtle
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted
import com.example.ui.theme.TrophyGold
import com.example.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStaffManagerScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val staffMembers by viewModel.staffMembers.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingStaff by remember { mutableStateOf<StaffMemberDto?>(null) }
    var staffToDelete by remember { mutableStateOf<StaffMemberDto?>(null) }

    val activeStaffCount = staffMembers.count { it.isActive }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SlateCardBg,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier.testTag("staff_manager_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimaryDark
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Staff & Role Permissions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Main Admin Control • Add & manage staff",
                                fontSize = 11.sp,
                                color = TextSecondaryMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            editingStaff = null
                            showAddOrEditDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("staff_add_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add Staff",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingStaff = null
                    showAddOrEditDialog = true
                },
                containerColor = HighContrastDark,
                contentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = FireOrange
                    )
                },
                text = {
                    Text(
                        text = "Add New Staff",
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.testTag("staff_add_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SlateLightBg)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Overview Banner
            item {
                HighContrastCard {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(FireOrange.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = FireOrange,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "STAFF ACCESS & PERMISSION CONTROL",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = FireOrange,
                                        letterSpacing = 0.6.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Main Admin: ${currentUser?.username?.ifBlank { "Uday" } ?: "Uday"} (Master Owner)",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = SuccessGreen.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, SuccessGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$activeStaffCount / ${staffMembers.size} ACTIVE",
                                    color = Color(0xFF4ADE80),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Assign granular permissions to staff members. Toggle any switch below to instantly enable or disable Create Match, Manage Balance, Support Messages, Broadcast, Withdrawals, or Settings.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            if (staffMembers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                        border = BorderStroke(1.dp, SlateMutedBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(FireOrange.copy(alpha = 0.14f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = FireOrange,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Staff Members Added Yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap 'Add New Staff' to add a moderator, match organizer, or support manager and choose which Admin Panel actions they are allowed to perform.",
                                fontSize = 12.sp,
                                color = TextSecondaryMuted,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    editingStaff = null
                                    showAddOrEditDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("empty_state_add_staff_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add First Staff Member", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(staffMembers, key = { it.id }) { staff ->
                    StaffMemberPermissionCard(
                        staff = staff,
                        onToggleActive = { enabled ->
                            viewModel.toggleStaffActiveStatus(staff, enabled)
                        },
                        onTogglePermission = { key, enabled ->
                            viewModel.toggleStaffPermission(staff, key, enabled)
                        },
                        onEdit = {
                            editingStaff = staff
                            showAddOrEditDialog = true
                        },
                        onDelete = {
                            staffToDelete = staff
                        }
                    )
                }
            }
        }
    }

    if (showAddOrEditDialog) {
        val candidatePlayers = remember(allUsers, staffMembers) {
            allUsers.filterNot { u ->
                AuthManager.isMasterAdminIdentity(u.email, u.username, u.id)
            }
        }
        StaffEditorModalDialog(
            initialStaff = editingStaff,
            existingUsers = candidatePlayers,
            currentAdminName = currentUser?.username?.ifBlank { "Uday" } ?: "Uday",
            onDismiss = { showAddOrEditDialog = false },
            onSave = { updatedStaff, plainPassword ->
                viewModel.saveOrUpdateStaffMember(updatedStaff, plainPassword) { success, msg ->
                    if (success) {
                        showAddOrEditDialog = false
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    staffToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            containerColor = SlateCardBg,
            title = {
                Text(
                    text = "Remove Staff Member?",
                    fontWeight = FontWeight.ExtraBold,
                    color = LiveRed
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${target.username}' (${target.roleTitle}) from the Admin Panel? Their admin permissions will be revoked immediately.",
                    fontSize = 13.sp,
                    color = TextPrimaryDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStaffMember(target.id)
                        staffToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    modifier = Modifier.testTag("confirm_delete_staff_button")
                ) {
                    Text("Remove Staff", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { staffToDelete = null }) {
                    Text("Cancel", color = TextSecondaryMuted)
                }
            }
        )
    }
}

@Composable
private fun StaffMemberPermissionCard(
    staff: StaffMemberDto,
    onToggleActive: (Boolean) -> Unit,
    onTogglePermission: (String, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val activePermCount = staff.activePermissionLabels().size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("staff_card_${staff.usernameLower}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCardBg),
        border = BorderStroke(
            width = if (staff.isActive) 1.5.dp else 1.dp,
            color = if (staff.isActive) FireOrange.copy(alpha = 0.65f) else SlateMutedBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Avatar, Name, Role Badge, and Master Active Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (staff.isActive) HighContrastDark else SlateSubtle,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = staff.username.take(1).uppercase().ifBlank { "S" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = if (staff.isActive) FireOrange else TextSecondaryMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = staff.username,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Surface(
                                color = if (staff.isActive) Color(0xFFDCFCE7) else SlateSubtle,
                                border = BorderStroke(
                                    1.dp,
                                    if (staff.isActive) Color(0xFF16A34A) else SlateMutedBorder
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (staff.isActive) "● ACTIVE" else "● DISABLED",
                                    color = if (staff.isActive) Color(0xFF15803D) else TextSecondaryMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "${staff.roleTitle} • ${staff.email.ifBlank { "Username Login: @${staff.username}" }}",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = staff.isActive,
                    onCheckedChange = onToggleActive,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SuccessGreen,
                        checkedBorderColor = SuccessGreen,
                        uncheckedThumbColor = Color(0xFF475569),
                        uncheckedTrackColor = Color(0xFFE2E8F0),
                        uncheckedBorderColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("staff_active_switch_${staff.usernameLower}")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SlateMutedBorder)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERMISSIONS ($activePermCount/6 ENABLED)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FireOrangeDark,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xFF3B82F6).copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onEdit() }
                            .testTag("staff_edit_button_${staff.usernameLower}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Staff",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Edit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2563EB),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        color = LiveRed.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDelete() }
                            .testTag("staff_delete_button_${staff.usernameLower}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Staff",
                                tint = LiveRed,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = LiveRed,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 6 Interactive Permission Toggle Rows
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PermissionToggleItem(
                    title = "Create & Manage Match",
                    subtitle = "Create/edit tournaments, publish Room ID/Pass & settle results",
                    icon = Icons.Default.SportsEsports,
                    accentColor = FireOrange,
                    checked = staff.canCreateMatch,
                    enabled = staff.isActive,
                    onCheckedChange = { onTogglePermission("canCreateMatch", it) },
                    tag = "perm_match_${staff.usernameLower}"
                )

                PermissionToggleItem(
                    title = "Manage Balance & Users",
                    subtitle = "Adjust player wallet balances, edit IGN/UID & suspend accounts",
                    icon = Icons.Default.ManageAccounts,
                    accentColor = Color(0xFF3B82F6),
                    checked = staff.canManageBalance,
                    enabled = staff.isActive,
                    onCheckedChange = { onTogglePermission("canManageBalance", it) },
                    tag = "perm_balance_${staff.usernameLower}"
                )

                PermissionToggleItem(
                    title = "Manage Support Messages",
                    subtitle = "View & reply to live player support chats and resolve tickets",
                    icon = Icons.Default.SupportAgent,
                    accentColor = Color(0xFF0EA5E9),
                    checked = staff.canManageSupport,
                    enabled = staff.isActive,
                    onCheckedChange = { onTogglePermission("canManageSupport", it) },
                    tag = "perm_support_${staff.usernameLower}"
                )

                PermissionToggleItem(
                    title = "Broadcast Alerts",
                    subtitle = "Send real-time push notifications & custom room alerts",
                    icon = Icons.Default.Campaign,
                    accentColor = Color(0xFF10B981),
                    checked = staff.canBroadcast,
                    enabled = staff.isActive,
                    onCheckedChange = { onTogglePermission("canBroadcast", it) },
                    tag = "perm_broadcast_${staff.usernameLower}"
                )

                PermissionToggleItem(
                    title = "Withdrawal Management",
                    subtitle = "Approve & mark paid or reject player UPI withdrawal requests",
                    icon = Icons.Default.Payments,
                    accentColor = SuccessGreen,
                    checked = staff.canManageWithdrawals,
                    enabled = staff.isActive,
                    onCheckedChange = { onTogglePermission("canManageWithdrawals", it) },
                    tag = "perm_withdrawals_${staff.usernameLower}"
                )

                PermissionToggleItem(
                    title = "Application & Ads Settings",
                    subtitle = "Configure Unity Ads account details, Game ID, bonus & rules",
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFF8B5CF6),
                    checked = staff.canManageSettings,
                    enabled = staff.isActive,
                    onCheckedChange = { onTogglePermission("canManageSettings", it) },
                    tag = "perm_settings_${staff.usernameLower}"
                )
            }
        }
    }
}

@Composable
private fun PermissionToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (checked && enabled) accentColor.copy(alpha = 0.10f) else SlateSubtle,
        border = BorderStroke(
            width = if (checked && enabled) 1.5.dp else 1.dp,
            color = if (checked && enabled) accentColor.copy(alpha = 0.65f) else Color(0xFFCBD5E1)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (checked && enabled) accentColor.copy(alpha = 0.18f) else SlateCardBg,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked && enabled) accentColor else TextSecondaryMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (enabled) TextPrimaryDark else TextSecondaryMuted,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondaryMuted,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    checkedBorderColor = accentColor,
                    uncheckedThumbColor = Color(0xFF475569),
                    uncheckedTrackColor = Color(0xFFE2E8F0),
                    uncheckedBorderColor = Color(0xFF94A3B8),
                    disabledCheckedThumbColor = Color.White.copy(alpha = 0.7f),
                    disabledCheckedTrackColor = accentColor.copy(alpha = 0.4f),
                    disabledUncheckedThumbColor = Color(0xFF94A3B8),
                    disabledUncheckedTrackColor = Color(0xFFF1F5F9),
                    disabledUncheckedBorderColor = Color(0xFFCBD5E1)
                )
            )
        }
    }
}

@Composable
private fun StaffEditorModalDialog(
    initialStaff: StaffMemberDto?,
    existingUsers: List<UserDto>,
    currentAdminName: String,
    onDismiss: () -> Unit,
    onSave: (StaffMemberDto, String) -> Unit
) {
    val isEditing = initialStaff != null
    var selectedUserId by remember { mutableStateOf(initialStaff?.userId ?: "") }
    var username by remember { mutableStateOf(initialStaff?.username ?: "") }
    var email by remember { mutableStateOf(initialStaff?.email ?: "") }
    var roleTitle by remember { mutableStateOf(initialStaff?.roleTitle ?: "Match Moderator") }
    var plainPassword by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(initialStaff?.isActive ?: true) }

    var canCreateMatch by remember { mutableStateOf(initialStaff?.canCreateMatch ?: true) }
    var canManageBalance by remember { mutableStateOf(initialStaff?.canManageBalance ?: false) }
    var canManageSupport by remember { mutableStateOf(initialStaff?.canManageSupport ?: true) }
    var canBroadcast by remember { mutableStateOf(initialStaff?.canBroadcast ?: false) }
    var canManageWithdrawals by remember { mutableStateOf(initialStaff?.canManageWithdrawals ?: false) }
    var canManageSettings by remember { mutableStateOf(initialStaff?.canManageSettings ?: false) }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = TextPrimaryDark,
        unfocusedTextColor = TextPrimaryDark,
        focusedBorderColor = FireOrange,
        unfocusedBorderColor = Color(0xFF94A3B8),
        focusedLabelColor = FireOrangeDark,
        unfocusedLabelColor = TextSecondaryMuted,
        cursorColor = FireOrange
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 640.dp)
                    .fillMaxHeight(0.90f),
                shape = RoundedCornerShape(20.dp),
                color = SlateCardBg,
                border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                shadowElevation = 16.dp,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Pinned Top Header
                    Surface(
                        color = HighContrastDark,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(FireOrange.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = FireOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEditing) "Edit Staff & Permissions" else "Add New Admin Staff",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Set login details & allowed admin permissions",
                                        fontSize = 11.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Scrollable Middle Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Quick Pick Existing Registered User (if adding new staff)
                        if (!isEditing && existingUsers.isNotEmpty()) {
                            Text(
                                text = "QUICK SELECT FROM REGISTERED PLAYERS (OPTIONAL)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireOrangeDark,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                existingUsers.take(15).forEach { user ->
                                    val isSelected = selectedUserId == user.id && user.id.isNotBlank()
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) HighContrastDark else SlateSubtle,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) FireOrange else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                selectedUserId = user.id
                                                username = user.username
                                                email = user.email
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = user.username.ifBlank { user.email.substringBefore("@") },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextPrimaryDark,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Staff Username *") },
                            placeholder = { Text("e.g. RahulMod") },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_username_input")
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Staff Email (for Google/Email Sign-In)") },
                            placeholder = { Text("e.g. staff@gmail.com") },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_email_input")
                        )

                        OutlinedTextField(
                            value = plainPassword,
                            onValueChange = { plainPassword = it },
                            label = {
                                Text(
                                    if (isEditing) "Reset Login Password (Optional)"
                                    else "Login Password (Optional — for Username Login)"
                                )
                            },
                            placeholder = { Text("Enter password so staff can log in directly") },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_password_input")
                        )

                        // Role Presets
                        Text(
                            text = "STAFF ROLE PRESET",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FireOrangeDark,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presets = listOf(
                                "Match Moderator" to {
                                    roleTitle = "Match Moderator"
                                    canCreateMatch = true
                                    canBroadcast = true
                                    canManageSupport = true
                                    canManageBalance = false
                                    canManageWithdrawals = false
                                    canManageSettings = false
                                },
                                "Support Manager" to {
                                    roleTitle = "Support Manager"
                                    canCreateMatch = false
                                    canBroadcast = true
                                    canManageSupport = true
                                    canManageBalance = false
                                    canManageWithdrawals = false
                                    canManageSettings = false
                                },
                                "Finance & Payouts" to {
                                    roleTitle = "Finance & Payouts"
                                    canCreateMatch = false
                                    canBroadcast = false
                                    canManageSupport = true
                                    canManageBalance = true
                                    canManageWithdrawals = true
                                    canManageSettings = false
                                },
                                "Co-Admin (Full)" to {
                                    roleTitle = "Co-Admin"
                                    canCreateMatch = true
                                    canBroadcast = true
                                    canManageSupport = true
                                    canManageBalance = true
                                    canManageWithdrawals = true
                                    canManageSettings = true
                                }
                            )

                            presets.forEach { (label, applyPreset) ->
                                val selected = roleTitle.equals(label.substringBefore(" ("), ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) HighContrastDark else SlateSubtle,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) FireOrange else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { applyPreset() }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else TextPrimaryDark,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = roleTitle,
                            onValueChange = { roleTitle = it },
                            label = { Text("Custom Role Title") },
                            singleLine = true,
                            colors = textFieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("staff_role_input")
                        )

                        // Permissions Checkboxes / Switches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ALLOWED PERMISSIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FireOrangeDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            canCreateMatch = true
                                            canManageBalance = true
                                            canManageSupport = true
                                            canBroadcast = true
                                            canManageWithdrawals = true
                                            canManageSettings = true
                                        }
                                ) {
                                    Text(
                                        text = "Enable All",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = SuccessGreen,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Surface(
                                    color = LiveRed.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.45f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            canCreateMatch = false
                                            canManageBalance = false
                                            canManageSupport = false
                                            canBroadcast = false
                                            canManageWithdrawals = false
                                            canManageSettings = false
                                        }
                                ) {
                                    Text(
                                        text = "Clear All",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = LiveRed,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        PermissionToggleItem(
                            title = "Create & Manage Match",
                            subtitle = "Create/edit matches, publish Room ID/Pass & settle prizes",
                            icon = Icons.Default.SportsEsports,
                            accentColor = FireOrange,
                            checked = canCreateMatch,
                            onCheckedChange = { canCreateMatch = it },
                            tag = "dialog_perm_create_match"
                        )

                        PermissionToggleItem(
                            title = "Manage Balance & Users",
                            subtitle = "Add/deduct user wallet balance, edit IGN/UID & suspend users",
                            icon = Icons.Default.ManageAccounts,
                            accentColor = Color(0xFF3B82F6),
                            checked = canManageBalance,
                            onCheckedChange = { canManageBalance = it },
                            tag = "dialog_perm_manage_balance"
                        )

                        PermissionToggleItem(
                            title = "Manage Support Messages",
                            subtitle = "Reply to player support chats & mark tickets resolved",
                            icon = Icons.Default.SupportAgent,
                            accentColor = Color(0xFF0EA5E9),
                            checked = canManageSupport,
                            onCheckedChange = { canManageSupport = it },
                            tag = "dialog_perm_manage_support"
                        )

                        PermissionToggleItem(
                            title = "Broadcast Alerts",
                            subtitle = "Send custom push notifications & match announcements",
                            icon = Icons.Default.Campaign,
                            accentColor = Color(0xFF10B981),
                            checked = canBroadcast,
                            onCheckedChange = { canBroadcast = it },
                            tag = "dialog_perm_broadcast"
                        )

                        PermissionToggleItem(
                            title = "Withdrawal Management",
                            subtitle = "Approve & mark paid or reject player UPI withdrawal requests",
                            icon = Icons.Default.Payments,
                            accentColor = SuccessGreen,
                            checked = canManageWithdrawals,
                            onCheckedChange = { canManageWithdrawals = it },
                            tag = "dialog_perm_withdrawals"
                        )

                        PermissionToggleItem(
                            title = "Application & Ads Settings",
                            subtitle = "Edit app settings, signup bonus & Unity Ads account config",
                            icon = Icons.Default.Settings,
                            accentColor = Color(0xFF8B5CF6),
                            checked = canManageSettings,
                            onCheckedChange = { canManageSettings = it },
                            tag = "dialog_perm_settings"
                        )
                    }

                    // Pinned Bottom Action Bar
                    HorizontalDivider(color = Color(0xFFCBD5E1), thickness = 1.dp)
                    Surface(
                        color = SlateCardBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, Color(0xFF94A3B8)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimaryDark
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                            }

                            Button(
                                onClick = {
                                    val cleanName = username.trim().ifBlank { email.trim().substringBefore("@") }
                                    val usernameKey = AuthManager.normalizeUsernameKey(cleanName)
                                    val staffObj = StaffMemberDto(
                                        id = initialStaff?.id ?: "staff_$usernameKey",
                                        userId = selectedUserId.ifBlank { initialStaff?.userId ?: "user_$usernameKey" },
                                        username = cleanName,
                                        usernameLower = usernameKey,
                                        email = email.trim().lowercase(),
                                        roleTitle = roleTitle.trim().ifBlank { "Staff Moderator" },
                                        passwordHash = initialStaff?.passwordHash ?: "",
                                        isActive = isActive,
                                        canCreateMatch = canCreateMatch,
                                        canManageBalance = canManageBalance,
                                        canManageSupport = canManageSupport,
                                        canBroadcast = canBroadcast,
                                        canManageWithdrawals = canManageWithdrawals,
                                        canManageSettings = canManageSettings,
                                        addedBy = currentAdminName,
                                        createdAt = initialStaff?.createdAt ?: System.currentTimeMillis(),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    onSave(staffObj, plainPassword.trim())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FireOrange),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("dialog_save_staff_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEditing) "Save Changes" else "Save Staff",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
