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
import com.qiandaizi.app.core.SetAmountReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.WalletDto
import com.qiandaizi.app.core.WalletReq
import com.qiandaizi.app.core.WalletTxnReq
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.md
import com.qiandaizi.app.core.money
import com.qiandaizi.app.ui.common.ConfirmDialog
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

@Composable
fun WalletsScreen(
    onBack: () -> Unit,
    push: (com.qiandaizi.app.ui.Route) -> Unit
) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var wallets by remember { mutableStateOf<List<WalletDto>>(emptyList()) }
    var addOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<WalletDto?>(null) }

    fun load() {
        scope.launch {
            runCatching { appState.api().wallets() }
                .onSuccess { wallets = it.wallets }
                .onFailure { appState.notify(explainError(it)) }
        }
    }
    LaunchedEffect(Unit) { load() }

    SubPageScaffold(
        title = "钱包",
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "新增钱包",
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
            WhiteCard {
                if (wallets.isEmpty()) {
                    Text("还没有钱包，点击右上角 + 创建", fontSize = 13.sp,
                        color = TextSub, modifier = Modifier.padding(vertical = 12.dp))
                }
                wallets.forEach { w ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                push(com.qiandaizi.app.ui.Route.WalletDetail(w.id, w.name))
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFF3C4)),
                            contentAlignment = Alignment.Center
                        ) { Text(w.icon, fontSize = 19.sp) }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(w.name, fontSize = 14.sp, color = TextMain)
                            if (w.target > 0) {
                                Text("目标 ${money(w.target)}", fontSize = 11.sp,
                                    color = TextSub, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Text(money(w.balance), fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            color = TextMain)
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除钱包",
                            tint = Color(0xFFE5484D),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { deleteTarget = w }
                                .padding(6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (addOpen) {
        WalletEditDialog(
            initial = null,
            onDismiss = { addOpen = false }
        ) { name, icon, target, note ->
            addOpen = false
            scope.launch {
                runCatching {
                    appState.api().addWallet(
                        WalletReq(
                            name = name,
                            icon = icon,
                            target = target,
                            note = note.ifBlank { null }
                        )
                    )
                }.onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    deleteTarget?.let { w ->
        ConfirmDialog(
            title = "删除钱包",
            message = "确定删除钱包「${w.name}」？其存取记录会一并删除。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    runCatching { appState.api().deleteWallet(w.id) }
                        .onSuccess { load() }
                        .onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
fun WalletDetailScreen(id: Int, name: String, onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var wallet by remember { mutableStateOf<WalletDto?>(null) }
    var txns by remember {
        mutableStateOf<List<com.qiandaizi.app.core.WalletTxnDto>>(emptyList())
    }
    var addOpen by remember { mutableStateOf(false) }
    var deleteTxn by remember {
        mutableStateOf<com.qiandaizi.app.core.WalletTxnDto?>(null)
    }

    fun load() {
        scope.launch {
            runCatching {
                val all = appState.api().wallets().wallets
                wallet = all.firstOrNull { it.id == id }
                txns = appState.api().walletTxns(id).list
            }.onFailure { appState.notify(explainError(it)) }
        }
    }
    LaunchedEffect(Unit) { load() }

    SubPageScaffold(
        title = name,
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Add,
                contentDescription = "新增存取记录",
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
            WhiteCard {
                Text("当前余额", fontSize = 13.sp, color = TextSub)
                Spacer(Modifier.height(6.dp))
                Text(money(wallet?.balance), fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = TextMain)
                if ((wallet?.target ?: 0.0) > 0) {
                    Spacer(Modifier.height(8.dp))
                    val fraction = ((wallet?.balance ?: 0.0) / wallet!!.target).toFloat()
                        .coerceIn(0f, 1f)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFEEEEEE))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(com.qiandaizi.app.core.Yellow)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "距离目标 ${money(wallet!!.target)}，还差 ${money(wallet!!.target - wallet!!.balance)}",
                        fontSize = 12.sp, color = TextSub
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            WhiteCard {
                Text("存取记录（${txns.size}）", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (txns.isEmpty()) {
                    Text("暂无记录", fontSize = 13.sp, color = TextSub,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
                txns.forEach { t ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF5F6F8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(md(t.ymd), fontSize = 10.sp, color = TextSub)
                        }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                t.note.ifBlank { "存取" },
                                fontSize = 14.sp, color = TextMain
                            )
                            Text(
                                (if (t.amount >= 0) "存入" else "支出") +
                                    " · ${t.op_user.ifBlank { "—" }}",
                                fontSize = 11.sp, color = TextSub,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        Text(money(kotlin.math.abs(t.amount)), fontSize = 14.sp,
                            fontWeight = FontWeight.Bold, color = TextMain)
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除记录",
                            tint = Color(0xFFE5484D),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { deleteTxn = t }
                                .padding(6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (addOpen) {
        WalletTxnDialog(
            onDismiss = { addOpen = false }
        ) { direction, amount, ymd, note ->
            addOpen = false
            scope.launch {
                runCatching {
                    appState.api().addWalletTxn(
                        id,
                        WalletTxnReq(
                            amount = amount,
                            direction = direction,
                            ymd = ymd,
                            note = note.ifBlank { null }
                        )
                    )
                }.onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    deleteTxn?.let { t ->
        ConfirmDialog(
            title = "删除记录",
            message = "确定删除这条存取记录？",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteTxn = null
                scope.launch {
                    runCatching { appState.api().deleteWalletTxn(t.id) }
                        .onSuccess { load() }
                        .onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { deleteTxn = null }
        )
    }
}

@Composable
private fun WalletEditDialog(
    initial: WalletDto?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, target: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: "👛") }
    var target by remember {
        mutableStateOf(initial?.target?.takeIf { it != 0.0 }?.toString() ?: "")
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增钱包", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
            OutlinedTextField(value = name, onValueChange = { name = it },
                singleLine = true, placeholder = { Text("钱包名称", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Text("图标", fontSize = 12.sp, color = TextSub)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("👛", "🐷", "🏦", "💳", "💰").forEach { e ->
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
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = target, onValueChange = {
                if (it.all { c -> c.isDigit() || c == '.' }) target = it
            }, singleLine = true, placeholder = { Text("目标金额（可空）", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = note, onValueChange = { note = it },
                singleLine = true, placeholder = { Text("备注（可空）", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Text("保存", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        if (name.isBlank()) {
                            AppGraph.state.notify("请填写钱包名称")
                            return@clickable
                        }
                        onConfirm(
                            name.trim(), icon,
                            target.toDoubleOrNull() ?: 0.0, note.trim()
                        )
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
private fun WalletTxnDialog(
    onDismiss: () -> Unit,
    onConfirm: (direction: String, amount: Double, ymd: String, note: String) -> Unit
) {
    var direction by remember { mutableStateOf("in") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增存取记录", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF2F3F5))
                    .padding(3.dp)
            ) {
                listOf("in" to "存入", "out" to "支出").forEach { (key, label) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (direction == key) Color.White else Color.Transparent)
                            .clickable { direction = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, fontSize = 13.sp,
                            color = if (direction == key) TextMain else TextSub)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = amountText,
                onValueChange = {
                    if (it.all { c -> c.isDigit() || c == '.' }) amountText = it
                }, singleLine = true,
                placeholder = { Text("金额", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = note, onValueChange = { note = it },
                singleLine = true, placeholder = { Text("备注（可空）", fontSize = 13.sp) },
                modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Text("保存", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        val v = amountText.toDoubleOrNull() ?: 0.0
                        if (v <= 0) {
                            AppGraph.state.notify("请填写正确金额")
                            return@clickable
                        }
                        onConfirm(direction, v, com.qiandaizi.app.core.today(), note.trim())
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
