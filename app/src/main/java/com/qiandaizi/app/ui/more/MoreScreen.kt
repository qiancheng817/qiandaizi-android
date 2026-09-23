package com.qiandaizi.app.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.Yellow
import com.qiandaizi.app.ui.Route
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(onOpen: (Route) -> Unit) {
    val appState = AppGraph.state
    val scope = rememberCoroutineScope()
    val me = appState.user()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF6F7F9))
    ) {
        // ===== 顶部双头像 =====
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 34.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Box(
                    Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Yellow)
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        me?.nickname?.take(1) ?: "我",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-26).dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE9EBF0)),
                    contentAlignment = Alignment.Center
                ) { Text("☁️", fontSize = 28.sp) }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${me?.nickname ?: ""} · ${me?.username ?: ""}",
                fontSize = 13.sp, color = TextSub
            )
        }

        Column(Modifier.padding(horizontal = 16.dp)) {

            // 两个快捷入口
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShortcutCard("懒人记账", "说句话就记好", "🔥") { onOpen(Route.QuickAi) }
                ShortcutCard("自动记账", "定期自动生成", "⏰") { onOpen(Route.Recurring) }
            }

            Spacer(Modifier.height(14.dp))

            // ===== 常用功能 =====
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Text("常用功能", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = TextMain)
                    Spacer(Modifier.height(8.dp))

                    val isAdmin = me?.role == "admin"
                    val common = buildList {
                        add("📒" to ("账本管理" to Route.Books))
                        add("📊" to ("预算管理" to Route.Budgets))
                        add("🏷" to ("类目管理" to Route.Categories))
                        add("⏰" to ("定时记账" to Route.Recurring))
                        add("💵" to ("资产管理" to Route.Savings))
                        add("👛" to ("钱包" to Route.Wallets))
                        add("📋" to ("常用名称" to Route.Presets))
                        add("🗓" to ("月度账单" to Route.Bills))
                        add("🗑" to ("回收站" to Route.Trash))
                        add("📜" to ("操作日志" to Route.OpLogs))
                        if (isAdmin) add("👪" to ("用户管理" to Route.AdminUsers))
                    }
                    common.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { (icon, pair) ->
                                GridCell(icon, pair.first, Modifier.weight(1f)) {
                                    onOpen(pair.second)
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ===== 设置与服务 =====
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                Column(Modifier.padding(16.dp)) {
                    Text("设置与服务", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = TextMain)
                    Spacer(Modifier.height(8.dp))

                    val settings = listOf(
                        "🤖" to ("AI设置" to Route.AiSettings),
                        "👤" to ("账号管理" to Route.Account),
                        "🔄" to ("切换服务器" to Route.ServerSwitch),
                        "ℹ️" to ("关于我们" to Route.About)
                    )
                    settings.chunked(4).forEach { row ->
                        Row(Modifier.fillMaxWidth()) {
                            row.forEach { (icon, pair) ->
                                GridCell(icon, pair.first, Modifier.weight(1f)) {
                                    onOpen(pair.second)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFFFDF2F2))
                            .clickable {
                                scope.launch {
                                    appState.logout()
                                    appState.notify("已退出登录")
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("退出登录", color = Color(0xFFE5484D), fontSize = 14.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowScope.ShortcutCard(
    title: String,
    subtitle: String,
    icon: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 30.sp)
            Spacer(Modifier.size(10.dp))
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextMain)
                Text(subtitle, fontSize = 11.sp, color = TextSub)
            }
        }
    }
}

@Composable
private fun GridCell(icon: String, label: String, modifier: Modifier = Modifier,
                     onClick: () -> Unit) {
    Column(
        modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 26.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = TextMain)
    }
}
