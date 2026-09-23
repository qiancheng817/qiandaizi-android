package com.qiandaizi.app.ui.record

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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.qiandaizi.app.core.AttrMember
import com.qiandaizi.app.core.CategoryDto
import com.qiandaizi.app.core.FlowReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val PanelGray = Color(0xFFF5F6F8)
private val KeyGray = Color(0xFFF3F4F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    val form = remember {
        FlowFormState(defaultAttributionUid = appState.user()?.id)
    }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var members by remember { mutableStateOf<List<AttrMember>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var datePickerOpen by remember { mutableStateOf(false) }
    var panelCollapsed by remember { mutableStateOf(false) }

    LaunchedEffect(appState.epoch) {
        runCatching { appState.api().categories() }
            .onSuccess { list ->
                categories = list
                val visible = list.filter { it.type == form.type }
                if (form.category.isBlank()) {
                    form.category = visible.firstOrNull()?.name ?: ""
                }
            }
        runCatching { appState.api().attributions() }
            .onSuccess { members = it.members }
    }

    val myUid = appState.user()?.id
    val otherUid = members.firstOrNull { it.id != myUid }?.id

    fun saveFlow(then: () -> Unit) {
        errorMsg = null
        if (form.amount <= 0) {
            errorMsg = "请输入正确的金额"
            return
        }
        saving = true
        scope.launch {
            val category = form.category.ifBlank {
                categories.firstOrNull { it.type == form.type }?.name ?: "其他"
            }
            runCatching {
                appState.api().createFlow(
                    FlowReq(
                        type = form.type,
                        amount = form.amount,
                        category = category,
                        description = form.description.ifBlank { category },
                        flowTime = form.date,
                        attributionUid = form.attributionUid
                    )
                )
            }.onSuccess {
                appState.bump()
                then()
            }.onFailure {
                errorMsg = explainError(it)
            }
            saving = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7F9))
    ) {
        // ============ 顶部栏：返回 / 支出·收入 / 设置 ============
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextMain,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.weight(1f))
            TypeTab("支出", selected = form.type == "expense") {
                form.type = "expense"
                // 切换类型后校验分类
                if (categories.none { it.name == form.category && it.type == "expense" }) {
                    form.category = categories.firstOrNull { it.type == "expense" }?.name ?: ""
                }
            }
            Spacer(Modifier.size(22.dp))
            TypeTab("收入", selected = form.type == "income") {
                form.type = "income"
                if (categories.none { it.name == form.category && it.type == "income" }) {
                    form.category = categories.firstOrNull { it.type == "income" }?.name ?: ""
                }
            }
            Spacer(Modifier.weight(1f))

            // 折叠/展开底部面板：折叠后展示更多分类
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { panelCollapsed = !panelCollapsed },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (panelCollapsed) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (panelCollapsed) "展开面板" else "折叠面板",
                    tint = Color(0xFF555555),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.size(8.dp))

            // 设置 → 类目管理
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "类目管理",
                    tint = Color(0xFF555555),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // ============ 分类网格（5 列，可上下滑动） ============
        val visibleCats = categories.filter { it.type == form.type }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp)
        ) {
            visibleCats.chunked(5).forEach { rowCats ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    rowCats.forEach { cat ->
                        CategoryCell(
                            cat = cat,
                            selected = form.category == cat.name,
                            modifier = Modifier.weight(1f)
                        ) { form.category = cat.name }
                    }
                    repeat(5 - rowCats.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (visibleCats.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无分类，点右上角设置去添加", color = TextSub, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ============ 底部面板（可折叠） ============
        if (!panelCollapsed) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // 日期 + 归属（自己/对方）
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 日期
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(YellowColor())
                            .clickable { datePickerOpen = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = TextMain,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(form.date, fontSize = 13.sp, color = TextMain,
                            fontWeight = FontWeight.Medium)
                    }

                    Spacer(Modifier.weight(1f))

                    // 归属人：自己 / 对方（多于1名成员时显示）
                    if (members.size > 1) {
                        AttrPill(
                            "自己",
                            selected = form.attributionUid == myUid
                        ) { form.attributionUid = myUid }
                        Spacer(Modifier.size(8.dp))
                        if (otherUid != null) {
                            AttrPill(
                                "对方",
                                selected = form.attributionUid == otherUid
                            ) { form.attributionUid = otherUid }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 备注 + 金额
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RemarkField(
                        value = form.description,
                        onValueChange = { input ->
                            if (input.length <= 25) form.description = input
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (form.amount > 0) money(form.amount) else "¥0.00",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (form.type == "income")
                                com.qiandaizi.app.core.IncomeGreen else TextMain
                        )
                        Text(
                            "${form.description.length}/25",
                            fontSize = 10.sp, color = TextSub
                        )
                    }
                }

                errorMsg?.let {
                    Text(it, fontSize = 11.sp, color = Color(0xFFE5484D),
                        modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(10.dp))

                // ============ 数字键盘 ============
                val pressDigit: (String) -> Unit = { d ->
                    val cur = form.amountText
                    val hasDot = cur.contains('.')
                    val decimalLen = cur.substringAfter('.', "").length
                    if (!(hasDot && decimalLen >= 2) && !(!hasDot && cur.length >= 7)) {
                        form.amountText = cur + d
                    }
                }
                KeypadRow {
                    KeypadKey("1", KeyGray) { pressDigit("1") }
                    KeypadKey("2", KeyGray) { pressDigit("2") }
                    KeypadKey("3", KeyGray) { pressDigit("3") }
                    KeypadKey("⌫", KeyGray) {
                        form.amountText = form.amountText.dropLast(1)
                    }
                }
                Spacer(Modifier.height(8.dp))
                KeypadRow {
                    KeypadKey("4", KeyGray) { pressDigit("4") }
                    KeypadKey("5", KeyGray) { pressDigit("5") }
                    KeypadKey("6", KeyGray) { pressDigit("6") }
                    KeypadKey("+", KeyGray) {
                        form.type = "income"
                        if (categories.none { it.name == form.category && it.type == "income" }) {
                            form.category = categories.firstOrNull { it.type == "income" }?.name ?: ""
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                KeypadRow {
                    KeypadKey("7", KeyGray) { pressDigit("7") }
                    KeypadKey("8", KeyGray) { pressDigit("8") }
                    KeypadKey("9", KeyGray) { pressDigit("9") }
                    KeypadKey("-", KeyGray) {
                        form.type = "expense"
                        if (categories.none { it.name == form.category && it.type == "expense" }) {
                            form.category = categories.firstOrNull { it.type == "expense" }?.name ?: ""
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                KeypadRow {
                    // 再记：保存后清空继续记
                    KeypadKey("再记", KeyGray) {
                        if (!saving) saveFlow {
                            form.amountText = ""
                            form.description = ""
                            appState.notify("已保存，可继续记一笔")
                        }
                    }
                    KeypadKey("0", KeyGray) { pressDigit("0") }
                    KeypadKey(".", KeyGray) {
                        if (!form.amountText.contains('.')) {
                            form.amountText =
                                if (form.amountText.isBlank()) "0." else form.amountText + "."
                        }
                    }
                    KeypadKey("保存", com.qiandaizi.app.core.Yellow,
                        textColor = TextMain, bold = true) {
                        if (!saving) saveFlow {
                            appState.notify("记账成功")
                            onBack()
                        }
                    }
                }
            }
        }
        }
    }

    // 日期选择
    if (datePickerOpen) {
        val initialMillis = remember(form.date) {
            runCatching {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fmt.parse(form.date)?.time
            }.getOrNull()
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        form.date = fmt.format(Date(ms))
                    }
                    datePickerOpen = false
                }) { Text("确定", color = com.qiandaizi.app.core.YellowDark) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOpen = false }) {
                    Text("取消", color = TextSub)
                }
            }
        ) { DatePicker(state = pickerState) }
    }
}

/* ==================== 小组件 ==================== */

@Composable
private fun TypeTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text,
            fontSize = 18.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) TextMain else TextSub
        )
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .height(3.dp)
                .then(if (selected) Modifier.fillMaxWidth(0.6f) else Modifier.width(0.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(com.qiandaizi.app.core.YellowDark)
        )
    }
}

@Composable
private fun CategoryCell(
    cat: CategoryDto,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (selected) Modifier.background(Color(0xFFFFF3C4))
                else Modifier.background(PanelGray)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(cat.icon, fontSize = 24.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            cat.name,
            fontSize = 12.sp,
            color = if (selected) Color(0xFF8A6D1B) else TextMain,
            maxLines = 1
        )
    }
}

@Composable
private fun AttrPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) com.qiandaizi.app.core.Yellow else PanelGray)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            text, fontSize = 13.sp,
            color = if (selected) TextMain else TextSub,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun RemarkField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text("点击输入备注", color = TextSub, fontSize = 13.sp) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = TextSub,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = com.qiandaizi.app.core.YellowDark,
            unfocusedBorderColor = Color(0xFFECEEF1),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}

@Composable
private fun KeypadRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun RowScope.KeypadKey(
    text: String,
    bg: Color,
    textColor: Color = TextMain,
    bold: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .weight(1f)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 17.sp,
            color = textColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/* 颜色辅助（避免与顶层名字混淆） */
private fun YellowColor(): Color = com.qiandaizi.app.core.Yellow
