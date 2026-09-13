package wishKnish.knishIO.client.storage.keystore

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.libraries.MlKemBackend
import java.io.File

class MlKemWithoutGraalVmJvmTest {

  @Test
  fun `graalvm polyglot is excluded from classpath and bouncycastle backend is selected`() {
    assertThrows(ClassNotFoundException::class.java) {
      Class.forName("org.graalvm.polyglot.Context")
    }
    assertEquals("bouncycastle", MlKemBackend.instance.name)
  }

  @Test
  fun `mlkem vectors match on bouncycastle backend without graalvm`() {
    val vectorsPath = System.getProperty("knishio.vectors")
      ?: throw IllegalStateException("System property knishio.vectors is not set")
    val jsonString = File(vectorsPath).readText()
    val vectors = Json.parseToJsonElement(jsonString).jsonObject["vectors"]!!.jsonObject

    // ML-KEM-1024 keygen
    val v1024k = vectors["mlkem1024"]!!.jsonObject["keygen"]!!.jsonObject
    val wallet1024 = Wallet(
      secret = v1024k["secret"]!!.jsonPrimitive.content,
      token = v1024k["token"]!!.jsonPrimitive.content,
      position = v1024k["position"]!!.jsonPrimitive.content
    )
    assertEquals(v1024k["expectedPubkey"]!!.jsonPrimitive.content, wallet1024.pubkey)

    // ML-KEM-768 keygen
    val v768k = vectors["mlkem768"]!!.jsonObject["keygen"]!!.jsonObject
    val wallet768 = Wallet(
      secret = v768k["secret"]!!.jsonPrimitive.content,
      token = v768k["token"]!!.jsonPrimitive.content,
      position = v768k["position"]!!.jsonPrimitive.content,
      mlkemParameterSet = 768
    )
    assertEquals(v768k["expectedPubkey"]!!.jsonPrimitive.content, wallet768.pubkey)

    // ML-KEM-1024 decrypt
    val v1024d = vectors["mlkem1024"]!!.jsonObject["decrypt"]!!.jsonObject
    val plaintext1024 = wallet1024.decryptMessage(
      mapOf(
        "cipherText" to v1024d["cipherText"]!!.jsonPrimitive.content,
        "encryptedMessage" to v1024d["encryptedMessage"]!!.jsonPrimitive.content
      )
    )
    assertEquals(v1024d["expectedPlaintext"]!!.jsonPrimitive.content, plaintext1024)

    // ML-KEM-768 decrypt
    val v768d = vectors["mlkem768"]!!.jsonObject["decrypt"]!!.jsonObject
    val plaintext768 = wallet768.decryptMessage(
      mapOf(
        "cipherText" to v768d["cipherText"]!!.jsonPrimitive.content,
        "encryptedMessage" to v768d["encryptedMessage"]!!.jsonPrimitive.content
      )
    )
    assertEquals(v768d["expectedPlaintext"]!!.jsonPrimitive.content, plaintext768)
  }
}
