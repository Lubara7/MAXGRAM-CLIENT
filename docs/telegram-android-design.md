# Telegram Android design comparison

Reference: [official Telegram Android](https://github.com/TelegramMessenger/Telegram-Android/tree/21f7d109da6ab3506c9e9577f98c6a38771ab335), the canonical repository reached by the supplied DrKLO/Telegram link. The MAX backend remains the pinned Komet integration. This change ports presentation geometry and original resources into Flutter; it does not claim to execute Telegram Java views.

| Area | Previous preview | Source and resulting behavior |
| --- | --- | --- |
| Main settings | Komet glossy cards, Material symbols, large headings | `ProfileActivity.java`, `TextCell.java`: original `msg2_*` icons at 24 dp, 50 dp rows, 71 dp label inset, regular 16 sp labels, flat full-width sections |
| Toolbar | Blue default header and inconsistent nested titles | `ThemeColors.java`: white default bar, dark text, original back/search/overflow icons, 56 dp height, 20 sp medium title |
| Switch | Standard Flutter Material switch | `Switch.java`: 31×14 dp track, 37×20 dp drawing, outer/inner thumb radii 10/8 dp, 48 dp accessible touch area |
| Sliders | Material thumb/value decorations | `SeekBarView.java` geometry: 3 dp track, 6 dp thumb, keyboard and accessibility adjustments, persisted values |
| Themes | Generated Material seed colors and Komet style settings | Original `ThemeColors.java`, `day.attheme`, `night.attheme`, `darkblue.attheme`; separate palette, accent and automatic-night controls |
| Default wallpaper | Flat generated color or Komet gradients | `Theme.java`, `MotionBackgroundDrawable.java`, `jni/image.cpp`: original SVG, four original colors and phase-0 gradient shader |
| Text bubbles | Rounded rectangles | `MessageDrawable.java`: normal bubble/tail outline, mirrored incoming shape; grouped/media layouts keep their own boundaries |
| Message size | Appearance settings mixed with interface scaling | Separate 12–30 sp message preference, reflected in the actual message widget and preview; existing interface-scale setting remains separate |
| Wallpaper picker | Komet pattern collection | Telegram theme wallpapers and gallery selection; old wallpaper IDs still resolve for existing installations |
| Nested settings | Differing cards, switches, chevrons and icons | Shared flat sections, Telegram navigation rows and switch/radio/seekbar components, preserving existing MAX actions |

## Original resource provenance

`integrations/komet/telegram-design.lock.json` records every original asset path and SHA-256. `scripts/import_telegram_ui_assets.py` refuses a different source revision and exports the four palettes. Preparation verifies each imported asset before building. Telegram GPL notices and the Roboto font notice are bundled in the application and the matching release source.

The default font is Roboto. Telegram's medium face is taken from its repository; regular, bold and italic faces are the public Roboto assets bundled with Flutter 3.44.3. The original bytes are copied without redrawing icons.

## Verification

`maxgram_telegram_design_test.dart` checks row geometry, original icon mapping, enabled/disabled switch interaction, message-size persistence independent of interface scale, theme persistence, wallpaper results, narrow-screen layout and one-time migration behavior. Six golden images cover settings, chat settings and controls in light/dark themes. They use a synthetic identity and demonstration messages only.

The source preparation verifies 54 protected MAX backend/transport/protocol/storage files. The previously reviewed message-history fix remains the sole allowed backend difference. This design work adds no MAX transport changes.

## Remaining limits

Golden comparisons prevent regressions against the reviewed Flutter implementation; they are not a pixel-diff certification against every Telegram Android screen. Camera, calls, media editors, story views and account-dependent screens still require a complete screen-by-screen review on real devices. MAX account authentication and 72-hour acceptance cannot be established by UI fixtures or an emulator startup test.
