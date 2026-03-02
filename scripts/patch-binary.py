import os
import sys

def patch_file(path, old_str, new_str):
    if len(new_str) != len(old_str):
        print(f"Error: length mismatch ({len(old_str)} vs {len(new_str)}) for {path}")
        return
    with open(path, 'rb') as f:
        data = f.read()
    if old_str.encode() not in data:
        return
    print(f"Binary patching {path}...")
    new_data = data.replace(old_str.encode(), new_str.encode())
    with open(path, 'wb') as f:
        f.write(new_data)

if __name__ == "__main__":
    # Exact length matches are CRITICAL for binary integrity
    # /data/data/com.termux/files/usr  (31)
    # /data/data/com.newtermux.app//u  (31)
    old_u = "/data/data/com.termux/files/usr"
    new_u = "/data/data/com.newtermux.app//u"
    
    # /data/data/com.termux/files/home (32)
    # /data/data/com.newtermux.app//h  (32)
    old_h = "/data/data/com.termux/files/home"
    new_h = "/data/data/com.newtermux.app//h"
    
    for root, dirs, files in os.walk('.'):
        for name in files:
            p = os.path.join(root, name)
            # Skip some types if needed, but replace handles all
            patch_file(p, old_u, new_u)
            patch_file(p, old_h, new_h)
