package com.qiandaizi.app.ui.auth

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.qiandaizi.app.core.CardWhite
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.Yellow
import com.qiandaizi.app.core.YellowDark
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.ui.common.ConfirmDialog
import com.qiandaizi.app.ui.common.TextInputDialog
import kotlinx.coroutines.launch
import okhttp3.toHttpUrlOrNull

@Composable
fun ServerScreen(onBack: (() -> Unit)? = null) {
    val state = AppGraph.state
    val scope = rememberCoroutineScope()

    var addDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun validate(url: String): String {
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            throw IllegalArgumentException("地址需以 http:// 或 https:// 开头")
        if (url.toHttpUrlOrNull() == null)
            throw IllegalArgumentException("地址格式不正确")
        return url
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Yellow)
            .verticalScroll(rememberScrollState())
    ) {
        if (onBack != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 10.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = TextMain,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onBack() }
                        .padding(7.dp)
                )
            }
        }
        Spacer(Modifier.height(if (onBack != null) 20.dp else 60.dp))
        Box(
            Modifier
                .padding(bottom = 10.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFFFF3C4)),
                contentAlignment = Alignment.Center
            ) { Text("💰", fontSize = 48.sp) }
        }
        Text(
            "钱袋子",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextMain,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            "先配置你的钱袋子服务器地址",
            fontSize = 14.sp,
            color = Color(0xFF7A6520),
            modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Column(Modifier.padding(horizontal = 18.dp)) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "服务器列表",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Spacer(Modifier.height(8.dp))

                    if (state.session.servers.isEmpty()) {
                        Text(
                            "还没有服务器，点击下方按钮添加",
                            fontSize = 13.sp,
                            color = TextSub,
                            modifier = Modifier.padding(vertical = 14.dp)
                        )
                    }

                    state.session.servers.forEach { url ->
                        val active = url == state.session.activeServer
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    scope.launch {
                                        runCatching { state.selectServer(url) }
                                            .onFailure { errorMsg = explainError(it) }
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (active) YellowDark else Color(0xFFE4E6EA)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.size(10.dp))
                            Text(
                                url,
                                fontSize = 14.sp,
                                color = TextMain,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Filled.Edit,
                                contentDescription = "编辑",
                                tint = TextSub,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { editTarget = url }
                                    .padding(7.dp)
                            )
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除",
                                tint = Color(0xFFE5484D),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { deleteTarget = url }
                                    .padding(7.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF3C4))
                            .clickable { addDialog = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "＋ 添加服务器",
                            color = Color(0xFF8A6D1B),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (addDialog) {
        TextInputDialog(
            title = "添加服务器",
            placeholder = "http://服务器IP:9600",
            confirmText = "添加",
            onDismiss = { addDialog = false }
        ) { input ->
            addDialog = false
            scope.launch {
                runCatching {
                    validate(input)
                    state.addServer(input)
                }.onFailure { errorMsg = explainError(it) }
            }
        }
    }

    editTarget?.let { old ->
        TextInputDialog(
            title = "编辑服务器地址",
            initial = old,
            confirmText = "保存",
            onDismiss = { editTarget = null }
        ) { input ->
            editTarget = null
            scope.launch {
                runCatching {
                    validate(input)
                    state.updateServer(old, input)
                }.onFailure { errorMsg = explainError(it) }
            }
        }
    }

    deleteTarget?.let { url ->
        ConfirmDialog(
            title = "删除服务器",
            message = "确定删除「$url」？该服务器上记住的账号也会一并删除。",
            confirmText = "删除",
            danger = true,
            onConfirm = {
                deleteTarget = null
                scope.launch {
                    runCatching { state.deleteServer(url) }
                        .onFailure { errorMsg = explainError(it) }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }

    errorMsg?.let {
        ConfirmDialog(
            title = "提示",
            message = it,
            confirmText = "我知道了",
            onConfirm = { errorMsg = null },
            onDismiss = { errorMsg = null }
        )
    }
}
