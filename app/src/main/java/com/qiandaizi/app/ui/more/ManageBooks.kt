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
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.BookDto
import com.qiandaizi.app.core.BookReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.UsernameReq
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.ui.common.ConfirmDialog
import com.qiandaizi.app.ui.common.Pill
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.TextInputDialog
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

@Composable
fun BooksScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var books by remember { mutableStateOf<List<BookDto>>(emptyList()) }
    var members by remember { mutableStateOf<List<com.qiandaizi.app.core.AttrMember>>(emptyList()) }
    var newMemberName by remember { mutableStateOf("") }
    var addDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<BookDto?>(null) }
    var deleteTarget by remember { mutableStateOf<BookDto?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            runCatching {
                val list = appState.api().books()
                books = list
                val cur = appState.bookId()
                if (cur != null) {
                    members = appState.api().bookMembers(cur)
                }
            }.onFailure { appState.notify(explainError(it)) }
        }
    }

    LaunchedEffect(Unit) { load() }

    SubPageScaffold(
        title = "账本管理",
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "新建账本",
                tint = TextMain,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { addDialog = true }
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
                Text("我的账本", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                books.forEach { b ->
                    val current = b.id == appState.bookId()
                    val isOwner = b.role == "owner"
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (current) Color(0xFFFFF8E1) else Color.Transparent)
                            .clickable {
                                scope.launch {
                                    appState.switchBook(b.id)
                                    members = appState.api().bookMembers(b.id)
                                    appState.notify("已切换到「${b.name}」")
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (current) com.qiandaizi.app.core.YellowDark
                                    else Color(0xFFE4E6EA)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (current) Text("✓", color = Color.White, fontSize = 11.sp)
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                color = TextMain)
                            Text(
                                "${b.members} 位成员 · ${b.flows} 笔记录",
                                fontSize = 11.sp, color = TextSub
                            )
                        }
                        Pill(
                            if (isOwner) "我创建的" else "共享",
                            if (isOwner) Color(0xFFFFF3C4) else Color(0xFFEEF0F3),
                            if (isOwner) Color(0xFF8A6D1B) else TextSub
                        )
                        if (isOwner) {
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "重命名",
                                tint = TextSub,
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { renameTarget = b }
                                    .padding(6.dp)
                            )
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除",
                                tint = Color(0xFFE5484D),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { deleteTarget = b }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 当前账本成员
            WhiteCard {
                Text("当前账本成员", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                members.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE79B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(m.nickname.take(1), fontSize = 13.sp, color = TextMain)
                        }
                        Spacer(Modifier.size(10.dp))
                        Text(m.nickname, fontSize = 14.sp, color = TextMain,
                            modifier = Modifier.weight(1f))
                        val isOwnerBook = books.find { it.id == appState.bookId() }?.role == "owner"
                        if (isOwnerBook && m.id != appState.user()?.id) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "移除成员",
                                tint = Color(0xFFE5484D),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        scope.launch {
                                            runCatching {
                                                appState.api().removeBookMember(
                                                    appState.bookId!!, m.id
                                                )
                                            }.onSuccess {
                                                members = appState.api()
                                                    .bookMembers(appState.bookId!!)
                                                appState.notify("已移除成员")
                                            }.onFailure {
                                                appState.notify(explainError(it))
                                            }
                                        }
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newMemberName,
                        onValueChange = { newMemberName = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入对方账号", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.size(10.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(com.qiandaizi.app.core.Yellow)
                            .clickable(enabled = !busy) {
                                if (newMemberName.isBlank()) return@clickable
                                busy = true
                                scope.launch {
                                    runCatching {
                                        appState.api().addBookMember(
                                            appState.bookId!!,
                                            UsernameReq(newMemberName.trim())
                                        )
                                    }.onSuccess {
                                        newMemberName = ""
                                        members = appState.api()
                                            .bookMembers(appState.bookId!!)
                                        appState.notify("成员已加入")
                                    }.onFailure {
                                        appState.notify(explainError(it))
                                    }
                                    busy = false
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text("邀请", fontSize = 14.sp, color = TextMain,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (addDialog) {
        TextInputDialog(
            title = "新建账本",
            placeholder = "给账本起个名字",
            confirmText = "创建",
            onDismiss = { addDialog = false }
        ) { input ->
            addDialog = false
            scope.launch {
                runCatching { appState.api().createBook(BookReq(input)) }
                    .onSuccess {
                        load()
                        appState.notify("账本已创建")
                    }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    renameTarget?.let { b ->
        TextInputDialog(
            title = "重命名账本",
            initial = b.name,
            onDismiss = { renameTarget = null }
        ) { input ->
            renameTarget = null
            scope.launch {
                runCatching { appState.api().renameBook(b.id, BookReq(input)) }
                    .onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    deleteTarget?.let { b ->
        ConfirmDialog(
            title = "删除账本",
            message = "确定删除「${b.name}」？账本内全部流水将被删除且不可恢复。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    runCatching { appState.api().deleteBook(b.id) }
                        .onSuccess {
                            load()
                            appState.notify("账本已删除")
                        }
                        .onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}
