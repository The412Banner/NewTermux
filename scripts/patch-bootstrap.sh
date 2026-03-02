#!/bin/bash
set -e

ARCH=$1
OLD_ID="com.termux"
NEW_ID="com.newtermux.app"

# Length-equivalent paths (both are 31 chars)
# /data/data/com.termux/files/usr  (31 chars)
# /data/data/com.newtermux.app/u   (31 chars)
OLD_USR="/data/data/com.termux/files/usr"
NEW_USR_SHORT="/data/data/com.newtermux.app/u"

# We'll use the full path for everything else (text files, etc.)
NEW_USR_FULL="/data/data/com.newtermux.app/files/usr"

mkdir -p extract
unzip -q "bootstrap-${ARCH}.zip" -d extract/
cd extract

echo "1. Creating shortcut symlinks in bootstrap root..."
# This is crucial for the shortcut paths to work
ln -sf files/usr u
ln -sf files/home h

echo "2. Patching text files and shebangs (Full Path)..."
# Use the full path for text files to be safe
grep -rIl "$OLD_ID" . | while read f; do
    if file "$f" | grep -qE 'text|script'; then
        sed -i "s|$OLD_ID|$NEW_ID|g" "$f" || true
    fi
done

echo "3. Patching ELF binaries (Surgical Shortcut Strategy)..."
# We only patch the USR path in binaries because lengths match exactly (31 chars).
# We avoid patching HOME in binaries to prevent corruption from length mismatch.
find . -type f | while read f; do
    if file "$f" | grep -qE 'ELF|executable|shared object'; then
        
        # A) Use patchelf for the formal ELF fields
        # Using the SHORT path ensures we never exceed the original string length
        
        # Update RPATH
        OLD_RPATH=$(patchelf --print-rpath "$f" 2>/dev/null || true)
        if [ -n "$OLD_RPATH" ]; then
            NEW_RPATH=$(echo "$OLD_RPATH" | sed "s|$OLD_USR|$NEW_USR_SHORT|g" | sed "s|$OLD_ID|$NEW_ID|g")
            patchelf --set-rpath "$NEW_RPATH" "$f" 2>/dev/null || true
        fi
        
        # Update Interpreter
        OLD_INTERP=$(patchelf --print-interpreter "$f" 2>/dev/null || true)
        if [ -n "$OLD_INTERP" ] && echo "$OLD_INTERP" | grep -q "$OLD_USR"; then
            NEW_INTERP=$(echo "$OLD_INTERP" | sed "s|$OLD_USR|$NEW_USR_SHORT|g")
            patchelf --set-interpreter "$NEW_INTERP" "$f" 2>/dev/null || true
        fi

        # B) Use a surgical python script for internal string constants.
        # This only replaces the USR path where length is identical.
        python3 -c "
import sys
with open('$f', 'rb') as f:
    data = f.read()
# Replace only if length matches exactly to avoid corruption
updated = data.replace(b'$OLD_USR', b'$NEW_USR_SHORT')
if updated != data:
    with open('$f', 'wb') as f:
        f.write(updated)
"
    fi
done

echo "4. Updating SYMLINKS.txt (Full Path)..."
if [ -f SYMLINKS.txt ]; then
    sed -i "s|$OLD_ID|$NEW_ID|g" SYMLINKS.txt
fi

echo "5. MOTD update..."
if [ -f etc/motd ]; then
    echo "Welcome to NewTermux (Robust Patch v2)!" > etc/motd
fi

echo "6. Repacking ${ARCH}..."
zip -q -r "../bootstrap-${ARCH}.zip" .
cd ..
rm -rf extract
echo "Done! Robustly patched bootstrap-${ARCH}.zip."
