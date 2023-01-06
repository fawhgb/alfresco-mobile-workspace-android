package com.alfresco.content.app.activity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.airbnb.mvrx.InternalMavericksApi
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.withState
import com.alfresco.auth.activity.LoginViewModel
import com.alfresco.auth.ui.observe
import com.alfresco.content.actions.Action
import com.alfresco.content.actions.MoveResultContract
import com.alfresco.content.activityViewModel
import com.alfresco.content.app.R
import com.alfresco.content.app.widget.ActionBarController
import com.alfresco.content.session.SessionManager
import com.alfresco.content.viewer.ViewerActivity
import com.alfresco.content.viewer.ViewerArgs.Companion.ID_KEY
import com.alfresco.content.viewer.ViewerArgs.Companion.KEY_FOLDER
import com.alfresco.content.viewer.ViewerArgs.Companion.MODE_KEY
import com.alfresco.content.viewer.ViewerArgs.Companion.TITLE_KEY
import com.alfresco.download.DownloadMonitor
import com.alfresco.ui.getColorForAttribute
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.ref.WeakReference

/**
 * Marked as MainActivity class
 */
class MainActivity : AppCompatActivity(), MavericksView {

    @OptIn(InternalMavericksApi::class)
    private val viewModel: MainActivityViewModel by activityViewModel()
    private val navController by lazy { findNavController(R.id.nav_host_fragment) }
    private val bottomNav by lazy { findViewById<BottomNavigationView>(R.id.bottom_nav) }
    private var actionBarController: ActionBarController? = null
    private var signedOutDialog = WeakReference<AlertDialog>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        observe(viewModel.navigationMode, ::navigateTo)

        viewModel.handleDataIntent(
            intent.extras?.getString(MODE_KEY, ""),
            intent.extras?.getBoolean(KEY_FOLDER, false) ?: false
        )

        // Check login during creation for faster transition on startup

        if (!resources.getBoolean(R.bool.isTablet)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun navigateTo(mode: MainActivityViewModel.NavigationMode) {
        val data = Triple(
            intent.extras?.getString(ID_KEY, "") ?: "",
            intent.extras?.getString(MODE_KEY, "") ?: "",
            "Preview"
        )

        when (mode) {
            MainActivityViewModel.NavigationMode.FOLDER -> {
                bottomNav.selectedItemId = R.id.nav_browse

                /*navController.navigate(
                    Uri.parse(
                        "alfresco://content/browsemenu/${
                            intent.extras?.getString(
                                MODE_KEY,
                                ""
                            )
                        }/${
                            intent.extras?.getString(
                                ID_KEY,
                                ""
                            )
                        }?title=Preview"
                    )
                )*/
            }
            MainActivityViewModel.NavigationMode.FILE -> navigateToViewer(data)
            MainActivityViewModel.NavigationMode.LOGIN -> navigateToLogin(data)
            MainActivityViewModel.NavigationMode.DEFAULT -> {
                if (viewModel.requiresLogin) {
                    navigateToLogin(data)
                } else {
                    configure()
                }
            }
        }
    }

    private fun navigateToViewer(data: Triple<String, String, String>) {
        startActivity(
            Intent(this, ViewerActivity::class.java)
                .putExtra(ID_KEY, data.first)
                .putExtra(MODE_KEY, data.second)
                .putExtra(TITLE_KEY, data.third)
        )
        finish()
    }

    private fun navigateToLogin(data: Triple<String, String, String>) {
        val i = Intent(this, LoginActivity::class.java)
        intent.extras?.let { i.putExtras(it) }
        startActivity(i)
        finish()
    }

    private fun configure() = withState(viewModel) { state ->
        val graph = navController.navInflater.inflate(R.navigation.nav_bottom)
        graph.setStartDestination(if (state.isOnline) R.id.nav_recents else R.id.nav_offline)
        navController.graph = graph

        val appBarConfiguration = AppBarConfiguration(bottomNav.menu)
        actionBarController = ActionBarController(findViewById(R.id.toolbar))
        actionBarController?.setupActionBar(this, navController, appBarConfiguration)

        bottomNav.setupWithNavController(navController)

        setupActionToasts()
        MoveResultContract.addMoveIntent(Intent(this, MoveActivity::class.java))
        setupDownloadNotifications()
    }

    override fun invalidate() = withState(viewModel) { state ->

        if (state.requiresReLogin) {
            if (state.isOnline) {
                showSignedOutPrompt()
            }
        } else {
            // Only when logged in otherwise triggers re-login prompts
            actionBarController?.setProfileIcon(viewModel.profileIcon)
        }
        if (actionBarController != null)
            actionBarController?.setOnline(state.isOnline)
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp()

    private fun showSignedOutPrompt() {
        val oldDialog = signedOutDialog.get()
        if (oldDialog != null && oldDialog.isShowing) return
        val dialog = MaterialAlertDialogBuilder(this).setTitle(resources.getString(R.string.auth_signed_out_title)).setMessage(resources.getString(R.string.auth_signed_out_subtitle))
            .setNegativeButton(resources.getString(R.string.sign_out_confirmation_negative), null).setPositiveButton(resources.getString(R.string.auth_basic_sign_in_button)) { _, _ ->
                navigateToReLogin()
            }.show()
        signedOutDialog = WeakReference(dialog)
    }

    private fun navigateToReLogin() {
        val i = Intent(this, LoginActivity::class.java)
        val acc = SessionManager.requireSession.account
        i.putExtra(LoginViewModel.EXTRA_IS_EXTENSION, false)
        i.putExtra(LoginViewModel.EXTRA_ENDPOINT, acc.serverUrl)
        i.putExtra(LoginViewModel.EXTRA_AUTH_TYPE, acc.authType)
        i.putExtra(LoginViewModel.EXTRA_AUTH_CONFIG, acc.authConfig)
        i.putExtra(LoginViewModel.EXTRA_AUTH_STATE, acc.authState)
        i.putExtra(ID_KEY, intent.extras?.getString(ID_KEY, ""))
        i.putExtra(MODE_KEY, intent.extras?.getString(MODE_KEY, ""))
        startActivity(i)
    }

    private fun setupActionToasts() = Action.showActionToasts(
        lifecycleScope, findViewById(android.R.id.content), bottomNav
    )

    private fun setupDownloadNotifications() = DownloadMonitor.smallIcon(R.drawable.ic_notification_small).tint(primaryColor(this)).observe(this)

    private fun primaryColor(context: Context) = context.getColorForAttribute(R.attr.colorPrimary)
}
