package com.alfresco.content.data

import android.os.Parcel
import android.os.Parcelable
import com.alfresco.content.models.DeletedNode
import com.alfresco.content.models.Favorite
import com.alfresco.content.models.FavoriteTargetNode
import com.alfresco.content.models.Node
import com.alfresco.content.models.NodeChildAssociation
import com.alfresco.content.models.PathInfo
import com.alfresco.content.models.ResultNode
import com.alfresco.content.models.SharedLink
import com.alfresco.content.models.Site
import com.alfresco.content.models.SiteRole
import com.alfresco.process.models.ContentDataEntry
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import io.objectbox.annotation.Convert
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.converter.PropertyConverter
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant
import java.time.ZonedDateTime

/**
 * Generic data model representing all types of objects.
 *
 * Currently representing the following:
 * * entries marked for offline sync: [isOffline]
 * * entries marked for upload: [isUpload]
 * * children of entries marked for offline sync [hasOfflineStatus] but ![isUpload] and ![isOffline]
 * * network results: ![hasOfflineStatus] which assumes ![isOffline] and ![isUpload]
 */
@Parcelize
@TypeParceler<ZonedDateTime, DateParceler>
@Entity
data class Entry(
    val id: String = "",
    val parentId: String? = null,
    @Convert(converter = BoxEntryTypeConverter::class, dbType = String::class)
    val type: Type = Type.UNKNOWN,
    val name: String = "",
    val path: String? = null,
    val mimeType: String? = null,
    @Convert(converter = PropertiesConverter::class, dbType = String::class)
    val properties: Map<String, String> = mapOf(),
    @Convert(converter = BoxDateConverter::class, dbType = Long::class)
    val modified: ZonedDateTime? = null,
    @Transient
    val isPartial: Boolean = false,
    @Transient
    val isFavorite: Boolean = false,
//    @Transient
    val canDelete: Boolean = false,
    @Transient
    val canCreate: Boolean = false,
    @Transient
    val isTrashed: Boolean = false,
    @Transient
    val otherId: String? = null,
    val isOffline: Boolean = false,
    val isUpload: Boolean = false,
    @Convert(converter = BoxOfflineStatusConverter::class, dbType = String::class)
    val offlineStatus: OfflineStatus = OfflineStatus.UNDEFINED,
    @Id var boxId: Long = 0,
    val isExtension: Boolean = false,
    val totalCount: Int = 0,
    val isTotalEntry: Boolean = false,
    val isDocFile: Boolean = false,
    @Transient
    val parentPaths: MutableList<String> = mutableListOf(),
    /*APS content data*/
    @Convert(converter = BoxDateConverter::class, dbType = Long::class)
    val created: ZonedDateTime? = null,
    @Transient
    val userGroupDetails: UserGroupDetails? = null,
    val isRelatedContent: Boolean? = false,
    val isContentAvailable: Boolean? = false,
    val hasLink: Boolean? = false,
    val simpleType: String? = "",
    val source: String? = "",
    val sourceId: String? = "",
    val previewStatus: String? = "",
    val thumbnailStatus: String? = "",
    @Convert(converter = BoxUploadServerTypeConverter::class, dbType = String::class)
    val uploadServer: UploadServerType = UploadServerType.DEFAULT,
    val isReadOnly: Boolean = false,
    var isSelectedForMultiSelection: Boolean = false,
) : ParentEntry(), Parcelable {

    val isSynced: Boolean
        get() = offlineStatus == OfflineStatus.SYNCED

    val isFile: Boolean
        get() = type == Type.FILE

    val isFolder: Boolean
        get() = type == Type.FOLDER

    val isLink: Boolean
        get() = type == Type.FILE_LINK || type == Type.FOLDER_LINK

    val hasOfflineStatus: Boolean
        get() = offlineStatus != OfflineStatus.UNDEFINED

    val fileName: String
        get() = "$id-$name"

    // TODO: move to repository level
    fun withOfflineStatus(): Entry {
        val offline = OfflineRepository().fetchOfflineEntry(this)
        return if (offline != null) {
            copy(
                boxId = offline.boxId,
                isOffline = offline.isOffline,
            )
        } else {
            this
        }
    }

    fun metadataEquals(other: Entry): Boolean =
        id == other.id &&
            parentId == other.parentId &&
            type == other.type &&
            name == other.name &&
            path == other.path &&
            mimeType == other.mimeType &&
            modified?.toEpochSecond() == other.modified?.toEpochSecond() &&
            isFavorite == other.isFavorite &&
            canDelete == other.canDelete &&
            otherId == other.otherId

    fun copyWithMetadata(other: Entry) =
        copy(
            parentId = other.parentId,
            type = other.type,
            name = other.name,
            path = other.path,
            mimeType = other.mimeType,
            modified = other.modified,
            isFavorite = other.isFavorite,
            canDelete = other.canDelete,
            otherId = other.otherId,
        )

    enum class Type {
        FILE,
        FOLDER,
        SITE,
        FILE_LINK,
        FOLDER_LINK,
        GROUP,
        UNKNOWN,
        ;

        companion object {
            fun from(value: String, isFile: Boolean = false, isFolder: Boolean = false): Type {
                when (value) {
                    "cm:content" -> return FILE
                    "cm:folder" -> return FOLDER
                    "st:sites" -> return FOLDER // Special folder for admins
                    "st:site" -> return SITE
                    "app:filelink" -> return FILE_LINK
                    "app:folderlink" -> return FOLDER_LINK
                }
                if (isFile) return FILE
                if (isFolder) return FOLDER
                return UNKNOWN
            }
        }
    }

    companion object {
        fun with(node: Node): Entry {
            return Entry(
                node.id,
                node.parentId,
                Type.from(node.nodeType, node.isFile, node.isFolder),
                node.name,
                node.path?.formattedString(),
                node.content?.mimeType,
                propertiesCompat(node.properties),
                node.modifiedAt,
                node.isFavorite == null || node.allowableOperations == null,
                node.isFavorite ?: false,
                canDelete(node.allowableOperations),
                canCreate(node.allowableOperations),
            ).withOfflineStatus()
        }

        fun with(result: ResultNode): Entry {
            return Entry(
                result.id,
                result.parentId,
                Type.from(result.nodeType, result.isFile, result.isFolder),
                result.name,
                result.path?.formattedString(),
                result.content?.mimeType,
                propertiesCompat(result.properties),
                result.modifiedAt,
                result.isFavorite == null || result.allowableOperations == null,
                result.isFavorite ?: false,
                canDelete(result.allowableOperations),
                canCreate(result.allowableOperations),
                parentPaths = result.path?.elements?.map { it.id!! }?.toMutableList() ?: mutableListOf(),
            ).withOfflineStatus()
        }

        /**
         * return the Entry obj with isExtension value true
         */
        fun with(result: ResultNode, isExtension: Boolean): Entry {
            return Entry(
                result.id,
                result.parentId,
                Type.from(result.nodeType, result.isFile, result.isFolder),
                result.name,
                result.path?.formattedString(),
                result.content?.mimeType,
                propertiesCompat(result.properties),
                result.modifiedAt,
                result.isFavorite == null || result.allowableOperations == null,
                result.isFavorite ?: false,
                canDelete(result.allowableOperations),
                canCreate(result.allowableOperations),
                isExtension = isExtension,
                parentPaths = result.path?.elements?.map { it.id!! }?.toMutableList() ?: mutableListOf(),
            ).withOfflineStatus()
        }

        fun with(node: NodeChildAssociation): Entry {
            return Entry(
                node.id,
                node.parentId,
                Type.from(node.nodeType, node.isFile, node.isFolder),
                node.name,
                node.path?.formattedString(),
                node.content?.mimeType,
                propertiesCompat(node.properties),
                node.modifiedAt,
                node.isFavorite == null || node.allowableOperations == null,
                node.isFavorite ?: false,
                canDelete(node.allowableOperations),
                canCreate(node.allowableOperations),
                otherId = node.properties?.get("cm:destination") as String?,
            ).withOfflineStatus()
        }

        /**
         * returns the Entry obj with extension value true
         */
        fun with(node: NodeChildAssociation, isExtension: Boolean): Entry {
            return Entry(
                node.id,
                node.parentId,
                Type.from(node.nodeType, node.isFile, node.isFolder),
                node.name,
                node.path?.formattedString(),
                node.content?.mimeType,
                propertiesCompat(node.properties),
                node.modifiedAt,
                node.isFavorite == null || node.allowableOperations == null,
                node.isFavorite ?: false,
                canDelete(node.allowableOperations),
                canCreate(node.allowableOperations),
                otherId = node.properties?.get("cm:destination") as String?,
                isExtension = isExtension,
            ).withOfflineStatus()
        }

        fun with(favorite: Favorite): Entry {
            val map = favorite.target
            if (map.file != null) {
                val file: FavoriteTargetNode = map.file!!
                return Entry(
                    file.id,
                    file.parentId,
                    Type.FILE,
                    file.name,
                    file.path?.formattedString(),
                    file.content?.mimeType,
                    propertiesCompat(file.properties),
                    file.modifiedAt,
                    file.allowableOperations == null,
                    true,
                    canDelete(file.allowableOperations),
                ).withOfflineStatus()
            }
            if (map.folder != null) {
                val folder: FavoriteTargetNode = map.folder!!
                return Entry(
                    folder.id,
                    folder.parentId,
                    Type.FOLDER,
                    folder.name,
                    folder.path?.formattedString(),
                    null,
                    propertiesCompat(folder.properties),
                    folder.modifiedAt,
                    folder.allowableOperations == null,
                    true,
                    canDelete(folder.allowableOperations),
                    canCreate(folder.allowableOperations),
                ).withOfflineStatus()
            }
            if (map.site != null) {
                val site = map.site!!
                return with(site).copy(isPartial = false, isFavorite = true)
            }
            throw IllegalStateException()
        }

        fun with(site: Site): Entry {
            return Entry(
                site.guid,
                null,
                Type.SITE,
                site.title,
                null,
                null,
                isPartial = true,
                canDelete = site.role == Site.RoleEnum.SITEMANAGER,
                otherId = site.id,
            ).withOfflineStatus()
        }

        fun with(role: SiteRole): Entry {
            return Entry(
                role.site.guid,
                null,
                Type.SITE,
                role.site.title,
                null,
                null,
                isPartial = true,
                canDelete = role.role == SiteRole.RoleEnum.SITEMANAGER,
                otherId = role.site.id,
            ).withOfflineStatus()
        }

        fun with(link: SharedLink): Entry {
            return Entry(
                link.nodeId ?: "",
                null,
                Type.FILE,
                link.name ?: "",
                link.path?.formattedString(),
                link.content?.mimeType,
                propertiesCompat(link.properties),
                link.modifiedAt,
                link.isFavorite == null || link.allowableOperations == null,
                link.isFavorite ?: false,
                link.allowableOperations?.contains("delete") ?: false,
            ).withOfflineStatus()
        }

        fun with(node: DeletedNode): Entry {
            return Entry(
                node.id,
                node.parentId,
                Type.from(node.nodeType, node.isFile, node.isFolder),
                node.name,
                node.path?.formattedString(),
                node.content?.mimeType,
                propertiesCompat(node.properties),
                node.modifiedAt,
                isPartial = false,
                node.isFavorite ?: false,
                canDelete = false,
                isTrashed = true,
            ).withOfflineStatus()
        }

        /**
         * return the Entry obj by using the contentEntry obj.
         */
        fun convertContentEntryToEntry(contentEntry: Entry, isDocFile: Boolean, uploadServer: UploadServerType): Entry {
            val id = if (contentEntry.sourceId.isNullOrEmpty()) contentEntry.id else contentEntry.sourceId
            return Entry(
                id = id,
                name = contentEntry.name,
                mimeType = contentEntry.mimeType,
                uploadServer = uploadServer,
                isDocFile = isDocFile,
                source = contentEntry.source,
                sourceId = contentEntry.sourceId,
            )
        }

        /**
         * return the ContentEntry obj after converting the data from ContentDataEntry obj
         */
        fun with(data: ContentDataEntry, parentId: String? = null, uploadServer: UploadServerType): Entry {
            return Entry(
                id = data.id?.toString() ?: "",
                parentId = parentId,
                name = data.name ?: "",
                type = Type.FILE,
                created = data.created,
                userGroupDetails = data.createdBy?.let { UserGroupDetails.with(it) } ?: UserGroupDetails(),
                isRelatedContent = data.relatedContent,
                isContentAvailable = data.contentAvailable,
                mimeType = data.mimeType,
                simpleType = data.simpleType,
                source = data.source,
                sourceId = data.sourceId,
                previewStatus = data.previewStatus,
                thumbnailStatus = data.thumbnailStatus,
                uploadServer = uploadServer,
            )
        }

        /**
         * update entry after downloading content from process services.
         */
        fun updateDownloadEntry(entry: Entry, path: String): Entry {
            return Entry(id = entry.id, name = entry.name, mimeType = entry.mimeType, path = path, uploadServer = entry.uploadServer)
        }

        /**
         * return the default APS content entry obj
         */
        fun defaultAPSEntry(taskId: String?): Entry {
            return Entry(uploadServer = UploadServerType.UPLOAD_TO_TASK, parentId = taskId)
        }

        /**
         * return the default Workflow content entry obj
         */
        fun defaultWorkflowEntry(id: String?): Entry {
            return Entry(uploadServer = UploadServerType.UPLOAD_TO_PROCESS, parentId = id)
        }

        fun withSelectedEntries(entries: List<Entry>): Entry {
            return Entry(
                id = entries.joinToString(separator = ",") { it.id },
                name = entries.joinToString(separator = ",") { it.name },
            )
        }

        private fun PathInfo.formattedString(): String? {
            return elements?.map { it.name }
                ?.reduce { out, el -> "$out \u203A $el" }
        }

        private fun canDelete(operations: List<String>?) =
            operations?.contains("delete") ?: false

        private fun canCreate(operations: List<String>?) =
            operations?.contains("create") ?: false

        private fun propertiesCompat(src: Map<String, Any?>?): MutableMap<String, String> {
            val map = mutableMapOf<String, String>()

            if (src != null) {
                for ((k, v) in src) {
                    when (v) {
                        is String -> {
                            map[k] = v
                        }

                        is List<*> -> {
                            map[k] = v.joinToString { ", " }
                        }

                        else -> {
                            // Ignored
                        }
                    }
                }
            }

            return map
        }

        /**
         * update existing entry with readOnly value.
         */
        fun withTaskForm(entry: Entry): Entry {
            return Entry(
                id = entry.id,
                name = entry.name,
                created = entry.created,
                mimeType = entry.mimeType,
                simpleType = entry.simpleType,
                previewStatus = entry.previewStatus,
                thumbnailStatus = entry.thumbnailStatus,
                isReadOnly = true,
            )
        }
    }
}

enum class OfflineStatus {
    PENDING,
    SYNCING,
    SYNCED,
    ERROR,
    UNDEFINED,
    ;

    fun value() = name.lowercase()
}

/**
 * Marked as UploadServerType enum
 */
enum class UploadServerType {
    DEFAULT,
    UPLOAD_TO_TASK,
    UPLOAD_TO_PROCESS,
    NONE,
    ;

    fun value() = name.lowercase()
}

class BoxOfflineStatusConverter : PropertyConverter<OfflineStatus, String> {
    override fun convertToEntityProperty(databaseValue: String?) =
        try {
            OfflineStatus.valueOf(databaseValue?.uppercase() ?: "")
        } catch (_: Exception) {
            OfflineStatus.UNDEFINED
        }

    override fun convertToDatabaseValue(entityProperty: OfflineStatus?) =
        entityProperty?.value()
}

class BoxEntryTypeConverter : PropertyConverter<Entry.Type, String> {
    override fun convertToEntityProperty(databaseValue: String?) =
        try {
            Entry.Type.valueOf(databaseValue?.uppercase() ?: "")
        } catch (_: Exception) {
            Entry.Type.UNKNOWN
        }

    override fun convertToDatabaseValue(entityProperty: Entry.Type?) =
        entityProperty?.name?.lowercase()
}

/**
 * convert the type to string to save in local DB
 */
class BoxUploadServerTypeConverter : PropertyConverter<UploadServerType, String> {
    override fun convertToEntityProperty(databaseValue: String?) =
        try {
            UploadServerType.valueOf(databaseValue?.uppercase() ?: "")
        } catch (e: Exception) {
            UploadServerType.NONE
        }

    override fun convertToDatabaseValue(entityProperty: UploadServerType?) =
        entityProperty?.value()
}

object DateParceler : Parceler<ZonedDateTime> {
    private val zone = ZonedDateTime.now().zone

    override fun create(parcel: Parcel): ZonedDateTime {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(parcel.readLong()), zone)
    }

    override fun ZonedDateTime.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(this.toInstant().epochSecond)
    }
}

class BoxDateConverter : PropertyConverter<ZonedDateTime, Long> {
    private val zone = ZonedDateTime.now().zone

    override fun convertToEntityProperty(databaseValue: Long?): ZonedDateTime? =
        if (databaseValue != null) {
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(databaseValue), zone)
        } else {
            null
        }

    override fun convertToDatabaseValue(entityProperty: ZonedDateTime?) =
        entityProperty?.toInstant()?.epochSecond
}

class PropertiesConverter : PropertyConverter<Map<String, String>, String> {
    private var moshi = Moshi.Builder().build()
    private var type = Types.newParameterizedType(
        Map::class.java,
        String::class.java,
        String::class.java,
    )
    private var jsonAdapter: JsonAdapter<Map<String, String>> = moshi.adapter(type)

    override fun convertToEntityProperty(databaseValue: String?): Map<String, String> {
        return if (databaseValue == null) {
            mapOf()
        } else {
            jsonAdapter.fromJson(databaseValue) ?: mapOf()
        }
    }

    override fun convertToDatabaseValue(entityProperty: Map<String, String>): String? {
        return jsonAdapter.toJson(entityProperty)
    }
}
