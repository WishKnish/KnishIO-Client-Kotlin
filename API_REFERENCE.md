# Knish.IO Kotlin SDK API Reference

This document provides detailed API documentation for the Knish.IO Kotlin SDK.

## Table of Contents

- [KnishIOClient](#knishioclient)
- [Wallet](#wallet)
- [Molecule](#molecule)
- [Atom](#atom)
- [Query Methods](#query-methods)
- [Mutation Methods](#mutation-methods)
- [Cryptographic Functions](#cryptographic-functions)
- [Exception Types](#exception-types)

## KnishIOClient

The main client class for interacting with Knish.IO nodes.

### Constructor

```kotlin
class KnishIOClient(
    nodeUris: List<URI>,
    encrypt: Boolean = false,
    cellSlug: String? = null,
    logging: Boolean = false
)
```

**Parameters:**
- `nodeUris`: List of GraphQL endpoint URIs for Knish.IO nodes
- `encrypt`: Enable encryption for node communication (default: false)
- `cellSlug`: Default cell slug for operations (optional)
- `logging`: Enable debug logging (default: false)

### Core Methods

#### Authentication

##### `requestAuthToken(secret: String, cellSlug: String? = null): ResponseRequestAuthorization`

Authenticate with the Knish.IO node and receive an authorization token.

**Parameters:**
- `secret`: 2048-character secret for wallet generation
- `cellSlug`: Optional cell slug for cell-specific authentication

**Returns:** `ResponseRequestAuthorization` containing auth token and wallet information

**Example:**
```kotlin
val response = client.requestAuthToken(mySecret)
val token = response.payload()?.token
```

#### Wallet Operations

##### `queryBalance(token: String, bundleHash: String? = null): ResponseBalance`

Query the balance of a wallet for a specific token.

**Parameters:**
- `token`: Token slug to query (e.g., "USER", "MTK")
- `bundleHash`: Optional bundle hash to query specific wallet bundle

**Returns:** `ResponseBalance` containing balance and wallet information

##### `queryWallets(bundleHash: String? = null, token: String? = null): ResponseWalletList`

List all wallets in a bundle or for a specific token.

**Parameters:**
- `bundleHash`: Optional bundle hash filter
- `token`: Optional token slug filter

**Returns:** `ResponseWalletList` containing array of wallets

##### `createWallet(token: String): ResponseProposeMolecule`

Declare a new wallet on the ledger for a specific token.

**Parameters:**
- `token`: Token slug for the new wallet

**Returns:** `ResponseProposeMolecule` with wallet creation confirmation

#### Token Operations

##### `createToken(token: String, amount: Number, meta: MutableList<MetaData>): ResponseProposeMolecule`

Issue a new token type on the ledger.

**Parameters:**
- `token`: Unique token slug (3-16 characters)
- `amount`: Initial supply amount
- `meta`: Token metadata including name, fungibility, supply type, decimals

**Returns:** `ResponseProposeMolecule` with token creation confirmation

**Required Metadata:**
- `name`: Human-readable token name
- `fungibility`: "fungible" or "nonfungible"
- `supply`: "limited" or "replenishable"
- `decimals`: Number of decimal places (0-18)

##### `transferToken(recipient: Wallet, token: String, amount: Number): ResponseProposeMolecule`

Transfer tokens from source wallet to recipient.

**Parameters:**
- `recipient`: Recipient wallet object
- `token`: Token slug to transfer
- `amount`: Amount to transfer

**Returns:** `ResponseProposeMolecule` with transfer confirmation

#### Metadata Operations

##### `createMeta(metaType: String, metaId: String, meta: MutableList<MetaData>): ResponseProposeMolecule`

Store metadata on the ledger.

**Parameters:**
- `metaType`: Category/type of metadata
- `metaId`: Unique identifier within the type
- `meta`: Key-value pairs to store

**Returns:** `ResponseProposeMolecule` with storage confirmation

##### `queryMeta(metaType: String?, metaIds: List<String>?, keys: List<String>?, values: List<String>?): ResponseMetaType`

Query stored metadata with various filters.

**Parameters:**
- `metaType`: Optional type filter
- `metaIds`: Optional list of specific IDs
- `keys`: Optional list of keys to filter
- `values`: Optional list of values to search

**Returns:** `ResponseMetaType` containing matching metadata

#### Advanced Operations

##### `proposeMolecule(molecule: Molecule): ResponseProposeMolecule`

Submit a custom molecule directly to the ledger.

**Parameters:**
- `molecule`: Pre-built and signed molecule

**Returns:** `ResponseProposeMolecule` with submission confirmation

##### `claimShadowWallet(token: String, walletAddress: String, molecules: List<Molecule>? = null): ResponseProposeMolecule`

Claim ownership of a shadow wallet.

**Parameters:**
- `token`: Token slug
- `walletAddress`: Address of shadow wallet to claim
- `molecules`: Optional batch of molecules

**Returns:** `ResponseProposeMolecule` with claim confirmation

## Wallet

Represents a Knish.IO wallet with cryptographic keys and position tracking.

### Constructor

```kotlin
class Wallet(
    secret: String,
    token: String,
    position: String? = null,
    characters: String? = null,
    batchId: String? = null,
    trihash: String? = null
)
```

**Parameters:**
- `secret`: 2048-character secret for key generation
- `token`: Token slug this wallet manages
- `position`: ContinuID position (auto-generated if null)
- `characters`: Character set for encoding (default: hexadecimal)
- `batchId`: Batch identifier for grouped operations
- `trihash`: Triple hash for additional security

### Properties

- `address`: Wallet address (public key hash)
- `bundle`: Bundle hash for wallet grouping
- `position`: Current ContinuID position
- `key`: Private key for signing
- `token`: Associated token slug
- `balance`: Current balance (when queried)
- `molecules`: Associated molecules

### Static Methods

#### `generatePrivateKey(secret: String, token: String, position: String): String`

Generate a private key for signing operations.

#### `generateBundleHash(secret: String): String`

Generate bundle hash from secret.

## Molecule

Container for atomic operations that are processed together.

### Constructor

```kotlin
class Molecule(
    secret: String? = null,
    sourceWallet: Wallet? = null,
    remainderWallet: Wallet? = null,
    cellSlug: String? = null
)
```

**Parameters:**
- `secret`: Secret for signing (if not using sourceWallet)
- `sourceWallet`: Source wallet for operations
- `remainderWallet`: Wallet for handling remainders
- `cellSlug`: Target cell for the molecule

### Methods

#### `addAtom(atom: Atom): Molecule`

Add an atom to the molecule.

#### `sign(): Molecule`

Sign the molecule with WOTS+ signature.

#### `check(): Boolean`

Validate molecular integrity and signatures.

### Properties

- `molecularHash`: Computed hash of all atoms
- `bundle`: Bundle hash
- `status`: Processing status
- `createdAt`: Creation timestamp
- `atoms`: List of atoms in the molecule

## Atom

Smallest unit of operation in a molecule.

### Constructor

```kotlin
class Atom(
    position: String,
    walletAddress: String,
    isotope: Char,
    token: String? = null,
    value: String? = null,
    batchId: String? = null,
    metaType: String? = null,
    metaId: String? = null,
    meta: MutableList<MetaData>? = null,
    index: Int? = null,
    createdAt: String? = null
)
```

**Parameters:**
- `position`: ContinuID position
- `walletAddress`: Wallet performing the operation
- `isotope`: Operation type (C, V, M, U, I)
- `token`: Token slug (for value operations)
- `value`: Amount (negative for outgoing, positive for incoming)
- `batchId`: Batch identifier
- `metaType`: Metadata type
- `metaId`: Metadata identifier
- `meta`: Key-value metadata
- `index`: Atom index in molecule
- `createdAt`: Timestamp

### Isotope Types

- **C**: ContinuID - Identity and position management
- **V**: Value - Token transfers and balance operations
- **M**: Metadata - Store and update metadata
- **U**: Unit - Token definition and properties
- **I**: Instance - Batch and instance operations

## Query Methods

### Available Queries

- `QueryActiveSession`: Get active session information
- `QueryBalance`: Query wallet balance
- `QueryBatch`: Query batch information
- `QueryBatchHistory`: Get batch operation history
- `QueryContinuId`: Validate ContinuID positions
- `QueryMetaType`: Query metadata types and instances
- `QueryUserActivity`: Get user activity logs
- `QueryWalletBundle`: Query wallet bundle information
- `QueryWalletList`: List wallets with filters

## Mutation Methods

### Available Mutations

- `MutationRequestAuthorization`: Authenticate and get token
- `MutationProposeMolecule`: Submit molecule to ledger
- `MutationCreateToken`: Issue new token type
- `MutationCreateWallet`: Declare new wallet
- `MutationTransferTokens`: Transfer tokens between wallets
- `MutationCreateMeta`: Store metadata
- `MutationClaimShadowWallet`: Claim shadow wallet
- `MutationCreateIdentifier`: Create unique identifier

## Cryptographic Functions

### Crypto Object Methods

#### `generateBundleHash(secret: String): String`

Generate bundle hash from secret using SHAKE256.

#### `generateWalletAddress(key: String): String`

Generate wallet address from public key.

#### `signWOTS(message: String, privateKey: String): String`

Sign message with WOTS+ quantum-resistant signature.

#### `verifyWOTS(message: String, signature: String, address: String): Boolean`

Verify WOTS+ signature against wallet address.

#### `encryptMLKEM(message: String, publicKey: String): String`

Encrypt with ML-KEM768 post-quantum encryption.

#### `decryptMLKEM(ciphertext: String, privateKey: String): String`

Decrypt ML-KEM768 encrypted message.

#### `generateMolecularHash(molecule: Molecule): String`

Compute molecular hash for all atoms.

## Exception Types

### Authentication Exceptions

- `UnauthenticatedException`: Authentication required or failed
- `CodeException`: Invalid authorization code

### Balance Exceptions

- `BalanceInsufficientException`: Insufficient balance for operation
- `TransferBalanceException`: Transfer validation failed
- `TransferRemainderException`: Invalid remainder calculation

### Validation Exceptions

- `MolecularHashMismatchException`: Molecular hash verification failed
- `SignatureMismatchException`: Signature verification failed
- `SignatureMalformedException`: Invalid signature format
- `AtomIndexException`: Invalid atom indexing
- `AtomsMissingException`: Required atoms missing

### Token Exceptions

- `WrongTokenTypeException`: Invalid token type for operation
- `NegativeAmountException`: Negative amount where positive required
- `StackableUnitAmountException`: Invalid amount for stackable tokens
- `StackableUnitDecimalsException`: Decimal mismatch

### Transfer Exceptions

- `TransferMalformedException`: Invalid transfer structure
- `TransferMismatchedException`: Transfer validation failed
- `TransferToSelfException`: Cannot transfer to same wallet

### Metadata Exceptions

- `MetaMissingException`: Required metadata missing
- `BatchIdException`: Invalid batch identifier

### General Exceptions

- `InvalidResponseException`: Invalid node response
- `BaseException`: Base class for all SDK exceptions

## Response Objects

All API responses implement the `IResponse` interface:

```kotlin
interface IResponse<T> {
    fun json(): String
    fun payload(): T?
    fun success(): Boolean
    fun status(): String?
    fun reason(): String?
    fun exception(): BaseException?
}
```

## Best Practices

1. **Error Handling**: Always check `response.success()` before accessing payload
2. **Secret Management**: Never hardcode secrets, use secure storage
3. **Batch Operations**: Use molecules for atomic multi-operation transactions
4. **Cell Isolation**: Use cells for application-specific sub-ledgers
5. **Position Management**: Let SDK handle ContinuID positions automatically
6. **Quantum Security**: All operations use post-quantum cryptography by default

## Version Compatibility

This API reference is for SDK version 1.0.0-RC1 and is compatible with:
- Knish.IO Node API v2.0+
- JavaScript SDK v2.0+
- PHP SDK v2.0+

For the latest updates, see the [GitHub repository](https://github.com/WishKnish/KnishIO-Client-Kotlin).