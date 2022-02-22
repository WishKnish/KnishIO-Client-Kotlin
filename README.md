<div style="text-align:center">
  <img src="https://raw.githubusercontent.com/WishKnish/KnishIO-Technical-Whitepaper/master/KnishIO-Logo.png" alt="Knish.IO: Post-Blockchain Platform" />
</div>
<div style="text-align:center">info@wishknish.com | https://wishknish.com</div>

# Knish.IO Kotlin Client SDK

This is an experimental Kotlin / Java implementation of the Knish.IO client SDK. Its purpose is to expose class
libraries for building and signing Knish.IO Molecules, composing Atoms, generating Wallets, and so much more.

## Installation

The SDK can be installed via either of the following:

Gradle:
```
dependencies {
     implementation("io.knish:KnishIO-Client-Kotlin:0.0.1")
}
```
Maven:
```
<dependency>
  <groupId>io.knish</groupId>
  <artifactId>KnishIO-Client-Kotlin</artifactId>
  <version>0.0.1</version>
</dependency>
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
   val client = KnishIOClient(listOf(URI("myNodeURI")))
```

3. Set the Cell to match your app:
```kotlin
   client.setCellSlug( "myCellSlug" )
```
   (**Note:** the `knishio_cells` table on the node must contain an entry for this Cell)


4. Request authorization token from the node:
```kotlin
   client.requestAuthToken(seed = "myTopSecretCode")
```

   (**Note:** the `seed` parameter can be a salted combination of username + password, a biometric hash, an existing
   user identifier from an external authentication process, for example)


5. Begin using `client` to trigger commands described below...

### KnishIOClient Methods

- Query metadata for a **Wallet Bundle**. Omit the `bundleHash` parameter to query your own Wallet Bundle:
```kotlin
  val result = client.queryBundle(
    bundle="c47e20f99df190e418f0cc5ddfa2791e9ccc4eb297cfa21bd317dc0f98313b1d"
  )

  when {
    result.success() -> {
      val wallet = result.payload()
      println(wallet?.bundle)
    }
    else -> println(result.status())
  }
```

- Query metadata for a **Meta Asset**. Omit any parameters to widen the search:

```kotlin
  val result = client.queryMeta ( 
    metaType = 'Vehicle',
    metaIds = listOf("Meta ID"),
    keys = listOf("LicensePlate"),
    values = listOf("1H17P"),
    latest = true // Limit meta values to latest per key
   )

  // Raw Metadata
  println(result)
```

- Writing new metadata for a **Meta Asset**.

```kotlin
  import kotlinx.serialization.json.Json
  import kotlinx.serialization.json.encodeToJsonElement
  
  val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
  }
  
  val result = client.createMeta ( 
    metaType = "Pokemon",
    metaId = "Charizard",
    meta = mutableListOf(
    MetaData("type", "fire"),
    MetaData(
      "weaknesses", 
      json.encodeToJsonElement(listOf("rock", "water", "electric")).toString()
    ),
    MetaData(
      "immunities", 
      json.encodeToJsonElement(listOf("ground")).toString()
    ),
    MetaData("hp", "78"),
    MetaData("attack", "84")
  ) 
 )

  when {
   result.success() -> println(result.data())
    else -> println(result.status())
  }
```

- Query Wallets associated with a Wallet Bundle:

```kotlin
  val result = client.queryWallets (
    bundle = "c47e20f99df190e418f0cc5ddfa2791e9ccc4eb297cfa21bd317dc0f98313b1d",
    unspent = true // limit results to unspent wallets?
  )

  println(result) // Raw response
```

- Declaring new **Wallets**. If Tokens are sent to undeclared Wallets, **Shadow Wallets** will be used (placeholder
  Wallets that can receive, but cannot send) to store tokens until they are claimed.

```kotlin
  val result = client.createWallet ( 
    token = 'FOO' // Token Slug for the wallet we are declaring
  )

  when {
      result.success() -> println(result.data())
    else -> println(result.status())
  }
```

- Issuing new **Tokens**:

```kotlin
  val meta = mutableListOf(
    MetaData("name", "CrazyCoin"),
    MetaData("fungibility", "fungible"),
    MetaData("supply", "limited"),
    MetaData("decimals", "2")
  )
  val result = client.createToken ( 
    token = "CRZY", // Token slug (ticker symbol)
    amount = "100000000", // Initial amount to issue
    meta = meta
  )

  when {
      result.success() -> println(result.data())
    else -> println(result.status())
  }
```

- Transferring **Tokens** to other users:

```kotlin
  import wishKnish.knishIO.client.Wallet

  val result = client.transferToken ( 
    recipient = Wallet(secret, "CRZY"), // Recipient's wallet,
    token = "CRZY", // Token slug
    amount = '100'
   )

  when {
      result.success() -> println(result.data())
    else -> println(result.status())
  }
```

## The Hard Way: working directly with Molecules

- Return a Molecule instance that you can manually add atoms to:
```kotlin
    client.createMolecule()
```

- Return a customized Query instance that can be used to generate arbitrary transactions to the ledger for
  the supplied Query class:
```kotlin
    client.createMoleculeMutation (
      mutationClass = myQueryClass // More info on these below
    )
```

- Retrieves the active balance (in the form of a Wallet object:
```kotlin
    client.queryBalance (
      token = myTokenSlug,
      bundle = myBundleHash // Omit to get your own balance
    )
```

- Create a new Token on the ledger and places initial balance into a new wallet created for you; `tokenMetadata` object
  must contain properties for `name` and `fungibility` (which can presently be `'fungible'`, `'nonfungible'`,
  or `'stackable'`):
```kotlin
    client.createToken (
      token = tokenSlug, 
      amount = initialAmount,
      meta = tokenMetadata
    )
```

- Retrieve a list of Shadow Wallets (wallets that have a balance in a particular token, but no keys - as can happen when
  you are sent tokens for which you lack a prior wallet):
```kotlin
    client.queryShadowWallets (
      token = tokenSlug,
      bundle = myBundleHash // Omit to get your own balance
    )
```

- Attempt to claim a Shadow Wallet by generating keys for it, which turns it into a usable Wallet:
```kotlin
    client.claimShadowWallet (
      token = tokenSlug
    )
```

- Transfer tokens to a recipient Wallet:
```kotlin
    client.transferToken (
      recipient = wallet,
      token = tokenSlug,
      amount = transferAmount
    )
```

### Knish.IO Query Classes

The `KnishIOClient` can utilize a wide variety of built-in query classes
via `client.createMoleculeMutation ( myMutationClass )`, in case you need something more flexible than the built-in methods.

After calling `client.createMoleculeMutation ( myMutationClass )`, you will receive a `Mutation` class instance, which will let
you add any necessary metadata to fulfill the GraphQL query or mutation. The metadata required will be different based
on the type of `Mutation` class you choose, via an overloaded `fill()` method.

Here are the most commonly used ones:

#### `QueryMetaType` (for retrieving Meta Asset information)

```kotlin
// Build the query
val query = client.createQuery(QueryMetaType::class)

// Define variable parameters
// (eg: which MetaType we are querying)
val variables = MetaTypeVariable(metaType = 'SomeMetaType')
  

// Execute the query
val result = query.execute(variables = variables)

println(result.data())
```

#### `QueryWalletBundle` (for retrieving information about Wallet Bundles)

```kotlin
// Build the query
val query = client.createQuery(QueryWalletBundle::class)

// Define variable parameters
// (eg: how we want to filter Wallet Bundles)
val variables = WalletBundleVariable(key = "publicName", value = "Eugene")


// Execute the query
val result = query.execute(variables = variables)

println(result.data())
```

#### `QueryWalletList` (for getting a list of Wallets)

```kotlin
// Build the query
val query = client.createQuery(QueryWalletList::class)

// Define variable parameters
// (eg: how we want to filter Wallet Bundles)
val variables = WalletListVariable(token = "DYD") 



// Execute the query
val result = query.execute(variables = variables)

println(result.data())
```

## The Extreme Way: DIY Everything

This method involves individually building Atoms and Molecules, triggering the signature and validation processes, and
communicating the resulting signed Molecule mutation or Query to a Knish.IO node via your favorite GraphQL client.

1. Include the relevant classes in your application code:
```kotlin
  import wishKnish.knishIO.client.Wallet
  import wishKnish.knishIO.client.Molecule
```

2. Generate a 2048-symbol hexadecimal secret, either randomly, or via hashing login + password + salt, OAuth secret ID,
   biometric ID, or any other static value


3. (optional) Initialize a signing wallet with:
```kotlin
   val wallet = Wallet( 
     secret = mySecret,
     token = tokenSlug,
     position = myCustomPosition // (optional) instantiate specific wallet instance vs. random
   
     // (optional) helps you override the character set used by the wallet, for inter-ledger compatibility. Currently supported options are: `GMP`, `BITCOIN`, `FLICKR`, `RIPPLE`, and `IPFS`.
     // characters: myCharacterSet
   )
```

   **WARNING 1:** If ContinuID is enabled on the node, you will need to use a specific wallet, and therefore will first
   need to query the node to retrieve the `position` for that wallet.

   **WARNING 2:** The Knish.IO protocol mandates that all C and M transactions be signed with a `USER` token wallet.


4. Build your molecule with:
```kotlin
   val molecule = Molecule( 
     secret = mySecret,
     sourceWallet = mySourceWallet, // (optional) wallet for signing
     remainderWaller = myRemainderWallet, // (optional) wallet to receive remainder tokens
     cellSlug = myCellSlug // (optional) used to point a transaction to a specific branch of the ledger
   )
```

5. Either use one of the shortcut methods provided by the `Molecule` class (which will build `Atom` instances for you),
   or create `Atom` instances yourself.

   DIY example:
```kotlin
    // This example records a new Wallet on the ledger

    // Define metadata for our new wallet

  val newWalletMeta = mutableListOf(
    MetaData("address", newWallet.address),
    MetaData("token", newWallet.token),
    MetaData("bundle", newWallet.bundle),
    MetaData("position", newWallet.position), 
    MetaData("batch_id", newWallet.batchId)
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
  molecule.addUserRemainderAtom(Wallet(secret))
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

6. Sign the molecule with the stored user secret:
```kotlin
    molecule.sign()
```

7. Make sure everything checks out by verifying the molecule:
```kotlin
    if (!molecule.check()) {
       // Throw some exception?
    }
    
    // If we're validating a V isotope transaction,
    // add the source wallet as a parameter
    if (!molecule.check(sourceWallet)) {
       // Insufficient tokens?
    }
```

8. Broadcast the molecule to a Knish.IO node:
```kotlin
    // Build our query object using the KnishIOClient wrapper
    val query = MutationProposeMolecule(client, molecule)
   
    // Send the query to the node and get a response
    val response = query.execute(MoleculeMutationVariable(query.molecule()?))
```

9. Inspect the response...
```kotlin
    // For basic queries, we look at the data property:
    println(response.data())
```
   If you are sending a mutation, you can also check if the molecule was accepted by the ledger:
```kotlin
  // For mutations only 
  println(response.success())
   
  // We can also check the reason for rejection
  println(response.reason())
```
   Some queries may also produce a payload, with additional data:
```kotlin 
    println(response.payload())
```
   Payloads are provided by responses to the following queries:
    1. `QueryBalance` and `QueryContinuId` -> returns a `Wallet` instance
    2. `QueryWalletList` -> returns a list of `Wallet` instances
    3. `MutationProposeMolecule`, `MutationRequestAuthorization`, `MutationCreateIdentifier`, `MutationLinkIdentifier`
       , `MutationClaimShadowWallet`, `MutationCreateToken`, `MutationRequestTokens`, and `MutationTransferTokens` ->
       returns molecule metadata

## Getting Help

Knish.IO is active development, and our team is ready to assist with integration questions. The best way to seek help is
to stop by our [Telegram Support Channel](https://t.me/wishknish). You can
also [send us a contact request](https://knish.io/contact) via our website.
