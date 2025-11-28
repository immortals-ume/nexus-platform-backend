#!/bin/bash

set -e

echo "=========================================="
echo "Setup GitHub Packages Publishing"
echo "=========================================="
echo ""

read -p "Enter your GitHub username: " GITHUB_USERNAME
read -p "Enter your GitHub repository name (e.g., nexus-composite): " GITHUB_REPO
read -sp "Enter your GitHub Personal Access Token: " GITHUB_TOKEN
echo ""
echo ""

if [ -z "$GITHUB_USERNAME" ] || [ -z "$GITHUB_REPO" ] || [ -z "$GITHUB_TOKEN" ]; then
    echo "❌ Error: All fields are required"
    exit 1
fi

echo "🔧 Configuring Maven settings..."

SETTINGS_FILE="$HOME/.m2/settings.xml"
SETTINGS_BACKUP="$HOME/.m2/settings.xml.backup.$(date +%Y%m%d_%H%M%S)"

if [ -f "$SETTINGS_FILE" ]; then
    echo "📋 Backing up existing settings.xml to $SETTINGS_BACKUP"
    cp "$SETTINGS_FILE" "$SETTINGS_BACKUP"
fi

mkdir -p "$HOME/.m2"

cat > "$SETTINGS_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <servers>
        <server>
            <id>github</id>
            <username>$GITHUB_USERNAME</username>
            <password>$GITHUB_TOKEN</password>
        </server>
    </servers>
    
    <profiles>
        <profile>
            <id>github</id>
            <repositories>
                <repository>
                    <id>github</id>
                    <url>https://maven.pkg.github.com/$GITHUB_USERNAME/$GITHUB_REPO</url>
                    <snapshots>
                        <enabled>true</enabled>
                    </snapshots>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                </repository>
            </repositories>
        </profile>
    </profiles>
    
    <activeProfiles>
        <activeProfile>github</activeProfile>
    </activeProfiles>
</settings>
EOF

echo "✅ Maven settings.xml created at $SETTINGS_FILE"
echo ""

echo "🔧 Updating POM files..."

find . -name "pom.xml" -type f | while read pom; do
    if grep -q "YOUR_GITHUB_USERNAME" "$pom"; then
        sed -i.bak "s/YOUR_GITHUB_USERNAME/$GITHUB_USERNAME/g" "$pom"
        sed -i.bak "s/YOUR_REPO_NAME/$GITHUB_REPO/g" "$pom"
        rm "${pom}.bak" 2>/dev/null || true
        echo "   ✅ Updated: $pom"
    fi
done

echo ""
echo "🔧 Setting up environment variables..."

SHELL_RC=""
if [ -f "$HOME/.zshrc" ]; then
    SHELL_RC="$HOME/.zshrc"
elif [ -f "$HOME/.bashrc" ]; then
    SHELL_RC="$HOME/.bashrc"
fi

if [ -n "$SHELL_RC" ]; then
    echo "" >> "$SHELL_RC"
    echo "# GitHub Packages Configuration" >> "$SHELL_RC"
    echo "export GITHUB_USERNAME=$GITHUB_USERNAME" >> "$SHELL_RC"
    echo "export GITHUB_REPO=$GITHUB_REPO" >> "$SHELL_RC"
    echo "export GITHUB_TOKEN=$GITHUB_TOKEN" >> "$SHELL_RC"
    echo "✅ Environment variables added to $SHELL_RC"
    echo "   Run: source $SHELL_RC"
fi

echo ""
echo "=========================================="
echo "✅ Setup Complete!"
echo "=========================================="
echo ""
echo "📝 Next steps:"
echo "   1. Source your shell config: source $SHELL_RC"
echo "   2. Run: ./publish-to-github.sh"
echo ""
echo "🔗 Repository URL:"
echo "   https://maven.pkg.github.com/$GITHUB_USERNAME/$GITHUB_REPO"
echo ""
