package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SwimTrackViewModel
import com.example.ui.navigation.SwimTrackApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SwimTrackViewModel = viewModel(
                factory = SwimTrackViewModel.provideFactory(applicationContext)
            )
            SwimTrackApp(viewModel = viewModel)
        }
    }
}
