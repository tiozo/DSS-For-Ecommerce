#!/bin/bash

# DSS-Core Admin System - Quick Start Script
# This script sets up PostgreSQL and starts the application

set -e

echo "=========================================="
echo "DSS-Core Admin System - Quick Start"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if PostgreSQL is installed
echo -n "Checking PostgreSQL installation... "
if command -v psql &> /dev/null; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
    echo "PostgreSQL is not installed. Please install it first:"
    echo "  sudo apt install postgresql postgresql-contrib"
    exit 1
fi

# Check if PostgreSQL is running
echo -n "Checking PostgreSQL service... "
if sudo systemctl is-active --quiet postgresql; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${YELLOW}Starting PostgreSQL...${NC}"
    sudo systemctl start postgresql
    echo -e "${GREEN}✓${NC}"
fi

# Create database if it doesn't exist
echo -n "Setting up database 'dss_core'... "
if sudo -u postgres psql -lqt | cut -d \| -f 1 | grep -qw dss_core; then
    echo -e "${YELLOW}Already exists${NC}"
else
    sudo -u postgres psql -c "CREATE DATABASE dss_core;" > /dev/null 2>&1
    echo -e "${GREEN}✓${NC}"
fi

# Ensure user has privileges
echo -n "Configuring database permissions... "
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE dss_core TO postgres;" > /dev/null 2>&1
echo -e "${GREEN}✓${NC}"

# Check Java version
echo -n "Checking Java version... "
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        echo -e "${GREEN}✓ (Java $JAVA_VERSION)${NC}"
    else
        echo -e "${RED}✗ (Java $JAVA_VERSION < 17)${NC}"
        echo "Please install Java 17 or higher"
        exit 1
    fi
else
    echo -e "${RED}✗${NC}"
    echo "Java is not installed. Please install Java 17+"
    exit 1
fi

# Check Maven
echo -n "Checking Maven... "
if command -v mvn &> /dev/null; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
    echo "Maven is not installed. Please install Maven 3.8+"
    exit 1
fi

echo ""
echo "=========================================="
echo "Building Application..."
echo "=========================================="
mvn clean install -DskipTests

echo ""
echo "=========================================="
echo "Starting Application..."
echo "=========================================="
echo ""
echo -e "${GREEN}Dashboard will be available at:${NC}"
echo -e "${YELLOW}http://localhost:8080/dashboard.html${NC}"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

mvn spring-boot:run
