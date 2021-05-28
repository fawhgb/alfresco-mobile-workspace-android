package com.alfresco.content.session

import android.content.Context
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.util.CoilUtils
import com.alfresco.auth.AuthInterceptor
import com.alfresco.content.account.Account
import com.alfresco.content.tools.GeneratedCodeConverters
import com.alfresco.kotlin.sha1
import java.io.File
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit

class Session(
    val context: Context,
    var account: Account
) {
    var ticket: String? = null

    private var authInterceptor: AuthInterceptor
    private var loggingInterceptor: HttpLoggingInterceptor? = null
    private var onSignedOut: (() -> Unit)? = null

    init {
        require(context == context.applicationContext)

        authInterceptor = AuthInterceptor(
            context,
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

        if (BuildConfig.DEBUG) {
            loggingInterceptor = HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            }
        }

        val imageLoader = ImageLoader.Builder(context)
            .crossfade(true)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor(authInterceptor)
                    .addOptionalInterceptor(loggingInterceptor)
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
            .addOptionalInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .addConverterFactory(GeneratedCodeConverters.converterFactory())
            .baseUrl(baseUrl)
            .build()
        return retrofit.create(service)
    }

    private fun OkHttpClient.Builder.addOptionalInterceptor(interceptor: Interceptor?) =
        if (interceptor != null) addInterceptor(interceptor) else this

    fun onSignedOut(callback: () -> Unit) {
        onSignedOut = callback
    }

    fun finish() {
        authInterceptor.finish()
    }

    val filesDir: File =
        createIfMissing(File(context.filesDir, account.id.sha1()))

    val cacheDir: File =
        createIfMissing(File(context.cacheDir, account.id.sha1()))

    val captureDir: File =
        createIfMissing(File(filesDir, CAPTURE_DIR))

    val uploadDir: File =
        createIfMissing(File(filesDir, UPLOAD_DIR))

    private fun createIfMissing(dir: File): File {
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    private companion object {
        const val CAPTURE_DIR = "capture"
        const val UPLOAD_DIR = "upload"
    }
}
