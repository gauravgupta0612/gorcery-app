package com.golagrocery.app

import com.golagrocery.app.data.remote.HomeDto
import com.golagrocery.app.data.remote.Network
import com.golagrocery.app.data.remote.OrderDetailDto
import com.golagrocery.app.data.remote.Quote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifies the app tolerates the backend's JSON (nulls omitted, unknown fields, BigDecimal numbers). */
class DtoParsingTest {
    @Test
    fun parsesCartQuoteWithOmittedNulls() {
        val json = """{"lines":[{"variantId":5,"productId":2,"productName":"Shimla Apple","variantLabel":"1 kg","unitMrp":220.00,"unitPrice":179.00,
            "quantity":2,"lineTotal":358.00,"lineMrp":440.00,"gstRate":0,"tax":0.00,"availableQty":10,"maxPerOrder":10}],
            "itemTotal":358.00,"mrpTotal":440.00,"productSavings":82.00,"couponValid":false,"couponDiscount":0,"deliveryFee":0.00,
            "handlingFee":4.00,"taxTotal":0.00,"grandTotal":362.00,"minOrderAmount":99.00,"meetsMinimum":true,"freeDeliveryAbove":299.00,
            "amountForFreeDelivery":0.00,"hasProblems":false,"someFutureField":"x"}"""
        val q = Network.json.decodeFromString(Quote.serializer(), json)
        assertEquals(2, q.itemCount)
        assertEquals(362.0, q.grandTotal, 0.001)
        assertNull(q.couponCode)
        assertNull(q.lines.first().problem)
    }

    @Test
    fun parsesOrderDetailMinimal() {
        val json = """{"id":1,"orderNumber":"GG100001","status":"PLACED","paymentMethod":"COD","paymentStatus":"COD_PENDING",
            "address":{"name":"Riya","phone":"9876543210","line1":"B-12","city":"Noida","state":"UP","pincode":"201301"},
            "itemTotal":358,"grandTotal":362,"placedAt":"2026-11-17T06:00:00Z","deliveryOtp":"4821","canCancel":true}"""
        val o = Network.json.decodeFromString(OrderDetailDto.serializer(), json)
        assertEquals("4821", o.deliveryOtp)
        assertTrue(o.canCancel)
        assertTrue(o.items.isEmpty())
        assertEquals("B-12, Noida, UP - 201301", o.address.oneLine())
    }

    @Test
    fun parsesEmptyHome() {
        val h = Network.json.decodeFromString(HomeDto.serializer(), "{}")
        assertTrue(h.banners.isEmpty() && h.sections.isEmpty())
    }

    @Test
    fun resolvesRelativeImageUrls() {
        assertEquals(BuildConfig.API_BASE_URL.trimEnd('/') + "/files/catalog/x.png", Network.imageUrl("/files/catalog/x.png"))
        assertEquals("https://cdn.example.com/a.png", Network.imageUrl("https://cdn.example.com/a.png"))
        assertNull(Network.imageUrl(""))
    }
}
