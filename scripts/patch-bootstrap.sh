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

# /data/data/com.termux/files/home (32 chars)
# /data/data/com.newtermux.app/h    (31 chars + null/extra)
OLD_HOME="/data/data/com.termux/files/home"
NEW_HOME_SHORT="/data/data/com.newtermux.app/h"

mkdir -p extract
unzip -q "bootstrap-${ARCH}.zip" -d extract/
cd extract

echo "1. Creating shortcut symlinks in bootstrap root..."
# This allows the short paths to work
ln -s files/usr u
ln -s files/home h

echo "2. Patching text files and shebangs..."
grep -rIl "$OLD_ID" . | xargs sed -i "s|$OLD_ID|$NEW_ID|g" || true

echo "3. Patching binaries with Fixed-Length Shortcut Strategy..."
# We use sed with a specific binary-safe approach or a small python script
# Since we are in a shell script, we'll use a python one-liner for safety
find . -type f | while read f; do
    if file "$f" | grep -qE 'ELF|executable|shared object'; then
        # Replace USR path (31 to 31 chars)
        python3 -c "
import sys
with open('$f', 'rb') as f:
    data = f.read()
updated = data.replace(b'$OLD_USR', b'$NEW_USR_SHORT')
updated = updated.replace(b'$OLD_HOME', b'$NEW_HOME_SHORT' + b'\x00' * (len('$OLD_HOME') - len('$NEW_HOME_SHORT')))
if updated != data:
    with open('$f', 'wb') as f:
        f.write(updated)
        print('Patched binary: $f')
"
    fi
done

echo "4. Patching ELF headers (RPATH/INTERP) with patchelf..."
find . -type f | while read f; do
    if file "$f" | grep -q 'ELF'; then
        # Update RPATH
        OLD_RPATH=$(patchelf --print-rpath "$f" 2>/dev/null || true)
        if [ -n "$OLD_RPATH" ]; then
            NEW_RPATH=$(echo "$OLD_RPATH" | sed "s|$OLD_ID|$NEW_ID|g")
            patchelf --set-rpath "$NEW_RPATH" "$f" 2>/dev/null || true
        fi
        
        # Update Interpreter
        OLD_INTERP=$(patchelf --print-interpreter "$f" 2>/dev/null || true)
        if [ -n "$OLD_INTERP" ] && echo "$OLD_INTERP" | grep -q "$OLD_ID"; then
            NEW_INTERP=$(echo "$OLD_INTERP" | sed "s|$OLD_ID|$NEW_ID|g")
            patchelf --set-interpreter "$NEW_INTERP" "$f" 2>/dev/null || true
        fi
    fi
done

echo "5. Updating SYMLINKS.txt..."
if [ -f SYMLINKS.txt ]; then
    # Update symlink targets to use the new ID
    sed -i "s|$OLD_ID|$NEW_ID|g" SYMLINKS.txt
fi

echo "6. MOTD update..."
if [ -f etc/motd ]; then
    echo "Welcome to NewTermux (Patched)!" > etc/motd
fi

echo "7. Repacking ${ARCH}..."
zip -q -r "../bootstrap-${ARCH}.zip" .
cd ..
rm -rf extract
echo "Done! Robustly patched bootstrap-${ARCH}.zip using Shortcut Strategy."
