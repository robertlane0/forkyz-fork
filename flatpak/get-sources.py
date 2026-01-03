
import re
import sys
import hashlib
import requests

from typing import Optional

DOWNLOAD_RE = re.compile("Downloading (.*) to.*")
URL_GRP = 1
MAVEN_SLUGS = "m2|maven2|jitpack.io"

SOURCE_ENTRY = """{{
  "type": "file",
  "url": "{url}",
  "sha512": "{sha512}",
  "dest": "dependencies{dest}",
  "dest-filename": "{filename}"
}}"""

def extract_source(line : str) -> Optional[str]:
    if m := DOWNLOAD_RE.match(line):
        url = m[URL_GRP]
        path_file = re.split(MAVEN_SLUGS, url)[1]
        bits = path_file.rsplit("/", 1)
        path = bits[0]
        file = bits[1]
        sha512 = hashlib.sha512(requests.get(url).content).hexdigest()
        return SOURCE_ENTRY.format(
            url = url,
            sha512 = sha512,
            dest = path,
            filename = file,
        )
    else:
        return None

with open(sys.argv[1]) as f:
    sources = [
        source
        for line in f
        if (source := extract_source(line))
    ]

print(",\n".join(sources))
