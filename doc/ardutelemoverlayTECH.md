For a project like this, I'd structure it similarly to a lightweight video editor. The key principle is:

> **One project model, one rendering engine, multiple telemetry parsers.**

---

# Overall Architecture

```text
                        User Interface
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
        ▼                      ▼                      ▼
 Project Manager         Playback Engine      Export Manager
        │                      │                      │
        └──────────────┬───────┴──────────────┬──────┘
                       ▼
                Render Engine
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  Video Decoder   Telemetry Engine   Overlay Engine
        │              │              │
        ▼              ▼              ▼
     FFmpeg      Telemetry Cache    Widgets
```

---

# Project Structure

```text
Project

├── Video
│      video.mp4
│
├── Telemetry
│      flight.bin
│      or
│      flight.tlog
│      or
│      flight.csv
│
├── Mapping
│      csv_mapping.json
│
├── Overlay
│      overlay.json
│
├── Presets
│      *.preset
│
└── Export Settings
```

Everything the user edits is stored in a single project.

---

# UI Modules

```text
UI

├── Welcome Screen
│
├── Import Wizard
│
├── CSV Mapping Wizard
│
├── Overlay Designer
│
├── Export Window
│
└── Preferences
```

---

# Import Flow

```text
Start

↓

Select Video
(optional)

↓

Select Telemetry
(required)

↓

BIN ?
     ↓

Parse BIN

↓

TLOG ?
     ↓

Parse MAVLink

↓

CSV ?
     ↓

Mapping Wizard

↓

Telemetry Cache

↓

Overlay Designer
```

---

# Telemetry Engine

```
TelemetryEngine

├── BinParser
├── TlogParser
├── CsvParser
└── TelemetryCache
```

All three produce exactly the same object.

```java
TelemetryFrame

timestamp

latitude
longitude
altitude

roll
pitch
yaw

groundSpeed
airSpeed

batteryVoltage
batteryCurrent

satellites

flightMode

homeDistance

homeBearing

climbRate

verticalSpeed

gpsFix

rssi

etc...
```

Renderer never knows the original file format.

---

# CSV Mapping

CSV parser should never assume column names.

Instead

```text
Timestamp

↓

Select Column

↓

Latitude

↓

Select Column

↓

Longitude

↓

Select Column

↓

etc.
```

Mapping is saved

```
mapping.json
```

so user only maps once.

---

# Video Engine

```
VideoEngine

↓

FFmpeg

↓

Seek()

↓

Decode Frame()

↓

BufferedImage
```

Functions

```
open()

seek(time)

nextFrame()

previousFrame()

currentFrame()

close()
```

Nothing else.

---

# Timeline

Timeline stores

```
Current Time

00:04:15.723
```

NOT frame number.

Every subsystem converts

```
Frame = Time × FPS
```

---

# Playback Engine

Responsible only for playback.

```
Play

↓

Timer

↓

next video frame

↓

get telemetry

↓

render

↓

display
```

No overlay logic here.

---

# Render Engine

Most important class.

Input

```
Video Frame

Telemetry

Overlay Settings

Current Time
```

Output

```
Rendered BufferedImage
```

Pseudo

```
Video Frame

↓

Graphics2D

↓

Static Widgets

↓

Dynamic Widgets

↓

Output Frame
```

Exactly same renderer is used

Preview

and

Export.

---

# Overlay Engine

```
OverlayEngine

├── Widget Manager
├── Selection
├── Dragging
├── Grid
├── Snapping
├── Alignment
└── Layers
```

---

# Widget System

Everything is a widget.

```
Widget

↓

Artificial Horizon

Compass

Altitude

Speed

Battery

GPS

Map

Crosshair

Heading

Text

Logo

Image

Home Arrow

Flight Mode

Vertical Speed

Waypoint

Custom Widget
```

Every widget

```
draw()

update()

properties()
```

---

# Widget Properties

Example

```
Compass

x

y

width

height

rotation

font

color

opacity

visible

layer
```

User edits only these.

---

# Preview

Preview is

```
BufferedImage

↓

JavaFX Image

↓

ImageView
```

Nothing fancy.

---

# Playback

```
Play

↓

Read Frame

↓

Telemetry Lookup

↓

Render

↓

Display

↓

Repeat
```

---

# Seeking

When user drags timeline

```
Mouse

↓

seek(time)

↓

decode one frame

↓

telemetry lookup

↓

render

↓

display
```

No video regeneration.

---

# Telemetry Lookup

Never parse while rendering.

Instead

```
Load BIN

↓

Parse once

↓

ArrayList<TelemetryFrame>
```

Then

```
binary search(timestamp)
```

Interpolation

```
Frame A

Frame B

↓

Linear interpolation

↓

Smooth values
```

---

# Overlay Layers

```
Layer 0

Video

Layer 1

Map

Layer 2

Artificial Horizon

Layer 3

Compass

Layer 4

Battery

Layer 5

Text

Layer 6

Crosshair
```

User can reorder.

---

# Export Engine

```
Video Decoder

↓

Render Engine

↓

Video Encoder

↓

Output MP4
```

Notice

Same renderer.

No duplicate code.

---

# Presets

```
HUD

↓

Save

↓

preset.json
```

Contains

```
Widget list

Positions

Fonts

Colors

Visibility

Scale
```

No telemetry.

---

# Project Save

```
project.ato
```

Contains

```
video path

telemetry path

mapping

offset

trim

widgets

presets

export settings
```

Just JSON.

---

# Recommended Package Structure

```text
src/

app/
    Main.java
    Project.java

ui/
    WelcomeView
    ImportView
    MappingView
    OverlayView
    ExportView

video/
    VideoEngine
    FrameGrabber
    FrameCache

telemetry/
    parser/
        BinParser
        TlogParser
        CsvParser

    cache/
        TelemetryCache

    model/
        TelemetryFrame

overlay/
    OverlayEngine

    widgets/
        HorizonWidget
        CompassWidget
        BatteryWidget
        MapWidget
        TextWidget
        ...

    properties/

renderer/
    RenderEngine
    RenderContext

playback/
    PlaybackController
    TimelineController

export/
    ExportManager
    FFmpegEncoder

project/
    ProjectIO
    PresetIO

util/
```

# Design Principles

* **Single source of truth:** One `Project` object holds all state.
* **Format independence:** BIN, TLOG, and CSV are normalized into a common `TelemetryFrame` model.
* **Single renderer:** The same `RenderEngine` powers both live preview and final export, ensuring identical output.
* **Separation of concerns:** Video decoding, telemetry parsing, rendering, playback, and UI are isolated into dedicated modules.
* **Extensibility:** New telemetry formats or overlay widgets can be added by implementing the appropriate parser or widget interface without changing the rest of the system.

This architecture is modular, maintainable, and scalable enough to support future additions such as PX4 ULog, GPX, animated widgets, hardware-accelerated rendering, or timeline keyframing without requiring major redesign.

