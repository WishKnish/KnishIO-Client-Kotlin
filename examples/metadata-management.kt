package examples

import wishKnish.knishIO.client.KnishIOClient
import wishKnish.knishIO.client.data.MetaData
import wishKnish.knishIO.client.libraries.Strings
import java.net.URI

/**
 * Metadata Management Example
 * 
 * This example demonstrates how to store, query, and manage metadata on the Knish.IO ledger.
 * Metadata allows you to store arbitrary key-value pairs associated with your application.
 */
fun main() {
    // Initialize client
    val nodeUri = System.getenv("KNISHIO_NODE_URI") ?: "https://node.wishknish.com/graphql"
    val client = KnishIOClient(
        nodeUris = listOf(URI(nodeUri)),
        encrypt = true
    )
    
    val secret = System.getenv("KNISHIO_SECRET") ?: Strings.generateSecret()
    
    println("=== Knish.IO Metadata Management Demo ===")
    println()
    
    try {
        // Step 1: Authenticate
        println("1. Authenticating...")
        val authResponse = client.requestAuthToken(secret)
        if (!authResponse.success()) {
            println("✗ Authentication failed: ${authResponse.reason()}")
            return
        }
        println("✓ Authenticated successfully")
        println()
        
        // Step 2: Store user profile metadata
        println("2. Storing user profile metadata...")
        
        val profileMeta = mutableListOf(
            MetaData("username", "john_doe"),
            MetaData("email", "john@example.com"),
            MetaData("firstName", "John"),
            MetaData("lastName", "Doe"),
            MetaData("avatar", "https://example.com/avatar.jpg"),
            MetaData("bio", "Blockchain enthusiast and developer"),
            MetaData("joined", System.currentTimeMillis().toString())
        )
        
        val profileResponse = client.createMeta(
            metaType = "UserProfile",
            metaId = "user_${System.currentTimeMillis()}",
            meta = profileMeta
        )
        
        val profileId = if (profileResponse.success()) {
            val metaId = "user_${System.currentTimeMillis()}"
            println("✓ User profile stored!")
            println("  Type: UserProfile")
            println("  ID: $metaId")
            println("  Molecular hash: ${profileResponse.payload()?.molecularHash}")
            metaId
        } else {
            println("✗ Failed to store profile: ${profileResponse.reason()}")
            null
        }
        println()
        
        // Step 3: Store product metadata
        println("3. Storing product catalog metadata...")
        
        val products = listOf(
            Triple("PROD001", "Laptop Pro", "2499.99"),
            Triple("PROD002", "Wireless Mouse", "29.99"),
            Triple("PROD003", "USB-C Hub", "49.99")
        )
        
        products.forEach { (sku, name, price) ->
            val productMeta = mutableListOf(
                MetaData("name", name),
                MetaData("sku", sku),
                MetaData("price", price),
                MetaData("currency", "USD"),
                MetaData("category", "Electronics"),
                MetaData("inStock", "true"),
                MetaData("description", "High-quality $name"),
                MetaData("manufacturer", "TechCorp"),
                MetaData("warranty", "12 months")
            )
            
            val productResponse = client.createMeta(
                metaType = "Product",
                metaId = sku,
                meta = productMeta
            )
            
            if (productResponse.success()) {
                println("  ✓ Product $sku: $name - $$price")
            }
        }
        println()
        
        // Step 4: Store vehicle registration metadata
        println("4. Storing vehicle registration...")
        
        val vehicleMeta = mutableListOf(
            MetaData("type", "vehicle"),
            MetaData("make", "Tesla"),
            MetaData("model", "Model 3"),
            MetaData("year", "2024"),
            MetaData("vin", "5YJ3E1EA1JF000001"),
            MetaData("color", "Pearl White"),
            MetaData("licensePlate", "EV-2024"),
            MetaData("owner", "John Doe"),
            MetaData("registeredDate", System.currentTimeMillis().toString())
        )
        
        val vehicleResponse = client.createMeta(
            metaType = "VehicleRegistration",
            metaId = "5YJ3E1EA1JF000001",
            meta = vehicleMeta
        )
        
        if (vehicleResponse.success()) {
            println("✓ Vehicle registration stored!")
            println("  VIN: 5YJ3E1EA1JF000001")
            println()
        }
        
        // Step 5: Query metadata by type
        println("5. Querying all products...")
        
        val productsQuery = client.queryMeta(
            metaType = "Product",
            metaIds = null,
            keys = null,
            values = null
        )
        
        if (productsQuery.success()) {
            val products = productsQuery.payload()?.instances ?: emptyList()
            println("✓ Found ${products.size} product(s):")
            
            products.forEach { product ->
                val name = product.metas?.find { it.key == "name" }?.value
                val price = product.metas?.find { it.key == "price" }?.value
                println("  - $name: $$price")
            }
            println()
        }
        
        // Step 6: Query metadata by specific keys
        println("6. Searching products by price range...")
        
        val priceQuery = client.queryMeta(
            metaType = "Product",
            keys = listOf("price"),
            values = null  // Would filter by specific values if provided
        )
        
        if (priceQuery.success()) {
            val products = priceQuery.payload()?.instances ?: emptyList()
            products.forEach { product ->
                val name = product.metas?.find { it.key == "name" }?.value
                val price = product.metas?.find { it.key == "price" }?.value?.toDoubleOrNull() ?: 0.0
                
                if (price < 100) {
                    println("  ✓ Affordable: $name - $$price")
                }
            }
            println()
        }
        
        // Step 7: Query metadata by specific ID
        if (profileId != null) {
            println("7. Retrieving specific user profile...")
            
            val userQuery = client.queryMeta(
                metaType = "UserProfile",
                metaIds = listOf(profileId)
            )
            
            if (userQuery.success()) {
                val profiles = userQuery.payload()?.instances ?: emptyList()
                if (profiles.isNotEmpty()) {
                    val profile = profiles.first()
                    println("✓ User Profile Found:")
                    profile.metas?.forEach { meta ->
                        println("  ${meta.key}: ${meta.value}")
                    }
                    println()
                }
            }
        }
        
        // Step 8: Store application settings
        println("8. Storing application settings...")
        
        val settingsMeta = mutableListOf(
            MetaData("theme", "dark"),
            MetaData("language", "en"),
            MetaData("notifications", "enabled"),
            MetaData("autoSave", "true"),
            MetaData("syncInterval", "300"),
            MetaData("debugMode", "false")
        )
        
        val settingsResponse = client.createMeta(
            metaType = "AppSettings",
            metaId = "settings_${client.getBundle()}",
            meta = settingsMeta
        )
        
        if (settingsResponse.success()) {
            println("✓ Application settings stored")
            println()
        }
        
        // Step 9: Store IoT sensor data
        println("9. Storing IoT sensor readings...")
        
        val sensorMeta = mutableListOf(
            MetaData("sensorId", "TEMP-001"),
            MetaData("location", "Living Room"),
            MetaData("temperature", "22.5"),
            MetaData("humidity", "45"),
            MetaData("pressure", "1013.25"),
            MetaData("timestamp", System.currentTimeMillis().toString()),
            MetaData("unit", "celsius"),
            MetaData("status", "active")
        )
        
        val sensorResponse = client.createMeta(
            metaType = "SensorReading",
            metaId = "reading_${System.currentTimeMillis()}",
            meta = sensorMeta
        )
        
        if (sensorResponse.success()) {
            println("✓ Sensor data recorded")
            println("  Temperature: 22.5°C")
            println("  Humidity: 45%")
            println()
        }
        
        // Step 10: Query metadata with complex filters
        println("10. Performing complex metadata queries...")
        
        // Find all active sensors
        val activeSensors = client.queryMeta(
            metaType = "SensorReading",
            keys = listOf("status"),
            values = listOf("active")
        )
        
        if (activeSensors.success()) {
            val readings = activeSensors.payload()?.instances ?: emptyList()
            println("✓ Found ${readings.size} active sensor(s)")
        }
        
        // Search vehicles by VIN
        val vehicleSearch = client.queryMeta(
            metaType = "VehicleRegistration",
            keys = listOf("vin"),
            values = listOf("5YJ3E1EA1JF000001")
        )
        
        if (vehicleSearch.success()) {
            val vehicles = vehicleSearch.payload()?.instances ?: emptyList()
            if (vehicles.isNotEmpty()) {
                println("✓ Vehicle found by VIN search")
            }
        }
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
    
    println()
    println("=== Metadata Management Demo Complete ===")
    println()
    println("Summary:")
    println("- Metadata provides flexible key-value storage")
    println("- Use metaType to categorize data")
    println("- Use metaId for unique identification")
    println("- Query by type, ID, keys, or values")
    println("- Perfect for application data, settings, and records")
}