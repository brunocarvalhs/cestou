# Play Store listing metadata

Short and full descriptions for `en-US`, `pt-BR`, `es-ES`, in the standard
[fastlane supply](https://docs.fastlane.tools/actions/supply/) metadata layout — usable as-is if
this project ever wires up fastlane, and otherwise just copy-pasteable into Play Console's
"Store listing" page today.

## What's still missing

Fastlane's convention also expects, none of which exist yet and none of which can be produced
without a device/emulator or a design tool:

- `images/phoneScreenshots/*.png` (at least 2, up to 8) per locale
- `images/featureGraphic.png` (1024×500)
- `images/icon.png` (512×512, high-res version of the launcher icon)

Once there's a device to run the app on, `.maestro/test_suite.yaml` walks through every main
screen already — a screenshot taken at each step of that flow would cover most of what's needed.
