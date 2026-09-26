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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import com.qiandaizi.app.core.AiModelDto
import com.qiandaizi.app.core.AiModelsDto
import com.qiandaizi.app.core.AiModelsPutReq
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.BaiduOcrDto
import com.qiandaizi.app.core.BaiduOcrPutReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.ui.common.ConfirmDialog
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

@Composable
fun AiSettingsScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var models by remember { mutableStateOf<List<AiModelDto>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf(-1) }
    var deleteIndex by remember { mutableStateOf(-1) }
    var baidu by remember { mutableStateOf<BaiduOcrDto?>(null) }
    var showBaiduEdit by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            runCatching { appState.api().aiModels() }
                .onSuccess { d: AiModelsDto ->
                    models = d.models
                    isAdmin = appState.user()?.role == "admin"
                    loaded = true
                }
                .onFailure { appState.notify(explainError(it)) }
        }
        scope.launch {
            runCatching { appState.api().baiduOcrConfig() }
                .onSuccess { baidu = it }
                .onFailure { }
        }
    }
    LaunchedEffect(Unit) { load() }

    SubPageScaffold(
        title = "AI 设置",
        onBack = onBack,
        actions = {
            if (isAdmin) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "添加模型",
                    tint = TextMain,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { editIndex = models.size }
                        .padding(6.dp)
                )
            }
        }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(14.dp)
        ) {
            Text(
                if (isAdmin) "配置后 AI 记账与图片识别将使用所选模型"
                else "仅管理员可修改模型配置",
                fontSize = 12.sp, color = TextSub,
                modifier = Modifier.padding(bottom = 10.dp))

            if (loaded && models.isEmpty()) {
                WhiteCard {
                    Text("还没有配置任何模型", fontSize = 13.sp, color = TextSub)
                }
            }

            models.forEachIndexed { index, m ->
                WhiteCard(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(m.name.ifBlank { "未命名模型" }, fontSize = 15.sp,
                            fontWeight = FontWeight.Bold, color = TextMain,
                            modifier = Modifier.weight(1f))
                        if (m.isDefault) {
                            Icon(Icons.Filled.Star, contentDescription = "默认",
                                tint = Color(0xFFF7B500), modifier = Modifier.size(18.dp))
                        }
                        if (isAdmin) {
                            Icon(Icons.Filled.Edit, contentDescription = "编辑",
                                tint = TextSub,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { editIndex = index }
                                    .padding(6.dp))
                            Icon(Icons.Filled.Delete, contentDescription = "删除",
                                tint = Color(0xFFE5484D),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { deleteIndex = index }
                                    .padding(6.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    InfoLine("服务商", m.provider.ifBlank { "—" })
                    InfoLine("接口地址", m.baseUrl.ifBlank { "—" })
                    InfoLine("文本模型", m.model.ifBlank { "—" })
                    InfoLine("图片模型", m.imageModel.ifBlank { "—" })
                    InfoLine("密钥状态", if (m.apiKey.isNotBlank()) "已配置 ✓" else "未配置")
                }
            }
            Spacer(Modifier.height(8.dp))

            // 百度 OCR（图片识别，无需大模型）
            WhiteCard(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("百度 OCR 图片识别", fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, color = TextMain,
                        modifier = Modifier.weight(1f))
                    if (isAdmin) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑",
                            tint = TextSub,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { showBaiduEdit = true }
                                .padding(6.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "在百度智能云「应用管理」创建应用（勾选文字识别），填入 API Key 和 Secret Key 即可。配置后拍照 / 图片记账自动走百度 OCR + 本地规则，无需 AI 大模型。",
                    fontSize = 11.sp, color = TextSub,
                    modifier = Modifier.padding(bottom = 6.dp))
                InfoLine("状态", if (baidu?.enabled == true) "已启用 ✓" else "未启用")
                InfoLine("API Key", if (!baidu?.apiKey.isNullOrBlank()) "已配置 ✓" else "未配置")
                InfoLine("Secret Key", if (baidu?.hasSecret == true) "已配置 ✓" else "未配置")
            }
        }
    }

    if (editIndex in 0..models.size) {
        val editing = editIndex < models.size
        val initial = if (editing) models[editIndex] else AiModelDto()
        ModelEditDialog(
            initial = initial,
            title = if (editing) "编辑模型" else "添加模型",
            onDismiss = { editIndex = -1 }
        ) { updated ->
            val newList = if (editing)
                models.toMutableList().also { it[editIndex] = updated }
            else models + updated
            val finalList = if (updated.isDefault)
                newList.mapIndexed { i, m -> m.copy(isDefault = i == (if (editing) editIndex else newList.size - 1)) }
            else newList
            editIndex = -1
            scope.launch {
                runCatching {
                    appState.api().saveAiModels(AiModelsPutReq(finalList))
                }.onSuccess {
                    models = finalList
                    appState.notify("已保存")
                }.onFailure { appState.notify(explainError(it)) }
            }
        }
    }

    if (deleteIndex >= 0) {
        val m = models[deleteIndex]
        ConfirmDialog(
            title = "删除模型",
            message = "确定删除模型「${m.name}」？",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                val newList = models.filterIndexed { i, _ -> i != deleteIndex }
                deleteIndex = -1
                scope.launch {
                    runCatching {
                        appState.api().saveAiModels(AiModelsPutReq(newList))
                    }.onSuccess {
                        models = newList
                    }.onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { deleteIndex = -1 }
        )
    }

    if (showBaiduEdit) {
        BaiduOcrEditDialog(
            onDismiss = { showBaiduEdit = false }
        ) { apiKey, secretKey ->
            showBaiduEdit = false
            scope.launch {
                runCatching {
                    appState.api().saveBaiduOcr(BaiduOcrPutReq(apiKey = apiKey, secretKey = secretKey))
                }.onSuccess {
                    appState.notify("百度 OCR 设置已保存")
                    load()
                }.onFailure { appState.notify(explainError(it)) }
            }
        }
    }
}

@Composable
private fun BaiduOcrEditDialog(
    onDismiss: () -> Unit,
    onConfirm: (apiKey: String, secretKey: String) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("百度 OCR 配置", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "百度智能云控制台 → 应用管理 → 创建应用（勾选「文字识别」），把应用详情里的 API Key 和 Secret Key 填入下方。",
                    fontSize = 11.sp, color = TextSub,
                    modifier = Modifier.padding(bottom = 8.dp))
                DialogInput("API Key", apiKey) { apiKey = it }
                DialogInput("Secret Key", secretKey) { secretKey = it }
            }
        },
        confirmButton = {
            Text("保存", fontSize = 14.sp, color = com.qiandaizi.app.core.YellowDark,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable {
                        if (apiKey.isBlank() || secretKey.isBlank()) {
                            AppGraph.state.notify("请填写 API Key 和 Secret Key")
                            return@clickable
                        }
                        onConfirm(apiKey.trim(), secretKey.trim())
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
private fun InfoLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)) {
        Text(label, fontSize = 12.sp, color = TextSub,
            modifier = Modifier.width(64.dp))
        Text(value, fontSize = 12.sp, color = TextMain)
    }
}

@Composable
private fun ModelEditDialog(
    initial: AiModelDto,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (AiModelDto) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var provider by remember { mutableStateOf(initial.provider) }
    var baseUrl by remember { mutableStateOf(initial.baseUrl) }
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf(initial.model) }
    var imageModel by remember { mutableStateOf(initial.imageModel) }
    var isDefault by remember { mutableStateOf(initial.isDefault) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
            DialogInput("显示名称", name) { name = it }
            DialogInput("服务商（如 openai）", provider) { provider = it }
            DialogInput("接口地址 Base URL", baseUrl) { baseUrl = it }
            Text(
                if (initial.apiKey.isNotBlank())
                    "密钥留空且显示 ${initial.apiKey} 时沿用原密钥，输入新值则覆盖"
                else "API Key",
                fontSize = 11.sp, color = TextSub,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            OutlinedTextField(value = apiKey,
                onValueChange = { apiKey = it },
                singleLine = true,
                placeholder = { Text(if (initial.apiKey.isNotBlank()) initial.apiKey else "sk-...",
                    fontSize = 12.sp)},
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth())
            DialogInput("文本模型名（如 gpt-4o-mini）", model) { model = it }
            DialogInput("图片模型名（可空）", imageModel) { imageModel = it }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isDefault = !isDefault }) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDefault) Color(0xFFF7B500) else Color(0xFFE4E6EA)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDefault) Text("✓", color = Color.White, fontSize = 11.sp)
                }
                Spacer(Modifier.size(8.dp))
                Text("设为默认模型", fontSize = 13.sp, color = TextMain)
            }
            }
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
                            AiModelDto(
                                id = initial.id,
                                name = name.trim(),
                                provider = provider.trim(),
                                baseUrl = baseUrl.trim(),
                                apiKey = apiKey.trim().ifBlank { initial.apiKey },
                                model = model.trim(),
                                imageModel = imageModel.trim(),
                                isDefault = isDefault
                            )
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
private fun DialogInput(label: String, value: String, onChange: (String) -> Unit) {
    Text(label, fontSize = 11.sp, color = TextSub,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
