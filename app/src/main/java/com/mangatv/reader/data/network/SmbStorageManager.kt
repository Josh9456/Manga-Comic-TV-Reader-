package com.mangatv.reader.data.network

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.EnumSet

data class SmbFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long
)

class SmbStorageManager {

    private val client = SMBClient()
    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null

    suspend fun connect(
        host: String,
        shareName: String,
        username: String = "",
        password: String = "",
        domain: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            disconnect()
            val conn = client.connect(host)
            this@SmbStorageManager.connection = conn

            val authContext = if (username.isBlank()) {
                AuthenticationContext.anonymous()
            } else {
                AuthenticationContext(username, password.toCharArray(), domain)
            }

            val sess = conn.authenticate(authContext)
            this@SmbStorageManager.session = sess

            val share = sess.connectShare(shareName) as DiskShare
            this@SmbStorageManager.diskShare = share
            true
        } catch (e: Exception) {
            e.printStackTrace()
            disconnect()
            false
        }
    }

    suspend fun listFiles(remotePath: String = ""): List<SmbFileItem> = withContext(Dispatchers.IO) {
        val share = diskShare ?: return@withContext emptyList()
        val items = mutableListOf<SmbFileItem>()
        try {
            val list = share.list(remotePath)
            for (f in list) {
                val name = f.fileName
                if (name == "." || name == "..") continue
                val isDir = (f.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                val fullPath = if (remotePath.isBlank()) name else "$remotePath/$name"
                items.add(
                    SmbFileItem(
                        name = name,
                        path = fullPath,
                        isDirectory = isDir,
                        size = f.endOfFile
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        items
    }

    suspend fun downloadToCache(remotePath: String, destFile: File): Boolean = withContext(Dispatchers.IO) {
        val share = diskShare ?: return@withContext false
        try {
            destFile.parentFile?.mkdirs()
            val file = share.openFile(
                remotePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            file.inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            file.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun disconnect() {
        try {
            diskShare?.close()
        } catch (e: Exception) {}
        try {
            session?.close()
        } catch (e: Exception) {}
        try {
            connection?.close()
        } catch (e: Exception) {}
        diskShare = null
        session = null
        connection = null
    }
}
