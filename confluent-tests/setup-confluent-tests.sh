#!/bin/bash

# setup-confluent-tests.sh
# Script để thiết lập và chạy Confluent compatibility tests

set -e  # Exit on error

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     Confluent Schema Registry Compatibility Tests        ║${NC}"
echo -e "${BLUE}║              Setup & Execution Script                     ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""

# Configuration
SCHEMA_REGISTRY_URL=${SCHEMA_REGISTRY_URL:-"http://localhost:8081"}
TEST_MODULE_DIR="confluent-compatibility-tests"

# Step 1: Check if Schema Registry is running
echo -e "${YELLOW}[1/6] Checking if Schema Registry is running...${NC}"
if curl -s -f "$SCHEMA_REGISTRY_URL/" > /dev/null; then
    echo -e "${GREEN}✓ Schema Registry is running at $SCHEMA_REGISTRY_URL${NC}"
else
    echo -e "${RED}✗ Cannot connect to Schema Registry at $SCHEMA_REGISTRY_URL${NC}"
    echo -e "${YELLOW}Please ensure your Schema Registry is running${NC}"
    echo ""
    echo "To start with Docker:"
    echo "  docker-compose up -d"
    echo ""
    exit 1
fi

# Step 2: Create test module directory
echo -e "${YELLOW}[2/6] Creating test module directory...${NC}"
mkdir -p "$TEST_MODULE_DIR"
cd "$TEST_MODULE_DIR"
echo -e "${GREEN}✓ Directory created: $TEST_MODULE_DIR${NC}"

# Step 3: Copy POM file
echo -e "${YELLOW}[3/6] Setting up Maven POM...${NC}"
if [ -f "../confluent-compatibility-tests-pom.xml" ]; then
    cp ../confluent-compatibility-tests-pom.xml pom.xml
    echo -e "${GREEN}✓ POM file copied${NC}"
else
    echo -e "${RED}✗ POM file not found: ../confluent-compatibility-tests-pom.xml${NC}"
    echo -e "${YELLOW}Please ensure confluent-compatibility-tests-pom.xml exists${NC}"
    exit 1
fi

# Step 4: Create test directory structure
echo -e "${YELLOW}[4/6] Creating test directory structure...${NC}"
mkdir -p src/test/java/io/confluent/kafka/schemaregistry/compatibility
mkdir -p src/test/resources

# Copy test files
if [ -f "../ConfluentRestApiCompatibilityTest.java" ]; then
    cp ../ConfluentRestApiCompatibilityTest.java \
       src/test/java/io/confluent/kafka/schemaregistry/compatibility/
    echo -e "${GREEN}✓ Test files copied${NC}"
else
    echo -e "${YELLOW}⚠ Test file not found, will need to be added manually${NC}"
fi

# Create test resources
cat > src/test/resources/logback-test.xml << 'EOF'
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
    
    <logger name="io.confluent" level="DEBUG"/>
    <logger name="org.apache.http" level="WARN"/>
</configuration>
EOF

echo -e "${GREEN}✓ Test resources created${NC}"

# Step 5: Download dependencies
echo -e "${YELLOW}[5/6] Downloading Maven dependencies...${NC}"
mvn dependency:resolve dependency:resolve-plugins -q
echo -e "${GREEN}✓ Dependencies downloaded${NC}"

# Step 6: Display next steps
echo ""
echo -e "${BLUE}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                   Setup Complete!                         ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}Test module created at: $TEST_MODULE_DIR${NC}"
echo ""
echo -e "${YELLOW}To run the tests:${NC}"
echo ""
echo "  cd $TEST_MODULE_DIR"
echo ""
echo "  # Run all tests"
echo "  mvn clean test"
echo ""
echo "  # Run with custom Schema Registry URL"
echo "  mvn clean test -Dschema.registry.url=http://localhost:8081"
echo ""
echo "  # Run specific test class"
echo "  mvn test -Dtest=ConfluentRestApiCompatibilityTest"
echo ""
echo "  # Run in quick mode (skip slow tests)"
echo "  mvn test -Pquick"
echo ""
echo "  # Generate test report"
echo "  mvn surefire-report:report"
echo ""
echo -e "${YELLOW}Available test profiles:${NC}"
echo "  -Plocal    : Test against localhost:8081 (default)"
echo "  -Pdocker   : Test against Docker container"
echo "  -Premote   : Test against remote server"
echo "  -Pquick    : Skip slow tests"
echo ""

# Optional: Run tests immediately
read -p "Do you want to run the tests now? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo -e "${BLUE}Running compatibility tests...${NC}"
    echo ""
    mvn clean test -Dschema.registry.url="$SCHEMA_REGISTRY_URL"
    
    # Show summary
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║            Tests Execution Completed                      ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Test report generated at:"
    echo "  $TEST_MODULE_DIR/target/surefire-reports/"
    echo ""
fi

cd ..
echo -e "${GREEN}Done!${NC}"
