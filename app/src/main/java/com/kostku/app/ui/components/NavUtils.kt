package com.kostku.app.ui.components

import androidx.navigation.NavHostController

/** Pindah antar tab bottom-bar dengan menyimpan state masing-masing tab. */
fun NavHostController.navigateTab(route: String, homeRoute: String) {
    navigate(route) {
        popUpTo(homeRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
