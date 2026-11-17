package com.golagrocery.app

import com.golagrocery.app.data.ApiResult
import com.golagrocery.app.data.remote.LoginRequest
import com.golagrocery.app.data.remote.Network
import com.golagrocery.app.data.remote.TokenResponse
import com.golagrocery.app.data.safeCall
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class SafeCallTest {
    interface TestApi { @POST("api/v1/auth/login") suspend fun login(@Body b: LoginRequest): TokenResponse }

    private lateinit var server: MockWebServer
    private lateinit var api: TestApi

    @Before fun setUp() {
        server = MockWebServer(); server.start()
        api = Retrofit.Builder().baseUrl(server.url("/"))
            .addConverterFactory(Network.json.asConverterFactory("application/json".toMediaType())).build().create(TestApi::class.java)
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun mapsBackendErrorBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"code":"UNAUTHORIZED","message":"Invalid credentials"}"""))
        val r = safeCall { api.login(LoginRequest("9876543210", "x")) }
        assertTrue(r is ApiResult.Err)
        r as ApiResult.Err
        assertEquals("Invalid credentials", r.message)
        assertEquals(401, r.status)
    }

    @Test fun prefersFieldValidationMessage() = runTest {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"code":"VALIDATION_FAILED","message":"Please check","fields":{"phone":"Enter a valid 10-digit Indian mobile number"}}"""))
        val r = safeCall { api.login(LoginRequest("1", "x")) } as ApiResult.Err
        assertEquals("Enter a valid 10-digit Indian mobile number", r.message)
    }

    @Test fun mapsNetworkFailure() = runTest {
        server.shutdown()
        val r = safeCall { api.login(LoginRequest("9876543210", "x")) } as ApiResult.Err
        assertTrue(r.isNetwork)
    }

    @Test fun parsesSuccess() = runTest {
        server.enqueue(MockResponse().setBody("""{"accessToken":"a","refreshToken":"r","expiresIn":3600,"user":{"id":1,"fullName":"Riya","phone":"9876543210","roles":["CUSTOMER"]}}"""))
        val r = safeCall { api.login(LoginRequest("9876543210", "secret123")) } as ApiResult.Ok
        assertTrue(r.data.user.isCustomer)
    }
}
