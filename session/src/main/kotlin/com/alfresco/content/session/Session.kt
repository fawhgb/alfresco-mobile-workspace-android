package com.alfresco.content.session

import android.content.Context
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.util.CoilUtils
import com.alfresco.auth.AuthInterceptor
import com.alfresco.content.account.Account
import com.alfresco.content.tools.GeneratedCodeConverters
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

class Session(context: Context, var account: Account) {

    private var authInterceptor: AuthInterceptor
    private var loggingInterceptor: HttpLoggingInterceptor
    private var onSignedOut: (() -> Unit)? = null

    init {
        val appContext = context.applicationContext

        authInterceptor = AuthInterceptor(
            appContext,
            account.id,
            account.authType,
            account.authState,
            account.authConfig
        )

        authInterceptor.setListener(object : AuthInterceptor.Listener {
            override fun onAuthFailure(accountId: String) {
                Log.d("Session", "onAuthFailure")
                if (onSignedOut != null) {
                    onSignedOut?.invoke()
                }
            }

            override fun onAuthStateChange(accountId: String, authState: String) {
                Log.d("Session", "onAuthStateChange")
                Account.update(context, accountId, authState)
                this@Session.account = Account.getAccount(context)!!
            }
        })

        loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE

        val imageLoader = ImageLoader.Builder(context)
            .crossfade(true)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .cache(CoilUtils.createDefaultCache(context))
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    val baseUrl get() = "${account.serverUrl}/api/-default-/public/"

    fun <T> createService(service: Class<T>): T {
        val okHttpClient: OkHttpClient = OkHttpClient()
            .newBuilder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GeneratedCodeConverters.converterFactory())
            .baseUrl(baseUrl)
            .build()
        return retrofit.create(service)
    }

    fun onSignedOut(callback: () -> Unit) {
        onSignedOut = callback
    }

    fun finish() {
        authInterceptor.finish()
    }
}
