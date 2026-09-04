# Changelog

All notable changes to the KnishIO Client Kotlin SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Releases are published to Maven Central (`io.knish:knishio-client-kotlin`) from a
`v`-prefixed git tag — this repo is the one exception to the ecosystem's bare-tag
convention, because its publish job triggers on `refs/tags/v*`.
Conventions for tags, commits, and these entries: `docs/SDK-RELEASE-CONVENTIONS.md`
in the KnishIOClientSDK monorepo.

Entries above `0.8.0` were backfilled on 2026-07-27 from the repository's own tag
and commit history rather than written at release time; where the history does
not substantiate a detail, the entry says so instead of guessing.

## [Unreleased]

## [0.9.4] — 2026-09-04

### Added

- **Hardware Envelope Encryption & Secure Memory Provider**: Introduced `SecretStorageProvider`,
  `SecretStorageMetadata`, `EncryptedSecretPayload`, `StorageOptions`, and `StorageBackend` contracts
  (`wishKnish.knishIO.client.storage`).
- **AES-GCM Envelope Encryption Provider** (`AesGcmSecretStorageProvider`): Standard JCA
  `AES/GCM/NoPadding` envelope encryption with PBKDF2WithHmacSHA256 (100,000 iterations) key
  derivation, 12-byte random IV, 16-byte random salt, pluggable `StorageBackend` (supporting
  Android `SharedPreferences` / `EncryptedSharedPreferences`), and auto-zeroized byte buffers.
- **In-Memory Storage Provider** (`MemorySecretStorageProvider`): Thread-safe in-memory fallback
  using `ConcurrentHashMap` for test harnesses and headless environments.
- **Memory Hygiene & Zeroization Utilities** (`SecureMemory`): Explicit byte/char array clearing
  (`zeroize`), scoped execution (`withSecureBytes`, `withSecureChars`), and timing-safe comparison
  (`constantTimeEquals`).
- **SecretStorageFactory**: Factory utility for instantiating default storage providers.
- **KnishIOClient Secret Storage Integration**: `KnishIOClient` accepts `secretStorage` in constructor,
  provides `setSecretStorage()`, `getSecretStorage()`, and `retrieveSecret()`, and unwraps the master secret
  just-in-time for molecule construction (`createMolecule()`) without permanently retaining cleartext
  secrets in client heap memory.
- **SecretStorageException**: Typed exception extending `BaseException` with companion factory methods
  `notFound()`, `decryptionFailed()`, and `unavailable()`.

## [0.9.3] — 2026-08-05

### Security

- The JitPack `tweetnacl-java` dependency was replaced with a BouncyCastle NaCl
  reimplementation, removing an unpinned JitPack-sourced artifact from the
  dependency graph.

### Changed

- Version currency: Kotlin 2.2.21, coroutines and serialization 1.11.0.
- Dokka migrated v1 → v2 (Gradle 8.13, vanniktech 0.36), which drops the old
  Jackson transitive.

### Changed — cross-SDK gauntlet reporting integrity

- The self-test now publishes cross-validation **coverage**, not just a verdict:
  `crossValidation.{ran,targetsExpected,targetsValidated}` and `runId` sit alongside
  `crossSdkCompatible` in the results file. The boolean alone could not distinguish
  "validated every peer, all passed" from "validated nothing and so found no failures".
- `crossSdkCompatible` now defaults to **false** and must be earned. It was `true`, so every early return out of cross-validation published a pass.
- Cross-validation **fails** instead of reporting "compatible" when the shared results
  directory is missing or holds no peer results. Absence of evidence is not evidence of
  compatibility.
- Round 1 no longer asserts a cross-SDK verdict it cannot have; it records that no
  cross-validation ran.
- A coverage floor is required before a pass: every expected peer must have been validated,
  in addition to no individual check having failed.
- Each peer is now checked for all 7 required molecule types. The validation loop iterates
  the molecule keys that are **present**, so an omitted molecule was indistinguishable from
  a validated one.
- Peer results are matched with `*-results.json`. `endsWith(".json")` also matched the
  canonical vector **masters** living in that directory and fed them into the peer loop
  as though they were SDK results.
- The Round-1 exit code no longer requires `crossSdkCompatible`, a check Round 1 skips by
  design.

Contract for these fields: `sdks/canonical-test-keys.json` in the KnishIOClientSDK
monorepo. Audit: `docs/audits/REPORTING-INTEGRITY-2026-08-05.md`.

## [0.9.2] — 2026-07-12

Coordinated dependency-security release across all 8 SDKs. Release record:
`docs/sdk-release-0.9.2-execution-2026-07-12.md` (monorepo).

### Security

- BouncyCastle 1.85, Ktor 3.5.1, GraalVM 25.1.3, graphql-java 26.

### Added

- Dependency-audit CI job: a runtime-scoped CycloneDX SBOM scanned with
  OSV-Scanner. The action must be pinned to an exact version
  (`google/osv-scanner-action@v2.3.8`) — upstream publishes no floating major
  tag, so `@v2` does not resolve.

### Fixed

- The Shadow fat jar is excluded from the Maven Central publication; it had
  inflated the published artifact set.

### Notes

- `0.9.1` was staged in `build.gradle.kts` on 2026-06-30 (a clear error when a
  node advertises a non-ML-KEM recipient key) but was never tagged and never
  published to Maven Central. That fix ships in `0.9.2`.

## [0.9.0] — 2026-06-29

Coordinated `0.9.0` across all 8 SDKs, marking the post-quantum ML-KEM transport
milestone. Runbook: `docs/sdk-release-audit-2026-06-29.md` (monorepo).

### Added

- **ML-KEM768 CipherHash encrypted transport** (PQ Phase E): the transport
  migrated from classical NaCl to ML-KEM768 on the request side, and the source
  wallet's ML-KEM public key is conveyed at auth to complete the round trip.
- Buffer family (B-isotope): `Molecule.initDepositBuffer` and
  `Molecule.initWithdrawBuffer`, client-level `depositBufferToken` and
  `withdrawBufferToken`, locked by the `buffer_deposit_conservation` and
  `buffer_withdraw_conservation` vectors.
- Multi-recipient stackable (NFT) transfer builder.
- `mlkem768` keygen + decrypt vector; canonical patent vectors re-synced to the
  shared master.
- A detekt lint gate plus a CI lint job.

### Fixed

- The CipherHash response is decoded under its PascalCase GraphQL key.
- Wallet positions are random, mirroring JS/C/C++ — `createToken` is accepted
  live.
- ContinuID resolution no longer throws (token argument and `Data` field name),
  and `Response.errors` is deserialized.
- The live HTTPS path works (OkHttp/TLS 1.3 plus 7 live-path fixes).
- `TokenUnit.metas` deserializes tolerantly, so stackable `tokenUnits` can be
  read; `Wallet.splitUnits` no longer loses the kept units.
- `burnToken` rebuilt as the JS-canonical 3-atom zero-sum molecule;
  `burnTokens` uses the resolved signing wallet instead of dereferencing a null
  source.
- `isotopeV` B/F-isotope bypass corrected.
- The USER ContinuID I-atom is registered on auth.
- `initWalletCreation`, `initShadowWalletClaim`, and the `initTokenCreation`
  C-atom metadata reconciled to the JS canonical form (adds `setMetaWallet`).
- `queryBalance` bundle handling fixed.

### Removed

- Dead `QueryUserActivity` query, the dead `NobleMLKEMBridge` wrappers, and the
  divergent `Double` `initTokenCreation` overload (a V+M footgun). The
  vestigial `token` parameter was dropped from `initShadowWalletClaim`.
- `PostQuantumCrypto` deprecated in favour of the designated canonical ML-KEM
  envelope.

### Notes

- Local version `0.8.2` was staged on 2026-06-15 (the `initTokenCreation` C-atom
  metadata fix) but was never tagged or published; it reaches consumers here.

## [0.8.1] — 2026-06-15

### Fixed

- ContinuID I-atom metadata is populated and timestamps are fixed, completing
  full 8-SDK molecular-hash parity.

### Changed

- README notes that network reads are fresh by construction (no response cache).

## [0.8.0] - 2026-06-05

### Fixed
- Atom value serialization now emits an integer string for whole-number values
  (e.g. `-1000` instead of `-1000.0`). The validator parses V/B/F values as
  integers, so the previous `Double.toString()` output (`-1000.0`) was rejected —
  this restores cross-SDK value-transfer acceptance (parity with JS/TS/PHP/Rust).

### Changed
- First release published to Maven Central via the Sonatype Central Portal
  (the legacy OSSRH endpoint was decommissioned). Build migrated to the
  Vanniktech Gradle Maven Publish plugin.
- Corrected the published POM license to GPL-3.0-or-later (matches the bundled
  `LICENSE` and the rest of the KnishIO SDK ecosystem).

## [1.0.0-RC1] - 2025-08-07

### Added
- Full cross-platform compatibility with JavaScript client
- Post-quantum cryptography support:
  - XMSS (eXtended Merkle Signature Scheme) signatures
  - ML-KEM768 (Kyber) quantum-resistant encryption (NIST FIPS-203 compliant)
  - One-time signature mechanism preventing key reuse
- DAG-based distributed ledger architecture:
  - Organism-inspired transaction model (Atoms/Molecules/Cells)
  - Network-bound scalability
  - Asynchronous "pay-it-forward" consensus
- Comprehensive SDK features:
  - Full KnishIO GraphQL API support
  - Wallet management with ContinuID
  - Token creation and transfers
  - Metadata management
  - Batch operations
- NobleMLKEMBridge for JavaScript noble-post-quantum library compatibility
- Comprehensive test suite with cross-platform validation
- Maven Central and GitHub Packages publishing configuration
- JaCoCo test coverage reporting
- KDoc/Javadoc generation

### Fixed
- WOTS+ signature verification algorithm alignment
- Molecular hash generation for cross-platform compatibility
- Private key generation consistency between signing and wallet creation
- BigInteger normalization for cross-platform cryptographic operations

### Security
- Removed hardcoded secrets from example code
- Implemented secure defaults for all cryptographic operations
- Added quantum-resistant cryptographic primitives throughout

## [0.0.1] - 2024-01-01

### Added
- Initial implementation of KnishIO Client for Kotlin
- Basic wallet functionality
- GraphQL client implementation
- Token transfer capabilities
- Metadata support
- Basic cryptographic operations with TweetNaCl
- BouncyCastle integration

---

## Version History

- **1.0.0-RC1** - First release candidate with full post-blockchain DLT support
- **0.0.1** - Initial development version

## Upcoming

### [1.0.0] - Target: 2025-09-01
- Production release after RC feedback
- Performance optimizations
- Additional documentation and examples
- Extended test coverage

### [1.1.0] - Future
- Enhanced cellular architecture support
- Advanced cross-cell communication protocols
- Performance monitoring and metrics
- Extended GraphQL query optimizations

> **Note (2026-07-27):** the two sections above predate the coordinated `0.9.x`
> SDK version line and are retained as written. The `1.0.0` / `1.1.0` targets in
> "Upcoming" are stale and do not reflect current plans.

[Unreleased]: https://github.com/WishKnish/KnishIO-Client-Kotlin/compare/v0.9.4...HEAD
[0.9.4]: https://github.com/WishKnish/KnishIO-Client-Kotlin/releases/tag/v0.9.4
[0.9.3]: https://github.com/WishKnish/KnishIO-Client-Kotlin/releases/tag/v0.9.3
[0.9.2]: https://github.com/WishKnish/KnishIO-Client-Kotlin/releases/tag/v0.9.2
[0.9.0]: https://github.com/WishKnish/KnishIO-Client-Kotlin/releases/tag/v0.9.0
[0.8.1]: https://github.com/WishKnish/KnishIO-Client-Kotlin/releases/tag/v0.8.1
[0.8.0]: https://github.com/WishKnish/KnishIO-Client-Kotlin/releases/tag/v0.8.0