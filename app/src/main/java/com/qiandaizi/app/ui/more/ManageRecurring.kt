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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.qiandaizi.app.core.RecurringDto
import com.qiandaizi.app.core.RecurringReq
import com.qiandaizi.app.core.TextMain
import androidx.compose.ui.text.style.TextAlign
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import com.qiandaizi.app.ui.common.ConfirmDialog
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

@Composable
fun RecurringScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var list by remember { mutableStateOf<List<RecurringDto>>(emptyList()) }
    var showEditor by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<RecurringDto?>(null) }
    var deleteTarget by remember { mutableStateOf<RecurringDto?>(null) }
    var generating by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            runCatching { appState.api().recurring() }
                .onSuccess { list = it }
                .onFailure { appState.notify(explainError(it)) }
        }
    }
    LaunchedEffect(Unit) { load() }

    SubPageScaffold(
        title = "定期记账",
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "新增",
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("自动生成本月定期账单", fontSize = 14.sp, color = TextMain,
                            fontWeight = FontWeight.Medium)
                        Text("每月固定支出如房租、会员费可自动入账", fontSize = 11.sp,
                            color = TextSub, modifier = Modifier.padding(top = 2.dp))
                    }
                    Button(
                        onClick = {
                            generating = true
                            scope.launch {
                                runCatching { appState.api().generateRecurring() }
                                    .onSuccess {
                                        appState.notify("已生成 ${it.generated} 笔定期账单")
                                        appState.bump()
                                    }
                                    .onFailure { appState.notify(explainError(it)) }
                                generating = false
                            }
                        },
                        enabled = !generating,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.qiandaizi.app.core.Yellow)
                    ) {
                        Text(if (generating) "…" else "立即生成", color = TextMain,
                            fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            WhiteCard {
                Text("定期账单列表（${list.size}）", fontSize = 15.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (list.isEmpty()) {
                    Text("暂无定期账单，点右上角 + 添加", fontSize = 13.sp,
                        color = TextSub, modifier = Modifier.padding(vertical = 12.dp))
                }
                list.forEach { r ->
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
                                    if (r.type == "income") Color(0xFFE7F8F0)
                                    else Color(0xFFFDECEC)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (r.type == "income") "收" else "支",
                                fontSize = 13.sp,
                                color = if (r.type == "income") Color(0xFF16875B)
                                else Color(0xFFCB3232)
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${r.category} · ${r.description.ifBlank { r.category }}",
                                fontSize = 14.sp, color = TextMain
                            )
                            Text(
                                if (r.freq == "yearly")
                                    "每年 ${r.monthOfYear}月${r.dayOfMonth}日"
                                else "每月 ${r.dayOfMonth} 日" +
                                    (if (r.paymentMethod.isNotBlank()) " · ${r.paymentMethod}" else ""),
                                fontSize = 11.sp, color = TextSub,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text(money(r.amount), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = TextMain)
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑",
                            tint = TextSub,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable {
                                    editTarget = r
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
                                .clickable { deleteTarget = r }
                                .padding(6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showEditor) {
        RecurringEditorDialog(
            initial = editTarget,
            onDismiss = { showEditor = false },
            onSaved = {
                showEditor = false
                load()
            }
        )
    }

    deleteTarget?.let { r ->
        ConfirmDialog(
            title = "删除定期账单",
            message = "确定删除「${r.description.ifBlank { r.category }}」？",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    runCatching { appState.api().deleteRecurring(r.id) }
                        .onSuccess { load() }
                        .onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun RecurringEditorDialog(
    initial: RecurringDto?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf(initial?.type ?: "expense") }
    var category by remember { mutableStateOf(initial?.category ?: "其他") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var payment by remember { mutableStateOf(initial?.paymentMethod ?: "") }
    var freq by remember { mutableStateOf(initial?.freq ?: "monthly") }
    var day by remember { mutableStateOf((initial?.dayOfMonth ?: 1).toString()) }
    var month by remember { mutableStateOf((initial?.monthOfYear ?: 1).toString()) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var saving by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "新增定期账单" else "编辑定期账单",
                fontSize = 16.sp, fontWeight = FontWeight.Bold
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
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (type == key) Color.White else Color.Transparent)
                                .clickable { type = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 13.sp,
                                color = if (type == key) TextMain else TextSub)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                DialogField("分类", category) { category = it }
                DialogField("名称描述", description) { description = it }
                DialogField("金额", amountText, numeric = true) { amountText = it }
                DialogField("支付方式", payment) { payment = it }

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF2F3F5))
                        .padding(3.dp)
                ) {
                    listOf("monthly" to "每月", "yearly" to "每年").forEach { (key, label) ->
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (freq == key) Color.White else Color.Transparent)
                                .clickable { freq = key }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 13.sp,
                                color = if (freq == key) TextMain else TextSub)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                DialogField("几号扣款（1-28）", day, numeric = true) { day = it }
                if (freq == "yearly") {
                    DialogField("几月（1-12）", month, numeric = true) { month = it }
                }
                DialogField("备注", note) { note = it }
            }
        },
        confirmButton = {
            Text(
                "保存",
                fontSize = 14.sp,
                color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        val amount = amountText.toDoubleOrNull() ?: 0.0
                        val dayVal = day.toIntOrNull()?.coerceIn(1, 28) ?: 1
                        if (amount <= 0) {
                            appState.notify("请填写正确金额")
                            return@clickable
                        }
                        saving = true
                        scope.launch {
                            val req = RecurringReq(
                                type = type,
                                category = category.ifBlank { "其他" },
                                description = description.ifBlank { null },
                                amount = amount,
                                paymentMethod = payment.ifBlank { null },
                                freq = freq,
                                dayOfMonth = dayVal,
                                monthOfYear = month.toIntOrNull()?.coerceIn(1, 12) ?: 1,
                                note = note.ifBlank { null }
                            )
                            runCatching {
                                if (initial == null) appState.api().addRecurring(req)
                                else appState.api().updateRecurring(initial.id, req)
                            }.onSuccess { onSaved() }
                                .onFailure { appState.notify(explainError(it)) }
                            saving = false
                        }
                    }
                    .padding(8.dp)
            )
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
private fun DialogField(
    label: String,
    value: String,
    numeric: Boolean = false,
    onChange: (String) -> Unit
) {
    Text(label, fontSize = 12.sp, color = TextSub)
    Spacer(Modifier.height(4.dp))
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = if (numeric)
            androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            )
        else androidx.compose.foundation.text.KeyboardOptions(),
        shape = RoundedCornerShape(10.dp)
    )
    Spacer(Modifier.height(8.dp))
}
