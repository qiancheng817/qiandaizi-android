package com.qiandaizi.app.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun money(n: Double?): String {
    val v = n ?: 0.0
    return String.format(Locale.CHINA, "¥%,.2f", v)
}

/** 不带货币符号的金额，如 5,463.75 */
fun amount(n: Double?): String = String.format(Locale.CHINA, "%,.2f", n ?: 0.0)

fun signedAmount(type: String, n: Double?): String {
    val v = amount(n)
    return if (type == "income") "+$v" else "-$v"
}

fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())

fun nowMonth(): String = SimpleDateFormat("yyyy-MM", Locale.CHINA).format(Date())

fun nowYear(): String = SimpleDateFormat("yyyy", Locale.CHINA).format(Date())

/** YYYY-MM-DD → MM-DD */
fun md(date: String?): String {
    if (date.isNullOrBlank() || date.length < 10) return date ?: ""
    return date.substring(5, 10)
}

/** 2026-09 → 2026年9月 */
fun monthCn(ym: String): String {
    val p = ym.split("-")
    if (p.size < 2) return ym
    return "${p[0]}年${p[1].trimStart('0')}月"
}

/** 本月起止日期 */
fun monthRange(ym: String): Pair<String, String> {
    val cal = Calendar.getInstance(Locale.CHINA)
    val (y, m) = ym.split("-").map { it.toInt() }
    cal.clear()
    cal.set(y, m - 1, 1)
    val start = "%04d-%02d-01".format(y, m)
    val last = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val end = "%04d-%02d-%02d".format(y, m, last)
    return start to end
}

fun addMonth(ym: String, delta: Int): String {
    val cal = Calendar.getInstance(Locale.CHINA)
    val (y, m) = ym.split("-").map { it.toInt() }
    cal.clear()
    cal.set(y, m - 1, 1)
    cal.add(Calendar.MONTH, delta)
    return SimpleDateFormat("yyyy-MM", Locale.CHINA).format(cal.time)
}

fun weekdayCn(date: String): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
        timeZone = TimeZone.getDefault()
    }
    val d = fmt.parse(date) ?: return ""
    val names = arrayOf("日", "一", "二", "三", "四", "五", "六")
    val cal = Calendar.getInstance().apply { time = d }
    return names[cal.get(Calendar.DAY_OF_WEEK) - 1]
}
