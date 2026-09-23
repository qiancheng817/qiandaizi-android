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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.qiandaizi.app.core.SavingsItemDto
import com.qiandaizi.app.core.SavingsItemReq
import com.qiandaizi.app.core.SavingsGoalReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun SavingsScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var goalTarget by remember { mutableStateOf(0.0) }
    var goalNote by remember { mutableStateOf("") }
    var hasGoal by remember { mutableStateOf(false) }
    var netAsset by remember { mutableStateOf(0.0) }
    var percent by remember { mutableStateOf(0) }
    var items by remember { mutableStateOf<List<SavingsItemDto>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var showItemDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<SavingsItemDto?>(null) }
    var amountTarget by remember { mutableStateOf<SavingsItemDto?>(null) }
    var deleteItem by remember { mutableStateOf<SavingsItemDto?>(null) }

    fun load() {
        scope.launch {
            runCatching { appState.api().savings() }
                .onSuccess { d ->
                    d.goal?.let { g ->
                        goalTarget = g["target"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                        goalNote = g["note"]?.jsonPrimitive?.content ?: ""
                        hasGoal = true
                    }
                    d.current?.let { c ->
                        netAsset = c["net"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                        percent = c["percent"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    }
                    items = d.items
                    loaded = true
                }
                .onFailure {
                    appState.notify(explainError(it))
                }
        }
    }
    LaunchedEffect(Unit) { load() }

    com.qiandaizi.app.ui.common.SubPageScaffold(
        title = "资产管理",
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "新增资产项",
                tint = TextMain,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable {
                        editItem = null
                        showItemDialog = true
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
            // 目标卡片
            com.qiandaizi.app.ui.common.WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("攒钱目标", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text("编辑 ›", fontSize = 13.sp, color = Color(0xFF8A6D1B),
                        modifier = Modifier.clickable { showGoalDialog = true })
                }
                Spacer(Modifier.height(12.dp))
                if (!hasGoal || goalTarget <= 0) {
                    Text("还没有设置目标金额", fontSize = 13.sp, color = TextSub)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("目标 ", fontSize = 13.sp, color = TextSub)
                        Text(money(goalTarget), fontSize = 19.sp,
                            fontWeight = FontWeight.Bold, color = TextMain)
                        Spacer(Modifier.size(10.dp))
                        Text("$percent%", fontSize = 13.sp,
                            color = Color(0xFF8A6D1B), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFEEEEEE))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(com.qiandaizi.app.core.Yellow)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("当前净资产 ", fontSize = 13.sp, color = TextSub)
                        Text(money(netAsset), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = if (netAsset >= 0) com.qiandaizi.app.core.IncomeGreen
                            else com.qiandaizi.app.core.ExpenseRed)
                    }
                    if (goalNote.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("备注：$goalNote", fontSize = 12.sp, color = TextSub)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            com.qiandaizi.app.ui.common.WhiteCard {
                Text("资产 / 负债（${items.size}）", fontSize = 15.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (loaded && items.isEmpty()) {
                    Text("暂无资产项，点右上角 + 添加", fontSize = 13.sp,
                        color = TextSub, modifier = Modifier.padding(vertical = 12.dp))
                }
                items.forEach { item ->
                    val isAsset = item.sign >= 0
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isAsset) Color(0xFFE7F8F0) else Color(0xFFFDECEC)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isAsset) "💰" else "💳", fontSize = 17.sp)
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontSize = 14.sp, color = TextMain)
                            Text(
                                (if (isAsset) "资产" else "负债") +
                                    (if (item.note.isNotBlank()) " · ${item.note}" else ""),
                                fontSize = 11.sp, color = TextSub,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text(money(item.amount), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (isAsset) TextMain else com.qiandaizi.app.core.ExpenseRed)
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "更新金额",
                            tint = TextSub,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { amountTarget = item }
                                .padding(6.dp)
                        )
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除",
                            tint = Color(0xFFE5484D),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { deleteItem = item }
                                .padding(6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            initialTarget = if (hasGoal) goalTarget.toString() else "",
            initialNote = goalNote,
            onDismiss = { showGoalDialog = false }
        ) { target, note ->
            showGoalDialog = false
            scope.launch {
                runCatching {
                    appState.api().setSavingsGoal(
                        SavingsGoalReq(target = target, note = note.ifBlank { null })
                    )
                }.onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    if (showItemDialog) {
        ItemDialog(
            initial = editItem,
            onDismiss = { showItemDialog = false }
        ) { name, sign, amount, note ->
            showItemDialog = false
            scope.launch {
                val req = SavingsItemReq(name, sign, amount, note.ifBlank { null })
                runCatching {
                    if (editItem == null) appState.api().addSavingsItem(req)
                    else appState.api().updateSavingsItem(editItem!!.id, req)
                }.onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    amountTarget?.let { item ->
        SetAmountDialog(
            title = "更新「${item.name}」金额",
            initial = item.amount.toString(),
            onDismiss = { amountTarget = null }
        ) { amount ->
            amountTarget = null
            scope.launch {
                runCatching {
                    appState.api().setSavingsItemAmount(
                        item.id,
                        com.qiandaizi.app.core.SetAmountReq(amount = amount)
                    )
                }.onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    deleteItem?.let { item ->
        com.qiandaizi.app.ui.common.ConfirmDialog(
            title = "删除资产项",
            message = "确定删除「${item.name}」？",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteItem = null
                scope.launch {
                    runCatching { appState.api().deleteSavingsItem(item.id) }
                        .onSuccess { load() }
                        .onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { deleteItem = null }
        )
    }
}

@Composable
private fun GoalDialog(
    initialTarget: String,
    initialNote: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var target by remember { mutableStateOf(initialTarget) }
    var note by remember { mutableStateOf(initialNote) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("攒钱目标", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
            OutlinedTextField(
                value = target,
                onValueChange = { target = it },
                singleLine = true,
                placeholder = { Text("目标金额", fontSize = 13.sp) }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                singleLine = true,
                placeholder = { Text("备注（如：买房首付）", fontSize = 13.sp) }
            )
            }
        },
        confirmButton = {
            Text("保存", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        val v = target.toDoubleOrNull() ?: 0.0
                        if (v <= 0) {
                            AppGraph.state.notify("请填写正确金额")
                            return@clickable
                        }
                        onConfirm(v, note.trim())
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

@Composable
private fun ItemDialog(
    initial: SavingsItemDto?,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sign by remember { mutableStateOf(initial?.sign ?: 1) }
    var amountText by remember {
        mutableStateOf(initial?.amount?.takeIf { it != 0.0 }?.toString() ?: "")
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "新增资产项" else "编辑资产项",
                fontSize = 16.sp, fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF2F3F5))
                    .padding(3.dp)
            ) {
                listOf(1 to "资产", -1 to "负债").forEach { (key, label) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sign == key) Color.White else Color.Transparent)
                            .clickable { sign = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 13.sp,
                            color = if (sign == key) TextMain else TextSub)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("名称，如：储蓄卡余额", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    if (it.all { c -> c.isDigit() || c == '.' }) amountText = it
                },
                singleLine = true,
                placeholder = { Text("当前金额", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                singleLine = true,
                placeholder = { Text("备注（可空）", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth()
            )
            }
        },
        confirmButton = {
            Text("保存", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        if (name.isBlank() || amount < 0) {
                            AppGraph.state.notify("请填写名称和正确金额")
                            return@clickable
                        }
                        onConfirm(name.trim(), sign, amount, note.trim())
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

@Composable
private fun SetAmountDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    if (it.all { c -> c.isDigit() || c == '.' }) text = it
                },
                singleLine = true,
                placeholder = { Text("新的金额", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Text("确定", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        val v = text.toDoubleOrNull() ?: -1.0
                        if (v < 0) {
                            AppGraph.state.notify("请填写正确金额")
                            return@clickable
                        }
                        onConfirm(v)
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
