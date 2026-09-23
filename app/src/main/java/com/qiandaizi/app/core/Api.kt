package com.qiandaizi.app.core

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface Api {

    // ---------- Meta / Auth ----------
    @GET("meta")
    suspend fun meta(): MetaDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginReq): LoginResultResp

    @GET("auth/me")
    suspend fun me(): UserWrapDto

    @PUT("auth/me")
    suspend fun updateMe(@Body body: UpdateMeReq): UserWrapDto

    // ---------- 账本 ----------
    @GET("books")
    suspend fun books(): List<BookDto>

    @POST("books")
    suspend fun createBook(@Body body: BookReq): IdDto

    @PUT("books/{id}")
    suspend fun renameBook(@Path("id") id: Int, @Body body: BookReq): OkDto

    @DELETE("books/{id}")
    suspend fun deleteBook(@Path("id") id: Int): OkDto

    @GET("books/{id}/members")
    suspend fun bookMembers(@Path("id") id: Int): List<AttrMember>

    @POST("books/{id}/members")
    suspend fun addBookMember(@Path("id") id: Int, @Body body: UsernameReq): OkDto

    @DELETE("books/{id}/members/{uid}")
    suspend fun removeBookMember(@Path("id") id: Int, @Path("uid") uid: Int): OkDto

    // ---------- 分类 ----------
    @GET("categories")
    suspend fun categories(): List<CategoryDto>

    @POST("categories")
    suspend fun createCategory(@Body body: CategoryReq): IdDto

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body body: CategoryReq): OkDto

    @DELETE("categories/{id}")
    suspend fun deleteCategory(
        @Path("id") id: Int,
        @Query("mergeTo") mergeTo: Int? = null
    ): OkDto

    // ---------- 流水 ----------
    @GET("flows")
    suspend fun flows(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("payment") payment: String? = null,
        @Query("attributionUid") attributionUid: Int? = null,
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30,
        @Query("sortBy") sortBy: String = "flow_time",
        @Query("order") order: String = "desc"
    ): FlowPageDto

    @POST("flows")
    suspend fun createFlow(@Body body: FlowReq): IdDto

    @PUT("flows/{id}")
    suspend fun updateFlow(@Path("id") id: Int, @Body body: FlowReq): OkDto

    @DELETE("flows/{id}")
    suspend fun deleteFlow(@Path("id") id: Int): OkDto

    @GET("flows/attributions")
    suspend fun attributions(): AttrListDto

    // ---------- 回收站 ----------
    @GET("flows/trash")
    suspend fun trash(@Query("limit") limit: Int = 500): TrashListDto

    @POST("flows/trash/{id}/restore")
    suspend fun restoreTrash(@Path("id") id: Int): OkDto

    @DELETE("flows/trash/{id}")
    suspend fun purgeTrash(@Path("id") id: Int): OkDto

    // ---------- 统计 ----------
    @GET("stats/overview")
    suspend fun overview(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): OverviewDto

    @GET("stats/moneybags")
    suspend fun moneybags(@Query("month") month: String? = null): MoneyBagsDto

    @GET("stats/category")
    suspend fun statCategory(
        @Query("type") type: String = "expense",
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): List<NameValueDto>

    @GET("stats/payment")
    suspend fun statPayment(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): List<NameValueDto>

    @GET("stats/attribution")
    suspend fun statAttribution(
        @Query("type") type: String = "expense",
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): List<NameValueDto>

    @GET("stats/daily")
    suspend fun statDaily(
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): List<DailyDto>

    @GET("stats/monthly")
    suspend fun statMonthly(
        @Query("year") year: Int,
        @Query("category") category: String? = null
    ): List<MonthlyDto>

    @GET("stats/calendar")
    suspend fun statCalendar(@Query("month") month: String): List<DailyDto>

    @GET("stats/facets")
    suspend fun facets(): FacetsDto

    // ---------- 预算 ----------
    @GET("budgets")
    suspend fun budgets(@Query("year") year: Int): BudgetDataDto

    @POST("budgets")
    suspend fun setBudget(@Body body: BudgetReq): OkDto

    @DELETE("budgets")
    suspend fun deleteBudget(
        @Query("year") year: Int,
        @Query("category") category: String = ""
    ): OkDto

    @POST("budgets/copy")
    suspend fun copyBudgets(@Body body: CopyBudgetReq): CopyBudgetResp

    // ---------- 攒钱 / 资产 ----------
    @GET("savings")
    suspend fun savings(): SavingsOverviewDto

    @PUT("savings/goal")
    suspend fun setSavingsGoal(@Body body: SavingsGoalReq): OkDto

    @POST("savings/items")
    suspend fun addSavingsItem(@Body body: SavingsItemReq): IdDto

    @PUT("savings/items/{id}")
    suspend fun updateSavingsItem(@Path("id") id: Int, @Body body: SavingsItemReq): OkDto

    @DELETE("savings/items/{id}")
    suspend fun deleteSavingsItem(@Path("id") id: Int): OkDto

    @POST("savings/items/{id}/set-amount")
    suspend fun setSavingsItemAmount(@Path("id") id: Int, @Body body: SetAmountReq): OkDto

    // ---------- 钱包 ----------
    @GET("wallets")
    suspend fun wallets(): WalletsDto

    @POST("wallets")
    suspend fun addWallet(@Body body: WalletReq): IdDto

    @PUT("wallets/{id}")
    suspend fun updateWallet(@Path("id") id: Int, @Body body: WalletReq): OkDto

    @DELETE("wallets/{id}")
    suspend fun deleteWallet(@Path("id") id: Int): OkDto

    @GET("wallets/{id}/txns")
    suspend fun walletTxns(@Path("id") id: Int): WalletTxnsResp

    @POST("wallets/{id}/txns")
    suspend fun addWalletTxn(@Path("id") id: Int, @Body body: WalletTxnReq): IdDto

    @DELETE("wallets/txns/{txnId}")
    suspend fun deleteWalletTxn(@Path("txnId") txnId: Int): OkDto

    // ---------- 账单 ----------
    @GET("bills/monthly")
    suspend fun billsMonthly(@Query("year") year: Int? = null): BillMonthlyDto

    @GET("bills/month-detail")
    suspend fun billMonthDetail(@Query("ym") ym: String): BillMonthDetailDto

    // ---------- 定期记账 ----------
    @GET("recurring")
    suspend fun recurring(): List<RecurringDto>

    @POST("recurring")
    suspend fun addRecurring(@Body body: RecurringReq): IdDto

    @PUT("recurring/{id}")
    suspend fun updateRecurring(@Path("id") id: Int, @Body body: RecurringReq): OkDto

    @DELETE("recurring/{id}")
    suspend fun deleteRecurring(@Path("id") id: Int): OkDto

    @POST("recurring/generate")
    suspend fun generateRecurring(): GenerateResp

    // ---------- 常用名 ----------
    @GET("presets")
    suspend fun presets(
        @Query("type") type: String = "expense",
        @Query("limit") limit: Int = 200
    ): PresetsDto

    @POST("presets")
    suspend fun addPreset(@Body body: PresetReq): IdDto

    @PUT("presets/{id}")
    suspend fun updatePreset(@Path("id") id: Int, @Body body: PresetReq): OkDto

    @POST("presets/hide")
    suspend fun hidePreset(@Body body: PresetHideReq): OkDto

    @POST("presets/unhide")
    suspend fun unhidePreset(@Body body: PresetHideReq): OkDto

    @DELETE("presets/{id}")
    suspend fun deletePreset(@Path("id") id: Int): OkDto

    @POST("presets/scan")
    suspend fun scanPresets(): OkDto

    // ---------- AI 设置 / AI ----------
    @GET("settings/ai")
    suspend fun aiModels(): AiModelsDto

    @PUT("settings/ai")
    suspend fun saveAiModels(@Body body: AiModelsPutReq): OkDto

    @GET("ai/status")
    suspend fun aiStatus(): AiStatusDto

    @POST("ai/parse")
    suspend fun aiParse(@Body body: AiParseReq): AiParseDto

    @POST("ai/parse-image")
    suspend fun aiParseImage(@Body body: AiParseImageReq): AiParseDto

    @GET("ai/analyze")
    suspend fun aiAnalyze(@Query("month") month: String? = null): AiAnalyzeDto

    // ---------- 用户管理 ----------
    @GET("admin/users")
    suspend fun adminUsers(): List<AdminUserDto>

    @POST("admin/users")
    suspend fun createUser(@Body body: CreateUserReq): OkDto

    @PUT("admin/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body body: CreateUserReq): OkDto

    @DELETE("admin/users/{id}")
    suspend fun deleteUser(@Path("id") id: Int): OkDto

    // ---------- 操作日志 ----------
    @GET("oplogs")
    suspend fun oplogs(@Query("limit") limit: Int = 80): OpLogListDto
}

// 接口返回的补充包装类型
@kotlinx.serialization.Serializable
data class LoginResultResp(
    val token: String = "",
    val user: UserDto = UserDto()
)

@kotlinx.serialization.Serializable
data class UserWrapDto(val user: UserDto = UserDto())

@kotlinx.serialization.Serializable
data class UsernameReq(val username: String = "")

@kotlinx.serialization.Serializable
data class CopyBudgetReq(val fromYear: Int? = null, val toYear: Int? = null)

@kotlinx.serialization.Serializable
data class CopyBudgetResp(val ok: Boolean = true, val copied: Int = 0)

@kotlinx.serialization.Serializable
data class GenerateResp(val ok: Boolean = true, val generated: Int = 0)

@kotlinx.serialization.Serializable
data class WalletTxnDto(
    val id: Int = 0,
    val amount: Double = 0.0,
    val ymd: String = "",
    val note: String = "",
    val op_user: String = ""
)

@kotlinx.serialization.Serializable
data class WalletTxnsResp(val list: List<WalletTxnDto> = emptyList())

@kotlinx.serialization.Serializable
data class BillMonthDetailDto(
    val ym: String = "",
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val list: List<FlowDto> = emptyList()
)
