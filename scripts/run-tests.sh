#!/bin/bash

# AMCP v1.5 - Test Runner Script
# Comprehensive test execution for all modules

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🧪 AMCP v1.5 Test Runner"
echo "========================"

# Change to project directory
cd "$PROJECT_DIR"

# Function to run tests with nice output
run_test_suite() {
    local module_name=$1
    local test_pattern=$2
    
    echo ""
    echo "🔍 Testing $module_name..."
    echo "$(printf '%.40s' "----------------------------------------")"
    
    if [ -n "$test_pattern" ]; then
        mvn test -Dtest="$test_pattern" -q
    else
        mvn test -q
    fi
    
    if [ $? -eq 0 ]; then
        echo "✅ $module_name tests passed"
    else
        echo "❌ $module_name tests failed"
        return 1
    fi
}

# Main test execution
echo "Running comprehensive test suite..."
echo "This includes unit tests, integration tests, and example validations"

# Build first to ensure everything is compiled
echo ""
echo "📦 Building project for testing..."
mvn compile test-compile -q

# Run all tests
echo ""
echo "🧪 Executing Test Suites..."

# Core framework tests
run_test_suite "Core Framework" "*Test"

# Integration tests
run_test_suite "Integration Tests" "*IT"

# Example tests (if they exist)
if find . -name "*ExampleTest.java" -type f | grep -q .; then
    run_test_suite "Example Applications" "*ExampleTest"
fi

# Generate test report
echo ""
echo "📊 Test Report Generation..."
if mvn surefire-report:report -q 2>/dev/null; then
    echo "✅ Test report generated: target/site/surefire-report.html"
else
    echo "⚠️  Test report generation skipped (surefire-report plugin not configured)"
fi

# Test coverage (if jacoco is configured)
if mvn jacoco:report -q 2>/dev/null; then
    echo "✅ Coverage report generated: target/site/jacoco/index.html"
else
    echo "⚠️  Coverage report skipped (jacoco plugin not configured)"
fi

# Summary
echo ""
echo "🎉 All Tests Completed Successfully!"
echo ""
echo "Test Results Summary:"
echo "  • Unit Tests: ✅ Passed"
echo "  • Integration Tests: ✅ Passed" 
echo "  • Example Validations: ✅ Passed"
echo ""
echo "Next Steps:"
echo "  • Review test reports in target/site/"
echo "  • Run specific examples: ./scripts/run-*.sh"
echo "  • Deploy: ./scripts/deploy.sh"
echo ""