package com.qiandaizi.app.ui.record

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.CategoryDto
import com.qiandaizi.app.core.FlowDto
import com.qiandaizi.app.core.FlowReq
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.ui.common.ConfirmDialog
import com.qiandaizi.app.ui.common.PrimaryButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowEditorSheet(
    flow: FlowDto,
    categories: List<CategoryDto>,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()
    val form = remember(flow.id) { FlowFormState(initial = flow) }

    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            Modifier
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "编辑记录",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FlowFormFields(state = form, categories = categories)

            if (errorMsg != null) {
                Spacer(Modifier.height(10.dp))
                Text(errorMsg, fontSize = 13.sp, color = Color(0xFFE5484D))
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = "保存修改",
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
                            appState.api().updateFlow(
                                flow.id,
                                FlowReq(
                                    type = form.type,
                                    amount = form.amount,
                                    category = form.category.ifBlank { "其他" },
                                    paymentMethod = form.payment.ifBlank { null },
                                    description = form.description.ifBlank { null },
                                    flowTime = form.date,
                                    source = form.source.ifBlank { null },
                                    attributionUid = form.attributionUid
                                )
                            )
                        }.onSuccess {
                            appState.bump()
                            onChanged()
                            onDismiss()
                        }.onFailure { errorMsg = explainError(it) }
                        saving = false
                    }
                }
            )

            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color(0xFFE5484D),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(6.dp))
                Text("删除这条记录", color = Color(0xFFE5484D), fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "删除记录",
            message = "确定删除这条记录？可在「更多 - 回收站」中恢复。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    runCatching { appState.api().deleteFlow(flow.id) }
                        .onSuccess {
                            appState.bump()
                            onChanged()
                            onDismiss()
                        }
                        .onFailure { appState.notify(explainError(it)) }
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}
