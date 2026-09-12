package wishKnish.knishIO.client.storage

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.*
import kotlin.streams.toList
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class FileStorageBackendTest {

  @Test
  fun `creates directory automatically if missing`(@TempDir tempDir: Path) {
    val nestedDir = tempDir.resolve("nested/storage/dir")
    expectThat(Files.exists(nestedDir)).isFalse()

    val backend = FileStorageBackend(nestedDir)
    expectThat(Files.isDirectory(backend.directory)).isTrue()
    expectThat(backend.keys()).isEmpty()
  }

  @Test
  fun `persists, retrieves, and removes items across instances`(@TempDir tempDir: Path) {
    val backend1 = FileStorageBackend(tempDir)

    expectThat(backend1.getItem("testKey")).isNull()
    expectThat(backend1.removeItem("testKey")).isFalse()

    backend1.setItem("testKey", "testValue123")
    expectThat(backend1.getItem("testKey")).isEqualTo("testValue123")

    // Verify persistence with a completely new instance
    val backend2 = FileStorageBackend(tempDir)
    expectThat(backend2.getItem("testKey")).isEqualTo("testValue123")
    expectThat(backend2.keys()).containsExactly("testKey")

    // Removal
    expectThat(backend2.removeItem("testKey")).isTrue()
    expectThat(backend2.getItem("testKey")).isNull()
    expectThat(backend2.keys()).isEmpty()
    expectThat(backend2.removeItem("testKey")).isFalse()
  }

  @Test
  fun `handles special characters in keys like colons and slashes safely`(@TempDir tempDir: Path) {
    val backend = FileStorageBackend(tempDir)

    val secretKey = "knishio:secret:0123456789abcdef"
    val recoveryKey = "knishio:recovery:0123456789abcdef"
    val complexKey = "path/with/slashes:and:colons@123"

    backend.setItem(secretKey, "secret-payload")
    backend.setItem(recoveryKey, "recovery-payload")
    backend.setItem(complexKey, "complex-payload")

    expectThat(backend.getItem(secretKey)).isEqualTo("secret-payload")
    expectThat(backend.getItem(recoveryKey)).isEqualTo("recovery-payload")
    expectThat(backend.getItem(complexKey)).isEqualTo("complex-payload")

    val keys = backend.keys()
    expectThat(keys).contains(secretKey, recoveryKey, complexKey)
    expectThat(keys).hasSize(3)

    expectThat(backend.removeItem(secretKey)).isTrue()
    expectThat(backend.getItem(secretKey)).isNull()
    expectThat(backend.keys()).hasSize(2)
  }

  @Test
  fun `lists keys excluding temporary files`(@TempDir tempDir: Path) {
    val backend = FileStorageBackend(tempDir)

    backend.setItem("alpha", "1")
    backend.setItem("beta", "2")

    // Create dangling temp files simulating interrupted writes
    Files.createFile(tempDir.resolve(".tmp-interrupted1.tmp"))
    Files.createFile(tempDir.resolve(".tmp-interrupted2"))

    val keys = backend.keys()
    expectThat(keys).containsExactlyInAnyOrder("alpha", "beta")
  }

  @Test
  fun `overwrites existing keys atomically`(@TempDir tempDir: Path) {
    val backend = FileStorageBackend(tempDir)

    backend.setItem("mutableKey", "initialValue")
    expectThat(backend.getItem("mutableKey")).isEqualTo("initialValue")

    backend.setItem("mutableKey", "updatedValue")
    expectThat(backend.getItem("mutableKey")).isEqualTo("updatedValue")

    // Ensure only one file exists on disk for this key
    val files = Files.list(tempDir).use { it.toList() }
    expectThat(files).hasSize(1)
  }

  @Test
  fun `sets 0600 POSIX file permissions on supported filesystems`(@TempDir tempDir: Path) {
    val backend = FileStorageBackend(tempDir)
    backend.setItem("permissionTestKey", "protectedPayload")

    val isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
    if (isPosix) {
      val files = Files.list(tempDir).use { it.toList() }
      expectThat(files).hasSize(1)
      val storedFile = files.first()

      val permissions = Files.getPosixFilePermissions(storedFile)
      val permString = PosixFilePermissions.toString(permissions)
      expectThat(permString).isEqualTo("rw-------")
    }
  }

  @Test
  fun `clear removes all keys from storage`(@TempDir tempDir: Path) {
    val backend = FileStorageBackend(tempDir)
    backend.setItem("k1", "v1")
    backend.setItem("k2", "v2")
    backend.setItem("k3", "v3")

    expectThat(backend.keys()).hasSize(3)

    backend.clear()
    expectThat(backend.keys()).isEmpty()
    expectThat(backend.getItem("k1")).isNull()
    expectThat(backend.getItem("k2")).isNull()
    expectThat(backend.getItem("k3")).isNull()
  }

  @Test
  fun `integrates seamlessly with AesGcmSecretStorageProvider`(@TempDir tempDir: Path) {
    val backend = FileStorageBackend(tempDir)
    val passphrase = "test-master-passphrase"
    val provider = AesGcmSecretStorageProvider(backend = backend, defaultPassphrase = passphrase)

    val bundleHash = "bundle-hash-xyz-987"
    val masterSecret = "top-secret-master-seed-value"

    provider.storeSecret(bundleHash, masterSecret, StorageOptions(label = "Primary Key"))

    expectThat(provider.hasSecret(bundleHash)).isTrue()
    expectThat(provider.retrieveSecret(bundleHash)).isEqualTo(masterSecret)

    // Re-open with a new provider pointing to the same file backend directory
    val backendReopened = FileStorageBackend(tempDir)
    val providerReopened = AesGcmSecretStorageProvider(backend = backendReopened, defaultPassphrase = passphrase)

    expectThat(providerReopened.hasSecret(bundleHash)).isTrue()
    expectThat(providerReopened.retrieveSecret(bundleHash)).isEqualTo(masterSecret)

    val secrets = providerReopened.listSecrets()
    expectThat(secrets).hasSize(1)
    expectThat(secrets.first().bundleHash).isEqualTo(bundleHash)
    expectThat(secrets.first().label).isEqualTo("Primary Key")
    expectThat(secrets.first().hardwareBacked).isFalse()

    val unwrapped = providerReopened.withSecret(bundleHash) { it.uppercase() }
    expectThat(unwrapped).isEqualTo(masterSecret.uppercase())

    expectThat(providerReopened.deleteSecret(bundleHash)).isTrue()
    expectThat(providerReopened.hasSecret(bundleHash)).isFalse()
    expectThat(providerReopened.retrieveSecret(bundleHash)).isNull()
  }

  @Test
  fun `stores and lists keys with Windows-prohibited characters without producing illegal filenames`(@TempDir tempDir: Path) {
    val backend = FileStorageBackend(tempDir)
    val prohibitedChars = listOf(':', '/', '\\', '*', '?', '"', '<', '>', '|')
    val testKeys = prohibitedChars.mapIndexed { idx, ch -> "test${ch}key$idx" }

    testKeys.forEachIndexed { idx, key ->
      backend.setItem(key, "payload-$idx")
    }

    // Verify observable round-trip
    testKeys.forEachIndexed { idx, key ->
      expectThat(backend.getItem(key)).isEqualTo("payload-$idx")
    }
    expectThat(backend.keys()).containsExactlyInAnyOrder(testKeys)

    // Verify on-disk files contain no prohibited characters
    val onDiskFilenames = Files.list(tempDir).use { stream ->
      stream.map { it.fileName.toString() }.toList()
    }
    expectThat(onDiskFilenames).hasSize(testKeys.size)
    onDiskFilenames.forEach { filename ->
      prohibitedChars.forEach { ch ->
        expectThat(filename.contains(ch))
          .describedAs("Filename '$filename' should not contain prohibited character '$ch'")
          .isFalse()
      }
    }

    // Verify absent colon-bearing keys return null / false safely without throwing InvalidPathException
    val absentColonKey = "knishio:secret:absentnonexistentkey"
    expectThat(backend.getItem(absentColonKey)).isNull()
    expectThat(backend.removeItem(absentColonKey)).isFalse()
  }

  @Test
  fun `handles dots and traversal-like keys safely without escaping storage directory`(@TempDir tempDir: Path) {
    val storageDir = tempDir.resolve("storage")
    val backend = FileStorageBackend(storageDir)

    // Snapshot parent directory entries before storage operations
    val parentEntriesBefore = Files.list(tempDir).use { it.map { p -> p.fileName.toString() }.toList() }

    val dotKeys = listOf(".", "..", "../x", "x/../y", "abc.", "x.tmp", ".tmp-x")
    dotKeys.forEachIndexed { idx, key ->
      backend.setItem(key, "dot-val-$idx")
    }

    // Verify round-trip via getItem and keys()
    dotKeys.forEachIndexed { idx, key ->
      expectThat(backend.getItem(key)).isEqualTo("dot-val-$idx")
    }
    expectThat(backend.keys()).containsExactlyInAnyOrder(dotKeys)

    // Verify on-disk filenames: must match ^[A-Za-z0-9_+%-]+$ and live directly inside storage directory
    val nameRegex = Regex("^[A-Za-z0-9_+%-]+$")
    val createdFiles = Files.list(storageDir).use { stream ->
      stream.toList()
    }
    expectThat(createdFiles).hasSize(dotKeys.size)
    createdFiles.forEach { filePath ->
      expectThat(filePath.parent).isEqualTo(storageDir)
      val fname = filePath.fileName.toString()
      expectThat(nameRegex.matches(fname))
        .describedAs("On-disk filename '$fname' must only contain safe characters")
        .isTrue()
    }

    // Verify parent directory entries are untouched (no escaped files)
    val parentEntriesAfter = Files.list(tempDir).use { it.map { p -> p.fileName.toString() }.toList() }
    expectThat(parentEntriesAfter).containsExactlyInAnyOrder(parentEntriesBefore)

    // Verify removeItem round-trips
    dotKeys.forEach { key ->
      expectThat(backend.removeItem(key)).isTrue()
      expectThat(backend.getItem(key)).isNull()
    }
    expectThat(backend.keys()).isEmpty()

    // Verify empty key throws IllegalArgumentException on setItem, getItem, removeItem
    expectThrows<IllegalArgumentException> { backend.setItem("", "val") }
    expectThrows<IllegalArgumentException> { backend.getItem("") }
    expectThrows<IllegalArgumentException> { backend.removeItem("") }
  }
}
