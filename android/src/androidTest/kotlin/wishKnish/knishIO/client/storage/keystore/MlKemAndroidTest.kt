package wishKnish.knishIO.client.storage.keystore

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.libraries.MlKemBackend

@RunWith(AndroidJUnit4::class)
class MlKemAndroidTest {

  companion object {
    private lateinit var vectors: JsonObject

    @BeforeClass
    @JvmStatic
    fun loadVectors() {
      val context = InstrumentationRegistry.getInstrumentation().context
      val jsonString = context.assets.open("cross-platform-test-vectors.json").bufferedReader().use {
        it.readText()
      }
      vectors = Json.parseToJsonElement(jsonString).jsonObject["vectors"]!!.jsonObject
    }
  }

  @Test
  fun testGraalVmAbsentAndBouncyCastleBackendSelectedOnArt() {
    assertThrows(ClassNotFoundException::class.java) {
      Class.forName("org.graalvm.polyglot.Context")
    }
    assertEquals("bouncycastle", MlKemBackend.instance.name)
  }

  @Test
  fun testMlkem1024KeygenMatchesFrozenVector() {
    val v1024k = vectors["mlkem1024"]!!.jsonObject["keygen"]!!.jsonObject
    val wallet = Wallet(
      secret = v1024k["secret"]!!.jsonPrimitive.content,
      token = v1024k["token"]!!.jsonPrimitive.content,
      position = v1024k["position"]!!.jsonPrimitive.content
    )
    assertEquals(v1024k["expectedPubkey"]!!.jsonPrimitive.content, wallet.pubkey)
  }

  @Test
  fun testMlkem768KeygenMatchesFrozenVector() {
    val v768k = vectors["mlkem768"]!!.jsonObject["keygen"]!!.jsonObject
    val wallet = Wallet(
      secret = v768k["secret"]!!.jsonPrimitive.content,
      token = v768k["token"]!!.jsonPrimitive.content,
      position = v768k["position"]!!.jsonPrimitive.content,
      mlkemParameterSet = 768
    )
    assertEquals(v768k["expectedPubkey"]!!.jsonPrimitive.content, wallet.pubkey)
  }

  @Test
  fun testMlkem1024DecryptMatchesFrozenVector() {
    val v1024d = vectors["mlkem1024"]!!.jsonObject["decrypt"]!!.jsonObject
    val wallet = Wallet(
      secret = v1024d["secret"]!!.jsonPrimitive.content,
      token = v1024d["token"]!!.jsonPrimitive.content,
      position = v1024d["position"]!!.jsonPrimitive.content
    )
    val plaintext = wallet.decryptMessage(
      mapOf(
        "cipherText" to v1024d["cipherText"]!!.jsonPrimitive.content,
        "encryptedMessage" to v1024d["encryptedMessage"]!!.jsonPrimitive.content
      )
    )
    assertEquals(v1024d["expectedPlaintext"]!!.jsonPrimitive.content, plaintext)
  }

  @Test
  fun testMlkem768DecryptMatchesFrozenVector() {
    val v768d = vectors["mlkem768"]!!.jsonObject["decrypt"]!!.jsonObject
    val wallet = Wallet(
      secret = v768d["secret"]!!.jsonPrimitive.content,
      token = v768d["token"]!!.jsonPrimitive.content,
      position = v768d["position"]!!.jsonPrimitive.content,
      mlkemParameterSet = 768
    )
    val plaintext = wallet.decryptMessage(
      mapOf(
        "cipherText" to v768d["cipherText"]!!.jsonPrimitive.content,
        "encryptedMessage" to v768d["encryptedMessage"]!!.jsonPrimitive.content
      )
    )
    assertEquals(v768d["expectedPlaintext"]!!.jsonPrimitive.content, plaintext)
  }
}
