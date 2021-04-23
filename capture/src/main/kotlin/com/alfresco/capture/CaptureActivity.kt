package com.alfresco.capture

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.alfresco.capture.databinding.ActivityCaptureBinding
import com.alfresco.ui.KeyHandler

class CaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCaptureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configureNav()
    }

    private fun configureNav() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val inflater = navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_capture)
        navController.setGraph(graph, intent.extras)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val fragment =
            supportFragmentManager
                .primaryNavigationFragment
                ?.childFragmentManager
                ?.primaryNavigationFragment
        return if (fragment is KeyHandler && fragment.onKeyDown(keyCode, event)) {
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
}