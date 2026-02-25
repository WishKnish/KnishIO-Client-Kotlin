package wishKnish.knishIO.client.libraries

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import strikt.api.*
import strikt.assertions.*

class StringsTest {
    
    @Test
    fun `randomString should generate cryptographically secure random strings`() {
        // Test basic functionality
        val result = Strings.randomString(16)
        expectThat(result) {
            get { length }.isEqualTo(16)
            matches(Regex("^[a-f0-9]{16}$"))
        }
    }
    
    @Test
    fun `randomString should generate unique values`() {
        // Generate multiple random strings and ensure they're unique
        // This tests that we're using SecureRandom properly
        val results = mutableSetOf<String>()
        repeat(1000) {
            results.add(Strings.randomString(32))
        }
        
        // With SecureRandom, we should have 1000 unique values
        expectThat(results).hasSize(1000)
    }
    
    @Test
    fun `randomString should handle custom alphabets`() {
        val customAlphabet = "ABC123"
        val result = Strings.randomString(10, customAlphabet)
        
        expectThat(result) {
            get { length }.isEqualTo(10)
            get { toCharArray().all { char -> customAlphabet.contains(char) } }.isTrue()
        }
    }
    
    @Test
    fun `randomString should validate input parameters`() {
        // Test zero length
        assertThrows<IllegalArgumentException> {
            Strings.randomString(0)
        }.also { exception ->
            expectThat(exception.message).isEqualTo("Length must be positive")
        }
        
        // Test negative length
        assertThrows<IllegalArgumentException> {
            Strings.randomString(-1)
        }.also { exception ->
            expectThat(exception.message).isEqualTo("Length must be positive")
        }
        
        // Test empty alphabet
        assertThrows<IllegalArgumentException> {
            Strings.randomString(10, "")
        }.also { exception ->
            expectThat(exception.message).isEqualTo("Alphabet must not be empty")
        }
    }
    
    @Test
    fun `randomString should work with large lengths`() {
        val result = Strings.randomString(1024)
        expectThat(result.length).isEqualTo(1024)
    }
    
    @Test
    fun `hexToBase64 should correctly convert hex to base64`() {
        // Test with known values
        val hex = "48656c6c6f20576f726c64" // "Hello World" in hex
        val expectedBase64 = "SGVsbG8gV29ybGQ="
        
        val result = Strings.hexToBase64(hex)
        expectThat(result).isEqualTo(expectedBase64)
    }
    
    @Test
    fun `base64ToHex should correctly convert base64 to hex`() {
        val base64 = "SGVsbG8gV29ybGQ="
        val expectedHex = "48656c6c6f20576f726c64"
        
        val result = Strings.base64ToHex(base64)
        expectThat(result).isEqualTo(expectedHex.lowercase())
    }
    
    @Test
    fun `hexToBase64 and base64ToHex should be inverse operations`() {
        val originalHex = "deadbeefcafe1234567890abcdef"
        
        val base64 = Strings.hexToBase64(originalHex)
        val resultHex = Strings.base64ToHex(base64)
        
        expectThat(resultHex).isEqualTo(originalHex)
    }
    
    @Test
    fun `currentTimeMillis should return valid timestamp`() {
        val before = System.currentTimeMillis()
        val result = Strings.currentTimeMillis().toLong()
        val after = System.currentTimeMillis()
        
        expectThat(result) {
            isGreaterThanOrEqualTo(before)
            isLessThanOrEqualTo(after)
        }
    }
    
    @Test
    fun `charsetBaseConvert should convert between number bases`() {
        // Test hex to decimal
        val hexValue = "FF"
        val result = Strings.charsetBaseConvert(hexValue, 16, 10)
        expectThat(result).isEqualTo("255")
        
        // Test decimal to binary
        val decimalValue = "10"
        val binaryResult = Strings.charsetBaseConvert(decimalValue, 10, 2)
        expectThat(binaryResult).isEqualTo("1010")
    }
}