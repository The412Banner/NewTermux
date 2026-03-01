import os
import sys

def patch_file(path, old_str, new_str):
    if len(new_str) > len(old_str):
        return
    with open(path, 'rb') as f:
        data = f.read()
    if old_str.encode() not in data:
        return
    print(f"Binary patching {path}...")
    pad = b'\0' * (len(old_str) - len(new_str))
    replacement = new_str.encode() + pad
    new_data = data.replace(old_str.encode(), replacement)
    with open(path, 'wb') as f:
        f.write(new_data)

if __name__ == "__main__":
    old_u = "/data/data/com.termux/files/usr"
    new_u = "/data/data/com.newtermux.app/u"
    old_h = "/data/data/com.termux/files/home"
    new_h = "/data/data/com.newtermux.app/h"
    
    for root, dirs, files in os.walk('.'):
        for name in files:
            p = os.path.join(root, name)
            patch_file(p, old_u, new_u)
            patch_file(p, old_h, new_h)
            # Catch package name alone
            # Note: only safe if length is same, but here it is not.
            # So we only patch full paths.
