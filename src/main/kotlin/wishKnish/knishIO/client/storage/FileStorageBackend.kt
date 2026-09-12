@file:JvmName("FileStorageBackend")

package wishKnish.knishIO.client.storage

import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import kotlin.streams.toList

/**
 * File-backed storage backend storing individual key-value pairs as files in a directory,
 * with atomic writes and restrictive POSIX file permissions (0600) where supported.
 */
class FileStorageBackend(
  val directory: Path
) : StorageBackend {
  constructor(directoryPath: String) : this(Path.of(directoryPath))

  constructor(directoryFile: File) : this(directoryFile.toPath())

  init {
    Files.createDirectories(directory)
  }

  companion object {
    private const val TEMP_FILE_PREFIX = ".tmp-"
    private const val TEMP_FILE_SUFFIX = ".tmp"
    private val POSIX_0600: Set<PosixFilePermission> = PosixFilePermissions.fromString("rw-------")
  }

  private fun keyToFilename(key: String): String {
    require(key.isNotEmpty()) { "Storage key must not be empty" }
    return URLEncoder.encode(key, StandardCharsets.UTF_8.name())
      .replace("*", "%2A")
      .replace(".", "%2E")
  }

  private fun filenameToKey(filename: String): String {
    return try {
      URLDecoder.decode(filename, StandardCharsets.UTF_8.name())
    } catch (_: IllegalArgumentException) {
      filename
    }
  }

  private fun applyPosixPermissions(path: Path) {
    try {
      Files.setPosixFilePermissions(path, POSIX_0600)
    } catch (_: UnsupportedOperationException) {
      // Non-POSIX filesystem (e.g. Windows)
    } catch (_: ClassCastException) {
      // Attribute view unsupported
    } catch (_: SecurityException) {
      // Security manager restriction
    }
  }

  override fun setItem(key: String, value: String) {
    if (!Files.exists(directory)) {
      Files.createDirectories(directory)
    }

    val targetFile = directory.resolve(keyToFilename(key))
    val tempFile = Files.createTempFile(directory, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)

    try {
      applyPosixPermissions(tempFile)
      Files.writeString(tempFile, value, StandardCharsets.UTF_8)
      applyPosixPermissions(tempFile)

      try {
        Files.move(
          tempFile,
          targetFile,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING
        )
      } catch (_: AtomicMoveNotSupportedException) {
        Files.move(
          tempFile,
          targetFile,
          StandardCopyOption.REPLACE_EXISTING
        )
      }
    } catch (e: Exception) {
      try {
        Files.deleteIfExists(tempFile)
      } catch (_: Exception) {
        // Suppress cleanup exception
      }
      throw e
    }
  }

  override fun getItem(key: String): String? {
    val targetFile = directory.resolve(keyToFilename(key))
    if (!Files.isRegularFile(targetFile)) {
      return null
    }

    return try {
      Files.readString(targetFile, StandardCharsets.UTF_8)
    } catch (_: NoSuchFileException) {
      null
    }
  }

  override fun removeItem(key: String): Boolean {
    val targetFile = directory.resolve(keyToFilename(key))
    return try {
      Files.deleteIfExists(targetFile)
    } catch (_: Exception) {
      false
    }
  }

  override fun keys(): List<String> {
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      return emptyList()
    }

    return Files.list(directory).use { stream ->
      stream
        .filter { path ->
          if (!Files.isRegularFile(path)) return@filter false
          val name = path.fileName.toString()
          !name.startsWith(TEMP_FILE_PREFIX) && !name.endsWith(TEMP_FILE_SUFFIX)
        }
        .map { path ->
          filenameToKey(path.fileName.toString())
        }
        .toList()
    }
  }

  fun clear() {
    keys().forEach { removeItem(it) }
  }
}
