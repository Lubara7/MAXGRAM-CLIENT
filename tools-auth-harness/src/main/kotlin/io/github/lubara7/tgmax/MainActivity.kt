package io.github.lubara7.tgmax

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import io.github.lubara7.tgmax.ui.TgMaxTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val module = (application as TgMaxApplication).appModule
        setContent { TgMaxTheme { AppNavigation(module.authRepository) } }
    }
}
