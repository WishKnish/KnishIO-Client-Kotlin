package wishKnish.knishIO.client.libraries

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.security.SecureRandom

@Tag("mlkem")
class MlKemBackendParityTest {
  private var savedProperty: String? = null

  @BeforeEach
  fun setUp() {
    savedProperty = System.getProperty(MlKemBackend.BACKEND_PROPERTY)
  }

  @AfterEach
  fun tearDown() {
    if (savedProperty != null) {
      System.setProperty(MlKemBackend.BACKEND_PROPERTY, savedProperty!!)
    } else {
      System.clearProperty(MlKemBackend.BACKEND_PROPERTY)
    }
  }

  @Test
  @DisplayName("Parity between Noble and BouncyCastle ML-KEM backends across 768 and 1024")
  fun testNobleAndBouncyCastleParity() {
    val random = SecureRandom()

    for (parameterSet in listOf(768, 1024)) {
      val seed = ByteArray(64)
      random.nextBytes(seed)

      val (nobleEk, nobleDk) = NobleMlKemBackend.keygenFromSeed(seed, parameterSet)
      val (bcEk, bcDk) = BouncyCastleMlKemBackend.keygenFromSeed(seed, parameterSet)

      // 1. Keygen parity
      assertArrayEquals(nobleEk, bcEk, "Public keys (ek) must match for ML-KEM-$parameterSet")
      assertArrayEquals(nobleDk, bcDk, "Private keys (dk) must match for ML-KEM-$parameterSet")

      val expectedEkSize = if (parameterSet == 1024) 1568 else 1184
      val expectedDkSize = if (parameterSet == 1024) 3168 else 2400
      val expectedCtSize = if (parameterSet == 1024) 1568 else 1088

      assertEquals(expectedEkSize, nobleEk.size)
      assertEquals(expectedDkSize, nobleDk.size)

      // 2. Noble encapsulate -> BouncyCastle decapsulate
      val (nobleSs, nobleCt) = NobleMlKemBackend.encapsulate(nobleEk)
      assertEquals(32, nobleSs.size)
      assertEquals(expectedCtSize, nobleCt.size)
      val bcRecoveredSs = BouncyCastleMlKemBackend.decapsulate(nobleCt, bcDk)
      assertArrayEquals(nobleSs, bcRecoveredSs, "BC must decapsulate Noble's ciphertext for ML-KEM-$parameterSet")

      // 3. BouncyCastle encapsulate -> Noble decapsulate
      val (bcSs, bcCt) = BouncyCastleMlKemBackend.encapsulate(bcEk)
      assertEquals(32, bcSs.size)
      assertEquals(expectedCtSize, bcCt.size)
      val nobleRecoveredSs = NobleMlKemBackend.decapsulate(bcCt, nobleDk)
      assertArrayEquals(bcSs, nobleRecoveredSs, "Noble must decapsulate BC's ciphertext for ML-KEM-$parameterSet")
    }
  }

  @Test
  @DisplayName("MlKemBackend.select respects system property and defaults to noble on JVM with GraalVM")
  fun testBackendSelection() {
    System.setProperty(MlKemBackend.BACKEND_PROPERTY, "bouncycastle")
    assertEquals("bouncycastle", MlKemBackend.select().name)

    System.setProperty(MlKemBackend.BACKEND_PROPERTY, "noble")
    assertEquals("noble", MlKemBackend.select().name)

    System.setProperty(MlKemBackend.BACKEND_PROPERTY, "nope")
    val ex = assertThrows(IllegalArgumentException::class.java) {
      MlKemBackend.select()
    }
    assertTrue(ex.message!!.contains("expected 'noble' or 'bouncycastle'"))

    System.clearProperty(MlKemBackend.BACKEND_PROPERTY)
    assertEquals("noble", MlKemBackend.select().name, "Default on classpath with GraalVM must be noble")
  }
}
