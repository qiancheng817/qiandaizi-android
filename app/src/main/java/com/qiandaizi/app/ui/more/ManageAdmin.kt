package com.qiandaizi.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AdminUserDto
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.CreateUserReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.ui.common.ConfirmDialog
import com.qiandaizi.app.ui.common.Pill
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

@Composable
fun AdminUsersScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<AdminUserDto>>(emptyList()) }
    var addOpen by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<AdminUserDto?>(null) }
    var deleteTarget by remember { mutableStateOf<AdminUserDto?>(null) }
    val currentUser = appState.user()

    fun load() {
        scope.launch {
            runCatching { appState.api().adminUsers() }
                .onSuccess { users = it }
                .onFailure { appState.notify(explainError(it)) }
        }
    }
    LaunchedEffect(Unit) { load() }

    SubPageScaffold(
        title = "用户管理",
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "新增用户",
                tint = TextMain,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        editTarget = null
                        addOpen = true
                    }
                    .padding(6.dp)
            )
        }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            WhiteCard {
                users.forEach { u ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE79B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                u.nickname.take(1).ifBlank { u.username.take(1) },
                                fontSize = 15.sp, color = TextMain
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(u.nickname.ifBlank { u.username }, fontSize = 14.sp,
                                    color = TextMain)
                                Spacer(Modifier.size(6.dp))
                                Pill(
                                    if (u.role == "admin") "管理员" else "普通用户",
                                    if (u.role == "admin") Color(0xFFFFF3C4) else Color(0xFFEEF0F3),
                                    if (u.role == "admin") Color(0xFF8A6D1B) else TextSub
                                )
                            }
                            Text(
                                "${u.username} · ${u.books}账本 · ${u.flows}笔流水",
                                fontSize = 11.sp, color = TextSub,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑/重置密码",
                            tint = TextSub,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable {
                                    editTarget = u
                                    addOpen = true
                                }
                                .padding(7.dp)
                        )
                        if (u.id != currentUser?.id) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除用户",
                                tint = Color(0xFFE5484D),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { deleteTarget = u }
                                    .padding(7.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (addOpen) {
        UserEditDialog(
            initial = editTarget,
            onDismiss = {
                addOpen = false
                editTarget = null
            }
        ) { nickname, password, role, username ->
            val target = editTarget
            addOpen = false
            editTarget = null
            scope.launch {
                runCatching {
                    if (target == null) {
                        appState.api().createUser(
                            CreateUserReq(
                                username = username,
                                password = password,
                                nickname = nickname,
                                role = role
                            )
                        )
                    } else {
                        appState.api().updateUser(
                            target.id,
                            CreateUserReq(
                                username = username,
                                password = password.ifBlank { null },
                                nickname = nickname,
                                role = role
                            )
                        )
                    }
                }.onSuccess {
                    load()
                    appState.notify(if (target == null) "用户已创建" else "已保存")
                }.onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    deleteTarget?.let { u ->
        ConfirmDialog(
            title = "删除用户",
            message = "确定删除用户「${u.username}」？该用户的账本与流水不会删除。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    runCatching { appState.api().deleteUser(u.id) }
                        .onSuccess { load() }
                        .onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun UserEditDialog(
    initial: AdminUserDto?,
    onDismiss: () -> Unit,
    onConfirm: (nickname: String, password: String, role: String, username: String) -> Unit
) {
    var username by remember { mutableStateOf(initial?.username ?: "") }
    var nickname by remember { mutableStateOf(initial?.nickname ?: "") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(initial?.role ?: "user") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "新增用户" else "编辑用户",
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
            if (initial == null) {
                OutlinedTextField(value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    placeholder = { Text("登录账号", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }
            OutlinedTextField(value = nickname,
                onValueChange = { nickname = it },
                singleLine = true,
                placeholder = { Text("昵称（可空）", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = password,
                onValueChange = { password = it },
                singleLine = true,
                placeholder = {
                    Text(
                        if (initial == null) "初始密码"
                        else "新密码（留空表示不修改）",
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF2F3F5))
                    .padding(3.dp)
            ) {
                listOf("user" to "普通用户", "admin" to "管理员").forEach { (key, label) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (role == key) Color.White else Color.Transparent)
                            .clickable { role = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 13.sp,
                            color = if (role == key) TextMain else TextSub)
                    }
                }
            }
            }
        },
        confirmButton = {
            Text("保存", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        if (initial == null &&
                            (username.isBlank() || password.isBlank())) {
                            AppGraph.state.notify("请填写账号和初始密码")
                            return@clickable
                        }
                        onConfirm(nickname.trim(), password, role, username.trim())
                    }
                    .padding(8.dp))
        },
        dismissButton = {
            Text("取消", fontSize = 14.sp, color = TextSub,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp))
        }
    )
}
