# Knish.IO Kotlin SDK Examples

Each file in this directory is a self-contained program with its own `main`, in its own package.
The examples are not part of the SDK's Gradle build and are not published.

## Prerequisites

- JDK 17 or higher and Kotlin 2.3 or higher (see the [main README](../README.md#installation))
- A Knish.IO node to talk to
- Environment variables (all optional):
  - `KNISHIO_NODE_URI` - GraphQL endpoint (default `http://localhost:8000/graphql`)
  - `KNISHIO_SECRET` - the identity's secret (default: a random secret, i.e. a new identity)
  - `KNISHIO_CELL` - cell slug
  - `KNISHIO_RECIPIENT_SECRET` - second identity for `token-operations.kt`
  - `KNISHIO_TOKEN` - a token the identity holds, for `advanced-molecules.kt`

## Running an example

Copy the file into a Kotlin/JVM project that depends on the SDK:

```kotlin
dependencies {
    implementation("io.knish:knishio-client-kotlin:1.6.0")
}
```

and run its main class:

| File | Main class |
|---|---|
| `basic-usage.kt` | `examples.basicusage.BasicUsage` |
| `token-operations.kt` | `examples.tokenoperations.TokenOperations` |
| `metadata-management.kt` | `examples.metadatamanagement.MetadataManagement` |
| `wallet-management.kt` | `examples.walletmanagement.WalletManagement` |
| `advanced-molecules.kt` | `examples.advancedmolecules.AdvancedMolecules` |
| `complete-example.kt` | `examples.complete.CompleteExample` |

## Available Examples

1. **basic-usage.kt**: connect, log in with encrypted transport, read the USER balance, the
   bundle's wallets, bundle metadata and the ContinuID head.
2. **token-operations.kt**: issue a fungible token, transfer and burn it; issue a stackable token
   and transfer, replenish and fuse its units; the recipient claims its shadow wallet.
3. **metadata-management.kt**: write Meta Asset records and query them by type, id, key, value
   and filter.
4. **wallet-management.kt**: how wallets derive from a secret, ML-KEM encryption between two
   wallets, declaring and listing wallets, shadow wallets, the ContinuID head.
5. **advanced-molecules.kt**: build molecules with the `Molecule` builders and by hand, sign,
   verify locally, submit; multi-recipient transfers and buffer deposit/withdrawal.
6. **complete-example.kt**: one session end to end.

## Patterns

Log in: `requestAuthToken` returns the `AuthToken` and throws `UnauthenticatedException` when
the node rejects the login.

```kotlin
val client = KnishIOClient(uris = listOf(URI(nodeUri)))
val authToken = client.requestAuthToken(secret = secret, encrypt = true)
```

Ledger writes return a `ResponseProposeMolecule`:

```kotlin
val response = client.createWallet("FOO")
if (response.success()) {
    println("Accepted: ${response.data()?.molecularHash}")
} else {
    println("Rejected: ${response.reason()}")
}
```

Local checks (balance, token units, molecule verification) throw a subclass of
`wishKnish.knishIO.client.exception.BaseException` before anything is sent.

## Security notes

1. Never hardcode secrets. Keep them in a `SecretStorageProvider` (see the main README,
   "Secret storage").
2. A random secret (`Crypto.generateSecret()`) creates a new identity each run; it is only for
   trying things out.
3. Pass `encrypt = true` to `requestAuthToken` for CipherHash (ML-KEM) encrypted transport.

## License

These examples are part of the Knish.IO Kotlin SDK and are licensed under the GNU General Public
License v3.0 or later (GPL-3.0-or-later); see [LICENSE](../LICENSE).
