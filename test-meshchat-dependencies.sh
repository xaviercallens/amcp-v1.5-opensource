#!/bin/bash

# Quick test script to verify MeshChat dependencies are working
# This script tests that Jackson ObjectMapper can be loaded properly

set -e

echo "Testing MeshChat dependencies..."

# Build classpath using Maven
echo "Building classpath with Maven..."
MAVEN_CLASSPATH=$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q 2>/dev/null | tail -n 1)

CLASSPATH="core/target/classes:examples/target/classes:connectors/target/classes"
if [ -n "$MAVEN_CLASSPATH" ]; then
    CLASSPATH="$CLASSPATH:$MAVEN_CLASSPATH"
fi

echo "Classpath built successfully"

# Test Java class loading
echo "Testing ObjectMapper class loading..."
java -cp "$CLASSPATH" -c "
import com.fasterxml.jackson.databind.ObjectMapper;
public class Test {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println(\"✅ Jackson ObjectMapper loaded successfully: \" + mapper.getClass().getName());
        System.out.println(\"✅ Jackson version: \" + mapper.version());
    }
}
" 2>/dev/null || {
    echo "❌ ObjectMapper test failed, trying alternative approach..."
    
    # Test with a simple class instantiation
    java -cp "$CLASSPATH" -e "
    try {
        Class.forName(\"com.fasterxml.jackson.databind.ObjectMapper\");
        System.out.println(\"✅ Jackson ObjectMapper class found in classpath\");
    } catch (ClassNotFoundException e) {
        System.out.println(\"❌ Jackson ObjectMapper not found: \" + e.getMessage());
        System.exit(1);
    }
    "
}

echo "✅ Dependencies test completed successfully"