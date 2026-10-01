#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ASM="/usr/share/java/asm.jar:/usr/share/java/asm-tree.jar"
if [ "$#" -lt 1 ]; then
    echo "Usage: $0 <base-game.jar> [lwjgl.jar]" >&2
    exit 1
fi
BASE="$1"
LWJGL_JAR="${2:-}"
OUT="$HERE/build/chatfix.jar"
if [ ! -f "$BASE" ]; then
    echo "Base game jar not found: $BASE" >&2
    exit 1
fi
CP="$BASE"
if [ -n "$LWJGL_JAR" ]; then
    if [ ! -f "$LWJGL_JAR" ]; then
        echo "LWJGL jar not found: $LWJGL_JAR" >&2
        exit 1
    fi
    CP="$CP:$LWJGL_JAR"
fi
rm -rf "$HERE/build"
mkdir -p "$HERE/build"
javac --release 8 -cp "$CP" -d "$HERE/build" "$HERE/src/WrapChat.java" "$HERE/src/ClipboardHelper.java" "$HERE/src/ChatScroll.java" "$HERE/src/ChatInput.java" "$HERE/src/ChatInputRender.java" "$HERE/src/ChatHistoryCopy.java" "$HERE/src/ChatSplit.java"
javac -cp "$ASM" -d "$HERE/build" "$HERE/src/PatchChat.java" "$HERE/src/PatchInputEditor.java" "$HERE/src/PatchHistoryCopy.java"
java -cp "$HERE/build:$ASM" PatchChat "$BASE" "$OUT"
echo "Built $OUT"
