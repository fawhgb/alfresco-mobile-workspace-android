package com.alfresco.capture

import android.content.Context
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import com.alfresco.content.withFragment
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

class CaptureHelperFragment : Fragment() {
    private lateinit var requestLauncher: ActivityResultLauncher<Unit>
    private var onResult: CancellableContinuation<CaptureItem?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestLauncher = registerForActivityResult(CapturePhotoResultContract()) {
            onResult?.resume(it, null)
        }
    }

    private suspend fun capturePhoto(): CaptureItem? =
        suspendCancellableCoroutine { continuation ->
            onResult = continuation
            requestLauncher.launch(Unit)
        }

    companion object {
        private val TAG = CaptureHelperFragment::class.java.simpleName

        suspend fun capturePhoto(
            context: Context
        ): CaptureItem? =
            withFragment(
                context,
                TAG,
                { it.capturePhoto() },
                { CaptureHelperFragment() }
            )
    }
}