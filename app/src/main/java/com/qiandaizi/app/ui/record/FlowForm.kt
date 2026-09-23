package com.qiandaizi.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.AttrMember
import com.qiandaizi.app.core.CategoryDto
import com.qiandaizi.app.core.FlowDto
import com.qiandaizi.app.core.PresetDto
import com.qiandaizi.app.core.PresetReq
import com.qiandaizi.app.core.PresetsDto
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.YellowDark
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.today
import com.qiandaizi.app.ui.common.SegmentedTabs
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val commonPayments = listOf("微信", "支付宝", "现金", "银行卡", "信用卡")

/** 记账表单状态（新建/编辑共用） */
class FlowFormState(
    initial: FlowDto? = null,
    defaultDate: String = today(),
    defaultAttributionUid: Int? = null
) {
    var type by mutableStateOf(initial?.type?.let { if (it == "income") "income" else "expense" } ?: "expense")
    var amountText by mutableStateOf(initial?.amount?.takeIf { it != 0.0 }?.let { amount ->
        if (amount % 1.0 == 0.0) amount.toLong().toString()
        else amount.toString()
    } ?: "")
    var category by mutableStateOf(initial?.category ?: "")
    var payment by mutableStateOf(initial?.paymentMethod ?: "")
    var description by mutableStateOf(initial?.description ?: "")
    var date by mutableStateOf(initial?.flowTime?.take(10) ?: defaultDate)
    var source by mutableStateOf(initial?.source ?: "")
    var attributionUid by mutableStateOf(initial?.attributionUid ?: defaultAttributionUid)

    val amount: Double get() = amountText.trim().toDoubleOrNull() ?: 0.0
}

@Composable
fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, color = TextSub, modifier = Modifier.padding(bottom = 8.dp))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FlowFormFields(
    state: FlowFormState,
    categories: List<CategoryDto>,
    members: List<AttrMember> = emptyList(),
    modifier: Modifier = Modifier
) {
    var datePickerOpen by remember { mutableStateOf(false) }
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    // -------- 名称候选（★已收藏 + 高频），与 Web FlowDialog 一致 --------
    var presets by remember { mutableStateOf<PresetsDto?>(null) }
    LaunchedEffect(state.type, appState.epoch) {
        runCatching { appState.api().presets(state.type) }
            .onSuccess { presets = it }
    }

    val cat = state.category
    val matchCat: (PresetDto) -> Boolean = { p ->
        cat.isBlank() || p.category.isBlank() || p.category == cat
    }
    val pinned = presets?.presets?.filter(matchCat) ?: emptyList()
    val frequent = presets?.frequent?.filter(matchCat) ?: emptyList()
    val isPinned = state.description.isNotBlank() && pinned.any { it.name == state.description }

    fun reloadPresets() {
        scope.launch {
            runCatching { appState.api().presets(state.type) }
                .onSuccess { presets = it }
        }
    }

    fun togglePin() {
        val name = state.description.trim()
        if (name.isBlank()) {
            appState.notify("请先填写名称")
            return
        }
        val exist = pinned.find { it.name == name }
        scope.launch {
            runCatching {
                if (exist != null) appState.api().deletePreset(exist.id)
                else appState.api().addPreset(
                    PresetReq(
                        name = name,
                        type = state.type,
                        category = state.category,
                        paymentMethod = state.payment
                    )
                )
            }.onSuccess {
                appState.notify(if (exist != null) "已取消常用" else "已加入常用")
                reloadPresets()
            }.onFailure { appState.notify(explainError(it)) }
        }
    }

    // 成员到达后，归属人默认取当前用户
    LaunchedEffect(members) {
        if (state.attributionUid == null && members.isNotEmpty()) {
            state.attributionUid = members.find { it.id == appState.user()?.id }?.id
                ?: members.firstOrNull()?.id
        }
    }

    Column(modifier) {
        SegmentedTabs(
            options = listOf("支出", "收入"),
            selected = if (state.type == "income") 1 else 0,
            onSelect = {
                state.type = if (it == 1) "income" else "expense"
                // 切类型后，若当前分类在目标类型中不存在则取第一个
                val visible = categories.filter { c -> c.type == state.type }
                if (visible.none { c -> c.name == state.category }) {
                    state.category = visible.firstOrNull()?.name ?: ""
                }
            }
        )

        Spacer(Modifier.height(18.dp))

        // -------- 分类（图标网格，复用后端分类数据） --------
        FieldLabel("分类")
        val visibleCats = categories.filter { it.type == state.type }
        if (visibleCats.isEmpty()) {
            OutlinedTextField(
                value = state.category,
                onValueChange = { state.category = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("如：餐饮、交通", color = TextSub) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YellowDark)
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visibleCats.forEach { c ->
                    val selected = state.category == c.name
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFFFFF3C4) else Color(0xFFF6F7F9))
                            .then(
                                if (selected) Modifier.border(
                                    1.dp, YellowDark, RoundedCornerShape(10.dp)
                                ) else Modifier
                            )
                            .clickable { state.category = c.name }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(c.icon, fontSize = 15.sp)
                        Spacer(Modifier.size(5.dp))
                        Text(
                            c.name,
                            fontSize = 13.sp,
                            color = if (selected) Color(0xFF8A6D1B) else TextMain
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // -------- 日期 --------
        FieldLabel("日期")
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF6F7F9))
                .clickable { datePickerOpen = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = YellowDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(state.date, fontSize = 14.sp, color = TextMain)
        }

        Spacer(Modifier.height(18.dp))

        // -------- 支付方式 --------
        FieldLabel("支付方式（可空）")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            commonPayments.forEach { p ->
                val selected = state.payment == p
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) Color(0xFFFFF3C4) else Color(0xFFF6F7F9))
                        .clickable { state.payment = if (selected) "" else p }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        p,
                        fontSize = 13.sp,
                        color = if (selected) Color(0xFF8A6D1B) else TextMain
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = if (state.payment in commonPayments) "" else state.payment,
            onValueChange = { state.payment = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("自定义支付方式（现金/微信/支付宝…）", color = TextSub) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YellowDark)
        )

        Spacer(Modifier.height(18.dp))

        // -------- 金额 --------
        FieldLabel("金额")
        OutlinedTextField(
            value = state.amountText,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || it == '.' }
                if (filtered.count { it == '.' } <= 1 &&
                    filtered.substringAfter('.', "").length <= 2
                ) {
                    state.amountText = filtered
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0.00", color = Color(0xFFC0C4CC), fontSize = 22.sp) },
            leadingIcon = { Text("¥", fontSize = 20.sp, color = TextMain) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YellowDark,
                unfocusedBorderColor = Color(0xFFECEEF1),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(Modifier.height(18.dp))

        // -------- 名称（★常用 + 候选） --------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("名称备注（可空）", fontSize = 13.sp, color = TextSub,
                modifier = Modifier.weight(1f))
            Text(
                if (isPinned) "★ 取消常用" else "☆ 设为常用",
                fontSize = 12.sp,
                color = if (isPinned) Color(0xFFE8A012) else TextSub,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { togglePin() }
                    .padding(4.dp)
            )
        }
        Spacer(Modifier.height(8.dp))

        if (pinned.isNotEmpty() || frequent.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pinned.forEach { p ->
                    NameChip("★ ${p.name}", selected = state.description == p.name) {
                        state.description = p.name
                    }
                }
                frequent.take(8).forEach { f ->
                    NameChip("${f.name} ×${f.count}",
                        selected = state.description == f.name) {
                        state.description = f.name
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = state.description,
            onValueChange = { state.description = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("这笔钱花在哪儿（留空则自动用分类名，如「餐饮」）", color = TextSub)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YellowDark,
                unfocusedBorderColor = Color(0xFFECEEF1)
            )
        )

        // -------- 归属人（账本多于 1 名成员时显示） --------
        if (members.size > 1) {
            Spacer(Modifier.height(18.dp))
            FieldLabel("归属人")
            AttributionDropdown(state = state, members = members)
        }
    }

    if (datePickerOpen) {
        val initialMillis = remember(state.date) {
            runCatching {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                fmt.parse(state.date)?.time
            }.getOrNull()
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { datePickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        state.date = fmt.format(Date(ms))
                    }
                    datePickerOpen = false
                }) { Text("确定", color = YellowDark) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerOpen = false }) {
                    Text("取消", color = TextSub)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun NameChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) YellowDark else Color.White)
            .then(
                if (selected) Modifier
                else Modifier.border(1.dp, Color(0xFFE4E6EA), RoundedCornerShape(14.dp))
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, fontSize = 12.sp,
            color = if (selected) Color.White else TextSub)
    }
}

@Composable
private fun AttributionDropdown(state: FlowFormState, members: List<AttrMember>) {
    var expanded by remember { mutableStateOf(false) }
    val selectedMember = members.find { it.id == state.attributionUid }
    val meId = AppGraph.state.user()?.id

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF6F7F9))
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selectedMember?.nickname ?: "请选择归属人",
                fontSize = 14.sp,
                color = if (selectedMember != null) TextMain else TextSub,
                modifier = Modifier.weight(1f)
            )
            Text(if (expanded) "▲" else "▼", fontSize = 10.sp, color = TextSub)
        }
        if (expanded) {
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFECEEF1), RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                members.forEach { m ->
                    val active = m.id == state.attributionUid
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                state.attributionUid = m.id
                                expanded = false
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            m.nickname + if (m.id == meId) "（我）" else "",
                            fontSize = 14.sp,
                            color = if (active) YellowDark else TextMain,
                            modifier = Modifier.weight(1f)
                        )
                        if (active) Text("✓", fontSize = 13.sp, color = YellowDark)
                    }
                }
            }
        }
    }
}
