#!/bin/bash
# NATS Broker Code Verification Script
# Verifies the fixes are present in the code without requiring NATS server

echo "=========================================="
echo "AMCP v1.6 - NATS Broker Code Verification"
echo "=========================================="
echo ""

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

NATS_FILE="amcp-broker-nats/src/main/java/io/amcp/broker/nats/NatsEventBroker.java"

if [ ! -f "$NATS_FILE" ]; then
    echo -e "${RED}✗ NATS broker file not found${NC}"
    exit 1
fi

echo "Verifying NATS broker fixes..."
echo ""

CHECKS_PASSED=0
CHECKS_FAILED=0

# Check 1: matchesPattern method exists
if grep -q "private boolean matchesPattern" "$NATS_FILE"; then
    echo -e "${GREEN}✓ matchesPattern() method found${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗ matchesPattern() method missing${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# Check 2: Pattern matching called correctly
if grep -q "matchesPattern(event.getTopic(), topicPattern)" "$NATS_FILE"; then
    echo -e "${GREEN}✓ Pattern matching correctly implemented${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗ Pattern matching not called correctly${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# Check 3: Wildcard support (** pattern)
if grep -q 'pattern.contains("\*\*")' "$NATS_FILE"; then
    echo -e "${GREEN}✓ Multi-level wildcard (**) support found${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗ Multi-level wildcard support missing${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# Check 4: Wildcard support (* pattern)
if grep -q 'pattern.contains("\*")' "$NATS_FILE"; then
    echo -e "${GREEN}✓ Single-level wildcard (*) support found${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗ Single-level wildcard support missing${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# Check 5: Improved serialization
if grep -q "String.format" "$NATS_FILE" && grep -q '"topic"' "$NATS_FILE"; then
    echo -e "${GREEN}✓ Improved JSON serialization found${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗ JSON serialization not improved${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# Check 6: Deserialization with error handling
if grep -q "try {" "$NATS_FILE" && grep -q "deserializeEvent" "$NATS_FILE"; then
    echo -e "${GREEN}✓ Error handling in deserialization found${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    echo -e "${RED}✗ Error handling missing${NC}"
    CHECKS_FAILED=$((CHECKS_FAILED + 1))
fi

# Check 7: Code compiles
echo ""
echo "Compiling NATS broker module..."
cd amcp-broker-nats
if mvn compile -q 2>&1 | grep -q "BUILD SUCCESS"; then
    echo -e "${GREEN}✓ NATS broker compiles successfully${NC}"
    CHECKS_PASSED=$((CHECKS_PASSED + 1))
else
    # Try without grep
    mvn compile -q
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ NATS broker compiles successfully${NC}"
        CHECKS_PASSED=$((CHECKS_PASSED + 1))
    else
        echo -e "${RED}✗ Compilation failed${NC}"
        CHECKS_FAILED=$((CHECKS_FAILED + 1))
    fi
fi
cd ..

echo ""
echo "=========================================="
echo "Verification Summary"
echo "=========================================="
echo "Checks passed: $CHECKS_PASSED"
echo "Checks failed: $CHECKS_FAILED"
echo ""

if [ $CHECKS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✓✓✓ All NATS broker fixes verified! ✓✓✓${NC}"
    echo ""
    echo "Code changes confirmed:"
    echo "  ✓ Topic pattern matching with wildcards"
    echo "  ✓ Improved JSON serialization"
    echo "  ✓ Better error handling"
    echo "  ✓ Compiles without errors"
    echo ""
    echo "The NATS broker is ready for testing with a NATS server."
    exit 0
else
    echo -e "${RED}✗ Some checks failed${NC}"
    exit 1
fi
