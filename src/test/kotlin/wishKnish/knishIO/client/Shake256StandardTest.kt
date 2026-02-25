package wishKnish.knishIO.client

import org.junit.jupiter.api.Test
import org.bouncycastle.crypto.digests.SHAKEDigest
import org.bouncycastle.util.encoders.Hex

class Shake256StandardTest {
    @Test
    fun testStandardShake256() {
        // Test with BouncyCastle directly
        val shake = SHAKEDigest(256)
        
        // Test empty string, 1 byte
        var output = ByteArray(1)
        shake.update(ByteArray(0), 0, 0)
        shake.doFinal(output, 0, 1)
        println("BC Empty string, 1 byte: " + Hex.toHexString(output))
        
        // Reset and test empty string, 4 bytes
        shake.reset()
        output = ByteArray(4)
        shake.update(ByteArray(0), 0, 0)
        shake.doFinal(output, 0, 4)
        println("BC Empty string, 4 bytes: " + Hex.toHexString(output))
        
        // Reset and test empty string, 32 bytes
        shake.reset()
        output = ByteArray(32)
        shake.update(ByteArray(0), 0, 0)
        shake.doFinal(output, 0, 32)
        println("BC Empty string, 32 bytes: " + Hex.toHexString(output))
        
        // Reset and test 'abc', 32 bytes
        shake.reset()
        output = ByteArray(32)
        val abc = "abc".toByteArray()
        shake.update(abc, 0, abc.size)
        shake.doFinal(output, 0, 32)
        println("BC 'abc', 32 bytes: " + Hex.toHexString(output))
    }
}