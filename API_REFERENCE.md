# Knish.IO Kotlin SDK API Reference

Reference for `io.knish:knishio-client-kotlin` 1.5.2. All classes are under the package
`wishKnish.knishIO.client`; the generated KDoc (`./gradlew dokkaGenerate`, output in
`build/dokka/html`) lists every public member. Parts of the JavaScript reference SDK that this
SDK does not have are listed in the [README](README.md#not-implemented).

Cryptography runs on the JVM: SHAKE256 and WOTS+ one-time signatures, ML-KEM (FIPS 203) from
BouncyCastle, AES-256-GCM from the JDK. There is no native `kcore` backend; the opt-in
`-Dknishio.mlkem.backend=noble` system property runs the noble ML-KEM bundle under GraalJS
instead.

## Table of Contents

- [KnishIOClient](#knishioclient)
- [Wallet](#wallet)
- [Molecule](#molecule)
- [Atom](#atom)
- [Responses](#responses)
- [Queries and Mutations](#queries-and-mutations)
- [Cryptographic Functions](#cryptographic-functions)
- [Exception Types](#exception-types)

## KnishIOClient

The client for a Knish.IO node. It holds the identity's secret (or a `SecretStorageProvider`),
the session token, and the ContinuID chain; it implements `Closeable`.

### Constructor

All parameters except `uris` are optional:

```kotlin
val client = KnishIOClient(
    uris = listOf(URI("http://localhost:8000/graphql")), // one is picked at random per request
    serverSdkVersion = 3,       // the node's SDK protocol version
    logging = false,            // print debug lines
    encrypt = false,            // start with CipherHash (ML-KEM) encrypted transport
    insecureTls = false,        // accept any TLS certificate (local test nodes only)
    secretStorage = null,       // a SecretStorageProvider that holds the secret
    mlkemParameterSet = 1024,   // 1024 or 768
    otsVersion = 2              // OTS version of new keys: 2 (64 chains) or 1 (16 chains)
)
```

### Session and secret

| Method | Description |
|---|---|
| `requestAuthToken(secret: String? = null, seed: String? = null, cellSlug: String? = null, encrypt: Boolean = false): AuthToken` | Logs in and stores the token. The secret is `secret`, else `Crypto.generateSecret(seed)`, else the secret in the client's `SecretStorageProvider`; with none of these it requests a guest token. Throws `UnauthenticatedException` when the node rejects the login. Encrypted transport is switched on or off to match `encrypt`. |
| `authorize(secret: String? = null, cellSlug: String? = null, encrypt: Boolean = false): AuthToken` | The same login with an explicit secret (`null` = guest). |
| `setAuthToken(authToken: AuthToken)` / `getAuthToken(): AuthToken?` | Set or read the session token (e.g. one restored with `AuthToken.restore(snapshot, secret)`). |
| `setSecret(value: String): KnishIOClient` | Sets the secret and derives the bundle hash. |
| `setSecretStorage(storage: SecretStorageProvider, bundleHash: String? = null): KnishIOClient` | Uses a secret storage provider instead of an in-memory secret. |
| `getSecretStorage(): SecretStorageProvider?` | The provider, if one is set. |
| `retrieveSecret(options: StorageOptions = StorageOptions()): String?` | The in-memory secret, else the secret from the provider. |
| `getSecret(): String` | As `retrieveSecret()`, but throws `UnauthenticatedException` when there is none. |
| `hasSecret(): Boolean`, `hasBundle(): Boolean` | Whether a secret / bundle hash is set. |
| `bundle(): String` | The identity's bundle hash; throws `UnauthenticatedException` before login. |
| `setCellSlug(cellSlug: String?)`, `cellSlug(): String?` | The cell that molecules and logins target. |
| `enableEncryption()`, `disableEncryption()`, `hasEncryption(): Boolean` | CipherHash transport switch. |
| `reset()` | Forgets the secret, bundle hash, secret storage provider and remainder wallet. |
| `close()` | Closes the HTTP client. |

```kotlin
val authToken = client.requestAuthToken(secret = secret, encrypt = true)
println("Token ${authToken.getToken()}, expires in ${authToken.getExpireInterval()} ms, expired: ${authToken.isExpired()}")
```

### Queries

| Method | Returns |
|---|---|
| `queryBalance(token: String, bundle: String? = null, type: String? = null)` | `ResponseBalance`; `payload()` is the `Wallet` holding `token` (own bundle by default; `type = "buffer"` for the buffer wallet), or `null`. |
| `queryContinuId(bundle: String, token: String = "USER")` | `ResponseContinuId`; `payload()` is the ContinuID head `Wallet`. |
| `queryWallets(bundle: String? = null, token: String? = null, unspent: Boolean? = null)` | `List<Wallet>` |
| `queryShadowWallets(token: String = "KNISH", bundle: String? = null)` | `List<Wallet>`: the bundle's wallets for `token` (own bundle by default); shadow wallets are those with `isShadow()`. |
| `queryBundle(bundle: String? = null, key: String? = null, value: String? = null, latest: Boolean = true)` | `Map<String, WalletBundle>` keyed by bundle hash (own bundle by default). |
| `queryBundleRaw(...)` (same parameters) | `ResponseWalletBundle` |
| `queryMeta(metaType: String? = null, metaIds: List<String> = listOf(), keys: List<String> = listOf(), values: List<String> = listOf(), latest: Boolean? = null, latestMetas: Boolean? = null, filter: List<MetaFilter> = listOf(), queryArgs: QueryArgs? = null, count: String? = null, countBy: String? = null)` | `ResponseMetaType.Response?` with `instances`, `instanceCount`, `paginatorInfo`. |
| `queryMetaInstance(metaType: String, metaId: String? = null, key: String? = null, value: String? = null, latest: Boolean? = null, filter: List<MetaFilter>? = null)` | `List<MetaType>?` as the node returned it. |
| `queryBatch(batchId: String)`, `queryBatchHistory(batchId: String)` | `ResponseBatch`, `ResponseBatchHistory`; `data()` is a `List<BatchInstance>?`. |
| `queryActiveSession(bundleHash: String? = null, metaType: String? = null, metaId: String? = null)` | `ResponseActiveSession`; `data()` is a `List<Session>?`. |

```kotlin
val result = client.queryMeta(
    metaType = "Vehicle",
    keys = listOf("LicensePlate"),
    filter = listOf(MetaFilter(key = "color", value = "red", comparison = "=")),
    latest = true
)
result?.instances.orEmpty().forEach { instance ->
    println("${instance.metaId}: ${instance.metas.orEmpty().joinToString { "${it.key}=${it.value}" }}")
}
```

### Ledger operations

Each builds and signs a molecule (signed by the identity's ContinuID head wallet, or by the token
wallet that pays), verifies it locally (`Molecule.verify`, which throws on a failure) and sends
it. The result is a `ResponseProposeMolecule`.

| Method | Description |
|---|---|
| `createWallet(token: String)` | Declares a wallet for `token`. |
| `createToken(token: String, amount: Number? = null, meta: MutableList<MetaData> = mutableListOf(), batchId: String? = null, units: MutableList<TokenUnit> = mutableListOf())` | Issues a token. `meta` holds `name`, `fungibility` (`fungible` / `nonfungible` / `stackable`), `supply` (`limited` / `replenishable`) and `decimals`. A stackable token gets a batch id; with `units` the amount is the unit count and `decimals` must be 0. |
| `createMeta(metaType: String, metaId: String, meta: MutableList<MetaData> = mutableListOf())` | Writes metadata to a Meta Asset. |
| `createIdentifier(type: String, contact: String, code: String)` | Records an identifier (e.g. `email`) with its verification code. |
| `requestTokens(token: String, to: String? = null, amount: Number? = null, units: MutableList<TokenUnit> = mutableListOf(), meta: MutableList<MetaData> = mutableListOf(), batchId: String? = null)` | Asks the node for tokens. `to` is a bundle hash or a recipient secret; empty = own bundle. An overload takes `to: Wallet?`. Pass `to` explicitly: with both overloads defaulting `to`, `requestTokens(token)` alone does not compile. |
| `transferToken(recipient: Wallet, token: String, amount: Number, units: MutableList<TokenUnit> = mutableListOf(), batchId: String? = null, sourceWallet: Wallet? = null)` | Transfers to a wallet. An overload takes `recipient: String` (a bundle hash or a recipient secret). Throws `TransferBalanceException` when the balance is too low; with `units`, `amount` must be 0. |
| `transferTokens(token: String, recipients: List<TransferRecipient>, sourceWallet: Wallet? = null)` | Several recipients in one molecule. `TransferRecipient(bundleHash, units = emptyList(), amount = null, batchId = null)`. |
| `burnTokens(token: String, amount: Number = 0, units: MutableList<TokenUnit> = mutableListOf(), sourceWallet: Wallet? = null)` | Burns an amount or the given units. |
| `replenishToken(token: String, amount: Number? = null, units: MutableList<TokenUnit> = mutableListOf())` | Adds supply to a replenishable token (units for a stackable token). |
| `fuseToken(tokenSlug: String, newTokenUnit: TokenUnit, fusedTokenUnitIds: List<String>, bundleHash: String? = null, sourceWallet: Wallet? = null)` | Fuses two or more held units into a new unit, delivered to `bundleHash` (own bundle by default). An overload takes `newTokenUnitId: String`. |
| `claimShadowWallet(token: String, batchId: String? = null, molecule: Molecule? = null)` | Claims a shadow wallet (the first one for `token` when `batchId` is null); throws `WalletShadowException` when there is none. |
| `depositBufferToken(token: String, amount: Number, tradeRates: Map<String, Any> = emptyMap(), sourceWallet: Wallet? = null)` | Moves tokens into the identity's buffer (B isotope) wallet with trade rates. |
| `withdrawBufferToken(token: String, amount: Number, sourceWallet: Wallet? = null)` | Moves tokens from the buffer back to the identity's bundle. |

```kotlin
val response = client.transferToken(
    recipient = recipientBundleHash,
    token = "CRZY",
    amount = 100
)
if (response.success()) {
    println("Accepted: ${response.data()?.molecularHash}")
} else {
    println("Rejected: ${response.reason()}")
}
```

### Building molecules yourself

| Method | Description |
|---|---|
| `createMolecule(secret: String? = null, sourceWallet: Wallet? = null, remainderWallet: Wallet? = null): Molecule` | A molecule signed by `sourceWallet` (default: the ContinuID head) with a USER remainder wallet. |
| `sourceWallet(): Wallet` | The identity's ContinuID head wallet. |
| `createMoleculeMutation(mutationClass, molecule: Molecule? = null): MutationProposeMolecule` | Wraps a molecule in a mutation; `execute(MoleculeMutationVariable(molecule))` sends it as it is. |
| `createQuery(queryClass): IQuery` | A query object bound to the client's HTTP client. |

```kotlin
val molecule = client.createMolecule()
molecule.initMeta(meta = mutableListOf(MetaData("foo", "Foo")), metaType = "MyMetaType", metaId = "MetaId123")
molecule.sign()
Molecule.verify(molecule, molecule.sourceWallet)

val mutation = client.createMoleculeMutation(MutationProposeMolecule::class, molecule)
val response = mutation.execute(MoleculeMutationVariable(molecule)) as ResponseProposeMolecule
```

## Wallet

A one-time-signature wallet derived from (secret, token, position).

### Constructor

```kotlin
val wallet = Wallet(
    secret = secret,          // null for a wallet rebuilt from ledger data (no keys)
    token = "USER",
    position = null,          // null = a random position
    batchId = null,
    characters = null,        // encoding of encrypted payloads; null = "BASE64"
    mlkemParameterSet = 1024, // 1024 or 768
    otsVersion = 1            // 1 (16 chains) or 2 (64 chains); KnishIOClient creates version 2 keys by default
)
```

`Wallet.create(secretOrBundle, token = "USER", batchId = null, characters = null, mlkemParameterSet = 1024, otsVersion = 1)`
accepts a secret (keyed wallet at a random position) or a bundle hash (a recipient wallet without keys).

### Properties

`token`, `position`, `address`, `bundle`, `key` (the one-time private key), `pubkey` (base64
ML-KEM public key), `balance`, `batchId`, `tokenUnits`, `characters`, `otsVersion`,
`mlkemParameterSet`. `isShadow()` is true for a wallet with neither position nor address.

### Functions

| Function | Description |
|---|---|
| `Wallet.generatePrivateKey(secret: String, token: String, position: String, otsVersion: Int = 1): String` | The one-time private key (2048 hex characters for version 1, 8192 for version 2). |
| `Wallet.generatePublicKey(key: String): String` | The wallet address of a private key. |
| `Wallet.detectOtsVersion(secret, token, position, address, preferred = 1): Int?` | Which OTS version derives `address`, or `null`. |
| `Wallet.generatePosition(saltLength: Int = 64): String` | A random position. |
| `Wallet.isBundleHash(code: String): Boolean` | Whether `code` is a 64-hex bundle hash. |
| `encryptMessage(message: String, recipientPubkey: String): Map<String, String>` | ML-KEM + AES-256-GCM envelope `{cipherText, encryptedMessage}` for the recipient's `pubkey`; the cross-SDK format. |
| `decryptMessage(encryptedData: Map<String, String>): String?` | Decrypts an envelope addressed to this wallet; `null` on failure. |
| `encryptStringML(message: String, recipientPubkey: String): String` | The CipherHash form: JSON `{ hashShare(recipientPubkey): envelope }`, in the wallet's own parameter set; a recipient key of the wrong length is rejected. |
| `decryptMyMessageML(message: Map<String, Map<String, String>>): String?` | Reads a CipherHash map addressed to either of this wallet's ML-KEM identities (1024 or 768). |

```kotlin
val sender = Wallet(secret = secret, token = "USER")
val recipient = Wallet(secret = recipientSecret, token = "USER")
val envelope = sender.encryptMessage("hello", recipient.pubkey!!)
println(recipient.decryptMessage(envelope)) // hello
```

## Molecule

A set of atoms that the ledger accepts or rejects as a whole.

### Constructor

```kotlin
val molecule = Molecule(
    secret = secret,              // signs the molecule and derives new wallets
    sourceWallet = sourceWallet,  // the signing wallet
    remainderWallet = null,       // null = a new wallet for the source token
    cellSlug = null,              // target cell
    mlkemParameterSet = 1024,
    minOtsVersion = 1             // lowest OTS version for wallets the molecule creates
)
```

`KnishIOClient.createMolecule()` sets these from the client and is the usual way to get one.

### Builders

Each adds its atoms and the USER remainder (ContinuID, isotope I) atom where the protocol needs
one, and returns the molecule:

`initValue(recipientWallet, amount)`, `initValues(recipientWallets, amounts)`,
`initWalletCreation(newWallet)`, `initTokenCreation(recipientWallet, amount, meta)`,
`initMeta(meta, metaType, metaId)`, `createRule(metaType, metaId, rule: JsonArray, policy: JsonObject = {})`,
`initIdentifierCreation(type, contact, code)`, `initShadowWalletClaim(wallet)`,
`initTokenRequest(token, amount, metaType = null, metaId = null, meta = mutableListOf(), batchId = null)`,
`initAuthorization(meta)`, `initDepositBuffer(amount, tradeRates)`, `initWithdrawBuffer(recipients)`,
`replenishTokens(amount, token, creditedWallet, units)`, `fuseToken(fusedTokenUnitIds, newTokenUnit, recipientWallet)`,
`burnToken(amount)`.

Low level: `addAtom(atom)`, `addUserRemainderAtom()`, `generateIndex()`, `finalMetas(metas, wallet)`.

Rules use the JS rule form, a list of `{condition, callback}` objects:

```kotlin
molecule.createRule(
    metaType = "MyMetaType",
    metaId = "MetaId123",
    rule = Json.parseToJsonElement(
        """[{"condition":[{"key":"status","value":"locked","comparison":"=="}],"callback":[{"action":"reject"}]}]"""
    ).jsonArray
)
```

### Signing and checking

| Method | Description |
|---|---|
| `sign(anonymous: Boolean = false, compressed: Boolean = true): String?` | Computes `molecularHash` and writes the WOTS+ signature fragments into the atoms. |
| `check(sourceWallet: Wallet? = null): Boolean` | All local checks; `false` on any failure. Pass the source wallet to check V-isotope balances. |
| `Molecule.verify(molecule: Molecule, sourceWallet: Wallet? = null): Boolean` | The same checks; throws the specific exception (see [Exception Types](#exception-types)). |
| `toJSON(includeValidationContext = true, includeOtsFragments = true, secureMode = false): Map<String, Any?>` | Serializable form. |
| `Molecule.fromJSON(json: String, includeValidationContext = true, validateStructure = true): Molecule` | Parses that form. |

`CheckMolecule.verifyFromServerData(json)` (in `libraries`) verifies a molecule as a node serves it.

Properties: `molecularHash`, `bundle`, `status`, `createdAt`, `cellSlug`, `atoms`, `sourceWallet`,
`remainderWallet`.

## Atom

### Constructor

```kotlin
val atom = Atom(
    position = sourceWallet.position!!,
    walletAddress = sourceWallet.address!!,
    isotope = 'M',
    token = sourceWallet.token,
    value = null,        // amount as an integer string, for value isotopes
    batchId = null,
    metaType = "MyMetaType",
    metaId = "MetaId123",
    meta = listOf(MetaData("foo", "Foo")),
    otsFragment = null,  // written by Molecule.sign()
    index = molecule.generateIndex()
)
```

`createdAt` defaults to the current time in milliseconds and `version` to `null`.
`Atom.hashAtoms(atoms, output = "base17")` computes a molecular hash.

### Isotopes

| Isotope | Use |
|---|---|
| C | Creation: wallet declaration, token issue, identifier, shadow wallet claim, replenishment |
| V | Value: transfers, burns, remainders |
| M | Metadata |
| I | ContinuID: the USER remainder wallet that signs the identity's next molecule |
| U | Authorization (login) |
| T | Token request |
| R | Rule |
| B | Buffer deposit / withdrawal |
| F | Fusion of stackable units |
| P, A | Peering and append requests; checked by `Molecule.verify`, but this SDK has no builder for them |

C, I, M, T, P and A atoms must be signed by a `USER` wallet (U atoms by `AUTH` or `USER`).

## Responses

Queries return subclasses of `response.Response`: `data()` is the node's result object,
`payload()` the client-side form (see the tables above), `success()` is true when the node
returned no GraphQL errors, and `status()` is the first error message.

`ResponseProposeMolecule` (every ledger operation) differs: `success()` is true when the
molecule's status is `accepted`; `status()` is that status, `reason()` the rejection reason,
`data()` the molecule as the node recorded it (`molecularHash`, `status`, `reason`, `createdAt`),
and `clientMolecule()` the molecule that was sent.

## Queries and Mutations

Query classes (`query`): `QueryActiveSession`, `QueryBalance`, `QueryBatch`, `QueryBatchHistory`,
`QueryContinuId`, `QueryMetaType`, `QueryWalletBundle`, `QueryWalletList`.

Mutation classes (`mutation`): `MutationProposeMolecule` and its subclasses
`MutationRequestAuthorization`, `MutationCreateWallet`, `MutationCreateToken`,
`MutationCreateMeta`, `MutationCreateIdentifier`, `MutationRequestTokens`,
`MutationTransferTokens`, `MutationClaimShadowWallet`, `MutationDepositBufferToken`,
`MutationWithdrawBufferToken`; and `MutationRequestAuthorizationGuest`.

## Cryptographic Functions

| Function | Description |
|---|---|
| `Crypto.generateSecret(seed: String? = null, length: Int = 2048): String` | A secret: derived from `seed`, or random. |
| `Crypto.generateBundleHash(secret: String): String` | SHAKE256 bundle hash of a secret. |
| `Crypto.generateBatchId(molecularHash: String? = null, index: Int? = null): String` | A batch id: derived from the molecular hash and index, or random. |
| `Crypto.hashShare(key: String, characters: String = "BASE64"): String` | The key under which a CipherHash map addresses a recipient. |
| `Wallet.generatePrivateKey`, `Wallet.generatePublicKey` | One-time keys and addresses (see [Wallet](#wallet)). |
| `Atom.hashAtoms(atoms, output = "base17")` | Molecular hash. |

```kotlin
val secret = Crypto.generateSecret("user@example.com:password:salt")
val bundleHash = Crypto.generateBundleHash(secret)
```

## Exception Types

All are in `exception` and extend `BaseException` (a `RuntimeException`).

- Session: `UnauthenticatedException` (no secret, or the node rejected the login),
  `SecretStorageException`, `CodeException` (an invalid query or mutation class, or an
  operation's internal error), `InvalidResponseException`.
- Balance and transfer: `BalanceInsufficientException`, `TransferBalanceException`,
  `TransferRemainderException`, `TransferMalformedException`, `TransferMismatchedException`,
  `TransferToSelfException`, `TransferUnbalancedException`, `NegativeAmountException`.
- Molecule checks: `MolecularHashMissingException`, `MolecularHashMismatchException`,
  `SignatureMismatchException`, `SignatureMalformedException`, `AtomIndexException`,
  `AtomsMissingException`, `MetaMissingException`, `BatchIdException`,
  `WrongTokenTypeException`, `PolicyInvalidException`, `RuleArgumentException`.
- Tokens and wallets: `StackableUnitAmountException`, `StackableUnitDecimalsException`,
  `WalletShadowException`.

## License

GNU General Public License v3.0 or later (GPL-3.0-or-later); see [LICENSE](LICENSE).
