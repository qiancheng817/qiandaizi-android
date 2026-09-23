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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.qiandaizi.app.core.BudgetCatDto
import com.qiandaizi.app.core.BudgetDataDto
import com.qiandaizi.app.core.BudgetReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch
import kotlin.math.min

@Composable
fun BudgetsScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var year by remember { mutableStateOf(appState.user()?.let { com.qiandaizi.app.core.nowYear() }
        ?: com.qiandaizi.app.core.nowYear()) }
    var data by remember { mutableStateOf<BudgetDataDto?>(null) }
    var editTarget by remember { mutableStateOf<BudgetCatDto?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var editTotal by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            runCatching { appState.api().budgets(year.toInt()) }
                .onSuccess { data = it }
                .onFailure { appState.notify(explainError(it)) }
        }
    }
    LaunchedEffect(year) { load() }

    SubPageScaffold(title = "预算管理", onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            // 年份切换
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .padding(8.dp)
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF6F7F9))
                        .clickable { year = (year.toInt() - 1).toString() },
                    contentAlignment = Alignment.Center
                ) { Text("‹", fontSize = 18.sp, color = Color(0xFF8A6D1B)) }
                Text("$year 年预算", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    color = TextMain, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Box(
                    Modifier
                        .padding(8.dp)
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF6F7F9))
                        .clickable { year = (year.toInt() + 1).toString() },
                    contentAlignment = Alignment.Center
                ) { Text("›", fontSize = 18.sp, color = Color(0xFF8A6D1B)) }
            }

            Spacer(Modifier.height(14.dp))

            val d = data
            WhiteCard {
                Text("总预算", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (d == null || d.total.amount == 0.0) {
                    Text("还没有设置总预算", fontSize = 13.sp, color = TextSub)
                } else {
                    Text("预算 ${money(d.total.amount)}", fontSize = 13.sp, color = TextSub)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("已用 ", fontSize = 13.sp, color = TextSub)
                        Text(money(d.total.spent), fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = TextMain)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            if (d.total.remaining >= 0) "剩余 ${money(d.total.remaining)}"
                            else "超支 ${money(-d.total.remaining)}",
                            fontSize = 12.sp,
                            color = if (d.total.remaining >= 0)
                                com.qiandaizi.app.core.IncomeGreen
                            else com.qiandaizi.app.core.ExpenseRed
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    ProgressTrack(
                        fraction = if (d.total.amount > 0)
                            min(1f, (d.total.spent / d.total.amount).toFloat()) else 0f,
                        over = d.total.remaining < 0
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { editTotal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.qiandaizi.app.core.Yellow)
                    ) { Text("设置总预算", color = TextMain, fontSize = 13.sp) }
                    Button(
                        onClick = {
                            editTarget = null
                            showEditor = true
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF6F7F9))
                    ) { Text("添加分类预算", color = TextMain, fontSize = 13.sp) }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            runCatching {
                                appState.api().copyBudgets(
                                    com.qiandaizi.app.core.CopyBudgetReq(
                                        (year.toInt() - 1), year.toInt()
                                    )
                                )
                            }.onSuccess {
                                load()
                                appState.notify("已复制上年预算")
                            }.onFailure { appState.notify(explainError(it)) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF6F7F9))
                ) {
                    Text("从 ${year.toInt() - 1} 年复制预算", color = TextSub, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            WhiteCard {
                Text("分类预算", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (d?.categories?.isEmpty() != false) {
                    Text("暂无分类预算", fontSize = 13.sp, color = TextSub,
                        modifier = Modifier.padding(vertical = 10.dp))
                } else {
                    d.categories.forEach { c ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    editTarget = c
                                    showEditor = true
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    c.category.ifBlank { c.expression.ifBlank { "未命名" } },
                                    fontSize = 14.sp, color = TextMain,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f))
                                Text(money(c.amount), fontSize = 14.sp,
                                    color = TextMain, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("已用 ${money(c.spent)}", fontSize = 11.sp, color = TextSub,
                                    modifier = Modifier.weight(1f))
                                Text(
                                    if (c.remaining >= 0) "剩 ${money(c.remaining)}"
                                    else "超 ${money(-c.remaining)}",
                                    fontSize = 11.sp,
                                    color = if (c.remaining >= 0)
                                        com.qiandaizi.app.core.IncomeGreen
                                    else com.qiandaizi.app.core.ExpenseRed
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            ProgressTrack(
                                fraction = if (c.amount > 0)
                                    min(1f, (c.spent / c.amount).toFloat()) else 0f,
                                over = c.remaining < 0
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (editTotal) {
        BudgetEditDialog(
            title = "设置总预算",
            initialAmount = data?.total.amount?.let { if (it != 0.0) it.toString() else "" },
            initialCategory = "",
            initialExpression = "",
            onDismiss = { editTotal = false }
        ) { amount, category, expression ->
            editTotal = false
            scope.launch {
                runCatching {
                    appState.api().setBudget(
                        BudgetReq(
                            year = year.toInt(),
                            amount = amount,
                            category = null,
                            expression = expression.ifBlank { null }
                        )
                    )
                }.onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    if (showEditor) {
        BudgetEditDialog(
            title = if (editTarget == null) "添加分类预算" else "编辑分类预算",
            initialAmount = editTarget?.amount?.toString() ?: "",
            initialCategory = editTarget?.category ?: "",
            initialExpression = editTarget?.expression ?: "",
            onDismiss = { showEditor = false }
        ) { amount, category, expression ->
            val target = editTarget
            showEditor = false
            scope.launch {
                runCatching {
                    if (target != null) {
                        appState.api().deleteBudget(
                            year.toInt(),
                            target.category
                        )
                    }
                    appState.api().setBudget(
                        BudgetReq(
                            year = year.toInt(),
                            amount = amount,
                            category = category.ifBlank { null },
                            expression = expression.ifBlank { null }
                        )
                    )
                }.onSuccess { load() }
                    .onFailure { appState.notify(explainError(it)) }
            }
        }
    }
}

@Composable
private fun ProgressTrack(fraction: Float, over: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFEEEEEE))
    ) {
        Box(
            Modifier
                .fillMaxWidth(min(1f, fraction))
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (over) com.qiandaizi.app.core.ExpenseRed
                    else com.qiandaizi.app.core.Yellow
                )
        )
    }
}

@Composable
private fun BudgetEditDialog(
    title: String,
    initialAmount: String,
    initialCategory: String,
    initialExpression: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, expression: String) -> Unit
) {
    var amountText by remember { mutableStateOf(initialAmount) }
    var category by remember { mutableStateOf(initialCategory) }
    var expression by remember { mutableStateOf(initialExpression) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("预算金额", fontSize = 12.sp, color = TextSub)
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        if (it.all { c -> c.isDigit() || c == '.' }) amountText = it
                    },
                    singleLine = true,
                    placeholder = { Text("0.00", fontSize = 13.sp) }
                )
                Spacer(Modifier.height(12.dp))
                Text("分类名称", fontSize = 12.sp, color = TextSub)
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    singleLine = true,
                    placeholder = { Text("如：餐饮（总预算留空）", fontSize = 13.sp) }
                )
                Spacer(Modifier.height(12.dp))
                Text("组合表达式（高级，可空）", fontSize = 12.sp, color = TextSub)
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = expression,
                    onValueChange = { expression = it },
                    singleLine = true,
                    placeholder = { Text("餐饮+交通", fontSize = 13.sp) }
                )
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
                        if (amount <= 0) {
                            AppGraph.state.notify("请填写正确的金额")
                            return@clickable
                        }
                        onConfirm(amount, category.trim(), expression.trim())
                    }
                    .padding(8.dp)
            )
        },
        dismissButton = {
            Text("取消", fontSize = 14.sp, color = TextSub,
                modifier = Modifier
                    .clickable(onDismiss)
                    .padding(8.dp))
        }
    )
}
