#!/bin/bash
set -e

ARCH=$1
OLD_USR="/data/data/com.termux/files/usr"
NEW_USR="/data/data/com.newtermux.app/u"
OLD_HOME="/data/data/com.termux/files/home"
NEW_HOME="/data/data/com.newtermux.app/h"

mkdir -p extract
unzip -q "bootstrap-${ARCH}.zip" -d extract/
cd extract

# 1. Text patch
echo "Patching scripts and configs..."
find . -type f | while read f; do
  if file "$f" | grep -qE 'text|script'; then
    sed -i "s|${OLD_USR}|${NEW_USR}|g" "$f" 2>/dev/null || true
    sed -i "s|${OLD_HOME}|${NEW_HOME}|g" "$f" 2>/dev/null || true
    sed -i "s|com.termux|com.newtermux.app|g" "$f" 2>/dev/null || true
    sed -i "s|Termux|NewTermux|g" "$f" 2>/dev/null || true
  fi
done

# 2. Binary patch using python script
echo "Patching binaries safely..."
python3 ../scripts/patch-binary.py

# 3. ELF header patch
echo "Patching ELF headers..."
find . -type f | while read f; do
  if file "$f" | grep -q 'ELF'; then
    INTERP=$(patchelf --print-interpreter "$f" 2>/dev/null || true)
    if [ -n "$INTERP" ] && echo "$INTERP" | grep -qF "com.termux"; then
      NEW_INTERP=$(echo "$INTERP" | sed "s|com.termux|com.newtermux.app/u|g" | sed "s|//|/|g")
      patchelf --set-interpreter "$NEW_INTERP" "$f" 2>/dev/null || true
    fi
    RPATH=$(patchelf --print-rpath "$f" 2>/dev/null || true)
    if [ -n "$RPATH" ] && echo "$RPATH" | grep -qF "com.termux"; then
      NEW_RPATH=$(echo "$RPATH" | sed "s|${OLD_USR}|${NEW_USR}|g")
      patchelf --set-rpath "$NEW_RPATH" "$f" 2>/dev/null || true
    fi
  fi
done

# 4. MOTD fix
if [ -f etc/motd ]; then
  echo "Welcome to NewTermux!" > etc/motd
fi

# 5. Permissions
find bin libexec -type f -exec chmod +x {} + 2>/dev/null || true
find usr/bin usr/libexec -type f -exec chmod +x {} + 2>/dev/null || true

# 6. Repack
zip -q -r "../bootstrap-${ARCH}.zip" .
cd ..
echo "Repacked bootstrap-${ARCH}.zip"
