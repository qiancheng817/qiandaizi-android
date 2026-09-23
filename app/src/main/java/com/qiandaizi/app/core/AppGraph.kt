package com.qiandaizi.app.core

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object AppGraph {
    lateinit var state: AppState
        private set

    fun init(context: Context) {
        if (!::state.isInitialized) state = AppState(SessionStore(context.applicationContext))
    }
}

class AppState(private val store: SessionStore) {

    var session by mutableStateOf(RawSession())
        private set
    var loaded by mutableStateOf(false)
        private set
    var syncing by mutableStateOf(false)
        private set
    var notice by mutableStateOf<String?>(null)
        private set
    var epoch by mutableIntStateOf(0)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val server: String? get() = session.activeServer

    fun account(): RawAccount? {
        val srv = server ?: return null
        val uname = session.activeUser[srv] ?: return null
        return session.accounts[srv]?.find { it.username == uname }
    }

    fun token(): String? = account()?.token
    fun user(): UserDto? = account()?.user
    fun bookId(): Int? = server?.let { session.books[it] }

    /** 当前服务器上记住的全部账号（切换账号用） */
    fun rememberedAccounts(): List<RawAccount> =
        server?.let { session.accounts[it] } ?: emptyList()

    val isReady: Boolean
        get() = loaded && server != null && account() != null && bookId() != null

    fun api(): Api = NetFactory.api(server ?: error("未选择服务器"))

    init {
        scope.launch {
            store.flow.collect {
                session = it
                loaded = true
            }
        }
    }

    private suspend fun commit(transform: (RawSession) -> RawSession) {
        val next = transform(session)
        session = next
        store.save(next)
    }

    fun notify(msg: String) {
        notice = msg
    }

    fun bump() {
        epoch++
    }

    // ---------------- 服务器管理 ----------------

    suspend fun addServer(url: String) {
        val u = url.trim().trimEnd('/')
        if (u.isBlank()) throw IllegalArgumentException("地址不能为空")
        commit { s ->
            if (s.servers.any { it.equals(u, true) }) {
                s.copy(activeServer = s.servers.first { it.equals(u, true) })
            } else {
                s.copy(servers = s.servers + u, activeServer = s.activeServer ?: u)
            }
        }
    }

    suspend fun updateServer(old: String, new: String) {
        val u = new.trim().trimEnd('/')
        if (u.isBlank()) throw IllegalArgumentException("地址不能为空")
        commit { s ->
            if (s.servers.any { it.equals(u, true) && !it.equals(old, true) })
                throw IllegalArgumentException("该服务器地址已存在")
            val newServers = s.servers.map { if (it == old) u else it }
            val newAccounts = s.accounts.entries.associate { (k, v) ->
                (if (k == old) u else k) to v
            }
            val newActiveUsers = s.activeUser.entries.associate { (k, v) ->
                (if (k == old) u else k) to v
            }
            val newBooks = s.books.entries.associate { (k, v) ->
                (if (k == old) u else k) to v
            }
            s.copy(
                servers = newServers,
                activeServer = if (s.activeServer == old) u else s.activeServer,
                accounts = newAccounts,
                activeUser = newActiveUsers,
                books = newBooks
            )
        }
    }

    suspend fun selectServer(url: String) {
        commit { it.copy(activeServer = url) }
    }

    /** 退出当前服务器（保留服务器记录，回到服务器选择界面） */
    suspend fun exitCurrentServer() {
        commit { it.copy(activeServer = null) }
        bump()
    }

    suspend fun deleteServer(url: String) {
        commit { s ->
            val remainingServers = s.servers.filterNot { it == url }
            val newActive = if (s.activeServer == url) remainingServers.firstOrNull() else s.activeServer
            s.copy(
                servers = remainingServers,
                activeServer = newActive,
                accounts = s.accounts.filterKeys { it != url },
                activeUser = s.activeUser.filterKeys { it != url },
                books = s.books.filterKeys { it != url }
            )
        }
    }

    // ---------------- 登录 / 账号 ----------------

    /**
     * 登录流程：
     * 1. 校验账号密码 → token
     * 2. 写入记住的账号（token 持久化，之后可免密切换）
     * 3. 拉取账本，自动选择上次/首个账本
     */
    suspend fun login(username: String, password: String) {
        val srv = server ?: throw IllegalStateException("请先添加并选择服务器")
        val uname = username.trim()
        if (uname.isBlank() || password.isBlank())
            throw IllegalArgumentException("请输入账号和密码")

        val api = NetFactory.api(srv)
        val res = api.login(LoginReq(uname, password))

        commit { s ->
            val list = (s.accounts[srv] ?: emptyList()).filterNot { it.username == uname } +
                RawAccount(uname, res.token, res.user)
            s.copy(
                accounts = s.accounts + (srv to list),
                activeUser = s.activeUser + (srv to uname)
            )
        }

        ensureBook()
    }

    /**
     * 确保当前服务器上有可用账本：
     * 优先用上次记住的，否则用第一个，都没有则自动创建。
     * 防止登录中途失败导致「缺少账本ID」。
     */
    suspend fun ensureBook(): Int {
        val srv = server ?: throw IllegalStateException("未选择服务器")
        val books = api().books()
        val remembered = session.books[srv]
        val picked = remembered?.takeIf { id -> books.any { it.id == id } }
            ?: books.firstOrNull()?.id
            ?: api().createBook(BookReq("我的账本")).id
        commit { s -> s.copy(books = s.books + (srv to picked)) }
        bump()
        return picked
    }

    /** 切换到本服务器上已记住的账号（无需重新登录） */
    suspend fun switchAccount(username: String) {
        val srv = server ?: return
        if (session.accounts[srv]?.none { it.username == username } != false)
            throw IllegalArgumentException("未找到该账号")
        commit { s -> s.copy(activeUser = s.activeUser + (srv to username)) }
        // 切换账号后重新确定账本
        ensureBook()
    }

    /** 用后端返回的最新资料刷新当前账号缓存（昵称/颜色变化时同步） */
    suspend fun refreshAccountUser(user: UserDto) {
        val srv = server ?: return
        commit { s ->
            val list = (s.accounts[srv] ?: emptyList()).map { acc ->
                if (acc.username == account()?.username) acc.copy(user = user) else acc
            }
            s.copy(accounts = s.accounts + (srv to list))
        }
    }

    /** 退出当前账号（保留账号记录，便于下次快速登录） */
    suspend fun logout() {
        val srv = server ?: return
        commit { s -> s.copy(activeUser = s.activeUser.filterKeys { it != srv }) }
    }

    suspend fun switchBook(bookId: Int) {
        val srv = server ?: return
        commit { s -> s.copy(books = s.books + (srv to bookId)) }
        bump()
    }

    /** 与后端校验连通并刷新全局数据 */
    suspend fun sync(): Boolean {
        if (server == null) return false
        syncing = true
        return try {
            api().meta()
            bump()
            true
        } catch (e: Exception) {
            false
        } finally {
            syncing = false
        }
    }
}

/** 把网络/服务器异常转换为中文提示 */
fun explainError(e: Throwable): String {
    if (e is HttpException) {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val serverMsg = body?.let {
            runCatching {
                val map = AppJson.decodeFromString(
                    MapSerializer(String.serializer(), String.serializer()),
                    it
                )
                map["error"]
            }.getOrNull()
        }
        return serverMsg ?: "服务器返回异常（HTTP ${e.code()}）"
    }
    return when (e) {
        is UnknownHostException -> "无法连接服务器，请检查服务器地址是否正确"
        is ConnectException -> "无法连接服务器，请确认后端已启动"
        is SocketTimeoutException -> "连接超时，请检查网络或服务器状态"
        else -> e.message ?: "请求失败"
    }
}
