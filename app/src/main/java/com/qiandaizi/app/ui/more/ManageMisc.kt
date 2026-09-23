package com.qiandaizi.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.qiandaizi.app.core.AiParseReq
import com.qiandaizi.app.core.BillRowDto
import com.qiandaizi.app.core.FlowReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import com.qiandaizi.app.core.monthCn
import com.qiandaizi.app.ui.auth.ServerScreen
import com.qiandaizi.app.ui.common.Pill
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

/* ================= 快捷 AI 记账 ================= */

@Composable
fun QuickAiScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var parsing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<com.qiandaizi.app.core.AiParseDto?>(null) }
    var saving by remember { mutableStateOf(false) }
    var done by rememberSaveable { mutableStateOf(false) }

    SubPageScaffold(title = "懒人记账", onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, contentDescription = null,
                        tint = Color(0xFF8A6D1B), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("说句话就记好", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("午饭35 / 发工资12000", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(com.qiandaizi.app.core.Yellow)
                        .clickable(enabled = !parsing && text.isNotBlank()) {
                            parsing = true
                            result = null
                            scope.launch {
                                runCatching {
                                    appState.api().aiParse(AiParseReq(text.trim()))
                                }.onSuccess { result = it }
                                    .onFailure { appState.notify(explainError(it)) }
                                parsing = false
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (parsing) "识别中…" else "识别", color = TextMain,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                result?.let { r ->
                    Spacer(Modifier.height(14.dp))
                    val isExpense = r.type != "income"
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(
                            if (isExpense) "支出" else "收入",
                            if (isExpense) com.qiandaizi.app.core.ExpenseRed
                            else com.qiandaizi.app.core.IncomeGreen
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(money(r.amount), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = if (isExpense) com.qiandaizi.app.core.ExpenseRed
                            else com.qiandaizi.app.core.IncomeGreen)
                        Spacer(Modifier.size(8.dp))
                        r.category?.let { Pill(it, Color(0xFFE9EBF0), TextMain) }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF6F7F9))
                                .clickable { result = null }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) { Text("取消", fontSize = 13.sp, color = TextMain) }
                        Spacer(Modifier.size(10.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(com.qiandaizi.app.core.Yellow)
                                .clickable(enabled = !saving) {
                                    saving = true
                                    scope.launch {
                                        runCatching {
                                            appState.api().createFlow(
                                                FlowReq(
                                                    type = r.type ?: "expense",
                                                    amount = r.amount ?: 0.0,
                                                    category = r.category ?: "其他",
                                                    paymentMethod = r.paymentMethod?.ifBlank { null },
                                                    description = r.description?.ifBlank { null }
                                                        ?: r.category,
                                                    flowTime = com.qiandaizi.app.core.today(),
                                                    source = "ai"
                                                )
                                            )
                                        }.onSuccess {
                                            result = null
                                            text = ""
                                            done = true
                                            appState.bump()
                                        }.onFailure {
                                            appState.notify(explainError(it))
                                        }
                                        saving = false
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                        ) { Text("确认记账", fontSize = 13.sp, color = TextMain,
                            fontWeight = FontWeight.SemiBold) }
                    }
                }
                if (done) {
                    Spacer(Modifier.height(10.dp))
                    Text("✓ 已记账，可继续说下一句", fontSize = 12.sp,
                        color = com.qiandaizi.app.core.IncomeGreen)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/* ================= 切换服务器 ================= */

@Composable
fun ServerSwitchScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var managing by remember { mutableStateOf(false) }

    if (managing) {
        ServerScreen(onBack = { managing = false })
        return
    }

    SubPageScaffold(
        title = "切换服务器",
        onBack = onBack,
        actions = {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "管理服务器",
                tint = TextMain,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { managing = true }
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
                Text("选择服务器后，将使用该服务器上记住的账号；未登录过则需要重新登录。",
                    fontSize = 12.sp, color = TextSub)
                Spacer(Modifier.height(10.dp))

                appState.session.servers.forEach { url ->
                    val active = url == appState.server
                    val hasAccount = appState.session.accounts[url]?.isNotEmpty() == true
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Color(0xFFFFF8E1) else Color.Transparent)
                            .clickable {
                                scope.launch {
                                    appState.selectServer(url)
                                    appState.notify("已切换服务器")
                                }
                            }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) com.qiandaizi.app.core.YellowDark
                                    else Color(0xFFE4E6EA)
                                ),
                            contentAlignment = Alignment.Center
                        ) { if (active) Text("✓", color = Color.White, fontSize = 11.sp) }
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(url, fontSize = 14.sp, color = TextMain)
                        }
                        Pill(
                            if (hasAccount) "有记住账号" else "需登录",
                            if (hasAccount) Color(0xFFE7F8F0) else Color(0xFFF2F3F5),
                            if (hasAccount) Color(0xFF16875B) else TextSub
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 新增服务器
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFFF3C4))
                    .clickable { managing = true }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("＋ 新增服务器", color = Color(0xFF8A6D1B), fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            // 退出当前服务器：回到服务器选择界面
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFFDF2F2))
                    .clickable {
                        scope.launch {
                            appState.exitCurrentServer()
                            appState.notify("已退出当前服务器")
                        }
                    }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("退出当前服务器", color = Color(0xFFE5484D), fontSize = 14.sp,
                    fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun BillsScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var year by rememberSaveable { mutableStateOf(com.qiandaizi.app.core.nowYear()) }
    var rows by remember { mutableStateOf<List<BillRowDto>>(emptyList()) }
    var summary by remember { mutableStateOf<BillRowDto?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var detailYm by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(year) {
        runCatching { appState.api().billsMonthly(year.toInt()) }
            .onSuccess {
                rows = it.rows
                summary = it.summary
                loaded = true
            }
            .onFailure {
                loaded = true
                appState.notify(explainError(it))
            }
    }

    SubPageScaffold(title = "月度账单", onBack = onBack) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                YearArrow("‹") { year = (year.toInt() - 1).toString() }
                Text("$year 年", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = TextMain, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                YearArrow("›") { year = (year.toInt() + 1).toString() }
            }

            Spacer(Modifier.height(14.dp))

            summary?.let { s ->
                WhiteCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text("全年汇总", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Row {
                        BillCell("收入", money(s.income),
                            com.qiandaizi.app.core.IncomeGreen, Modifier.weight(1f))
                        BillCell("支出", money(s.expense),
                            com.qiandaizi.app.core.ExpenseRed, Modifier.weight(1f))
                        BillCell("结余", money(s.balance),
                            if (s.balance >= 0) com.qiandaizi.app.core.IncomeGreen
                            else com.qiandaizi.app.core.ExpenseRed,
                            Modifier.weight(1f))
                    }
                }
            }

            WhiteCard {
                Text("按月查看（点击查看明细）", fontSize = 14.sp,
                    color = TextSub)
                Spacer(Modifier.height(6.dp))
                if (loaded && rows.isEmpty()) {
                    Text("该年没有账单", fontSize = 13.sp, color = TextSub,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
                rows.forEach { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { detailYm = r.ym }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${r.ym?.substring(5)?.trimStart('0') ?: "-"}月",
                            fontSize = 14.sp, color = TextMain,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(46.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "收 ${money(r.income)}",
                            fontSize = 12.sp,
                            color = com.qiandaizi.app.core.IncomeGreen,
                            modifier = Modifier.weight(1f))
                        Text(
                            "支 ${money(r.expense)}",
                            fontSize = 12.sp,
                            color = TextSub,
                            modifier = Modifier.weight(1f))
                        Text(money(r.balance), fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (r.balance >= 0) TextMain
                            else com.qiandaizi.app.core.ExpenseRed)
                    }
                }
            }
        }
    }

    detailYm?.let { ym ->
        MonthDetailDialog(ym = ym, onDismiss = { detailYm = null })
    }
}

@Composable
private fun YearArrow(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(8.dp)
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF6F7F9))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, fontSize = 18.sp, color = Color(0xFF8A6D1B)) }
}

@Composable
private fun BillCell(label: String, value: String, color: Color,
                      modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = TextSub)
        Text(value, fontSize = 15.sp, color = color, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MonthDetailDialog(ym: String, onDismiss: () -> Unit) {
    val appState = AppGraph.state
    var data by remember {
        mutableStateOf<com.qiandaizi.app.core.BillMonthDetailDto?>(null)
    }
    androidx.compose.runtime.LaunchedEffect(ym) {
        runCatching { appState.api().billMonthDetail(ym) }
            .onSuccess { data = it }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${monthCn(ym)}账单明细", fontSize = 16.sp,
            fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                val d = data
                if (d == null) {
                    Text("加载中…", fontSize = 13.sp, color = TextSub)
                } else {
                    Row {
                        Text("收入 ", fontSize = 12.sp, color = TextSub)
                        Text(money(d.income), fontSize = 12.sp,
                            color = com.qiandaizi.app.core.IncomeGreen,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.size(12.dp))
                        Text("支出 ", fontSize = 12.sp, color = TextSub)
                        Text(money(d.expense), fontSize = 12.sp,
                            color = com.qiandaizi.app.core.ExpenseRed,
                            fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    d.list.forEach { f ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${f.category} · ${f.description.ifBlank { f.category }}",
                                    fontSize = 13.sp, color = TextMain)
                                Text(com.qiandaizi.app.core.md(f.flowTime),
                                    fontSize = 10.sp, color = TextSub)
                            }
                            Text(
                                (if (f.type == "income") "+" else "-") +
                                    com.qiandaizi.app.core.amount(f.amount),
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (f.type == "income")
                                    com.qiandaizi.app.core.IncomeGreen else TextMain)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text("关闭", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp))
        }
    )
}
