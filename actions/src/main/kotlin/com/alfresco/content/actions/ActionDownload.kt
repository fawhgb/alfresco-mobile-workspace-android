package com.alfresco.content.actions

import android.Manifest
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.alfresco.content.PermissionFragment
import com.alfresco.content.data.BrowseRepository
import com.alfresco.content.data.Entry
import com.alfresco.content.data.OfflineRepository
import java.io.File
import java.io.FileNotFoundException
import kotlin.coroutines.cancellation.CancellationException

data class ActionDownload(
    override var entry: Entry,
    override val icon: Int = R.drawable.ic_download,
    override val title: Int = R.string.action_download_title
) : Action {

    override suspend fun execute(context: Context): Entry {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            PermissionFragment.requestPermissions(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
            if (entry.isSynced) {
                exportFile(context)
            } else {
                enqueueDownload(context)
            }
        } else {
            throw Action.Exception(context.resources.getString(R.string.action_download_failed_permissions))
        }

        return entry
    }

    private fun exportFile(context: Context) {
        val src = OfflineRepository().contentFile(entry)
        val resolver = context.contentResolver

        val filename = entry.title
        val mimeType = DocumentFile.fromFile(src).type

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                @Suppress("DEPRECATION")
                put(
                    MediaStore.MediaColumns.DATA, File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS
                        ), filename
                    ).absolutePath
                )
            }
        }

        val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val dest = resolver.insert(
            target,
            contentValues
        ) ?: throw FileNotFoundException("Could not create destination file.")

        try {
            resolver.openOutputStream(dest).use { output ->
                requireNotNull(output)

                src.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updatedValues = ContentValues().apply {
                    put(MediaStore.Images.ImageColumns.IS_PENDING, 0)
                }
                resolver.update(dest, updatedValues, null, null)
            }
        } catch (ex: Exception) {
            resolver.delete(dest, null, null)
            throw ex
        }
    }

    private fun enqueueDownload(context: Context) {
        val uri = BrowseRepository().contentUri(entry)

        val dm = ContextCompat.getSystemService(context, DownloadManager::class.java)
        val request = DownloadManager.Request(Uri.parse(uri))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setTitle(entry.title)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, entry.title)

        dm?.enqueue(request) ?: throw CancellationException("Missing DownloadManager service.")
    }

    override fun copy(_entry: Entry): Action = copy(entry = _entry)

    override fun showToast(view: View, anchorView: View?) =
        toastMessage.let { Action.showToast(view, anchorView, it) }

    private val toastMessage = if (entry.isSynced) {
        R.string.action_export_toast
    } else {
        R.string.action_download_toast
    }
}
