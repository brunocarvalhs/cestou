#!/bin/bash

# Reclaims disk space on the GitHub-hosted ubuntu-24.04 runner before a job
# that needs real headroom: a full AGP build under the CodeQL tracer, or
# downloading an emulator system image. Both have failed with
# "No space left on device" (#240) even though neither step is unusually
# heavy on its own - the runner's preinstalled toolchains (.NET, Haskell,
# Swift) plus cached Docker images already consume most of the free space
# before the job does anything. None of these toolchains are used anywhere
# in this Kotlin/Android project, and the project has no native (NDK) code,
# so removing the Android SDK's NDK bundle is also safe here.
#
# Every removal is best-effort (`|| true`): a toolchain missing from a future
# runner image is not a reason to fail the job.

set -uo pipefail

echo "📊 Espaço em disco antes da limpeza:"
df -h /

sudo rm -rf /usr/share/dotnet || true
sudo rm -rf /opt/ghc /usr/local/.ghcup || true
sudo rm -rf /usr/share/swift || true
sudo rm -rf /usr/local/lib/android/sdk/ndk || true
docker image prune -af > /dev/null 2>&1 || true

echo "📊 Espaço em disco depois da limpeza:"
df -h /
