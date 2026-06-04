#!/bin/bash

# Find adb executable path
ADB_PATH=$(which adb 2>/dev/null)

if [ -z "$ADB_PATH" ]; then
    # Fallback to standard home directory Android Sdk paths
    if [ -f "$HOME/Android/Sdk/platform-tools/adb" ]; then
        ADB_PATH="$HOME/Android/Sdk/platform-tools/adb"
    elif [ -n "$ANDROID_HOME" ] && [ -f "$ANDROID_HOME/platform-tools/adb" ]; then
        ADB_PATH="$ANDROID_HOME/platform-tools/adb"
    else
        ADB_PATH="adb"
    fi
fi

echo "Using adb binary at: $ADB_PATH"

echo "Killing existing adb server..."
sudo "$ADB_PATH" kill-server

echo "Starting adb server as root..."
sudo "$ADB_PATH" start-server

echo "Adb server restarted successfully. Listing devices:"
sudo "$ADB_PATH" devices
