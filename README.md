<div style="text-align:center">
  <img src="https://raw.githubusercontent.com/WishKnish/KnishIO-Technical-Whitepaper/master/KnishIO-Logo.png" alt="Knish.IO: Post-Blockchain Platform" />
</div>
<div style="text-align:center">info@wishknish.com | https://wishknish.com</div>

# Knish.IO Kotlin Client SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.knish/knishio-client-kotlin.svg)](https://central.sonatype.com/artifact/io.knish/knishio-client-kotlin)

This is the official Kotlin/Java implementation of the Knish.IO client SDK. Its purpose is to expose class libraries for building and signing Knish.IO Molecules, composing Atoms, generating Wallets, and much more.

## Installation

### Gradle
```kotlin
dependencies {
    implementation("io.knish:knishio-client-kotlin:1.5.2")
    implementation("io.knish:knishio-client-kotlin-android:1.5.2") // Android only: AndroidKeystoreSecretStorageProvider (AAR, minSdk 31)
}
```

### Maven
```xml
<dependency>
  <groupId>io.knish</groupId>
  <artifactId>knishio-client-kotlin</artifactId>
  <version>1.5.2</version>
</dependency>
<!-- Android only: AndroidKeystoreSecretStorageProvider (AAR, minSdk 31) -->
<dependency>
  <groupId>io.knish</groupId>
  <artifactId>knishio-client-kotlin-android</artifactId>
  <version>1.5.2</version>
  <type>aar</type>
</dependency>
```

**Requirements:**
- JDK 17 or higher — the published jar is Java 17 bytecode (class file major version 61)
- Gradle 9.x to build from source (the wrapper pins 9.8.2)
- Kotlin 2.3 or higher to consume 1.5.2 — it is built with Kotlin 2.4.21 and emits
  `@Metadata(mv = [2, 4, 0])`, which Kotlin compilers older than 2.3 cannot read. 0.9.4 and
  earlier emitted `mv = [2, 0]` and only required Kotlin 2.0.

After installation, import the SDK in your project:

```kotlin
import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.data.MetaData
import java.net.URI
```

Cryptography runs on the JVM: SHAKE256, WOTS+ signatures and ML-KEM (FIPS 203) come from
BouncyCastle, AES-GCM from the JDK. This SDK has no native `kcore` backend (the JS, PHP, Python
and Rust SDKs can load one); the opt-in `-Dknishio.mlkem.backend=noble` runs the noble ML-KEM
bundle under GraalJS instead, with byte-identical results.

## Basic Usage

The purpose of the Knish.IO SDK is to expose various ledger functions to new or existing applications.

There are two ways to take advantage of these functions:

1. The easy way: use the `KnishIOClient` wrapper class

2. The granular way: build `Atom` and `Molecule` instances and broadcast GraphQL messages yourself

This document will explain both ways.

## The Easy Way: KnishIOClient Wrapper

1. Include the wrapper class in your application code:
   ```kotlin
   import wishKnish.knishIO.client.KnishIOClient
   ```

2. Instantiate the class with your node URI:
   ```kotlin
   val client = KnishIOClient(
       uris = listOf(URI("http://localhost:8000/graphql"))
   )
   client.setCellSlug("my-cell-slug")
   ```

3. Request an authorization token from the node. `requestAuthToken` returns the `AuthToken` and
   throws `UnauthenticatedException` when the node rejects the login. `encrypt = true` asks for
   CipherHash (ML-KEM) encrypted transport; the client follows the flag given here after login:
   ```kotlin
   val authToken = client.requestAuthToken(secret = secret, encrypt = true)
   println("Authenticated: ${authToken.getToken()}, expired: ${authToken.isExpired()}")
   ```

   (**Note:** The `secret` parameter can be a salted combination of username + password, a biometric hash, an existing user identifier from an external authentication process, for example. Called with no secret, `requestAuthToken()` uses the secret held in the client's `SecretStorageProvider`, if one is set.)

4. Begin using `client` to trigger commands described below...

### KnishIOClient Methods

- Query metadata for a **Wallet Bundle**. Omit the `bundle` parameter to query your own Wallet Bundle:
  ```kotlin
  val bundles = client.queryBundle(
      bundle = "c47e20f99df190e418f0cc5ddfa2791e9ccc4eb297cfa21bd317dc0f98313b1d"
  )
  bundles.forEach { (bundleHash, walletBundle) -> println("$bundleHash: ${walletBundle.metas}") }
  ```

- Query metadata for a **Meta Asset**:

  ```kotlin
  val result = client.queryMeta(
      metaType = "Vehicle",
      metaIds = listOf("vehicle-123"), // Optional: Specific IDs
      keys = listOf("LicensePlate"), // Optional: Specific keys
      values = listOf("1H17P"), // Optional: Search by value
      latest = true // Optional: Limit meta values to latest per key
  )

  println(result) // Raw Metadata
  ```

- Writing new metadata for a **Meta Asset**:

  ```kotlin
  val metadata = mutableListOf(
      MetaData("type", "fire"),
      MetaData("weaknesses", listOf("rock", "water", "electric").toString()),
      MetaData("immunities", listOf("ground").toString()),
      MetaData("hp", "78"),
      MetaData("attack", "84")
  )

  val response = client.createMeta(
      metaType = "Pokemon",
      metaId = "Charizard",
      meta = metadata
  )

  if (response.success()) {
      println("Metadata created successfully!")
  } else {
      println("Rejected: ${response.reason()}")
  }
  ```

- Query Wallets associated with a Wallet Bundle (returns a list of `Wallet`):

  ```kotlin
  val wallets = client.queryWallets(
      bundle = "c47e20f99df190e418f0cc5ddfa2791e9ccc4eb297cfa21bd317dc0f98313b1d",
      token = "FOO" // Optional: Filter by token
  )

  wallets.forEach { println("${it.token}: ${it.balance}") }
  ```

- Declaring new **Wallets**:

  (**Note:** If Tokens are sent to undeclared Wallets, **Shadow Wallets** will be used (placeholder
  Wallets that can receive, but cannot send) to store tokens until they are claimed.)

  ```kotlin
  val response = client.createWallet("FOO") // Token Slug for the wallet we are declaring

  if (response.success()) {
      println("Wallet created successfully!")
  }
  ```

- Issuing new **Tokens**:

  ```kotlin
  val tokenMeta = mutableListOf(
      MetaData("name", "CrazyCoin"), // Public name for the token
      MetaData("fungibility", "fungible"), // Fungibility style (fungible / nonfungible / stackable)
      MetaData("supply", "limited"), // Supply style (limited / replenishable)
      MetaData("decimals", "2") // Decimal places
  )

  val response = client.createToken(
      token = "CRZY", // Token slug (ticker symbol)
      amount = 100000000, // Initial amount to issue
      meta = tokenMeta,
      batchId = null // Optional: for stackable tokens
  )

  if (response.success()) {
      println("Token created successfully!")
  }
  ```

- Transferring **Tokens** to other users:

  ```kotlin
  val recipientWallet = Wallet(recipientSecret, "CRZY")
  val response = client.transferToken(
      recipient = recipientWallet, // Or a recipient bundle hash
      token = "CRZY", // Token slug
      amount = 100
  )

  if (response.success()) {
      println("Token transferred successfully!")
  }
  ```

- Creating a new **Identifier**:

  ```kotlin
  val response = client.createIdentifier(
      type = "email",
      contact = "user@example.com",
      code = "verification-code-123"
  )

  if (response.success()) {
      println("Identifier created successfully!")
  }
  ```

- Claiming a **Shadow Wallet**:

  ```kotlin
  val response = client.claimShadowWallet(
      token = "MTK",
      batchId = null // Optional: the shadow wallet to claim; the first one listed when null
  )

  if (response.success()) {
      println("Shadow wallet claimed successfully!")
  }
  ```

- Query **ContinuID** information:

  ```kotlin
  val response = client.queryContinuId(client.bundle())

  if (response.success()) {
      val continuIdWallet = response.payload()
      println("ContinuID position: ${continuIdWallet?.position}")
  }
  ```

- Query **Batch** operations:

  ```kotlin
  val batchResponse = client.queryBatch("batch-id-123")
  val historyResponse = client.queryBatchHistory("batch-id-123")

  println(batchResponse.data())
  println(historyResponse.data())
  ```

- Request **Tokens** from the node:

  ```kotlin
  val response = client.requestTokens(
      token = "DEMO",
      to = client.bundle(), // A bundle hash, a recipient secret, or a Wallet
      amount = 1000
  )

  if (response.success()) {
      println("Tokens requested successfully!")
  }
  ```

- Getting the client's bundle hash:

  ```kotlin
  val bundle = client.bundle()
  println("Client bundle: $bundle")
  ```

  Further operations: `transferTokens` (several recipients in one molecule), `burnTokens`,
  `replenishToken`, `fuseToken`, `depositBufferToken`, `withdrawBufferToken`, `queryBalance`,
  `queryShadowWallets`, `queryMetaInstance`, `queryActiveSession`.

### Not implemented

These parts of the JS reference SDK have no Kotlin equivalent yet:

- Client methods: `registerPeer`, `appendRequest`, `linkIdentifier`, `createPolicy`, `queryPolicy`,
  `createRule` (build the molecule with `Molecule.createRule` instead), `queryAtom`,
  `queryMetaVerified`, `queryEmbeddingStatus`, `activeSession`, `claimShadowWallets`,
  `cancelQuery`, the `subscribe*` methods, `getFingerprint` and `requestGuestAuthToken`
  (`requestAuthToken()` with no secret and no secret storage requests a guest token).
- Queries `QueryAtom`, `QueryEmbeddingStatus`, `QueryMetaTypeViaAtom`, `QueryMetaTypeViaMolecule`,
  `QueryPolicy`, `QueryToken`; mutations `MutationActiveSession`, `MutationAppendRequest`,
  `MutationCreateRule`, `MutationLinkIdentifier`, `MutationPeering`; all WebSocket
  subscriptions.
- Classes `AtomMeta`, `Meta`, `PolicyMeta`, `Rule`/`Condition`/`Callback` (rule and policy JSON is
  built by `libraries/RuleMeta`), `Dot`, `Decimal`; exceptions `AuthorizationRejectedException`,
  `WalletCredentialException`, `DecryptionKeyException`.
- An expired `AuthToken` is not renewed automatically; call `requestAuthToken` again.

## Advanced Usage: Working with Molecules

For more granular control, you can work directly with Molecules:

- Create a new Molecule:
  ```kotlin
  import wishKnish.knishIO.client.Molecule

  val molecule = Molecule(
      secret = secret,
      sourceWallet = sourceWallet,
      cellSlug = cellSlug
  )
  ```

- Sign and check a Molecule:
  ```kotlin
  molecule.sign()
  if (molecule.check()) {
      println("Molecule validation passed!")
  } else {
      println("Molecule validation failed!")
  }
  ```

- Send a signed Molecule as it is (the client's own operations check it before sending):
  ```kotlin
  import wishKnish.knishIO.client.data.json.variables.MoleculeMutationVariable
  import wishKnish.knishIO.client.mutation.MutationProposeMolecule
  import wishKnish.knishIO.client.response.ResponseProposeMolecule

  val mutation = client.createMoleculeMutation(MutationProposeMolecule::class, molecule)
  val response = mutation.execute(MoleculeMutationVariable(molecule)) as ResponseProposeMolecule

  if (response.success()) {
      println("Molecule accepted!")
  }
  ```

## The Hard Way: DIY Everything

This method involves individually building Atoms and Molecules, triggering the signature and validation processes, and communicating the resulting signed Molecule mutation or Query to a Knish.IO node via GraphQL.

1. Include the relevant classes in your application code:
    ```kotlin
    import wishKnish.knishIO.client.Molecule
    import wishKnish.knishIO.client.Wallet
    import wishKnish.knishIO.client.Atom
    import wishKnish.knishIO.client.libraries.Crypto
    ```

2. Generate a 2048-symbol hexadecimal secret, either randomly, or via hashing login + password + salt, OAuth secret ID, biometric ID, or any other static value (`Crypto.generateSecret(seed)`).

3. (optional) Initialize a signing wallet with:
   ```kotlin
   val sourceWallet = Wallet(
       secret = secret,
       token = tokenSlug,
       position = customPosition // (optional) a specific wallet instead of a random position
   )
   ```

   **WARNING 1:** If ContinuID is enabled on the node, you will need to use a specific wallet, and therefore will first need to query the node to retrieve the `position` for that wallet.

   **WARNING 2:** The Knish.IO protocol mandates that all C, I, M and T atoms be signed with a `USER` token wallet; `Molecule.check()` rejects any other token.

4. Build your molecule with:
   ```kotlin
   val molecule = Molecule(
       secret = secret,
       sourceWallet = sourceWallet, // wallet for signing
       cellSlug = cellSlug // (optional) used to point a transaction to a specific branch of the ledger
   )
   ```

5. Either use one of the shortcut methods provided by the `Molecule` class (which will build `Atom` instances for you), or create `Atom` instances yourself.

   DIY example:
    ```kotlin
    // This example records a new Wallet on the ledger

    // Define metadata for our new wallet
    val newWalletMeta = mutableListOf(
        MetaData("walletTokenSlug", newWallet.token),
        MetaData("walletBundleHash", newWallet.bundle),
        MetaData("walletAddress", newWallet.address),
        MetaData("walletPosition", newWallet.position),
        MetaData("walletPubkey", newWallet.pubkey),
        MetaData("walletCharacters", newWallet.characters)
    )

    // Build the C isotope atom
    val walletCreationAtom = Atom(
        position = sourceWallet.position!!,
        walletAddress = sourceWallet.address!!,
        isotope = 'C',
        token = sourceWallet.token,
        metaType = "wallet",
        metaId = newWallet.address,
        meta = newWalletMeta,
        index = molecule.generateIndex()
    )

    // Add the atom to our molecule
    molecule.addAtom(walletCreationAtom)

    // Adding the ContinuID atom (a USER wallet that signs the next molecule)
    molecule.addUserRemainderAtom()
    ```

   Molecule shortcut method example:
    ```kotlin
    // This example commits metadata to some Meta Asset

    // Defining our metadata
    val metadata = mutableListOf(
        MetaData("foo", "Foo"),
        MetaData("bar", "Bar")
    )

    molecule.initMeta(
        meta = metadata,
        metaType = "MyMetaType",
        metaId = "MetaId123"
    )
    ```

   Rules use the JS rule form (a list of `{condition, callback}` objects):
    ```kotlin
    molecule.createRule(
        metaType = "MyMetaType",
        metaId = "MetaId123",
        rule = Json.parseToJsonElement(
            """[{"condition":[{"key":"status","value":"locked","comparison":"=="}],"callback":[{"action":"reject"}]}]"""
        ).jsonArray
    )
    ```

6. Sign the molecule with the stored user secret:
    ```kotlin
    molecule.sign()
    ```

7. Make sure everything checks out by verifying the molecule. `check()` returns `false` on any
   failure; `Molecule.verify()` throws the specific exception:
    ```kotlin
    // For a V isotope transaction, pass the source wallet so its balance is checked too
    if (molecule.check(sourceWallet)) {
        println("Molecule validation passed!")
    }

    try {
        Molecule.verify(molecule, sourceWallet)
    } catch (e: Exception) {
        println("Molecule check failed: ${e.message}")
    }
    ```

8. Broadcast the molecule to a Knish.IO node:
    ```kotlin
    val mutation = client.createMoleculeMutation(MutationProposeMolecule::class, molecule)
    val response = mutation.execute(MoleculeMutationVariable(molecule)) as ResponseProposeMolecule
    ```

9. Inspect the response...
    ```kotlin
    // Was the molecule accepted by the ledger?
    println(if (response.success()) "Accepted" else "Rejected")

    // The reason for a rejection
    println(response.reason())

    // The molecule as the node recorded it
    println(response.data()?.molecularHash)
    ```

   Payloads are provided by responses to the following queries:
    1. `queryBalance(...).payload()` and `queryContinuId(...).payload()` -> a `Wallet`
    2. `queryWallets` -> a list of `Wallet` instances
    3. `createToken`, `transferToken`, `createWallet`, `createMeta`, and other mutations -> a `ResponseProposeMolecule`

## Network Freshness

The GraphQL transport (Ktor) issues a fresh network request per query — there is
**no response caching** — so a long-lived client never serves a stale read of
ledger state. No fresh-read knob (e.g. a request policy) is required.

## Secret storage

Master secrets are stored at rest in the cross-SDK AES-256-GCM envelope (PBKDF2-HMAC-SHA256 ×100000, camelCase metadata; frozen in `sdks/shared-test-results/cross-platform-test-vectors.json`). Every SDK decrypts every other SDK's envelope.

| Provider | `providerType` | Custody (`hardwareBacked`) | Where the key lives | Recovery passphrase |
|---|---|---|---|---|
| `MemorySecretStorageProvider` | `memory` | Software (`false`) | Process memory | Optional |
| `AesGcmSecretStorageProvider` | `aes-gcm` | Software (`false`) | Any `StorageBackend` (`MemoryStorageBackend`, `FileStorageBackend`) | Optional |
| `AndroidKeystoreSecretStorageProvider` | `android-keystore-tee` \| `android-keystore-strongbox` | Hardware (`true`) | AndroidKeyStore KEK (derived from `KeyInfo.securityLevel`) | Required unless `allowUnrecoverable` |

`hardwareBacked` is derived by the provider from the platform, never accepted from the caller; software providers always report `false`. A hardware provider refuses to store without a recovery passphrase unless `allowUnrecoverable` is set — the recovery envelope (`knishio:recovery:<bundleHash>`, `providerType: "aes-gcm"`) is *software* custody whose strength is bounded by that passphrase: enforce passphrase entropy or keep it on a second device.

`AndroidKeystoreSecretStorageProvider` ships in the separate AAR `io.knish:knishio-client-kotlin-android` (minSdk 31), which depends on the core artifact with GraalVM excluded. ML-KEM runs on the BouncyCastle backend (`MlKemBackend`) on the JVM and on Android; on the JVM, `-Dknishio.mlkem.backend=noble` selects the GraalJS-evaluated noble backend instead. Both backends' keygen, encapsulation and decapsulation are byte-identical and are asserted against the frozen `mlkem768`/`mlkem1024` vectors.

Android backup note: place the `FileStorageBackend` directory under `context.noBackupFilesDir` and exclude it in `dataExtractionRules` — a restored blob is undecryptable without the on-device Keystore KEK, so this is hygiene, not a security control.

## Getting Help

Knish.IO is under active development, and our team is ready to assist with integration questions. The best way to seek help is to stop by our [Telegram Support Channel](https://t.me/wishknish). You can also [send us a contact request](https://knish.io/contact) via our website.

## License

The Knish.IO Kotlin Client SDK is licensed under the GNU General Public License v3.0 or later
(GPL-3.0-or-later); see [LICENSE](LICENSE).
