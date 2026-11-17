package com.golagrocery.app.ui.screens.auth

import androidx.navigation.NavHostController
import com.golagrocery.app.data.remote.UserDto
import com.golagrocery.app.di.AppContainer
import com.golagrocery.app.notifications.PushRegistrar
import com.golagrocery.app.ui.navigation.Routes
import com.golagrocery.app.ui.navigation.goRoot

/** Decides where a signed-in user lands: delivery console, location picker or home. */
suspend fun routeSignedIn(container: AppContainer, nav: NavHostController, user: UserDto) {
    PushRegistrar.register(container.appScope, container.engagement, user.isCustomer)
    when {
        user.isDeliveryPartner && !user.isCustomer -> nav.goRoot(Routes.DELIVERY_HOME)
        container.session.location.value == null -> nav.goRoot(Routes.LOCATION)
        else -> { container.cart.refresh(); nav.goRoot(Routes.HOME) }
    }
}
