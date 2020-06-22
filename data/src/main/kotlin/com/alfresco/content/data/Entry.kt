package com.alfresco.content.data

import com.alfresco.content.models.DeletedNode
import com.alfresco.content.models.Favorite
import com.alfresco.content.models.FavoriteTargetNode
import com.alfresco.content.models.Node
import com.alfresco.content.models.NodeChildAssociation
import com.alfresco.content.models.ResultNode
import com.alfresco.content.models.SharedLink
import com.alfresco.content.models.SiteRole

data class Entry(
    val id: String,
    val type: Type,
    val title: String,
    val subtitle: String?
) {
    enum class Type {
        File,
        Folder,
        Site,
        Link,
        Unknown;

        companion object {
            fun from(value: String): Type {
                when (value) {
                    "cm:content" -> return File
                    "cm:folder" -> return Folder
                    "cm:site" -> return Site
                    "cm:link" -> return Link
                }
                return Unknown
            }
        }
    }

    companion object {
        fun with(node: Node): Entry {
            return Entry(
                node.id,
                Type.from(node.nodeType),
                node.name,
                null
            )
        }

        fun with(result: ResultNode): Entry {
            return Entry(
                result.id,
                Type.from(result.nodeType),
                result.name,
                null
            )
        }

        fun with(node: NodeChildAssociation): Entry {
            return Entry(
                node.id,
                Type.from(node.nodeType),
                node.name,
                null
            )
        }

        fun with(favorite: Favorite): Entry {
            val map = favorite.target
            if (map.file != null) {
                val file: FavoriteTargetNode = map.file!!
                return Entry(
                    file.id,
                    Type.File,
                    file.name,
                    file.path?.name
                )
            }
            if (map.folder != null) {
                val folder: FavoriteTargetNode = map.folder!!
                return Entry(
                    folder.id,
                    Type.Folder,
                    folder.name,
                    folder.path?.name
                )
            }
            if (map.site != null) {
                val site = map.site!!
                return Entry(
                    site.id,
                    Type.Site,
                    site.title,
                    null
                )
            }
            throw IllegalStateException()
        }

        fun with(role: SiteRole): Entry {
            return Entry(
                role.site.id,
                Type.Site,
                role.site.title,
                null
            )
        }

        fun with(link: SharedLink): Entry {
            return Entry(
                link.id ?: "",
                Type.Link,
                link.name ?: "",
                null
            )
        }

        fun with(node: DeletedNode): Entry {
            return Entry(
                node.id,
                Type.from(node.nodeType),
                node.name,
                null
            )
        }
    }
}