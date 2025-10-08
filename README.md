<div style="text-align:center">
  <img src="https://raw.githubusercontent.com/WishKnish/KnishIO-Technical-Whitepaper/master/KnishIO-Logo.png" alt="Knish.IO: Post-Blockchain Platform" />
</div>
<div style="text-align:center">info@wishknish.com | https://wishknish.com</div>

# Knish.IO Kotlin Client SDK

[![CI/CD Pipeline](https://github.com/WishKnish/KnishIO-Client-Kotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/WishKnish/KnishIO-Client-Kotlin/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Maven Central](https://img.shields.io/maven-central/v/io.knish/knishio-client-kotlin.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.knish/knishio-client-kotlin)
[![codecov](https://codecov.io/gh/WishKnish/KnishIO-Client-Kotlin/branch/main/graph/badge.svg)](https://codecov.io/gh/WishKnish/KnishIO-Client-Kotlin)

This is the official Kotlin/Java implementation of the Knish.IO client SDK. Its purpose is to expose class libraries for building and signing Knish.IO Molecules, composing Atoms, generating Wallets, and much more.

## Installation

### Gradle
```kotlin
dependencies {
    implementation("io.knish:knishio-client-kotlin:1.0.0-RC1")
}
```

### Maven
```xml
<dependency>
  <groupId>io.knish</groupId>
  <artifactId>knishio-client-kotlin</artifactId>
  <version>1.0.0-RC1</version>
</dependency>
```

**Requirements:**
- JDK 8 or higher
- Gradle 6.0 or higher (for building from source)
- Kotlin 1.5.10 or higher

<details>
<summary>Alternative Installation Methods</summary>

### GitHub Packages
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/WishKnish/KnishIO-Client-Kotlin")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}
```

### JitPack
[![](https://jitpack.io/v/WishKnish/KnishIO-Client-Kotlin.svg)](https://jitpack.io/#WishKnish/KnishIO-Client-Kotlin)
```kotlin
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation 'com.github.WishKnish:KnishIO-Client-Kotlin:1.0.0-RC1'
}
```
</details>

After installation, import the SDK in your project:

```kotlin
import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.Wallet
import wishKnish.knishIO.client.Molecule
import wishKnish.knishIO.client.data.MetaData
import java.net.URI
```

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
       uris = listOf(URI("http://localhost:8000/graphql")),
       encrypt = true  // Optional: Enable encryption
   )
   client.setCellSlug("my-cell-slug")
   ```

3. Request authorization token from the node:
   ```kotlin
   val response = client.requestAuthToken(secret)
   
   if (response.success()) {
       // Authentication successful
       val authData = response.payload()
       println("Authenticated: ${authData?.token}")
   } else {
       throw Exception("Authentication failed: ${response.reason()}")
   }
   ```

   (**Note:** The `secret` parameter can be a salted combination of username + password, a biometric hash, an existing user identifier from an external authentication process, for example)

4. Begin using `client` to trigger commands described below...

### KnishIOClient Methods

- Query metadata for a **Wallet Bundle**. Omit the `bundleHash` parameter to query your own Wallet Bundle:
  ```kotlin
  val response = client.queryBundle(
      bundleHash = "c47e20f99df190e418f0cc5ddfa2791e9ccc4eb297cfa21bd317dc0f98313b1d"
  )
  
  if (response.success()) {
      val bundleData = response.payload()
      println(bundleData) // Raw Metadata
  }
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
      // Do things!
      println("Metadata created successfully!")
  }

  println(response.payload()) // Raw response
  ```

- Query Wallets associated with a Wallet Bundle:

  ```kotlin
  val wallets = client.queryWallets(
      bundleHash = "c47e20f99df190e418f0cc5ddfa2791e9ccc4eb297cfa21bd317dc0f98313b1d",
      token = "FOO" // Optional: Filter by token
  )

  println(wallets) // Raw response
  ```

- Declaring new **Wallets**:

  (**Note:** If Tokens are sent to undeclared Wallets, **Shadow Wallets** will be used (placeholder
  Wallets that can receive, but cannot send) to store tokens until they are claimed.)

  ```kotlin
  val response = client.createWallet("FOO") // Token Slug for the wallet we are declaring

  if (response.success()) {
      // Do things!
      println("Wallet created successfully!")
  }

  println(response.payload()) // Raw response
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
      units = mutableListOf(), // Optional: for stackable tokens
      batchId = null // Optional: for stackable tokens
  )

  if (response.success()) {
      // Do things!
      println("Token created successfully!")
  }

  println(response.payload()) // Raw response
  ```

- Transferring **Tokens** to other users:

  ```kotlin
  val recipientWallet = Wallet(recipientSecret, "CRZY")
  val response = client.transferToken(
      recipient = recipientWallet, // Or bundle hash string
      token = "CRZY", // Token slug
      amount = 100.0,
      units = mutableListOf(), // Optional: for stackable tokens
      batchId = null // Optional: for stackable tokens
  )

  if (response.success()) {
      // Do things!
      println("Token transferred successfully!")
  }

  println(response.payload()) // Raw response
  ```

- Creating a new **Identifier**:

  ```kotlin
  val response = client.createIdentifier(
      type = "email",
      contact = "user@example.com", 
      code = "verification-code-123"
  )

  if (response.success()) {
      // Do things!
      println("Identifier created successfully!")
  }

  println(response.payload()) // Raw response
  ```

- Claiming a **Shadow Wallet**:

  ```kotlin
  val response = client.claimShadowWallet(
      token = "MTK",
      molecules = moleculeList // Optional: Batch claim
  )

  if (response.success()) {
      // Do things!
      println("Shadow wallet claimed successfully!")
  }

  println(response.payload()) // Raw response
  ```

- Query **ContinuID** information:

  ```kotlin
  val response = client.queryContinuId(bundleHash)

  if (response.success()) {
      val continuData = response.payload()
      println("ContinuID: $continuData")
  }
  ```

- Query **Batch** operations:

  ```kotlin
  // Query batch information
  val batchResponse = client.queryBatch("batch-id-123")
  
  // Query batch history
  val historyResponse = client.queryBatchHistory("batch-id-123")

  println(batchResponse.payload())
  println(historyResponse.payload())
  ```

- Query **User Activity**:

  ```kotlin
  val response = client.queryUserActivity(
      bundleHash = bundleHash,
      metaType = "UserAction", // Optional: Filter by meta type
      metaId = "login", // Optional: Filter by meta ID
      limit = 10 // Optional: Limit results
  )

  println(response.payload()) // User activity data
  ```

- Request **Tokens** from faucet:

  ```kotlin
  val response = client.requestTokens(
      token = "DEMO",
      amount = 1000
  )

  if (response.success()) {
      println("Tokens requested successfully!")
  }
  ```

- Getting client information:

  ```kotlin
  // Note: Fingerprint and buffer token methods are not available in Kotlin SDK
  // Check client configuration and bundle information
  val bundle = client.getBundle()
  println("Client bundle: $bundle")
  ```

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

- Create a custom Mutation:
  ```kotlin
  import wishKnish.knishIO.client.mutation.MutationProposeMolecule
  
  val mutation = MutationProposeMolecule(molecule)
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

- Execute a custom Query or Mutation:
  ```kotlin
  val response = client.executeQuery(mutation)
  
  if (response.success()) {
      println("Molecule executed successfully!")
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

2. Generate a 2048-symbol hexadecimal secret, either randomly, or via hashing login + password + salt, OAuth secret ID, biometric ID, or any other static value.

3. (optional) Initialize a signing wallet with:
   ```kotlin
   val wallet = Wallet(
       secret = secret,
       token = tokenSlug,
       position = customPosition, // (optional) instantiate specific wallet instance vs. random  
       characters = characterSet // (optional) override the character set used by the wallet
   )
   ```

   **WARNING 1:** If ContinuID is enabled on the node, you will need to use a specific wallet, and therefore will first need to query the node to retrieve the `position` for that wallet.

   **WARNING 2:** The Knish.IO protocol mandates that all C and M transactions be signed with a `USER` token wallet.

4. Build your molecule with:
   ```kotlin
   val molecule = Molecule(
       secret = secret,
       sourceWallet = sourceWallet, // (optional) wallet for signing
       cellSlug = cellSlug // (optional) used to point a transaction to a specific branch of the ledger
   )
   ```

5. Either use one of the shortcut methods provided by the `Molecule` class (which will build `Atom` instances for you), or create `Atom` instances yourself.

   DIY example:
    ```kotlin
    // This example records a new Wallet on the ledger

    // Define metadata for our new wallet
    val newWalletMeta = listOf(
        MetaData("address", newWallet.address),
        MetaData("token", newWallet.token),
        MetaData("bundle", newWallet.bundle),
        MetaData("position", newWallet.position),
        MetaData("batchId", newWallet.batchId ?: "")
    )

    // Build the C isotope atom
    val walletCreationAtom = Atom(
        position = sourceWallet.position,
        walletAddress = sourceWallet.address,
        isotope = 'C',
        token = sourceWallet.token,
        metaType = "wallet",
        metaId = newWallet.address,
        meta = newWalletMeta,
        index = molecule.generateIndex()
    )

    // Add the atom to our molecule
    molecule.addAtom(walletCreationAtom)

    // Adding a ContinuID / remainder atom
    molecule.addContinuIdAtom()
    ```

   Molecule shortcut method example:
    ```kotlin
    // This example commits metadata to some Meta Asset

    // Defining our metadata
    val metadata = listOf(
        MetaData("foo", "Foo"),
        MetaData("bar", "Bar")
    )

    molecule.initMeta(
        meta = metadata,
        metaType = "MyMetaType",
        metaId = "MetaId123"
    )
    ```

6. Sign the molecule with the stored user secret:
    ```kotlin
    molecule.sign()
    ```

7. Make sure everything checks out by verifying the molecule:
    ```kotlin
    try {
        if (molecule.check()) {
            // If we're validating a V isotope transaction,
            // add the source wallet as a parameter
            println("Molecule validation passed!")
        } else {
            println("Molecule validation failed!")
        }
    } catch (e: Exception) {
        println("Molecule check failed: ${e.message}")
        // Handle the error
    }
    ```

8. Broadcast the molecule to a Knish.IO node:
    ```kotlin
    import wishKnish.knishIO.client.mutation.MutationProposeMolecule
    
    // Build our mutation object using the KnishIOClient wrapper
    val mutation = MutationProposeMolecule(molecule)

    // Send the mutation to the node and get a response
    val response = client.executeQuery(mutation)
    ```

9. Inspect the response...
    ```kotlin
    // For basic queries, we look at the data property:
    println(response.payload())

    // For mutations, check if the molecule was accepted by the ledger:
    println(if (response.success()) "Success" else "Failed")

    // We can also check the reason for rejection
    println(response.reason())

    // Some queries may also produce additional data:
    println(response.data())
    ```

   Payloads are provided by responses to the following queries:
    1. `queryBalance` and `queryContinuId` -> returns a `Wallet` instance
    2. `queryWallets` -> returns a list of `Wallet` instances
    3. `createToken`, `transferToken`, `createWallet`, `createMeta`, and other mutations -> returns molecule metadata

## Demo System

This SDK includes a comprehensive examples system with practical demonstrations. Explore the examples folder to see real implementations:

```bash
# From the project root directory
./gradlew build

# Run specific examples
./gradlew run -PmainClass=examples.BasicUsageKt
./gradlew run -PmainClass=examples.TokenOperationsKt  
./gradlew run -PmainClass=examples.MetadataManagementKt
./gradlew run -PmainClass=examples.WalletManagementKt
./gradlew run -PmainClass=examples.AdvancedMoleculesKt
./gradlew run -PmainClass=examples.CompleteExampleKt

# Or compile and run directly  
java -cp build/libs/knishio-client-kotlin-*.jar examples.BasicUsageKt
```

The examples system provides:
- **Basic usage** - Client initialization, authentication, and simple operations
- **Token operations** - Creating tokens (fungible, NFTs), transfers, and balance verification  
- **Metadata management** - Data storage, user profiles, product catalogs, complex queries
- **Wallet management** - Advanced wallet operations, bundle management, shadow wallets
- **Advanced molecules** - Low-level molecule construction for complex scenarios
- **Complete workflow** - Full feature demonstration with production patterns

See `examples/README.md` for complete setup instructions and detailed usage guides.

## Security

This SDK implements quantum-resistant cryptography for future-proof security:

- All signatures use XMSS (post-quantum secure)
- Encryption uses ML-KEM768 (NIST approved)
- One-time keys prevent signature reuse
- Secure random generation for all cryptographic operations

For security issues, please email security@wishknish.com instead of using the issue tracker.

## Features

- 🚀 **Post-Blockchain Architecture**: DAG-based distributed ledger with organism-inspired transaction model
- 🔐 **Quantum-Resistant Security**: XMSS signatures and ML-KEM768 (NIST FIPS-203) encryption
- ⚡ **Network-Bound Scalability**: Performance improves as the network grows
- 🔄 **Cross-Platform Compatibility**: Full compatibility with JavaScript client
- 📦 **Comprehensive SDK**: Complete API for wallets, tokens, metadata, and transactions
- 🧬 **Molecular Composition**: Atomic operations grouped into molecular transactions
- 🏢 **Cellular Architecture**: Application-specific sub-ledgers with isolation

## Getting Help

Knish.IO is under active development, and our team is ready to assist with integration questions. The best way to seek help is to stop by our [Telegram Support Channel](https://t.me/wishknish). You can also [send us a contact request](https://knish.io/contact) via our website.
