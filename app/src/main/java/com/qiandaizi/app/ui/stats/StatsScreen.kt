package com.qiandaizi.app.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiandaizi.app.core.AppGraph
import com.qiandaizi.app.core.MoneyBagsDto
import com.qiandaizi.app.core.TextMain
import com.qiandaizi.app.core.TextSub
import com.qiandaizi.app.core.addMonth
import com.qiandaizi.app.core.amount
import com.qiandaizi.app.core.explainError
import com.qiandaizi.app.core.money
import com.qiandaizi.app.core.monthCn
import com.qiandaizi.app.core.monthRange
import com.qiandaizi.app.core.nowMonth
import com.qiandaizi.app.core.nowYear
import com.qiandaizi.app.ui.common.SegmentedTabs
import com.qiandaizi.app.ui.common.WhiteCard
import com.qiandaizi.app.ui.common.ColumnBars
import com.qiandaizi.app.ui.common.DonutChart
import com.qiandaizi.app.ui.common.Slice
import com.qiandaizi.app.ui.common.TrendLine
import com.qiandaizi.app.ui.common.parseHex

private val PALETTE = listOf(
    Color(0xFF6366F1), Color(0xFFEF4444), Color(0xFFF59E0B),
    Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFFEC4899),
    Color(0xFF8B5CF6), Color(0xFF14B8A6), Color(0xFFF97316), Color(0xFF64748B)
)

@Composable
fun StatsScreen() {
    val appState = AppGraph.state

    var typeIndex by rememberSaveable { mutableIntStateOf(0) } // 0支出 1收入
    var rangeIndex by rememberSaveable { mutableIntStateOf(0) } // 0本月 1本年
    var selMonth by rememberSaveable { mutableStateOf(nowMonth()) }
    var selYear by rememberSaveable { mutableStateOf(nowYear()) }

    var overview by remember { mutableStateOf<com.qiandaizi.app.core.OverviewDto?>(null) }
    var category by remember { mutableStateOf<List<com.qiandaizi.app.core.NameValueDto>>(emptyList()) }
    var attribution by remember { mutableStateOf<List<com.qiandaizi.app.core.NameValueDto>>(emptyList()) }
    var daily by remember { mutableStateOf<List<com.qiandaizi.app.core.DailyDto>>(emptyList()) }
    var monthly by remember { mutableStateOf<List<com.qiandaizi.app.core.MonthlyDto>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val type = if (typeIndex == 1) "income" else "expense"
    val period = if (rangeIndex == 0) monthRange(selMonth)
    else ("$selYear-01-01" to "$selYear-12-31")
    val barYear = if (rangeIndex == 0) selMonth.substring(0, 4).toInt()
    else selYear.toInt()

    LaunchedEffect(typeIndex, rangeIndex, selMonth, selYear, appState.epoch) {
        errorMsg = null
        runCatching {
            val api = appState.api()
            val ov = api.overview(period.first, period.second)
            val cat = api.statCategory(type, period.first, period.second)
            val attr = api.statAttribution(type, period.first, period.second)
            val day = api.statDaily(period.first, period.second)
            val mon = api.statMonthly(barYear)
            StatsBundle(ov, cat, attr, day, mon)
        }.onSuccess { b ->
            overview = b.ov
            category = b.cat
            attribution = b.attr
            daily = b.day
            monthly = b.mon
        }.onFailure { errorMsg = explainError(it) }
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
                .padding(start = 20.dp, top = 24.dp, bottom = 20.dp)
        ) {
            Text("统计", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextMain)
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                SegmentedTabs(
                    options = listOf("支出", "收入"),
                    selected = typeIndex,
                    onSelect = { typeIndex = it }
                )
                SegmentedTabs(
                    options = listOf("本月", "本年"),
                    selected = rangeIndex,
                    onSelect = { rangeIndex = it }
                )
            }
        }

        Column(Modifier.padding(14.dp)) {

            if (rangeIndex == 0) {
                PeriodStepper(
                    text = monthCn(selMonth),
                    onPrev = { selMonth = addMonth(selMonth, -1) },
                    onNext = { selMonth = addMonth(selMonth, 1) }
                )
            } else {
                PeriodStepper(
                    text = "$selYear 年",
                    onPrev = { selYear = (selYear.toInt() - 1).toString() },
                    onNext = { selYear = (selYear.toInt() + 1).toString() }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (errorMsg != null) {
                WhiteCard {
                    Text(errorMsg, fontSize = 13.sp, color = TextSub)
                }
            } else {
                // 概览
                WhiteCard {
                    Text("收支概览", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Row {
                        OverviewCell(
                            "支出",
                            money(overview?.expense),
                            com.qiandaizi.app.core.ExpenseRed,
                            Modifier.weight(1f)
                        )
                        OverviewCell(
                            "收入",
                            money(overview?.income),
                            com.qiandaizi.app.core.IncomeGreen,
                            Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("结余", fontSize = 12.sp, color = TextSub)
                            Text(
                                money(overview?.balance),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if ((overview?.balance ?: 0.0) >= 0)
                                    com.qiandaizi.app.core.IncomeGreen
                                else com.qiandaizi.app.core.ExpenseRed
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("笔数", fontSize = 12.sp, color = TextSub)
                            Text(
                                "${overview?.count ?: 0} 笔",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMain
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 分类占比
                WhiteCard {
                    Text(
                        if (type == "income") "收入分类" else "支出分类",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    val slices = category.mapIndexed { i, d ->
                        Slice(
                            d.name, d.value,
                            parseHex(d.color) ?: PALETTE[i % PALETTE.size]
                        )
                    }
                    val total = category.sumOf { it.value }
                    DonutChart(
                        slices = slices,
                        centerText = money(total),
                        centerSub = "合计"
                    )
                    Spacer(Modifier.height(10.dp))
                    category.take(8).forEach { d ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(d.name, fontSize = 13.sp, color = TextMain,
                                modifier = Modifier.weight(1f))
                            Text("${d.count} 笔", fontSize = 12.sp, color = TextSub)
                            Spacer(Modifier.size(10.dp))
                            Text(money(d.value), fontSize = 13.sp, color = TextMain,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 归属占比
                WhiteCard {
                    Text(
                        if (type == "income") "收入归属" else "消费归属",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    if (attribution.isEmpty()) {
                        Text("暂无归属数据", fontSize = 13.sp, color = TextSub)
                    } else {
                        val slices = attribution.mapIndexed { i, d ->
                            Slice(
                                d.name, d.value,
                                parseHex(d.color) ?: PALETTE[i % PALETTE.size]
                            )
                        }
                        DonutChart(slices = slices, centerText = "", centerSub = "",
                            legend = true)
                        Spacer(Modifier.height(8.dp))
                        attribution.take(6).forEach { d ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(d.name, fontSize = 13.sp, color = TextMain,
                                    modifier = Modifier.weight(1f))
                                Text(money(d.value), fontSize = 13.sp, color = TextMain)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 每日趋势
                WhiteCard {
                    Text("每日趋势", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    if (daily.size < 2) {
                        Text("数据不足，无法绘制趋势", fontSize = 13.sp, color = TextSub)
                    } else {
                        val pts = daily.map { d ->
                            d.date.substring(5) to
                                (if (type == "income") d.income else d.expense)
                        }
                        TrendLine(
                            points = pts,
                            color = if (type == "income")
                                com.qiandaizi.app.core.IncomeGreen
                            else com.qiandaizi.app.core.ExpenseRed
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 月度柱状
                WhiteCard {
                    Text(
                        if (type == "income") "月度收入" else "月度支出",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    val items = monthly.map { m ->
                        m.month.substring(5).trimStart('0') + "月" to
                            (if (type == "income") m.income else m.expense)
                    }
                    ColumnBars(
                        items = items,
                        color = if (type == "income")
                            com.qiandaizi.app.core.IncomeGreen
                        else com.qiandaizi.app.core.BrandBlue
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PeriodStepper(text: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("‹", fontSize = 22.sp, color = com.qiandaizi.app.core.YellowDark,
            modifier = Modifier
                .size(40.dp)
                .clickable { onPrev() },
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextMain,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("›", fontSize = 22.sp, color = com.qiandaizi.app.core.YellowDark,
            modifier = Modifier
                .size(40.dp)
                .clickable { onNext() },
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun OverviewCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 12.sp, color = TextSub)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color,
            modifier = Modifier.padding(top = 4.dp))
    }
}

private data class StatsBundle(
    val ov: com.qiandaizi.app.core.OverviewDto,
    val cat: List<com.qiandaizi.app.core.NameValueDto>,
    val attr: List<com.qiandaizi.app.core.NameValueDto>,
    val day: List<com.qiandaizi.app.core.DailyDto>,
    val mon: List<com.qiandaizi.app.core.MonthlyDto>
)
