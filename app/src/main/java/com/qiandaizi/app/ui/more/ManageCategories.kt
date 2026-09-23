package com.qiandaizi.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.qiandaizi.app.core.CategoryDto
import com.qiandaizi.app.core.CategoryReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.ui.common.ConfirmDialog
import com.qiandaizi.app.ui.common.Pill
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

private val iconChoices = listOf(
    "🍔", "🍜", "☕", "🛒", "🚗", "🚌", "⛽", "🏠", "💡", "📱",
    "🎮", "👕", "💊", "📚", "✈️", "🎁", "💰", "💼", "🧧", "💄", "🐱", "🧹"
)
private val colorChoices = listOf(
    "#F04438", "#F79009", "#EAAA08", "#12B76A",
    "#2E90FA", "#6172F3", "#7A5AF8", "#EE46BC", "#667085"
)

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var list by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var editTarget by remember { mutableStateOf<CategoryDto?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CategoryDto?>(null) }

    fun load() {
        scope.launch {
            runCatching { appState.api().categories() }
                .onSuccess { list = it }
                .onFailure { appState.notify(explainError(it)) }
        }
    }
    LaunchedEffect(Unit) { load() }

    SubPageScaffold(
        title = "类目管理",
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "新增类目",
                tint = TextMain,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        editTarget = null
                        showEditor = true
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
                list.sortedWith(compareBy({ it.type }, { it.sort })).forEach { c ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF5F6F8)),
                            contentAlignment = Alignment.Center
                        ) { Text(c.icon, fontSize = 17.sp) }
                        Spacer(Modifier.size(10.dp))
                        Text(c.name, fontSize = 14.sp, color = TextMain,
                            modifier = Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    runCatching {
                                        Color(android.graphics.Color.parseColor(c.color))
                                    }.getOrDefault(Color.Gray)
                                )
                        )
                        Spacer(Modifier.size(8.dp))
                        Pill(
                            if (c.type == "income") "收入" else "支出",
                            if (c.type == "income") Color(0xFFE7F8F0) else Color(0xFFFDECEC),
                            if (c.type == "income") Color(0xFF16875B) else Color(0xFFCB3232)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text("${c.flowCount} 笔", fontSize = 11.sp, color = TextSub)
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑",
                            tint = TextSub,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    editTarget = c
                                    showEditor = true
                                }
                                .padding(6.dp)
                        )
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除",
                            tint = Color(0xFFE5484D),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { deleteTarget = c }
                                .padding(6.dp)
                        )
                    }
                }
                if (list.isEmpty()) {
                    Text("暂无类目，点击右上角 + 新建", fontSize = 13.sp,
                        color = TextSub, modifier = Modifier.padding(vertical = 14.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showEditor) {
        CategoryEditorDialog(
            initial = editTarget,
            onDismiss = { showEditor = false },
            onSaved = {
                showEditor = false
                load()
            }
        )
    }

    deleteTarget?.let { c ->
        ConfirmDialog(
            title = "删除类目",
            message = "确定删除类目「${c.name}」？历史记录中的该类目名称仍会保留。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    runCatching { appState.api().deleteCategory(c.id) }
                        .onSuccess { load() }
                        .onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    initial: CategoryDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "expense") }
    var icon by remember { mutableStateOf(initial?.icon ?: "💰") }
    var color by remember { mutableStateOf(initial?.color ?: "#6172F3") }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "新增类目" else "编辑类目",
                fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF2F3F5))
                        .padding(3.dp)
                ) {
                    listOf("expense" to "支出", "income" to "收入").forEach { (key, label) ->
                        val active = type == key
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) Color.White else Color.Transparent)
                                .clickable { type = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 13.sp,
                                color = if (active) TextMain else TextSub,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("名称", fontSize = 12.sp, color = TextSub)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF6F7F9))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("类目名称，如：餐饮", fontSize = 13.sp) },
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("图标", fontSize = 12.sp, color = TextSub)
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iconChoices.forEach { e ->
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (icon == e) Color(0xFFFFF3C4) else Color(0xFFF6F7F9)
                                )
                                .clickable { icon = e },
                            contentAlignment = Alignment.Center
                        ) { Text(e, fontSize = 16.sp) }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("颜色", fontSize = 12.sp, color = TextSub)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorChoices.forEach { hex ->
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .clickable { color = hex }
                        ) {
                            if (color == hex) Text("✓", color = Color.White, fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        appState.notify("请填写类目名称")
                        return@TextButton
                    }
                    saving = true
                    scope.launch {
                        runCatching {
                            val req = CategoryReq(name.trim(), type, icon, color)
                            if (initial == null) appState.api().createCategory(req)
                            else appState.api().updateCategory(initial.id, req)
                        }.onSuccess { onSaved() }
                            .onFailure { appState.notify(explainError(it)) }
                        saving = false
                    }
                },
                enabled = !saving
            ) { Text("保存", color = com.qiandaizi.app.core.YellowDark) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = TextSub) }
        }
    )
}
