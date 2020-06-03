package com.alfresco.content.browse

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.alfresco.content.mimetype.MimeType
import com.alfresco.content.models.Node
import kotlinx.android.synthetic.main.view_browse_list_row.view.icon
import kotlinx.android.synthetic.main.view_browse_list_row.view.subtitle
import kotlinx.android.synthetic.main.view_browse_list_row.view.title
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class BrowseListRow @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_browse_list_row, this, true)
    }

    @ModelProp
    fun setNode(node: Node) {
        title.text = node.name
        val type = if (node.isFolder) MimeType.FOLDER else MimeType.fromFilename(node.name)
        icon.setImageDrawable(resources.getDrawable(type.icon, null))
        subtitle.text = "Modified: " + node.modifiedAt.format(
            DateTimeFormatter.ofLocalizedDate(
                FormatStyle.MEDIUM)) + " by " + node.modifiedByUser.displayName
    }

    @CallbackProp
    fun setClickListener(listener: View.OnClickListener?) {
        setOnClickListener(listener)
    }
}
