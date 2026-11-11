package com.kostku.app

import android.content.Context
import com.kostku.app.data.local.AppDatabase
import com.kostku.app.data.local.DatabaseSeeder
import com.kostku.app.data.local.SessionStore
import com.kostku.app.data.repository.AuthRepository
import com.kostku.app.data.repository.KostRepository

/** Dependency container sederhana (tanpa Hilt) supaya mudah dipahami. */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.build(context)
    val sessionStore = SessionStore(context)
    val seeder = DatabaseSeeder(database)

    val authRepository = AuthRepository(
        adminDao = database.adminDao(),
        tenantDao = database.tenantDao(),
        roomDao = database.roomDao(),
        sessionStore = sessionStore
    )
    val kostRepository = KostRepository(database)
}
