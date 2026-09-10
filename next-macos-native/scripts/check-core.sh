#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/check-toolchain.sh
swift build
native_bin=$(swift build --show-bin-path)
native_objects=()
while IFS= read -r -d '' native_object; do native_objects+=("$native_object"); done < <(find "$native_bin/MooToolNextCore.build" "$native_bin/Yams.build" "$native_bin/CYaml.build" -name '*.o' -print0)
swiftc -parse-as-library -D STANDALONE_CHECK \
  -I "$native_bin/Modules" -I .build/checkouts/Yams/Sources/CYaml/include \
  Tests/MooToolNextCoreTests/CoreTests.swift scripts/StandaloneTests.swift \
  "${native_objects[@]}" -o "$native_bin/MooToolNativeCoreChecks"
"$native_bin/MooToolNativeCoreChecks"
