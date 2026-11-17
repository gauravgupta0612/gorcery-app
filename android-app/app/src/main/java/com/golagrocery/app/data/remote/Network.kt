package com.golagrocery.app.data.remote

import com.golagrocery.app.BuildConfig
import com.golagrocery.app.data.local.Session
import com.golagrocery.app.data.local.SessionStore
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object Network {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    /** Absolute URL for backend-relative paths such as /files/catalog/x.png. */
    fun imageUrl(path: String?): String? = when {
        path.isNullOrBlank() -> null
        path.startsWith("http") -> path
        else -> BuildConfig.API_BASE_URL.trimEnd('/') + path
    }

    fun create(session: SessionStore, onSessionExpired: () -> Unit): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        val bare = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
        val client = bare.newBuilder()
            .addInterceptor(AuthInterceptor(session))
            .authenticator(TokenRefreshAuthenticator(session, bare, onSessionExpired))
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}

private class AuthInterceptor(private val session: SessionStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.session.value?.accessToken
        val req = chain.request()
        if (token == null || req.url.encodedPath.contains("/auth/")) return chain.proceed(req)
        return chain.proceed(req.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

/** On 401, rotates the refresh token once and retries; if that fails the user is logged out. */
private class TokenRefreshAuthenticator(
    private val session: SessionStore,
    private val client: OkHttpClient,
    private val onSessionExpired: () -> Unit,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/auth/")) return null
        if (responseCount(response) >= 2) return null
        synchronized(lock) {
            val current = session.session.value ?: return null
            val sentToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            // Another thread already refreshed - retry with the new token.
            if (sentToken != null && sentToken != current.accessToken) {
                return response.request.newBuilder().header("Authorization", "Bearer ${current.accessToken}").build()
            }
            val body = Network.json.encodeToString(RefreshRequest.serializer(), RefreshRequest(current.refreshToken))
                .toRequestBody("application/json".toMediaType())
            val refreshReq = Request.Builder().url(BuildConfig.API_BASE_URL + "api/v1/auth/refresh").post(body).build()
            val refreshed = runCatching {
                client.newCall(refreshReq).execute().use { r ->
                    if (!r.isSuccessful) null else Network.json.decodeFromString(TokenResponse.serializer(), r.body!!.string())
                }
            }.getOrNull()
            if (refreshed == null) {
                runBlocking { session.clearSession() }
                onSessionExpired()
                return null
            }
            runBlocking { session.saveSession(Session(refreshed.accessToken, refreshed.refreshToken, refreshed.user)) }
            return response.request.newBuilder().header("Authorization", "Bearer ${refreshed.accessToken}").build()
        }
    }

    private fun responseCount(response: Response): Int {
        var r: Response? = response; var n = 1
        while (r?.priorResponse != null) { n++; r = r.priorResponse }
        return n
    }
}
