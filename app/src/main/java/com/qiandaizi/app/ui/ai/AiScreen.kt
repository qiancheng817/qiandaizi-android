package com.qiandaizi.app.ui.ai

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AiParseDto
import com.qiandaizi.app.core.AiStatusDto
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.FlowDto
import com.qiandaizi.app.core.FlowReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.addMonth
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import com.qiandaizi.app.core.monthCn
import com.qiandaizi.app.core.nowMonth
import com.qiandaizi.app.core.today
import com.qiandaizi.app.ui.common.GhostButton
import com.qiandaizi.app.ui.common.Pill
import com.qiandaizi.app.ui.common.WhiteCard
import com.qiandaizi.app.ui.record.FlowEditorSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive

private val examples = listOf("午饭 35", "打车回家28", "发工资12000", "超市购物156.5微信", "收到红包200")

@Composable
fun AiScreen() {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var text by remember { mutableStateOf("") }
    var parsing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<AiParseDto?>(null) }
    var saving by remember { mutableStateOf(false) }
    var created by remember { mutableStateOf<FlowDto?>(null) }
    var editing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<AiStatusDto?>(null) }

    // 月度分析
    var month by rememberSaveable { mutableStateOf(nowMonth()) }
    var analyzing by remember { mutableStateOf(false) }
    var analysis by remember { mutableStateOf("") }
    var summaryIn by remember { mutableStateOf(0.0) }
    var summaryEx by remember { mutableStateOf(0.0) }
    var summaryBa by remember { mutableStateOf(0.0) }
    var hasSummary by remember { mutableStateOf(false) }

    // 图片
    var imgDataUrl by remember { mutableStateOf<String?>(null) }
    var imgText by remember { mutableStateOf("") }
    var imgParsing by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { appState.api().aiStatus() }.onSuccess { status = it }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val resolver = context.contentResolver
                        val bytes = resolver.openInputStream(uri)!!.use { it.readBytes() }
                        val mime = resolver.getType(uri) ?: "image/jpeg"
                        "data:$mime;base64," +
                            Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }.onSuccess { imgDataUrl = it }
                    .onFailure { appState.notify("读取图片失败") }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(com.qiandaizi.app.core.Yellow)
                .padding(start = 20.dp, top = 24.dp, bottom = 22.dp)
        ) {
            Text("AI 记账", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(4.dp))
            Text("一句话就能记账，智能识别金额、分类和支付方式",
                fontSize = 13.sp, color = Color(0xFF7A6520))
        }

        Column(Modifier.padding(14.dp)) {

            // ===== 一句话记账 =====
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, contentDescription = null,
                        tint = Color(0xFF8A6D1B), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("一句话记账", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (status?.enabled == true) "已接入 AI 大模型，识别更聪明"
                    else "未配置 AI，使用本地规则识别（在「更多 - AI设置」配置模型）",
                    fontSize = 12.sp, color = TextSub
                )
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("午饭 35 / 发工资 12000", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = {
                            if (text.isBlank()) {
                                appState.notify("说点什么，比如「午饭35」")
                                return@Button
                            }
                            parsing = true
                            result = null
                            scope.launch {
                                runCatching { appState.api().aiParse(AiParseReq(text.trim())) }
                                    .onSuccess { result = it }
                                    .onFailure { appState.notify(explainError(it)) }
                                parsing = false
                            }
                        },
                        enabled = !parsing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.qiandaizi.app.core.Yellow),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text(if (parsing) "…" else "识别", color = TextMain,
                            fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    examples.forEach { e ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF6F7F9))
                                .clickable {
                                    text = e
                                    parsing = true
                                    result = null
                                    scope.launch {
                                        runCatching { appState.api().aiParse(AiParseReq(e)) }
                                            .onSuccess { result = it }
                                            .onFailure { appState.notify(explainError(it)) }
                                        parsing = false
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(e, fontSize = 12.sp, color = TextSub)
                        }
                    }
                }

                // 识别结果
                result?.let { r ->
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8F9FB))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isExpense = r.type != "income"
                            Pill(
                                if (isExpense) "支出" else "收入",
                                if (isExpense) com.qiandaizi.app.core.ExpenseRed
                                else com.qiandaizi.app.core.IncomeGreen
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                money(r.amount),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpense) com.qiandaizi.app.core.ExpenseRed
                                else com.qiandaizi.app.core.IncomeGreen
                            )
                            Spacer(Modifier.size(8.dp))
                            r.category?.let { Pill(it, Color(0xFFE9EBF0), TextMain) }
                            Spacer(Modifier.size(6.dp))
                            r.paymentMethod?.takeIf { it.isNotBlank() }?.let {
                                Pill(it, Color(0xFFE9EBF0), TextMain)
                            }
                        }
                        if (!r.description.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("名称：${r.description}", fontSize = 13.sp, color = TextSub)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "来源：${if (r.source == "ai") "AI模型" else "本地规则"}",
                            fontSize = 11.sp, color = TextSub
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            GhostButton("取消") { result = null }
                            Spacer(Modifier.size(10.dp))
                            Button(
                                onClick = {
                                    val amount = r.amount ?: 0.0
                                    if (amount <= 0) {
                                        appState.notify("金额无效，无法记账")
                                        return@Button
                                    }
                                    saving = true
                                    scope.launch {
                                        runCatching {
                                            appState.api().createFlow(
                                                FlowReq(
                                                    type = r.type ?: "expense",
                                                    amount = amount,
                                                    category = r.category ?: "其他",
                                                    paymentMethod = r.paymentMethod?.ifBlank { null },
                                                    description = (r.description?.ifBlank { null })
                                                        ?: r.category,
                                                    flowTime = today(),
                                                    source = "ai"
                                                )
                                            )
                                        }.onSuccess { idResp ->
                                            created = FlowDto(
                                                id = idResp.id,
                                                type = r.type ?: "expense",
                                                amount = amount,
                                                category = r.category ?: "其他",
                                                paymentMethod = r.paymentMethod ?: "",
                                                description = r.description ?: "",
                                                flowTime = today()
                                            )
                                            result = null
                                            text = ""
                                            appState.bump()
                                            appState.notify("记账成功")
                                        }.onFailure { appState.notify(explainError(it)) }
                                        saving = false
                                    }
                                },
                                enabled = !saving,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = com.qiandaizi.app.core.Yellow
                                )
                            ) {
                                Text("确认并记账", color = TextMain, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 已记账结果
                created?.let { c ->
                    Spacer(Modifier.height(14.dp))
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1FBF6))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Pill(
                                if (c.type == "income") "收入" else "支出",
                                if (c.type == "income") com.qiandaizi.app.core.IncomeGreen
                                else com.qiandaizi.app.core.ExpenseRed
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(money(c.amount), fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                color = if (c.type == "income") com.qiandaizi.app.core.IncomeGreen
                                else com.qiandaizi.app.core.ExpenseRed)
                            Spacer(Modifier.size(8.dp))
                            Text("✓ 已记账", fontSize = 12.sp,
                                color = com.qiandaizi.app.core.IncomeGreen)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            GhostButton("删除记账") {
                                scope.launch {
                                    runCatching { appState.api().deleteFlow(c.id) }
                                        .onSuccess {
                                            created = null
                                            appState.bump()
                                            appState.notify("已删除")
                                        }
                                        .onFailure { appState.notify(explainError(it)) }
                                }
                            }
                            Spacer(Modifier.size(10.dp))
                            Button(
                                onClick = { editing = true },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = com.qiandaizi.app.core.Yellow
                                )
                            ) { Text("修改记账", color = TextMain, fontSize = 13.sp) }
                        }
                    }
                }
            }

            // ===== 月度智能分析 =====
            Spacer(Modifier.height(14.dp))
            WhiteCard {
                Text("📊 月度智能分析", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF6F7F9))
                            .clickable {
                                month = addMonth(month, -1)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("‹", fontSize = 18.sp, color = com.qiandaizi.app.core.YellowDark) }
                    Text(monthCn(month), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = TextMain, modifier = Modifier.weight(2f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF6F7F9))
                            .clickable { month = addMonth(month, 1) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("›", fontSize = 18.sp, color = com.qiandaizi.app.core.YellowDark) }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        analyzing = true
                        scope.launch {
                            runCatching { appState.api().aiAnalyze(month) }
                                .onSuccess { d ->
                                    analysis = d.analysis
                                    d.summary?.let { s ->
                                        summaryIn = s["income"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                                        summaryEx = s["expense"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                                        summaryBa = s["balance"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                                        hasSummary = true
                                    }
                                }
                                .onFailure { appState.notify(explainError(it)) }
                            analyzing = false
                        }
                    },
                    enabled = !analyzing,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = com.qiandaizi.app.core.Yellow)
                ) {
                    Text(if (analyzing) "分析中…" else "生成分析", color = TextMain,
                        fontWeight = FontWeight.SemiBold)
                }

                if (hasSummary) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        AnalysisCell("收入", money(summaryIn),
                            com.qiandaizi.app.core.IncomeGreen, Modifier.weight(1f))
                        AnalysisCell("支出", money(summaryEx),
                            com.qiandaizi.app.core.ExpenseRed, Modifier.weight(1f))
                        AnalysisCell("结余", money(summaryBa),
                            if (summaryBa >= 0) com.qiandaizi.app.core.IncomeGreen
                            else com.qiandaizi.app.core.ExpenseRed, Modifier.weight(1f))
                    }
                }
                if (analysis.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        analysis,
                        fontSize = 13.sp,
                        color = Color(0xFF444444),
                        lineHeight = 22.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8F9FB))
                            .padding(14.dp)
                    )
                }
            }

            // ===== 图片记账 =====
            Spacer(Modifier.height(14.dp))
            WhiteCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Image, contentDescription = null,
                        tint = Color(0xFF8A6D1B), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("图片记账", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text("上传小票或账单截图，AI 自动识别金额、分类与名称",
                    fontSize = 12.sp, color = TextSub)
                Spacer(Modifier.height(12.dp))

                Row {
                    Box(
                        Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF6F7F9))
                            .clickable {
                                imagePicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val dataUrl = imgDataUrl
                        if (dataUrl != null) {
                            val bitmap = remember(dataUrl) {
                                val bytes = Base64.decode(
                                    dataUrl.substringAfter(","), Base64.DEFAULT
                                )
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Image, contentDescription = null,
                                    tint = TextSub, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("选择图片", fontSize = 12.sp, color = TextSub)
                            }
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = imgText,
                            onValueChange = { imgText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("补充说明（可选）", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (imgDataUrl == null) {
                                    appState.notify("请先选择一张小票 / 账单图片")
                                    return@Button
                                }
                                imgParsing = true
                                result = null
                                scope.launch {
                                    runCatching {
                                        appState.api().aiParseImage(
                                            com.qiandaizi.app.core.AiParseImageReq(
                                                image = imgDataUrl,
                                                text = imgText.ifBlank { null }
                                            )
                                        )
                                    }.onSuccess {
                                        result = it
                                        imgDataUrl = null
                                        imgText = ""
                                    }.onFailure { appState.notify(explainError(it)) }
                                    imgParsing = false
                                }
                            },
                            enabled = !imgParsing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.qiandaizi.app.core.Yellow
                            )
                        ) {
                            Text(if (imgParsing) "识别中…" else "识别并记账",
                                color = TextMain, fontSize = 13.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (editing && created != null) {
        FlowEditorSheet(
            flow = created!!,
            categories = emptyList(),
            onDismiss = { editing = false },
            onChanged = {
                created = null
                editing = false
            }
        )
    }
}

@Composable
private fun AnalysisCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = TextSub)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color,
            modifier = Modifier.padding(top = 4.dp))
    }
}
