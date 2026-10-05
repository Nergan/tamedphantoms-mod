"""Delete Modrinth versions so the next publish replaces files instead of adding them.

Usage: replace-modrinth-versions.py <project-id> <version-number> [<version-number> ...]
MODRINTH_TOKEN must be set. A missing version is not an error.
"""

import json
import os
import sys
import urllib.request

project = sys.argv[1]
wanted = set(sys.argv[2:])
token = os.environ["MODRINTH_TOKEN"]
headers = {
    "Authorization": token,
    "User-Agent": "Nergan/minecraft-mods",
}


def request(url, method="GET"):
    call = urllib.request.Request(url, headers=headers, method=method)
    with urllib.request.urlopen(call) as response:
        body = response.read()
        return response.status, body


status, body = request(f"https://api.modrinth.com/v2/project/{project}/version?limit=100")
versions = json.loads(body.decode("utf-8"))
for item in versions:
    number = item.get("version_number")
    if number not in wanted:
        continue
    print("delete", number, item["id"])
    deleted, _ = request(f"https://api.modrinth.com/v2/version/{item['id']}", method="DELETE")
    print(deleted)
print("status", status)
