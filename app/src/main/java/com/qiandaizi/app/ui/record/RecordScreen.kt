package com.qiandaizi.app.ui.record

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    val form = remember { FlowFormState() }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(appState.epoch) {
        runCatching { appState.api().categories() }
            .onSuccess { categories = it }
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
                .padding(start = 20.dp, top = 26.dp, bottom = 26.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Text("记一笔", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(4.dp))
            Text("随手记录，数据实时同步到服务器", fontSize = 13.sp, color = Color(0xFF7A6520))
        }

        Column(Modifier.padding(16.dp)) {
            WhiteCard {
                FlowFormFields(state = form, categories = categories)
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
                        runCatching {
                            appState.api().createFlow(
                                FlowReq(
                                    type = form.type,
                                    amount = form.amount,
                                    category = form.category.ifBlank { "其他" },
                                    paymentMethod = form.payment.ifBlank { null },
                                    description = form.description.ifBlank { null },
                                    flowTime = form.date,
                                    source = form.source.ifBlank { null }
                                )
                            )
                        }.onSuccess {
                            val savedAmount = form.amount
                            val savedType = form.type
                            // 重置表单，方便连续记账
                            form.amountText = ""
                            form.description = ""
                            form.category = ""
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
