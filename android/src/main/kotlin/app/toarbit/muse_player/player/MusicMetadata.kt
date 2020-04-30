package app.toarbit.muse_player.player

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by omit on 2020/4/24 for muse_player.
 */

data class MusicMetadata constructor(val obj: Map<String, Any?>) : Parcelable {

    val mediaId: String get() = obj["mediaId"] as String

    val title: String? get() = obj["title"] as String?

    val subtitle: String? get() = obj["subtitle"] as String

    val duration: Long? get() = (obj["duration"] as Number?)?.toLong()

    val iconUri: String? get() = obj["iconUri"] as String?

    val mediaUri: String? get() = obj["mediaUri"] as String?

    val artist: String? get() = ((obj["extras"] as? Map<String, Any?>)
            ?.get("artist") as? List<Map<String, Any?>>)
            ?.firstOrNull()
            ?.get("name") as String?
    val artistId: Int get() = ((obj["extras"] as? Map<String, Any?>)
            ?.get("artist") as? List<Map<String, Any?>>)
            ?.firstOrNull()
            ?.get("id") as? Int ?: -1

    val albumName: String? get() = ((obj["extras"] as? Map<String, Any?>)
            ?.get("album") as? Map<String, Any?>)
            ?.get("name") as String?
    val albumUrl: String? get() = ((obj["extras"] as? Map<String, Any?>)
            ?.get("album") as? Map<String, Any?>)
            ?.get("coverImageUrl") as String?
    val albumId: Int get() = ((obj["extras"] as? Map<String, Any?>)
            ?.get("album") as? Map<String, Any?>)
            ?.get("id") as? Int ?: -1
    val url: String? get() = (obj["extras"] as? Map<String, Any?>)?.get("url") as String?

    constructor(source: Parcel) : this(
            mutableMapOf<String, Any?>().apply {
                source.readMap(this, MusicMetadata::class.java.classLoader)
            }.toMap()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeMap(obj)
    }

    fun copyWith(duration: Long? = this.duration): MusicMetadata {
        val newObj = obj.toMutableMap()
        newObj["duration"] = duration
        return MusicMetadata(newObj)
    }

    fun toCompact(builder: StringBuilder): StringBuilder {
        builder.append(mediaId).append(',')
                .append(title ?: "").append(',')
                .append(subtitle ?: "").append(',')
                .append(artist ?: "").append(',')
                .append(artistId).append(',')
                .append(iconUri ?: "").append(',')
                .append(mediaUri ?: "").append(',')
                .append(albumName ?: "").append(',')
                .append(albumUrl ?: "").append(',')
                .append(albumId).append(',')
                .append(url ?: "")
                .append('\n')
        return builder
    }

    companion object {

        fun fromMap(obj: Map<String, Any?>): MusicMetadata {
            return MusicMetadata(obj)
        }


        @JvmField
        val CREATOR: Parcelable.Creator<MusicMetadata> =
                object : Parcelable.Creator<MusicMetadata> {
                    override fun createFromParcel(source: Parcel): MusicMetadata = MusicMetadata(source)
                    override fun newArray(size: Int): Array<MusicMetadata?> = arrayOfNulls(size)
                }


        fun fromCompact(compact: String): MusicMetadata? {
            if (compact.isEmpty()) return null
            return compact.split(',').let {
                if (it.size != 11) return null
                fromMap(mapOf(
                        "mediaId" to it[0],
                        "title" to it[1],
                        "subtitle" to it[2],
                        "extras" to mapOf(
                                "title" to it[1],
                                "artist" to listOf(mapOf(
                                        "name" to it[3],
                                        "id" to (it[4].toIntOrNull() ?: -1)
                                )),
                                "album" to mapOf(
                                        "coverImageUrl" to it[8],
                                        "name" to it[7],
                                        "id" to (it[9].toIntOrNull() ?: -1)
                                ),
                                "id" to it[0].toInt(),
                                "title" to it[1],
                                "url" to it[10]
                        ),
                        "iconUri" to it[5],
                        "mediaUri" to it[6]
                ))
            }
        }
    }
}