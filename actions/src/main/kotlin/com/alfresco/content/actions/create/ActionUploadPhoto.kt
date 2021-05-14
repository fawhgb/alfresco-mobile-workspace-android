package com.alfresco.content.actions.create

import android.content.Context
import android.view.View
import com.alfresco.content.ContentPickerFragment
import com.alfresco.content.actions.Action
import com.alfresco.content.actions.R
import com.alfresco.content.data.Entry
import com.alfresco.content.data.OfflineRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ActionUploadPhoto(
    override var entry: Entry,
    override val icon: Int = R.drawable.ic_action_upload_photo,
    override val title: Int = R.string.action_upload_photo_title
) : Action {

    private val repository = OfflineRepository()

    override suspend fun execute(context: Context): Entry {
        val result = ContentPickerFragment.pickItems(context, MIME_TYPES)
        if (result.count() > 0) {
            withContext(Dispatchers.IO) {
                result.map {
                    repository.scheduleContentForUpload(context, it, entry.id)
                }
            }
        } else {
            throw CancellationException("User Cancellation")
        }
        return entry
    }

    override fun copy(_entry: Entry): Action = copy(entry = _entry)

    override fun showToast(view: View, anchorView: View?) =
        Action.showToast(view, anchorView, R.string.action_upload_photo_toast)

    private companion object {
        val MIME_TYPES = arrayOf("image/*", "video/*")
    }
}
