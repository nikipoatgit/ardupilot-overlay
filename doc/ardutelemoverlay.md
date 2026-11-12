Your workflow is simple and minimizes unnecessary steps. Here's a clean flow.

```text
                    START
                      │
                      ▼
            ┌───────────────────┐
            │ Welcome / New Job │
            └─────────┬─────────┘
                      │
                      ▼
          ┌─────────────────────────┐
          │ Select Video (Optional) │
          │                         │
          │  Browse                 │
          │  Skip                   │
          └─────────┬───────────────┘
                    │
                    ▼
        ┌────────────────────────────┐
        │ Select Telemetry File      │
        │                            │
        │  BIN                       │
        │  TLOG                      │
        │  CSV                       │
        └───────┬─────────┬──────────┘
                │         │
       BIN/TLOG │         │ CSV
                │         ▼
                │   ┌──────────────────┐
                │   │ CSV Mapping      │
                │   │ Wizard           │
                │   └────────┬─────────┘
                └────────────┘
                             │
                             ▼
         ┌─────────────────────────────────┐
         │ Overlay Designer                │
         │                                 │
         │ Video Preview                   │
         │ Overlay Preview                 │
         │ Timeline                        │
         │ Playback Controls               │
         │ Add/Edit Widgets                │
         │ Move/Resize                     │
         │ Style                           │
         │ Sync                            │
         │ Trim                            │
         │ Offsets                         │
         │ Templates                       │
         │ Save/Load Presets               │
         └───────────────┬─────────────────┘
                         │
                         ▼
             ┌────────────────────────┐
             │ Export Settings        │
             │                        │
             │ Output File            │
             │ Resolution             │
             │ FPS                    │
             │ Codec                  │
             │ Quality                │
             │ Burn Overlay           │
             └──────────┬─────────────┘
                        │
                        ▼
              ┌───────────────────┐
              │ Rendering         │
              │ Progress          │
              │ ETA               │
              └─────────┬─────────┘
                        │
                        ▼
              ┌───────────────────┐
              │ Finished          │
              │ Open File         │
              │ Open Folder       │
              │ New Project       │
              └───────────────────┘
```

## Overlay Designer

This is where users will spend almost all of their time.

```
+---------------------------------------------------------------+
| Menu                                                         |
+---------------------------------------------------------------+
| Toolbar                                                      |
+---------------------------------------------------------------+
| Widgets |             Video Preview             | Properties |
|         |                                       |            |
| Horizon |                                       | Position   |
| Compass |                                       | Scale      |
| Speed   |                                       | Font       |
| Map     |                                       | Color      |
| Battery |                                       | Opacity    |
| GPS     |                                       | Offset     |
| ...     |                                       | Rotation   |
+---------+---------------------------------------+------------+
| Timeline                                               ▶️    |
+---------------------------------------------------------------+
```

## CSV Mapping Wizard

This only appears for CSV files.

```
Step 1
Choose delimiter
○ Comma
○ Semicolon
○ Tab

↓

Step 2
Preview first 20 rows

↓

Step 3
Map columns

Timestamp   → dropdown
Latitude    → dropdown
Longitude   → dropdown
Altitude    → dropdown
Roll        → dropdown
Pitch       → dropdown
Yaw         → dropdown
Speed       → dropdown
Voltage     → dropdown
Current     → dropdown

↓

Validate

↓

Finish
```

## Export Page

Keep this page focused.

```
Output File

Video

Resolution
FPS
Codec
Bitrate

Overlay

Quality
Antialiasing

Export
```

## Overall navigation

```
New Project
      │
      ▼
Video (optional)
      │
      ▼
Telemetry (required)
      │
      ├── BIN ───────┐
      ├── TLOG ──────┤
      └── CSV ─► Mapping Wizard
                     │
                     ▼
             Overlay Designer
                     │
                     ▼
               Export Settings
                     │
                     ▼
                 Rendering
                     │
                     ▼
                  Finished
```

This is a good minimal MVP. Every project follows the same path, with only one conditional branch (CSV mapping), making the application easy to learn while supporting all three telemetry formats.

