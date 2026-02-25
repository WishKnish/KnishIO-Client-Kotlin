# Knish.IO Kotlin SDK Examples

This directory contains example code demonstrating various features of the Knish.IO Kotlin SDK. Each example is self-contained and can be run independently.

## Prerequisites

Before running the examples, ensure you have:

1. **JDK 8 or higher** installed
2. **Gradle** configured (the project includes Gradle wrapper)
3. **Node access** - either set up your own or use the demo node
4. **Environment variables** (optional):
   - `KNISHIO_NODE_URI` - GraphQL endpoint (default: demo node)
   - `KNISHIO_SECRET` - Your 2048-character secret
   - `KNISHIO_CELL` - Application cell slug

## Running Examples

From the project root directory:

```bash
# Run a specific example
./gradlew run -PmainClass=examples.BasicUsageKt

# Or compile and run directly
./gradlew build
java -cp build/libs/knishio-client-kotlin-*.jar examples.BasicUsageKt
```

## Available Examples

### 1. **basic-usage.kt** - Getting Started
The simplest introduction to the SDK covering:
- Client initialization
- Authentication
- Balance queries
- Wallet listing
- Bundle information

**Perfect for:** First-time users wanting to understand the basics.

### 2. **token-operations.kt** - Token Management
Comprehensive token operations including:
- Creating fungible tokens
- Creating non-fungible tokens (NFTs)
- Setting token properties (decimals, supply type)
- Token transfers
- Balance verification

**Perfect for:** Developers building token-based applications.

### 3. **metadata-management.kt** - Data Storage
Store and query arbitrary data on the ledger:
- User profiles
- Product catalogs
- Vehicle registrations
- IoT sensor data
- Application settings
- Complex queries with filters

**Perfect for:** Applications needing decentralized data storage.

### 4. **wallet-management.kt** - Wallet Operations
Advanced wallet management features:
- Wallet generation mechanics
- Multi-token wallets
- Bundle operations
- Shadow wallet claiming
- ContinuID position tracking
- Multi-bundle scenarios

**Perfect for:** Understanding wallet architecture and identity management.

### 5. **advanced-molecules.kt** - Low-Level Operations
Manual molecule construction for complex scenarios:
- Multi-party transfers
- Batch operations
- Atomic swaps
- Custom isotope combinations
- Direct molecule composition

**Perfect for:** Advanced users needing fine-grained control.

### 6. **complete-example.kt** - Full Feature Demo
Comprehensive demonstration of all SDK features:
- Complete workflow from authentication to transfers
- All major SDK operations
- Error handling patterns
- Production-ready code structure

**Perfect for:** Understanding how all features work together.

## Example Patterns

### Basic Pattern
```kotlin
// 1. Initialize client
val client = KnishIOClient(listOf(URI(nodeUri)))

// 2. Authenticate
val auth = client.requestAuthToken(secret)

// 3. Perform operations
val balance = client.queryBalance("USER")
```

### Environment Configuration
```kotlin
// Use environment variables with defaults
val nodeUri = System.getenv("KNISHIO_NODE_URI") 
    ?: "https://node.wishknish.com/graphql"
    
val secret = System.getenv("KNISHIO_SECRET") 
    ?: generateDemoSecret()
```

### Error Handling
```kotlin
val response = client.someOperation()
if (response.success()) {
    // Handle success
    val data = response.payload()
} else {
    // Handle failure
    println("Failed: ${response.reason()}")
}
```

## Security Notes

⚠️ **Important Security Considerations:**

1. **Never hardcode secrets** in production code
2. **Use secure key management** systems
3. **Enable encryption** for node communication
4. **Validate all inputs** before ledger operations
5. **Handle errors gracefully** without exposing sensitive data

## Demo vs Production

These examples use demo patterns for simplicity:

**Demo Pattern:**
```kotlin
val secret = Strings.generateSecret() // Auto-generated
```

**Production Pattern:**
```kotlin
val secret = KeyVault.getSecret("knishio-secret") // From secure storage
```

## Troubleshooting

### Common Issues

1. **Authentication fails**
   - Check node URI is correct
   - Verify secret is 2048 characters
   - Ensure network connectivity

2. **Token creation fails**
   - Token slug might already exist
   - Check metadata requirements
   - Verify authentication succeeded

3. **Balance shows 0**
   - Wallet might not be declared
   - Token might not exist
   - Check correct bundle hash

### Debug Mode

Enable debug output:
```kotlin
val client = KnishIOClient(
    nodeUris = listOf(URI(nodeUri)),
    logging = true  // Enable debug logging
)
```

## Next Steps

After running these examples:

1. Read the [API Reference](../API_REFERENCE.md) for detailed documentation
2. Check the [main README](../README.md) for SDK features
3. Join the [Telegram community](https://t.me/wishknish) for support
4. Explore the [Technical Whitepaper](https://github.com/WishKnish/KnishIO-Technical-Whitepaper)

## Contributing

Have an example to share? We welcome contributions! See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## License

These examples are part of the Knish.IO Kotlin SDK and are licensed under the MIT License.