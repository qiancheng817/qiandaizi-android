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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AppBg
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.CardWhite
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.Yellow
import com.qiandaizi.app.core.YellowDark
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.ui.common.PrimaryButton
import com.qiandaizi.app.ui.common.QianField
import com.qiandaizi.app.ui.common.WhiteCard
import com.qiandaizi.app.ui.common.AppIcon
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    val state = AppGraph.state
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var quickLoading by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var managing by remember { mutableStateOf(false) }

    if (managing) {
        ServerScreen(onBack = { managing = false })
        return
    }

    Box(Modifier.fillMaxSize().background(Yellow)) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // 黄色头部（固定）
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 28.dp)
        ) {
            Box(
                Modifier
                    .size(66.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFF3C4)),
                contentAlignment = Alignment.Center
            ) {
                AppIcon(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                )
            }
            Spacer(Modifier.height(14.dp))
            Text("欢迎使用钱袋子", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(6.dp))
            Text("登录后数据将与服务器实时同步", fontSize = 13.sp, color = Color(0xFF7A6520))
        }

        // 主体（独立滚动，灰底）
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(AppBg)
        ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 当前服务器卡片
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { managing = true }
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3C4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF8A6D1B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("当前服务器", fontSize = 12.sp, color = TextSub)
                        Text(
                            state.server ?: "-",
                            fontSize = 14.sp,
                            color = TextMain,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("切换", fontSize = 13.sp, color = YellowDark)
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = YellowDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            WhiteCard {
                QianField(
                    label = "账号",
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "请输入账号"
                )
                Spacer(Modifier.height(14.dp))
                QianField(
                    label = "密码",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "请输入密码",
                    keyboardType = KeyboardType.Password,
                    password = true
                )

                errorMsg?.let { msg ->
                    Spacer(Modifier.height(10.dp))
                    Text(msg, fontSize = 13.sp, color = Color(0xFFE5484D))
                }

                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    text = "登 录",
                    loading = loading,
                    onClick = {
                        errorMsg = null
                        loading = true
                        scope.launch {
                            runCatching { state.login(username, password) }
                                .onFailure { errorMsg = explainError(it) }
                            loading = false
                        }
                    }
                )
            }

            // 已记住的账号 → 免密快速进入
            val remembered = state.rememberedAccounts()
            if (remembered.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                WhiteCard(padding = 14) {
                    Text(
                        "本机记住的账号（点击直接进入）",
                        fontSize = 13.sp,
                        color = TextSub,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    remembered.forEach { acc ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = quickLoading == null) {
                                    quickLoading = acc.username
                                    scope.launch {
                                        runCatching { state.switchAccount(acc.username) }
                                            .onFailure { errorMsg = explainError(it) }
                                        quickLoading = null
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFE79B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    acc.user.nickname.take(1).ifBlank { acc.username.take(1) },
                                    fontSize = 14.sp,
                                    color = TextMain
                                )
                            }
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    acc.user.nickname.ifBlank { acc.username },
                                    fontSize = 14.sp, color = TextMain
                                )
                                Text(acc.username, fontSize = 12.sp, color = TextSub)
                            }
                            if (quickLoading == acc.username) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = YellowDark
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextSub,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        }
    }
    }
}
