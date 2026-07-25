package com.srinivaskannan.divyaprabhandham.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The reading state that travels between a person's devices.
 *
 * Deliberately small and deliberately versioned. Everything here is either a
 * position, a collection or a preference — nothing derived, nothing that could
 * be rebuilt from the corpus, and nothing identifying.
 *
 * [updatedAt] drives conflict resolution. Two devices editing between syncs is
 * resolved last-writer-wins on the whole document, which is the same contract
 * the iOS build got from NSUbiquitousKeyValueStore. A per-field merge would
 * lose fewer edits but would also mean an unbookmarked verse could come back
 * from the dead, which is worse than losing a bookmark.
 */
@Serializable
data class SyncPayload(
    val version: Int = CURRENT_VERSION,
    val updatedAt: Long = 0L,
    val bookmarks: List<String> = emptyList(),
    val recentlyViewed: List<String> = emptyList(),
    val pinnedWorks: List<String> = emptyList(),
    val lastReadSectionId: String? = null,
    val lastReadStanzaKey: String? = null,
    val theme: String? = null,
    val fontSize: Float? = null,
    val accent: String? = null,
    val appearance: String? = null,
    val script: String? = null,
    val fontFamily: String? = null,
    val widgetAayiram: String? = null,
    val supporterSince: Long? = null,
    val tipPromptSilenced: Boolean = false,
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * A minimal Google Drive client for the hidden per-app folder.
 *
 * `appDataFolder` is the right home for this: the user's own Drive quota, no
 * server of ours, invisible in the Drive UI, and deleted with the app if they
 * ask Drive to forget it. Only three endpoints are needed, so this talks
 * straight to the REST API over HTTPS rather than pulling in the
 * google-api-client stack for the privilege.
 *
 * Every call takes an OAuth access token; obtaining and refreshing that token
 * is [GoogleSyncManager]'s job.
 */
internal object DriveAppData {

    private const val FILE_NAME = "reading-state.json"
    private const val FILES = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"
    private const val BOUNDARY = "dp-sync-boundary"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class FileList(val files: List<FileRef> = emptyList())

    @Serializable
    private data class FileRef(val id: String)

    /** The id of the state file, or null if this account has never synced. */
    suspend fun findFileId(token: String): String? = withContext(Dispatchers.IO) {
        val query = URLEncoder.encode("name = '$FILE_NAME'", "UTF-8")
        val url = "$FILES?spaces=appDataFolder&q=$query&fields=files(id)&pageSize=1"
        val body = request(url, "GET", token) ?: return@withContext null
        runCatching { json.decodeFromString<FileList>(body).files.firstOrNull()?.id }.getOrNull()
    }

    suspend fun download(token: String, fileId: String): SyncPayload? = withContext(Dispatchers.IO) {
        val body = request("$FILES/$fileId?alt=media", "GET", token) ?: return@withContext null
        runCatching { json.decodeFromString<SyncPayload>(body) }.getOrNull()
    }

    /** Creates the state file and returns its id. */
    suspend fun create(token: String, payload: SyncPayload): String? = withContext(Dispatchers.IO) {
        val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
        val content = json.encodeToString(payload)
        val multipart = buildString {
            append("--$BOUNDARY\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata).append("\r\n")
            append("--$BOUNDARY\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(content).append("\r\n")
            append("--$BOUNDARY--")
        }
        val body = request(
            url = "$UPLOAD?uploadType=multipart&fields=id",
            method = "POST",
            token = token,
            contentType = "multipart/related; boundary=$BOUNDARY",
            payload = multipart,
        ) ?: return@withContext null
        runCatching { json.decodeFromString<FileRef>(body).id }.getOrNull()
    }

    suspend fun update(token: String, fileId: String, payload: SyncPayload): Boolean =
        withContext(Dispatchers.IO) {
            request(
                url = "$UPLOAD/$fileId?uploadType=media",
                method = "PATCH",
                token = token,
                contentType = "application/json; charset=UTF-8",
                payload = json.encodeToString(payload),
            ) != null
        }

    private fun request(
        url: String,
        method: String,
        token: String,
        contentType: String? = null,
        payload: String? = null,
    ): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                // PATCH is not in HttpURLConnection's allow-list on every
                // Android version; POST + an override header is the documented
                // workaround and Drive honours it.
                if (method == "PATCH") {
                    requestMethod = "POST"
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                } else {
                    requestMethod = method
                }
                setRequestProperty("Authorization", "Bearer $token")
                contentType?.let { setRequestProperty("Content-Type", it) }
                connectTimeout = 15_000
                readTimeout = 20_000
                if (payload != null) {
                    doOutput = true
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(payload) }
                }
            }
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            // Sync is best-effort by design: a failure here must never surface
            // as an error in a reading app. The next launch tries again.
            null
        } finally {
            connection?.disconnect()
        }
    }
}
