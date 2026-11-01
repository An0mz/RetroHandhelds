#!/bin/sh
# Builds build/retrohandhelds-<version>-core-sources.tar for a release: the source code of every
# bundled core, at the commits listed in THIRD-PARTY-NOTICES.md, plus their notices. Run from the
# repository root and attach the result to the GitHub release.
set -eu

version=$(sed -n 's/^mod_version=//p' gradle.properties | tr -d '\r')
work=build/core-sources
out=build/retrohandhelds-$version-core-sources.tar

rm -rf "$work"
mkdir -p "$work"
cp THIRD-PARTY-NOTICES.md LICENSE-* "$work"/

grep -o 'https://github.com/[^/]*/[^/]*/tree/[0-9a-f]\{40\}' THIRD-PARTY-NOTICES.md | sort -u |
while read -r url; do
    repo=$(echo "$url" | cut -d/ -f5)
    commit=${url##*/}
    echo "$repo $commit"
    curl -fsSL -o "$work/$repo-$commit.tar.gz" "${url%/tree/*}/archive/$commit.tar.gz"
done

tar -cf "$out" -C "$work" .
echo "Wrote $out"
