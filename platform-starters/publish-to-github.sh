#!/bin/bash

set -e

echo "=========================================="
echo "Publishing Platform Starters to GitHub Packages"
echo "=========================================="
echo ""

if [ -z "$GITHUB_USERNAME" ]; then
    echo "❌ Error: GITHUB_USERNAME environment variable is not set"
    echo "Please set it with: export GITHUB_USERNAME=your-username"
    exit 1
fi

if [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ Error: GITHUB_TOKEN environment variable is not set"
    echo "Please set it with: export GITHUB_TOKEN=your-token"
    exit 1
fi

if [ -z "$GITHUB_REPO" ]; then
    echo "⚠️  Warning: GITHUB_REPO not set, using default 'nexus-composite'"
    GITHUB_REPO="nexus-composite"
fi

echo "📋 Configuration:"
echo "   GitHub Username: $GITHUB_USERNAME"
echo "   GitHub Repository: $GITHUB_REPO"
echo "   Token: ${GITHUB_TOKEN:0:4}****"
echo ""

read -p "Do you want to continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Aborted by user"
    exit 1
fi

echo ""
echo "🔧 Step 1: Updating POM files with repository information..."

find . -name "pom.xml" -type f | while read pom; do
    if grep -q "YOUR_GITHUB_USERNAME" "$pom"; then
        sed -i.bak "s/YOUR_GITHUB_USERNAME/$GITHUB_USERNAME/g" "$pom"
        sed -i.bak "s/YOUR_REPO_NAME/$GITHUB_REPO/g" "$pom"
        rm "${pom}.bak"
        echo "   ✅ Updated: $pom"
    fi
done

echo ""
echo "🧹 Step 2: Cleaning previous builds..."
mvn clean

echo ""
echo "🔨 Step 3: Building all modules..."
mvn install -DskipTests

echo ""
echo "📦 Step 4: Deploying to GitHub Packages..."
mvn deploy -DskipTests \
    -Dgithub.username=$GITHUB_USERNAME \
    -Dgithub.token=$GITHUB_TOKEN

echo ""
echo "=========================================="
echo "✅ Successfully published to GitHub Packages!"
echo "=========================================="
echo ""
echo "📦 Published packages:"
echo "   - cache-starter"
echo "   - common-starter"
echo "   - domain-starter"
echo "   - messaging-starter"
echo ""
echo "🔗 View packages at:"
echo "   https://github.com/$GITHUB_USERNAME/$GITHUB_REPO/packages"
echo ""
echo "📚 To use in other projects, add to pom.xml:"
echo ""
echo "<repositories>"
echo "    <repository>"
echo "        <id>github</id>"
echo "        <url>https://maven.pkg.github.com/$GITHUB_USERNAME/$GITHUB_REPO</url>"
echo "    </repository>"
echo "</repositories>"
echo ""
echo "<dependency>"
echo "    <groupId>com.immortals.platform</groupId>"
echo "    <artifactId>cache-starter</artifactId>"
echo "    <version>1.0.0</version>"
echo "</dependency>"
echo ""
