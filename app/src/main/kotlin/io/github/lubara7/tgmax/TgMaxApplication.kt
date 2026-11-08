package io.github.lubara7.tgmax

import android.app.Application

class TgMaxApplication : Application() {
    val appModule by lazy { AppModule(this) }
}
