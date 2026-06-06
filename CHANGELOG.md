# Changelog

All notable changes to the KnishIO Client Kotlin SDK will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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