# Chat Windows

A client-side Fabric mod for Minecraft Java that replaces the single vanilla chat box with
any number of **movable, resizable chat windows**, each holding its own set of **tabs**.
Every tab has fully independent filters and keyword highlighting, in the style of LabyMod's
chat customisation.

---

## Features

**Windows**
- Add as many windows as you want, each drawn independently.
- Drag to move, drag the bottom-right corner to resize, `Ctrl` + scroll to change scale.
- Anchor to any screen corner so the layout survives resolution / GUI-scale changes.
- Per-window: background hex colour, separate opacity for "chat closed" and "chat open",
  fade-out timer, line spacing, text shadow, stored-message count, tab bar on/off, visibility.

**Tabs**
- Each window has its own tab strip. Tabs show an unread marker when a message lands in
  a tab you're not looking at.
- Each tab keeps its own message backlog and its own scroll position.
- Optional per-tab send prefix (e.g. `/pc `) used by the "Open chat in active tab" keybind.

**Filters (per tab)**
- `Show only` rules act as a whitelist: if a tab has at least one, a message must match one
  of them to appear in that tab.
- `Hide` rules act as a blacklist and always win.
- Match modes: contains / starts with / ends with / exactly equals / regex, each with an
  optional case-sensitivity toggle.
- A built-in test box tells you whether a sample line would match before you commit to it.

**Highlights (per tab)**
- Highlights paint the **background** of the matching message and leave the text colours
  completely untouched.
- Colour is chosen by hex code (`#RRGGBB`), with eight one-click presets.
- Separate opacity slider per rule, plus an optional ping sound.
- Live preview of exactly how the highlighted line will look.

Editing filters or highlights re-applies them to the stored backlog immediately, so you can
see the effect without waiting for new messages.

---

## Building

```
./gradlew build
```

The jar lands in `build/libs/`. Drop it in `mods/` alongside Fabric API.

### Before your first build — check these version numbers

`gradle.properties` contains my best guesses. Open <https://fabricmc.net/develop/> , pick
1.21.11, and paste the exact values it gives you:

```
minecraft_version=1.21.11
yarn_mappings=1.21.11+build.X     <- update
loader_version=0.18.1
fabric_version=0.XXX.X+1.21.11    <- update
```

`build.gradle` uses Loom `1.14-SNAPSHOT`, which is what Fabric recommends for 1.21.11.
Note that 1.21.11 is the **last** version with Yarn mappings; if you'd rather move to
Mojang mappings now, swap the `mappings` line in `build.gradle` for
`mappings loom.officialMojangMappings()` and rename the Minecraft symbols accordingly.

---

## Using it

| Action | Default |
| --- | --- |
| Open the layout editor | `O` |
| Next / previous tab | `]` / `[` |
| Open chat in active tab (applies the tab's send prefix) | unbound |

Hotkeys are polled from GLFW rather than registered in the Controls menu, so they
are set in `config/chatwindows.json` as raw GLFW key codes (`keyOpenLayout`,
`keyNextTab`, `keyPrevTab`, `keyOpenChat`; `-1` unbinds). Codes are listed at
<https://www.glfw.org/docs/latest/group__keys.html>. They only fire when no screen
is open, so they can never trigger while you're typing.

| Open the layout editor from chat | `/chatwindows` |
| Inject a fake line to test filters offline | `/chatwindows test <text>` |
| Clear all windows | `/chatwindows clear` |
| Reload config from disk | `/chatwindows reload` |

In the layout editor:
- **Left-drag** a window body to move it (it snaps to screen edges; hold `Shift` to disable).
- **Left-drag the bottom-right corner** to resize.
- **Right-click** a window to open its settings.
- **Middle-click** to toggle its visibility.
- **Ctrl + scroll** over a window to change its scale.

While the chat box is open you can scroll each window separately with the wheel over it,
click tabs to switch, and click links inside messages as normal.

Config lives at `config/chatwindows.json` and is plain JSON, so you can hand-edit or share
layouts.

---

## How it works

`ClientReceiveMessageEvents.ALLOW_CHAT` / `ALLOW_GAME` intercept every incoming message.
Each one is stripped to plain text, tested against every tab's filters, and stored (once)
in whichever tabs accepted it. Because those events *cancel* delivery, the vanilla chat HUD
simply never receives anything — there's no need to suppress its rendering. If you'd rather
keep the vanilla chat too, set `"hideVanillaChat": false` in the config.

Rendering happens in a `TAIL` inject on `InGameHud#render`, so the windows draw over the
world and stay visible while the chat box is open.

---

## Compatibility notes (please read)

1.21.11 is newer than the API surface I could verify line-by-line, so three spots are the
likely places you'll need a one-line tweak if the build complains:

1. **Matrix stack** — `ChatWindowRenderer` uses the 2D `Matrix3x2fStack` API
   (`pushMatrix()` / `translate(x, y)` / `scale(x, y)`) introduced in 1.21.6. If your
   mappings still expose the 3D stack, the older equivalent is commented directly above
   that block.
2. **Keybinding category** — `ChatWindowsClient` passes a `String` category to
   `new KeyBinding(...)`. If your version wants a `KeyBinding.Category` object, swap that
   last argument. (The `/chatwindows` command works regardless.)
3. **Mixin targets** — `InGameHud#render(DrawContext, RenderTickCounter)` and
   `ChatScreen#mouseClicked` / `#mouseScrolled`. If a descriptor moved, adjust
   `InGameHudMixin` / `ChatScreenMixin`; `chatwindows.mixins.json` has
   `defaultRequire: 1`, so a miss fails loudly at startup rather than silently.

## Known limitations

- Clicking links inside a window doesn't work. `TextHandler#getStyleAt(OrderedText, int)`
  and `Screen#handleTextClick` were both removed in this version; re-adding this needs
  their replacements. Hover tooltips are likewise not drawn.
- Highlights don't play a sound. `PositionedSoundInstance.master` changed signature, so
  that option was dropped rather than guessed at.
- Ctrl + scroll to rescale a window in the layout editor is gone; use the Scale slider in
  the window's settings instead.
- Server-side message deletion (`ChatHud#removeMessage`) isn't mirrored into the windows.
- Chat report signatures are ignored — messages are stored as rendered text, so the
  "report" flow won't see them.
- Filters match the plain-text form of the line, so `§` colour codes and component
  structure aren't matchable. Regex is the escape hatch for anything more complex.

MIT licensed — do whatever you like with it.
