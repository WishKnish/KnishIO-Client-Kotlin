package wishKnish.knishIO.client.libraries

object NobleMlKemBackend : MlKemBackend {
  override val name: String = "noble"

  override fun keygenFromSeed(seed: ByteArray, parameterSet: Int): Pair<ByteArray, ByteArray> {
    val keyPair = NobleMLKEMBridge.generateMLKEMKeyPairFromSeed(seed, parameterSet)
    return Pair(keyPair.public.encoded, keyPair.private.encoded)
  }

  override fun encapsulate(publicKey: ByteArray): Pair<ByteArray, ByteArray> {
    return NobleMLKEMBridge.encapsulate(MlKemRawPublicKey(publicKey))
  }

  override fun decapsulate(cipherText: ByteArray, privateKey: ByteArray): ByteArray {
    return NobleMLKEMBridge.decapsulate(cipherText, MlKemRawPrivateKey(privateKey))
  }
}
