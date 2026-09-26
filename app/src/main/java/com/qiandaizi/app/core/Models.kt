package com.qiandaizi.app.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ============ 基础实体 ============

@Serializable
data class UserDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val role: String = "user",
    val color: String? = null
)

@Serializable
data class BookDto(
    val id: Int = 0,
    val name: String = "",
    @SerialName("owner_id") val ownerId: Int = 0,
    val role: String = "editor",
    val members: Int = 0,
    val flows: Int = 0
)

@Serializable
data class CategoryDto(
    val id: Int = 0,
    @SerialName("book_id") val bookId: Int = 0,
    val name: String = "",
    val type: String = "expense",
    val icon: String = "💰",
    val color: String = "#7c8cff",
    val sort: Int = 0,
    @SerialName("flow_count") val flowCount: Int = 0
)

@Serializable
data class FlowDto(
    val id: Int = 0,
    val type: String = "expense",
    val amount: Double = 0.0,
    val category: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    val description: String = "",
    @SerialName("flow_time") val flowTime: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val source: String = "",
    val attribution: String = "",
    @SerialName("attribution_color") val attributionColor: String? = null,
    @SerialName("attribution_uid") val attributionUid: Int? = null
)

@Serializable
data class FlowPageDto(
    val total: Int = 0,
    val expense: Double = 0.0,
    val income: Double = 0.0,
    val list: List<FlowDto> = emptyList()
)

@Serializable
data class IdDto(val id: Int = 0, val dup: Boolean = false)

@Serializable
data class OkDto(val ok: Boolean = true)

// ============ 统计 ============

@Serializable
data class OverviewDto(
    val expense: Double = 0.0,
    val income: Double = 0.0,
    val balance: Double = 0.0,
    val count: Int = 0,
    val totalCount: Int = 0
)

@Serializable
data class NameValueDto(
    val name: String = "",
    val value: Double = 0.0,
    val count: Int = 0,
    val color: String? = null
)

@Serializable
data class MemberSummaryDto(
    val name: String = "",
    val color: String? = null,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0
)

@Serializable
data class DailyDto(
    val date: String = "",
    val expense: Double = 0.0,
    val income: Double = 0.0,
    val top: JsonObject? = null
)

@Serializable
data class MonthlyDto(
    val month: String = "",
    val expense: Double = 0.0,
    val income: Double = 0.0
)

@Serializable
data class BagBucket(
    val uid: Int? = null,
    val nickname: String = "",
    val color: String? = null,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val yearIncome: Double = 0.0,
    val yearExpense: Double = 0.0
)

@Serializable
data class BagTotal(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0
)

@Serializable
data class MoneyBagsDto(
    val month: String = "",
    val year: String = "",
    val buckets: List<BagBucket> = emptyList(),
    val total: BagTotal = BagTotal()
)

@Serializable
data class FacetsDto(
    val years: List<String> = emptyList(),
    val months: List<String> = emptyList(),
    val attributions: List<String> = emptyList()
)

// ============ 预算 ============

@Serializable
data class BudgetTotalDto(
    val amount: Double = 0.0,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val percent: Int = 0
)

@Serializable
data class BudgetCatDto(
    val category: String = "",
    val categories: List<String> = emptyList(),
    val amount: Double = 0.0,
    val expression: String = "",
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val percent: Int = 0
)

@Serializable
data class BudgetDataDto(
    val year: Int = 0,
    val total: BudgetTotalDto = BudgetTotalDto(),
    val categories: List<BudgetCatDto> = emptyList()
)

// ============ 攒钱 / 资产 ============

@Serializable
data class SavingsItemDto(
    val id: Int = 0,
    val name: String = "",
    val sign: Int = 1,
    val amount: Double = 0.0,
    val note: String = "",
    @SerialName("as_of") val asOf: String = "",
    @SerialName("as_of_end") val asOfEnd: String = ""
)

@Serializable
data class SavingsCurrentDto(
    val asset: Double = 0.0,
    val liability: Double = 0.0,
    val net: Double = 0.0,
    val percent: Int = 0,
    val remaining: Double = 0.0
)

@Serializable
data class SavingsOverviewDto(
    val goal: JsonObject? = null,
    val items: List<SavingsItemDto> = emptyList(),
    val current: JsonObject? = null,
    val months: List<JsonObject> = emptyList()
)

// ============ 钱包 ============

@Serializable
data class WalletDto(
    val id: Int = 0,
    val name: String = "",
    val icon: String = "👛",
    val target: Double = 0.0,
    val balance: Double = 0.0,
    val note: String = ""
)

@Serializable
data class WalletsDto(
    val wallets: List<WalletDto> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalTarget: Double = 0.0
)

// ============ 常用名 ============

@Serializable
data class PresetDto(
    val id: Int = 0,
    val name: String = "",
    val type: String = "expense",
    val category: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    val amount: Double = 0.0,
    val count: Int = 0,
    @SerialName("avg_amount") val avgAmount: Double = 0.0
)

@Serializable
data class PresetsDto(
    val presets: List<PresetDto> = emptyList(),
    val frequent: List<PresetDto> = emptyList(),
    val recent: List<PresetDto> = emptyList(),
    val hidden: List<PresetDto> = emptyList()
)

// ============ 定期记账 ============

@Serializable
data class RecurringDto(
    val id: Int = 0,
    val type: String = "expense",
    val category: String = "其他",
    val description: String = "",
    val amount: Double = 0.0,
    @SerialName("payment_method") val paymentMethod: String = "",
    val freq: String = "monthly",
    @SerialName("day_of_month") val dayOfMonth: Int = 1,
    @SerialName("month_of_year") val monthOfYear: Int = 1,
    val note: String = "",
    @SerialName("next_run") val nextRun: String = "",
    val attribution: String = ""
)

// ============ 回收站 ============

@Serializable
data class TrashDto(
    val id: Int = 0,
    val type: String = "expense",
    val amount: Double = 0.0,
    val category: String = "",
    val description: String = "",
    @SerialName("payment_method") val paymentMethod: String = "",
    @SerialName("flow_time") val flowTime: String = "",
    @SerialName("deleted_by") val deletedBy: String = "",
    @SerialName("deleted_at") val deletedAt: String = "",
    val attribution: String = ""
)

@Serializable
data class TrashListDto(val list: List<TrashDto> = emptyList())

// ============ 用户管理 ============

@Serializable
data class AdminUserDto(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val role: String = "user",
    @SerialName("created_at") val createdAt: String = "",
    val color: String? = null,
    val books: Int = 0,
    val flows: Int = 0
)

// ============ 归属人 ============

@Serializable
data class AttrMember(val id: Int = 0, val nickname: String = "")

@Serializable
data class AttrListDto(
    val members: List<AttrMember> = emptyList(),
    val others: List<String> = emptyList()
)

// ============ AI ============

@Serializable
data class MetaDto(val name: String = "钱袋子", val version: String = "")

@Serializable
data class AiStatusDto(
    val enabled: Boolean = false,
    val provider: String? = null,
    val model: String? = null,
    @SerialName("imageModel") val imageModel: String? = null,
    @SerialName("baiduOcr") val baiduOcr: Boolean? = null
)

@Serializable
data class AiParseDto(
    val type: String? = null,
    val amount: Double? = null,
    val category: String? = null,
    val description: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val date: String? = null,
    val source: String? = null,
    val ocrType: String? = null,
    val raw: String? = null,
    val kind: String? = null
)

@Serializable
data class AiAnalyzeDto(
    val summary: JsonObject? = null,
    val analysis: String = "",
    val ai: Boolean = false
)

@Serializable
data class AiModelDto(
    val id: String = "",
    val name: String = "",
    val provider: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val imageModel: String = "",
    val isDefault: Boolean = false
)

@Serializable
data class AiModelsDto(val models: List<AiModelDto> = emptyList(), val enabled: Boolean = false)

// ============ 账单 ============

@Serializable
data class BillRowDto(
    val month: String? = null,
    val label: String = "",
    val ym: String? = null,
    val year: Int? = null,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val count: Int = 0
)

@Serializable
data class BillMonthlyDto(
    val year: Int = 0,
    val years: List<Int> = emptyList(),
    val summary: BillRowDto = BillRowDto(),
    val rows: List<BillRowDto> = emptyList()
)

// ============ 本地会话 ============

@Serializable
data class RawAccount(
    val username: String = "",
    val token: String = "",
    val user: UserDto = UserDto()
)

@Serializable
data class RawSession(
    val servers: List<String> = emptyList(),
    val activeServer: String? = null,
    val accounts: Map<String, List<RawAccount>> = emptyMap(),
    val activeUser: Map<String, String> = emptyMap(),
    val books: Map<String, Int> = emptyMap()
)

// ============ 操作日志 ============

@Serializable
data class OpLogDto(
    val id: Int = 0,
    val method: String = "",
    val path: String = "",
    val summary: String = "",
    val status: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class OpLogListDto(val list: List<OpLogDto> = emptyList())

// ============ 请求体（字段默认值不序列化，便于局部提交） ============

@Serializable
data class LoginReq(val username: String, val password: String)

@Serializable
data class UpdateMeReq(
    val nickname: String? = null,
    val oldPassword: String? = null,
    val newPassword: String? = null
)

@Serializable
data class BookReq(val name: String? = null)

@Serializable
data class CategoryReq(
    val name: String? = null,
    val type: String? = null,
    val icon: String? = null,
    val color: String? = null
)

@Serializable
data class FlowReq(
    val type: String? = null,
    val amount: Double? = null,
    val category: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val description: String? = null,
    @SerialName("flow_time") val flowTime: String? = null,
    val source: String? = null,
    @SerialName("attribution_uid") val attributionUid: Int? = null
)

@Serializable
data class BudgetReq(
    val year: Int? = null,
    val amount: Double? = null,
    val category: String? = null,
    val expression: String? = null,
    val categories: List<String>? = null
)

@Serializable
data class SavingsGoalReq(val target: Double? = null, val note: String? = null)

@Serializable
data class SavingsItemReq(
    val name: String? = null,
    val sign: Int? = null,
    val amount: Double? = null,
    val note: String? = null
)

@Serializable
data class SetAmountReq(
    val amount: Double? = null,
    val note: String? = null,
    val ymd: String? = null
)

@Serializable
data class WalletReq(
    val name: String? = null,
    val icon: String? = null,
    val target: Double? = null,
    val note: String? = null
)

@Serializable
data class WalletTxnReq(
    val amount: Double? = null,
    val direction: String? = null,
    val ymd: String? = null,
    val note: String? = null
)

@Serializable
data class RecurringReq(
    val type: String? = null,
    val category: String? = null,
    val description: String? = null,
    val amount: Double? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val freq: String? = null,
    @SerialName("day_of_month") val dayOfMonth: Int? = null,
    @SerialName("month_of_year") val monthOfYear: Int? = null,
    val note: String? = null
)

@Serializable
data class PresetReq(
    val name: String? = null,
    val type: String? = null,
    val category: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val amount: Double? = null
)

@Serializable
data class PresetHideReq(val name: String? = null)

@Serializable
data class CreateUserReq(
    val username: String? = null,
    val password: String? = null,
    val nickname: String? = null,
    val role: String? = null
)

@Serializable
data class AiParseReq(val text: String? = null)

@Serializable
data class AiParseImageReq(
    val image: String? = null,
    val text: String? = null,
    val ocrType: String? = null
)

@Serializable
data class AiModelsPutReq(val models: List<AiModelDto> = emptyList())

// 百度 OCR（图片识别，无需大模型）
@Serializable
data class BaiduOcrTypeDto(
    val id: String = "",
    val name: String = "",
    val used: Int = 0,
    val exhausted: Boolean = false
)

@Serializable
data class BaiduOcrDto(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val hasSecret: Boolean = false,
    val type: String = "",
    val types: List<BaiduOcrTypeDto> = emptyList()
)

@Serializable
data class BaiduOcrPutReq(
    val apiKey: String,
    val secretKey: String,
    val type: String = ""
)

// 水电气规则（透传 JSON）
typealias UtilityRulesResp = Map<String, JsonElement>
