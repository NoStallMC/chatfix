# Chatfix

Client-side chat improvements for **Minecraft Beta 1.7.3**, packaged as a jar
mod. Supports standart mouse + keyboard shortcuts.

## Features

- **Longer messages** — the 100-char input cap is removed and the 119-char
  packet cap is raised; long messages are split into ≤100-char.
- **Live color codes** — type `&a` etc. and the text after it is rendered in
  that color.
- **Chat selection** — Use mouse to select text to copy it.
- **Scrollback** — open chat and use the mouse wheel to scroll back through
  history (up to 500 lines).

## Building

Requires Java 8, a host JDK with ASM at `/usr/share/java/asm.jar` +
`asm-tree.jar`, and a Beta 1.7.3 game jar.

```sh
./build.sh <base-game.jar> [lwjgl.jar]
```

Output: `build/chatfix.jar`. Copy it into the instance's `jarmods/` folder and
enable it in the launcher's Version UI (I use MultiMC.)
