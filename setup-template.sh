#!/bin/bash

# Payment Service Template Setup Script
# This script helps customize the template for your specific project

set -e

echo "🚀 Payment Service Template Setup"
echo "================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ] || [ ! -d "payment-service" ]; then
    print_error "Please run this script from the project root directory"
    exit 1
fi

# Get project information
echo ""
print_info "Let's customize your payment service template..."
echo ""

read -p "Enter your company/group ID (e.g., com.yourcompany): " GROUP_ID
read -p "Enter your artifact ID (e.g., payment-service): " ARTIFACT_ID
read -p "Enter your project name (e.g., Payment Service): " PROJECT_NAME
read -p "Enter your project description: " PROJECT_DESCRIPTION
read -p "Enter your name: " AUTHOR_NAME
read -p "Enter your email: " AUTHOR_EMAIL

# Validate inputs
if [ -z "$GROUP_ID" ] || [ -z "$ARTIFACT_ID" ] || [ -z "$PROJECT_NAME" ]; then
    print_error "Group ID, Artifact ID, and Project Name are required"
    exit 1
fi

echo ""
print_info "Customizing project files..."

# Update pom.xml
if [ -f "payment-service/pom.xml" ]; then
    sed -i.bak "s/<groupId>com.example<\/groupId>/<groupId>$GROUP_ID<\/groupId>/g" payment-service/pom.xml
    sed -i.bak "s/<artifactId>demo<\/artifactId>/<artifactId>$ARTIFACT_ID<\/artifactId>/g" payment-service/pom.xml
    sed -i.bak "s/<name>demo<\/name>/<name>$PROJECT_NAME<\/name>/g" payment-service/pom.xml
    sed -i.bak "s/<description>.*<\/description>/<description>$PROJECT_DESCRIPTION<\/description>/g" payment-service/pom.xml
    rm payment-service/pom.xml.bak
    print_status "Updated pom.xml"
fi

# Update package structure
if [ -d "payment-service/src/main/java/com/example/demo" ]; then
    NEW_PACKAGE_PATH=$(echo $GROUP_ID | tr '.' '/')
    mkdir -p "payment-service/src/main/java/$NEW_PACKAGE_PATH"
    mkdir -p "payment-service/src/test/java/$NEW_PACKAGE_PATH"
    
    # Move source files
    cp -r payment-service/src/main/java/com/example/demo/* "payment-service/src/main/java/$NEW_PACKAGE_PATH/"
    cp -r payment-service/src/test/java/com/example/demo/* "payment-service/src/test/java/$NEW_PACKAGE_PATH/"
    
    # Update package declarations in Java files
    find payment-service/src -name "*.java" -exec sed -i.bak "s/package com.example.demo/package $GROUP_ID/g" {} \;
    find payment-service/src -name "*.java" -exec sed -i.bak "s/import com.example.demo/import $GROUP_ID/g" {} \;
    find payment-service/src -name "*.java" -exec sed -i.bak "s/com.example.demo/$GROUP_ID/g" {} \;
    
    # Remove old package structure
    rm -rf payment-service/src/main/java/com
    rm -rf payment-service/src/test/java/com
    
    # Clean up backup files
    find payment-service/src -name "*.bak" -delete
    
    print_status "Updated package structure to $GROUP_ID"
fi

# Update application properties
if [ -f "payment-service/src/main/resources/application.properties" ]; then
    sed -i.bak "s/spring.application.name=demo/spring.application.name=$ARTIFACT_ID/g" payment-service/src/main/resources/application.properties
    rm payment-service/src/main/resources/application.properties.bak
    print_status "Updated application.properties"
fi

# Update README.md
if [ -f "README.md" ]; then
    sed -i.bak "s/Payment System - Non-Blocking Architecture/$PROJECT_NAME/g" README.md
    sed -i.bak "s/paymentswithkafka/$ARTIFACT_ID/g" README.md
    rm README.md.bak
    print_status "Updated README.md"
fi

# Update Dockerfile
if [ -f "payment-service/Dockerfile" ]; then
    sed -i.bak "s/demo/$ARTIFACT_ID/g" payment-service/Dockerfile
    rm payment-service/Dockerfile.bak
    print_status "Updated Dockerfile"
fi

# Update docker-compose.yml
if [ -f "docker-compose.yml" ]; then
    sed -i.bak "s/demo/$ARTIFACT_ID/g" docker-compose.yml
    rm docker-compose.yml.bak
    print_status "Updated docker-compose.yml"
fi

# Create .gitignore from template
if [ -f ".gitignore.template" ]; then
    cp .gitignore.template .gitignore
    print_status "Created .gitignore from template"
fi

# Create initial commit
echo ""
print_info "Creating initial commit..."

git add .
git commit -m "Initial commit: $PROJECT_NAME

- Spring Boot payment service with Kafka integration
- WebSocket real-time updates
- Comprehensive test suite
- Docker containerization
- Production-ready configuration

Customized for: $AUTHOR_NAME <$AUTHOR_EMAIL>"

print_status "Created initial commit"

echo ""
print_status "Template setup complete!"
echo ""
print_info "Next steps:"
echo "1. Review the customized files"
echo "2. Update any remaining hardcoded values"
echo "3. Run tests: cd payment-service && mvn test"
echo "4. Start development: docker-compose up -d && cd payment-service && mvn spring-boot:run"
echo ""
print_warning "Remember to update the frontend URLs if you change the service port!"
echo ""
print_status "Happy coding! 🚀"
