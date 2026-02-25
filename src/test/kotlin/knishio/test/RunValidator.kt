package knishio.test

/**
 * Simple runner for the UnifiedTestVectorValidator
 * Uses SDK methods exclusively for validation (KISS & YAGNI)
 */
object RunValidator {
    @JvmStatic
    fun main(args: Array<String>) {
        UnifiedTestVectorValidator().run()
    }
}