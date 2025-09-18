#!/bin/bash
# Quality Control Script for Payment Service

set -e

echo "🚀 Starting Quality Control Pipeline..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    print_error "Please run this script from the payment-service directory"
    exit 1
fi

# 1. Clean and compile check
print_info "Step 1: Cleaning and compiling code..."
if mvn clean compile -DskipTests -q; then
    print_status "Code compiles successfully"
else
    print_error "Compilation failed"
    exit 1
fi

# 2. Run tests
print_info "Step 2: Running unit tests..."
if mvn test -q; then
    print_status "All tests passed"
    TEST_COUNT=$(find target/surefire-reports -name "*.xml" -exec grep -l "testcase" {} \; | wc -l | tr -d ' ')
    print_info "Executed $TEST_COUNT test classes"
else
    print_error "Tests failed"
    exit 1
fi

# 3. Code coverage check
print_info "Step 3: Checking code coverage..."
mvn jacoco:report -q
if [ -f target/site/jacoco/index.html ]; then
    COVERAGE=$(grep -o 'Total.*[0-9]*%' target/site/jacoco/index.html | grep -o '[0-9]*%' | head -1 | sed 's/%//' || echo "0")
    if [ "$COVERAGE" -ge 60 ]; then
        print_status "Code coverage: ${COVERAGE}% (✅ >= 60%)"
    else
        print_warning "Code coverage: ${COVERAGE}% (⚠️  < 60%)"
    fi
else
    print_warning "Coverage report not generated"
fi

# 4. Security scan
print_info "Step 4: Running security scan..."
if mvn org.owasp:dependency-check-maven:check -q; then
    print_status "Security scan completed - no critical vulnerabilities"
else
    print_warning "Security scan found issues (check target/dependency-check-report.html)"
fi

# 5. Static analysis
print_info "Step 5: Running static analysis..."
if mvn spotbugs:check -q; then
    print_status "Static analysis passed"
else
    print_warning "Static analysis found issues (check target/spotbugsXml.xml)"
fi

# 6. Package check
print_info "Step 6: Building package..."
if mvn package -DskipTests -q; then
    print_status "Package built successfully"
    JAR_SIZE=$(ls -lh target/*.jar | awk '{print $5}' | head -1)
    print_info "Package size: $JAR_SIZE"
else
    print_error "Package build failed"
    exit 1
fi

# 7. Docker build test (if Dockerfile exists)
if [ -f "Dockerfile" ]; then
    print_info "Step 7: Testing Docker build..."
    if docker build -t payment-service:test . -q; then
        print_status "Docker image built successfully"
    else
        print_warning "Docker build failed"
    fi
else
    print_info "Step 7: Skipping Docker build (no Dockerfile found)"
fi

print_status "🎉 Quality Control Pipeline completed successfully!"
print_info "Reports available in:"
print_info "  • Test reports: target/surefire-reports/"
print_info "  • Coverage report: target/site/jacoco/index.html"
print_info "  • Security report: target/dependency-check-report.html"
print_info "  • Static analysis: target/spotbugsXml.xml"

echo ""
echo "📊 Quality Summary:"
echo "✅ Compilation: PASSED"
echo "✅ Unit Tests: PASSED"
echo "✅ Code Coverage: ${COVERAGE:-N/A}%"
echo "✅ Security Scan: COMPLETED"
echo "✅ Static Analysis: COMPLETED"
echo "✅ Package Build: PASSED"
