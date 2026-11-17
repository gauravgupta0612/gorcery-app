package com.golagrocery.app.ui.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val OTP = "otp/{phone}/{purpose}?dev={dev}&to={to}"
    fun otp(phone: String, purpose: String, dev: String?, to: String? = null) =
        "otp/$phone/$purpose?dev=${dev ?: ""}&to=${Uri.encode(to ?: "")}"
    const val LOCATION = "location"
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val PRODUCTS = "products?categoryId={categoryId}&title={title}&q={q}"
    fun products(categoryId: Long? = null, title: String? = null, q: String? = null) =
        "products?categoryId=${categoryId ?: -1}&title=${Uri.encode(title ?: "")}&q=${Uri.encode(q ?: "")}"
    const val SEARCH = "search"
    const val PRODUCT = "product/{id}"
    fun product(id: Long) = "product/$id"
    const val CART = "cart"
    const val COUPONS = "coupons"
    const val CHECKOUT = "checkout"
    const val PAYMENT = "payment/{orderId}?method={method}"
    fun payment(orderId: Long, method: String) = "payment/$orderId?method=$method"
    const val ORDER_PLACED = "placed/{orderId}"
    fun orderPlaced(orderId: Long) = "placed/$orderId"
    const val ORDERS = "orders"
    const val ORDER = "order/{id}"
    fun order(id: Long) = "order/$id"
    const val ADDRESSES = "addresses"
    const val ADDRESS_EDIT = "address?id={id}"
    fun addressEdit(id: Long? = null) = "address?id=${id ?: -1}"
    const val WISHLIST = "wishlist"
    const val ACCOUNT = "account"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val SUPPORT = "support"
    const val REVIEW = "review/{orderId}"
    fun review(orderId: Long) = "review/$orderId"
    const val DELIVERY_HOME = "delivery"
    const val DELIVERY_ASSIGNMENT = "delivery/assignment/{id}"
    fun deliveryAssignment(id: Long) = "delivery/assignment/$id"
    const val DELIVERY_HISTORY = "delivery/history"

    val bottomBarRoutes = setOf(HOME, CATEGORIES, CART, ACCOUNT)
}
