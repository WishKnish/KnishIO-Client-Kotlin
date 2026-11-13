package wishKnish.knishIO.client.storage

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("Usage: SecretStorageCliKt <seal|open> [args...]")
        System.exit(1)
    }
    when (args[0]) {
        "seal", "seal-recovery" -> {
            if (args.size < 4) {
                System.err.println("Usage: SecretStorageCliKt seal <passphrase> <secret> <bundleHash> [label]")
                System.exit(1)
            }
            val passphrase = args[1]
            val secret = args[2]
            val bundleHash = args[3]
            val label = if (args.size >= 5 && args[4].isNotEmpty()) args[4] else null
            val meta = SecretStorageMetadata(
                bundleHash = bundleHash,
                label = label,
                createdAt = 1700000000000L,
                hardwareBacked = false,
                providerType = "aes-gcm"
            )
            val payload = SecretEnvelope.seal(secret, passphrase, meta)
            println(SecretEnvelope.encode(payload))
        }
        "open" -> {
            if (args.size < 3) {
                System.err.println("Usage: SecretStorageCliKt open <passphrase> <payloadJson>")
                System.exit(1)
            }
            val passphrase = args[1]
            val payloadJson = args[2]
            val payload = SecretEnvelope.decode(payloadJson)
            val bytes = SecretEnvelope.open(payload, passphrase)
            println(String(bytes))
        }
        else -> {
            System.err.println("Unknown command: ${args[0]}")
            System.exit(1)
        }
    }
}
