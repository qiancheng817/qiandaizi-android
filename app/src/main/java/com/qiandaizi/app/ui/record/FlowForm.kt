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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.CategoryDto
import com.qiandaizi.app.core.FlowDto
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.YellowDark
import com.qiandaizi.app.core.money
import com.qiandaizi.app.core.today
import com.qiandaizi.app.ui.common.SegmentedTabs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

val commonPayments = listOf("微信", "支付宝", "现金", "银行卡", "信用卡")

/** 记账表单状态（新建/编辑共用） */
class FlowFormState(initial: FlowDto? = null, defaultDate: String = today()) {
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
    var attributionUid by mutableStateOf(initial?.attributionUid)

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
    modifier: Modifier = Modifier
) {
    var datePickerOpen by remember { mutableStateOf(false) }

    Column(modifier) {
        SegmentedTabs(
            options = listOf("支出", "收入"),
            selected = if (state.type == "income") 1 else 0,
            onSelect = {
                state.type = if (it == 1) "income" else "expense"
                // 切类型后，若当前分类在目标类型中不存在则清空
                val valid = categories.any { c -> c.name == state.category && c.type == state.type }
                if (!valid) state.category = ""
            }
        )

        Spacer(Modifier.height(18.dp))
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
                visibleCats.forEach { cat ->
                    val selected = state.category == cat.name
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Color(0xFFFFF3C4) else Color(0xFFF6F7F9))
                            .then(
                                if (selected) Modifier.border(
                                    1.dp, YellowDark, RoundedCornerShape(10.dp)
                                ) else Modifier
                            )
                            .clickable { state.category = cat.name }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.icon, fontSize = 15.sp)
                        Spacer(Modifier.size(5.dp))
                        Text(
                            cat.name,
                            fontSize = 13.sp,
                            color = if (selected) Color(0xFF8A6D1B) else TextMain
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel("名称备注（可空）")
        OutlinedTextField(
            value = state.description,
            onValueChange = { state.description = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("如：午餐、地铁回家", color = TextSub) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YellowDark,
                unfocusedBorderColor = Color(0xFFECEEF1)
            )
        )

        Spacer(Modifier.height(18.dp))
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
        if (state.payment !in commonPayments && state.payment.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = if (state.payment in commonPayments) "" else state.payment,
            onValueChange = { state.payment = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("自定义支付方式", color = TextSub) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YellowDark)
        )

        Spacer(Modifier.height(18.dp))
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
