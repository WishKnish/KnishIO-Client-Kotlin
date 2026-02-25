package wishKnish.knishIO.client

import org.junit.jupiter.api.Test

class PubkeyComparisonTest {
    @Test
    fun comparePubkeyGeneration() {
        // JavaScript test vector from enhanced-js-test-vectors.json
        val jsSecret = "a".repeat(256)
        val jsToken = "USER"
        val jsPosition = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        
        // Expected JavaScript pubkey (from test vector)
        val expectedJsPubkey = "s1IitoeKdWGjbhQUvAatjSc48VGTnxR1aBouSOl2VSe9AiRpSWR4eRG8Iza14FwleoMMSlaWm1BA7FsvwHxjvMoQ6ulLAsE5grK/izN6VQaNAVobtkMyHnsoSxeONfHCESx/ZpFWQHxbPFY2gZjP9ooqlsyKeMQ0CdkW/8pfyPaJjHGS10IwsRao2HYjIGGkG9aXiWdf2emd4PQvfGqg2aAyXcm6j6vLT0ELJ9KyBls8RBuMupBhK0pyRwtYlREw0IYSa1XKXZumW0OqRGJIEgdEg5GpSzwZY6GdWpKG/hHFMfhSWanHqWJfgrGUJitnCPAbMjKqKrq3P7bNMIpWiTNQPSirk4rDWWJP+6MuWmtZ76xlkskTvWwFEzWEA6vA8Qw36vxbIDORG3hW6DFLV0IWyfgTvcEJX8vLMGevH7h9YjpBtfgQADtMrYkCYwMWOdpsQfco+YUY1GfHC1VRUMjJJXrLPzkegFLK3FFJPPoL6NDKXYA416sz3SCoaMUW4jZ+ObWFaunGqLLGddBHCbsOqMyno4FDh3yVbzMgsBVxhHIiD6oqwQw7dawK6USKMDMVjCUiAgqJQgHFL4oDfwZ/v+VCpLcDDTpS2As4IALL4feNeQNOk3Vjm9DKR8iEMsW9oywBuUW8wQIfpiA9ZWUlS/WOj8hcYehlryXIw1DM0brJNSivlQagS+W6F8xZjyKSPDit+cCQv2KaLNicQmtg+NAld3FXkVMhtbmgyCy6r5ifRROBVqlvDRZJYaQ2aTGXC/jOauuSEBcCdvHFUdgCh0EOJQOWSyuNFlMhHiQJSehINtGIP8CnjTeE0JkgAqatDRcyqXoL+SJnFBFrZRgTxbBkoCyTIhgLDiQ9qXIncoJmH6SxcxRKJVOzWjBu70ahkWoJdiMe2MBAH/uWSdBn+fIO5tjDluwHjhSArQaJo8Wgn/B/ZWNlPKHAvvp86Yap5btbufOA+XcBfzsHZSoj5lGHRtWG+XhSCch/2nVjHaMnk9Cyt+Q+b7Ff2CnP3SK4ZthmrvS800NNXJp4B6WTAPlNGyN1LntZhKDGADZQgAGfjwhVhyUA1diywkBJfqKLRVoWZol4AgNNjow4UNuPVaevA0iyMbQ1pYnMACJD7xoX8pFQVlt0UsJxHzwHbNaO5OJVqvg4WLmMPFlX6QBMjqInEVSJG6NS9UlT4ieZ48Ib5Xeh6aYAXaqgzuJ+xykBQtIZy1aYO7liy9FvysgretCpKmEWOnWJwZYbFeaGqSF0DdDKa+ElsjHAsRKQPFeOxIG4ejF76Bmj1LFt6oqX9buGTuFFoBYf0UcmVbmV9dZbHwIwYGFLLTc9IZq1IvQt1plgMrFt5rZH7mQxk9qcCEAHqAYs1ltMmDApuPxEmdNuUDicizfIsLl+FUSXRRKioophQKbE04Z0N+eFcSs3mrRExtlUtjYw8ZO5yhsrX/sw/3RN6pxVoXBtkKgoRLRV4CqI4eUyl8VS18g6CvUYWuivSza9IBACuGFSxudZiKetlCYbH2J5wBQ099aFdcgCoLyIgip5AjjWF06F4x094H/zQ3pe34fkxGOenl0="
        
        // Generate Kotlin wallet
        val kotlinWallet = Wallet(secret = jsSecret, token = jsToken, position = jsPosition)
        
        println("=".repeat(60))
        println("PUBKEY COMPARISON TEST")
        println("=".repeat(60))
        println()
        println("Expected JS pubkey length: ${expectedJsPubkey.length}")
        println("Actual Kotlin pubkey length: ${kotlinWallet.pubkey?.length ?: "null"}")
        println()
        println("Pubkeys match: ${expectedJsPubkey == kotlinWallet.pubkey}")
        println()
        
        // Compare first/last characters to debug differences
        val kotlinPubkey = kotlinWallet.pubkey ?: ""
        if (kotlinPubkey.isNotEmpty()) {
            println("JS first 50 chars: ${expectedJsPubkey.take(50)}")
            println("KT first 50 chars: ${kotlinPubkey.take(50)}")
            println()
            println("JS last 50 chars: ${expectedJsPubkey.takeLast(50)}")
            println("KT last 50 chars: ${kotlinPubkey.takeLast(50)}")
            
            // Check if it's just padding difference
            val jsTrimmed = expectedJsPubkey.trimEnd('=')
            val ktTrimmed = kotlinPubkey.trimEnd('=')
            println()
            println("Without padding - JS: ${jsTrimmed.length}, KT: ${ktTrimmed.length}")
            println("Match without padding: ${jsTrimmed == ktTrimmed}")
            
            // Check byte-level differences if base64 decode is possible
            try {
                val jsBytes = java.util.Base64.getDecoder().decode(expectedJsPubkey)
                val ktBytes = java.util.Base64.getDecoder().decode(kotlinPubkey)
                println()
                println("JS decoded bytes: ${jsBytes.size}")
                println("KT decoded bytes: ${ktBytes.size}")
                
                if (jsBytes.size == ktBytes.size) {
                    var firstDiff = -1
                    for (i in jsBytes.indices) {
                        if (jsBytes[i] != ktBytes[i]) {
                            firstDiff = i
                            break
                        }
                    }
                    if (firstDiff >= 0) {
                        println("First byte difference at index: $firstDiff")
                    } else {
                        println("Bytes are identical!")
                    }
                }
            } catch (e: Exception) {
                println("Error decoding base64: ${e.message}")
            }
        }
        
        println("=".repeat(60))
    }
}