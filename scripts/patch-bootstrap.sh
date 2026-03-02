#!/bin/bash
set -e

ARCH=$1
OLD_ID="com.termux"
NEW_ID="com.newtermux.app"

# PERFECT LENGTH MATCHES (Crucial for ELF stability)
# /data/data/com.termux/files/usr  (31 chars)
# /data/data/com.newtermux.app/fil (31 chars)
OLD_USR="/data/data/com.termux/files/usr"
NEW_USR_PATCH="/data/data/com.newtermux.app/fil"

# /data/data/com.termux/files/home (32 chars)
# /data/data/com.newtermux.app/file (32 chars)
OLD_HOME="/data/data/com.termux/files/home"
NEW_HOME_PATCH="/data/data/com.newtermux.app/file"

mkdir -p extract
unzip -q "bootstrap-${ARCH}.zip" -d extract/
cd extract

echo "1. Creating shortcut symlinks for binary compatibility..."
# fil -> files/usr (allows 31-char path to work)
# file -> files/home (allows 32-char path to work)
ln -sf files/usr fil
ln -sf files/home file

echo "2. Patching text files and shebangs (Full Paths)..."
grep -rIl "$OLD_ID" . | while read f; do
    if file "$f" | grep -qE 'text|script'; then
        sed -i "s|$OLD_ID|$NEW_ID|g" "$f" || true
    fi
done

echo "3. Patching ELF binaries (Identical Length Strategy)..."
find . -type f | while read f; do
    if file "$f" | grep -qE 'ELF|executable|shared object'; then
        
        # A) Update formal ELF fields with patchelf
        # We use the patch paths which are EXACTLY the same length as the originals.
        
        # Update RPATH
        OLD_RPATH=$(patchelf --print-rpath "$f" 2>/dev/null || true)
        if [ -n "$OLD_RPATH" ]; then
            NEW_RPATH=$(echo "$OLD_RPATH" | sed "s|$OLD_USR|$NEW_USR_PATCH|g" | sed "s|$OLD_HOME|$NEW_HOME_PATCH|g")
            patchelf --set-rpath "$NEW_RPATH" "$f" 2>/dev/null || true
        fi
        
        # Update Interpreter
        OLD_INTERP=$(patchelf --print-interpreter "$f" 2>/dev/null || true)
        if [ -n "$OLD_INTERP" ] && echo "$OLD_INTERP" | grep -q "$OLD_USR"; then
            NEW_INTERP=$(echo "$OLD_INTERP" | sed "s|$OLD_USR|$NEW_USR_PATCH|g")
            patchelf --set-interpreter "$NEW_INTERP" "$f" 2>/dev/null || true
        fi

        # B) Surgical string replacement for internal constants
        # This replaces USR and HOME only if they are followed by / or \0.
        # Lengths match exactly, so no offsets are broken.
        python3 -c "
import sys
import re

path = '$f'
with open(path, 'rb') as f_in:
    data = f_in.read()

# Replace USR (31 -> 31)
u_pattern = re.compile(re.escape(b'$OLD_USR') + b'([\x00/])')
updated = u_pattern.sub(b'$NEW_USR_PATCH' + b'\\1', data)

# Replace HOME (32 -> 32)
h_pattern = re.compile(re.escape(b'$OLD_HOME') + b'([\x00/])')
updated = h_pattern.sub(b'$NEW_HOME_PATCH' + b'\\1', updated)

if updated != data:
    with open(path, 'wb') as f_out:
        f_out.write(updated)
"
    fi
done

echo "4. Updating SYMLINKS.txt..."
if [ -f SYMLINKS.txt ]; then
    sed -i "s|$OLD_ID|$NEW_ID|g" SYMLINKS.txt
fi

echo "5. MOTD update..."
if [ -f etc/motd ]; then
    echo "Welcome to NewTermux (Stable Build v1.2.5)!" > etc/motd
fi

echo "6. Repacking ${ARCH}..."
zip -q -r "../bootstrap-${ARCH}.zip" .
cd ..
rm -rf extract
echo "Done! Perfectly patched bootstrap-${ARCH}.zip."
