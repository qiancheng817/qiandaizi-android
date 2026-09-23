package com.qiandaizi.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.AttrMember
import com.qiandaizi.app.core.CategoryDto
import com.qiandaizi.app.core.FlowReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.Yellow
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import com.qiandaizi.app.ui.common.PrimaryButton
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

@Composable
fun RecordScreen() {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    val form = remember {
        FlowFormState(defaultAttributionUid = appState.user()?.id)
    }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var members by remember { mutableStateOf<List<AttrMember>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(appState.epoch) {
        runCatching { appState.api().categories() }
            .onSuccess { list ->
                categories = list
                // 默认选第一个同类型分类（与 Web 一致）
                val visible = list.filter { it.type == form.type }
                if (form.category.isBlank()) {
                    form.category = visible.firstOrNull()?.name ?: ""
                }
            }
        runCatching { appState.api().attributions() }
            .onSuccess { members = it.members }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F7F9))
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Yellow)
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 26.dp)
        ) {
            Text("记一笔", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(4.dp))
            Text("随手记录，数据实时同步到服务器", fontSize = 13.sp, color = Color(0xFF7A6520))
        }

        Column(Modifier.padding(16.dp)) {
            WhiteCard {
                FlowFormFields(
                    state = form,
                    categories = categories,
                    members = members
                )
            }

            errorMsg?.let { msg ->
                Spacer(Modifier.height(10.dp))
                Text(msg, fontSize = 13.sp, color = Color(0xFFE5484D))
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "保 存",
                loading = saving,
                onClick = {
                    errorMsg = null
                    if (form.amount <= 0) {
                        errorMsg = "请输入正确的金额"
                        return@PrimaryButton
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
                                    paymentMethod = form.payment.ifBlank { null },
                                    // 名称留空自动用分类名
                                    description = form.description.ifBlank { category },
                                    flowTime = form.date,
                                    source = form.source.ifBlank { null },
                                    attributionUid = form.attributionUid
                                )
                            )
                        }.onSuccess {
                            val savedAmount = form.amount
                            val savedType = form.type
                            // 重置表单，方便连续记账
                            form.amountText = ""
                            form.description = ""
                            appState.bump()
                            appState.notify(
                                "已保存${if (savedType == "income") "收入" else "支出"} ${money(savedAmount)}"
                            )
                        }.onFailure { errorMsg = explainError(it) }
                        saving = false
                    }
                }
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}
