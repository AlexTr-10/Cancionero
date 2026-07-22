package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DropshipNavigationWrapper
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DropshipViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DropshipViewModel = viewModel()
            // Use lightTheme (darkTheme = false) for the Mercado Libre white/yellow/blue professional branding
            MyApplicationTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DropshipNavigationWrapper(viewModel)
                }
            }
        }
    }
}
