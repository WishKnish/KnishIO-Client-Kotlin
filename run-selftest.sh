#!/bin/bash

# KnishIO Kotlin SDK Self-Test Build and Run Script
# 
# This script builds the Kotlin SDK self-test and runs it,
# following the same pattern as other SDK self-test scripts.
# Uses Gradle task to manage dependencies automatically.

set -e  # Exit on any error

# Color codes for output  
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

echo -e "${BLUE}🏗️ Building KnishIO Kotlin SDK Self-Test...${NC}"

# Build the project first (following JavaScript SDK pattern)
echo -e "${YELLOW}📦 Building SDK with dependencies...${NC}"
./gradlew build -x test

# Run using gradle task (like npm run selftest or cargo run)
echo -e "${GREEN}✅ Build completed successfully${NC}"
echo -e "${BLUE}🚀 Running Kotlin SDK Self-Test (NobleMLKEM Bridge + ML-KEM768)...${NC}"
echo ""

# Use gradle task for proper dependency management
./gradlew --no-daemon selftest
TEST_EXIT_CODE=$?

# Report results
echo ""
if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo -e "${GREEN}🎉 Kotlin SDK Self-Test completed successfully!${NC}"
    echo -e "${CYAN}🔗 GraalVM Bridge: JavaScript Noble crypto compatibility${NC}"
    echo -e "${CYAN}🔒 ML-KEM768: Post-quantum encryption working${NC}"
    echo -e "${CYAN}⚡ Cross-SDK: All platform compatibility verified${NC}"
else
    echo -e "${RED}💥 Kotlin SDK Self-Test failed with exit code $TEST_EXIT_CODE${NC}"
fi

echo -e "${BLUE}📊 Results saved to ../shared-test-results/kotlin-results.json${NC}"

exit $TEST_EXIT_CODE
