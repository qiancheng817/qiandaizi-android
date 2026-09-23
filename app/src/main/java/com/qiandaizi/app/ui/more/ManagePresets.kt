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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.PresetDto
import com.qiandaizi.app.core.PresetHideReq
import com.qiandaizi.app.core.PresetReq
import com.qiandaizi.app.core.PresetsDto
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

@Composable
fun PresetsScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var typeIndex by rememberSaveable { mutableIntStateOf(0) }
    var data by remember { mutableStateOf<PresetsDto?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val type = if (typeIndex == 1) "income" else "expense"

    fun load() {
        scope.launch {
            runCatching { appState.api().presets(type) }
                .onSuccess { data = it }
                .onFailure { appState.notify(explainError(it)) }
        }
    }
    LaunchedEffect(type) { load() }

    SubPageScaffold(
        title = "常用名称",
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "添加常用名",
                tint = TextMain,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { addOpen = true }
                    .padding(6.dp)
            )
        }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F3F5))
                    .padding(3.dp)
            ) {
                listOf("支出", "收入").forEachIndexed { i, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (typeIndex == i) Color.White else Color.Transparent)
                            .clickable { typeIndex = i }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 13.sp,
                            color = if (typeIndex == i) TextMain else TextSub,
                            fontWeight = if (typeIndex == i) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable(enabled = !busy) {
                        busy = true
                        scope.launch {
                            runCatching { appState.api().scanPresets() }
                                .onSuccess {
                                    load()
                                    appState.notify("已重新扫描历史账单")
                                }
                                .onFailure { appState.notify(explainError(it)) }
                            busy = false
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { Text("🔍 重新扫描历史账单", fontSize = 13.sp, color = TextMain) }

            Spacer(Modifier.height(14.dp))

            val d = data
            PresetGroup("高频常用", d?.frequent ?: emptyList(), canHide = true)
            Spacer(Modifier.height(12.dp))
            PresetGroup("最近使用", d?.recent ?: emptyList(), canHide = true)
            Spacer(Modifier.height(12.dp))
            PresetGroup("已隐藏", d?.hidden ?: emptyList(), hiddenGroup = true)
            Spacer(Modifier.height(20.dp))
        }
    }

    if (addOpen) {
        AddPresetDialog(
            type = type,
            onDismiss = { addOpen = false }
        ) { name, cat, payment, amount ->
            addOpen = false
            scope.launch {
                runCatching {
                    appState.api().addPreset(
                        PresetReq(
                            name = name,
                            type = type,
                            category = cat.ifBlank { null },
                            paymentMethod = payment.ifBlank { null },
                            amount = amount
                        )
                    )
                }.onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }
}

@Composable
private fun PresetGroup(
    title: String,
    items: List<PresetDto>,
    canHide: Boolean = false,
    hiddenGroup: Boolean = false
) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    WhiteCard {
        Text("$title（${items.size}）", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (items.isEmpty()) {
            Text("暂无", fontSize = 12.sp, color = TextSub,
                modifier = Modifier.padding(vertical = 10.dp))
        }
        items.forEach { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 14.sp, color = TextMain)
                    Text(
                        listOf(p.category, p.paymentMethod)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .ifBlank { "—" },
                        fontSize = 11.sp, color = TextSub,
                        modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    if (p.count > 0) "${p.count}次" else "",
                    fontSize = 11.sp, color = TextSub
                )
                if (hiddenGroup) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = "取消隐藏",
                        tint = com.qiandaizi.app.core.YellowDark,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    runCatching {
                                        appState.api().unhidePreset(PresetHideReq(p.name))
                                    }.onSuccess {
                                        appState.notify("已取消隐藏")
                                    }.onFailure {
                                        appState.notify(explainError(it))
                                    }
                                }
                            }
                            .padding(7.dp)
                    )
                } else if (canHide) {
                    Icon(
                        Icons.Filled.VisibilityOff,
                        contentDescription = "隐藏",
                        tint = TextSub,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch {
                                    runCatching {
                                        appState.api().hidePreset(PresetHideReq(p.name))
                                    }.onFailure {
                                        appState.notify(explainError(it))
                                    }
                                }
                            }
                            .padding(7.dp)
                    )
                }
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除",
                    tint = Color(0xFFE5484D),
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable {
                            scope.launch {
                                runCatching { appState.api().deletePreset(p.id) }
                                    .onFailure { appState.notify(explainError(it)) }
                            }
                        }
                        .padding(7.dp)
                )
            }
        }
    }
}

@Composable
private fun AddPresetDialog(
    type: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, payment: String, amount: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加常用名称", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = Column {
            OutlinedTextField(value = name, onValueChange = { name = it },
                singleLine = true, placeholder = { Text("名称，如：星巴克", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = category, onValueChange = { category = it },
                singleLine = true, placeholder = { Text("默认分类（可空）", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = payment, onValueChange = { payment = it },
                singleLine = true, placeholder = { Text("默认支付方式（可空）", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = amountText, onValueChange = {
                if (it.all { c -> c.isDigit() || c == '.' }) amountText = it
            }, singleLine = true, placeholder = { Text("默认金额（可空）", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Text("保存", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        if (name.isBlank()) {
                            AppGraph.state.notify("请填写名称")
                            return@clickable
                        }
                        onConfirm(
                            name.trim(), category.trim(), payment.trim(),
                            amountText.toDoubleOrNull() ?: 0.0
                        )
                    }
                    .padding(8.dp))
        },
        dismissButton = {
            Text("取消", fontSize = 14.sp, color = TextSub,
                modifier = Modifier
                    .clickable(onDismiss)
                    .padding(8.dp))
        }
    )
}
