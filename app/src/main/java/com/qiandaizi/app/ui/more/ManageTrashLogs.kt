package com.qiandaizi.app.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.OpLogDto
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.ui.common.EmptyHint
import com.qiandaizi.app.ui.common.SubPageScaffold
import com.qiandaizi.app.ui.common.WhiteCard
import kotlinx.coroutines.launch

@Composable
fun TrashScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var list by remember {
        mutableStateOf<List<com.qiandaizi.app.core.TrashDto>>(emptyList())
    }
    var loaded by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            runCatching { appState.api().trash().list }
                .onSuccess {
                    list = it
                    loaded = true
                }
                .onFailure { appState.notify(explainError(it)) }
        }
    }
    LaunchedEffect(Unit) { load() }

    SubPageScaffold(title = "回收站", onBack = onBack) {
        if (!loaded) {
            com.qiandaizi.app.ui.common.LoadingBox()
            return@SubPageScaffold
        }
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            if (list.isEmpty()) {
                item { EmptyHint("回收站是空的") }
            }
            items(list, key = { it.id }) { t ->
                WhiteCard(modifier = Modifier.padding(bottom = 10.dp)) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${t.category} · ${t.description.ifBlank { t.category }}",
                                fontSize = 14.sp, color = com.qiandaizi.app.core.TextMain
                            )
                            Text(
                                "${com.qiandaizi.app.core.md(t.flowTime)} 删除 · ${t.deletedBy.ifBlank { "—" }}",
                                fontSize = 11.sp, color = TextSub,
                                modifier = Modifier.padding(top = 3.dp))
                        }
                        Text(
                            (if (t.type == "income") "+" else "-") +
                                com.qiandaizi.app.core.amount(t.amount),
                            fontSize = 14.sp,
                            color = if (t.type == "income")
                                com.qiandaizi.app.core.IncomeGreen
                            else com.qiandaizi.app.core.TextMain,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End) {
                        androidx.compose.material3.TextButton(onClick = {
                            scope.launch {
                                runCatching { appState.api().restoreTrash(t.id) }
                                    .onSuccess {
                                        list = list.filterNot { it.id == t.id }
                                        appState.bump()
                                        appState.notify("已恢复")
                                    }
                                    .onFailure { appState.notify(explainError(it)) }
                            }
                        }) {
                            Icon(Icons.Filled.Restore, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text("恢复", color = com.qiandaizi.app.core.YellowDark,
                                fontSize = 13.sp)
                        }
                        androidx.compose.material3.TextButton(onClick = {
                            scope.launch {
                                runCatching { appState.api().purgeTrash(t.id) }
                                    .onSuccess {
                                        list = list.filterNot { it.id == t.id }
                                        appState.notify("已彻底删除")
                                    }
                                    .onFailure { appState.notify(explainError(it)) }
                            }
                        }) {
                            Text("彻底删除", color = Color(0xFFE5484D), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OpLogsScreen(onBack: () -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()

    var list by remember { mutableStateOf<List<OpLogDto>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { appState.api().oplogs().list }
            .onSuccess {
                list = it
                loaded = true
            }
            .onFailure { appState.notify(explainError(it)) }
    }

    SubPageScaffold(title = "操作日志", onBack = onBack) {
        if (!loaded) {
            com.qiandaizi.app.ui.common.LoadingBox()
            return@SubPageScaffold
        }
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            if (list.isEmpty()) item { EmptyHint("暂无日志") }
            items(list, key = { it.id }) { l ->
                WhiteCard(modifier = Modifier.padding(bottom = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isGet = l.method == "GET"
                        Text(
                            l.method,
                            fontSize = 10.sp,
                            color = if (isGet) com.qiandaizi.app.core.BrandBlue
                            else com.qiandaizi.app.core.ExpenseRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                .background(
                                    if (isGet) Color(0xFFEAF1FF) else Color(0xFFFDECEC)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(l.summary.ifBlank { l.path }, fontSize = 13.sp,
                                color = com.qiandaizi.app.core.TextMain, maxLines = 1)
                            Text(
                                l.createdAt, fontSize = 10.sp, color = TextSub,
                                modifier = Modifier.padding(top = 2.dp))
                        }
                        Text("${l.status}", fontSize = 11.sp, color = TextSub)
                    }
                }
            }
        }
    }
}
