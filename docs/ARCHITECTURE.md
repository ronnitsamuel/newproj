# CoverMix — Architecture

Working name. A personal iPad app for adding EQ and effects to the audio of
voice-and-guitar cover videos, with optional voice/guitar separation. All
processing happens on the device.

Status: plan agreed, nothing built yet. Next step: the feasibility test app
(see "First test").

## Decisions

| Topic | Decision |
|---|---|
| Device | Only one: iPad (A16), iPadOS 27, 128 GB (75 GB free). A16 chip, 6 GB RAM, 16-core Neural Engine |
| Distribution | None. Built and run in **Swift Playgrounds** on the iPad. No Mac, no Apple Developer Program, no TestFlight, no App Store |
| Language / UI | Swift + SwiftUI, Swift 5 language mode to start |
| Audio effects | Our own Swift code using Accelerate (vDSP). C/C++ only if the first test shows Playgrounds can compile it |
| Separation | HTDemucs (MIT), Core ML fp16. The model file is picked from Files and compiled on the device with `MLModel.compileModel(at:)` |
| Processing location | On device only. Copying your own recordings over by AirDrop or iCloud Photos is fine |
| Licences | Personal use only, so MIT, BSD, LGPL, GPL and non-commercial model licences are all acceptable. Revisit if the app is ever shared |

## Architecture

```
SwiftUI views (Import · Mix · Export · Self-test)
  └ @Observable view models, one per screen
      └ Domain: Project (tracks, effect chains, parameters, schema version), undo, presets
          └ Services:
              MediaImport     – PhotosPicker / file importer, copies into the app sandbox
              PreviewEngine   – AVPlayer + one MTAudioProcessingTap on the original audio track
              ExportRenderer  – AVAssetReader → DSP → AVAssetWriter; video copied without re-encoding
              Separation      – Core ML HTDemucs + vDSP STFT/iSTFT, chunked, progress saved per chunk
              ProjectStore    – Codable JSON in the app's Documents folder
          └ DSP core (plain Swift, no UIKit/AVFoundation): effects, chain, parameter
            smoothing, latency compensation, meters, loudness (BS.1770)
```

### How audio flows

- **Preview.** AVPlayer plays the original video and keeps the clock. A single tap
  on the original audio track reports the time range being played. The tap takes
  the matching samples from the separated tracks (already loaded in memory), runs
  the effect chains and the master bus, and outputs the result. The audio thread
  never reads from disk.
- **Export.** The same DSP code and parameters run offline. Audio is written as
  24-bit ALAC or PCM (lossless) or AAC 256 kbps. The video samples are copied as-is,
  together with their rotation transform, HDR / Dolby Vision format description and
  metadata.
- **Separation.**
  1. Convert 48 kHz to 44.1 kHz.
  2. Process in overlapping chunks: STFT, Core ML, inverse STFT, overlap-add.
  3. Convert back to 48 kHz to get the voice track, then **guitar = original − voice**,
     so the two tracks always add back up to the original.

### Effects (one chain per track, plus a master bus)

Default order: Gate → EQ (parametric, `vDSP.Biquad`) → Compressor → Distortion
(oversampled) → Chorus → Reverb. The master bus has a limiter and a LUFS meter.
Every effect reports its latency, and the shorter chain is delayed so the voice
and guitar tracks stay aligned.

## Code design principles

1. **Audio-thread rules.** In the tap and render callbacks: no memory allocation,
   no locks, no I/O, no logging, no class instances or reference counting.
   Buffers are allocated before playback starts (`UnsafeMutableBufferPointer`).
   Parameters arrive through atomics and are smoothed over 10–20 ms.
2. **Block-size invariance.** The same input and parameters must produce identical
   output whatever buffer size the audio arrives in, so preview and export match
   exactly.
3. **`reset()` on seek** clears filter and reverb state.
4. **One-way data flow:** view → view model → service. Services are actors. Long
   jobs are cancellable `Task`s that report progress through `AsyncStream`.
5. **Non-destructive.** Imports are copied into the sandbox and never modified.
   The project file is versioned JSON with migrations.
6. **Swappable parts.** `protocol Effect` and `protocol SeparationModel`, so
   implementations can change without touching the UI.
7. **Errors** are typed, and mapped in one place to messages that say what
   happened and what to do.
8. **Design tokens.** Defined in code as dynamic colours: `UIColor { traits in … }`
   for light, dark and increased contrast. Asset catalogs are hard to edit on an
   iPad. No hard-coded colours or sizes outside `DesignSystem/`. The token values
   and contrast ratios agreed earlier (all text ≥ 4.5:1, controls ≥ 3:1) apply.

## Layout (planned)

```
CoverMix.swiftpm/
  Package.swift         Swift Playgrounds app manifest
  App/                  app entry point, navigation
  DesignSystem/         tokens + shared components
  DSP/                  effects, chain, smoothing, meters (no Apple UI/media imports)
  Media/                import, decode, export
  Separation/           Core ML wrapper, STFT, chunking
  Project/              model, persistence, undo
  Features/             Import · Mix · Export screens and view models
  SelfTest/             in-app test runner (replaces XCTest, which Playgrounds lacks)
docs/
```

## Testing and validation

Swift Playgrounds has no XCTest, debugger or Instruments. Instead:

- **In-app Self-test screen.** It checks:
  - EQ response is within ±0.1 dB of the target.
  - The compressor's gain curve and the gate's thresholds are correct.
  - Distortion aliasing stays below −80 dB.
  - There are no NaN or denormal values at extreme settings.
  - Output is identical at block sizes of 1, 64, 512 and 4096.
  - The reported latency matches an impulse test.
  - Bypassed output nulls against the input.
  - The two separated tracks add back up exactly to the original.
- **On-screen diagnostics.** CPU load per callback, `os_proc_available_memory()`,
  thermal state, separation time per chunk.
- **Golden files** for the DSP: fixed inputs whose outputs are stored as hashes.
- **Separation quality.** Record voice and guitar separately, mix them, separate the
  mix, and measure the voice track's SDR against the original voice recording.
- **Listening.** Blind A/B comparison before calling any setting "transparent".
- **Recordings stay out of the repo.** The repository is public.

## Running it

1. Install **Swift Playgrounds** (free, App Store).
2. Get the code onto the iPad by either:
   - GitHub page → *Code* → *Download ZIP* (Safari saves it to Files), then tap to unzip, or
   - a git client such as Working Copy, which makes updating easier.
3. Open `CoverMix.swiftpm` in Swift Playgrounds and tap **Run**.

The app runs inside Swift Playgrounds. There is no signing and no 7-day expiry.
Projects and exports live in the app's sandbox. Finished videos are saved with
the share sheet or the file exporter.

## Constraints

| Constraint | Effect |
|---|---|
| 6 GB RAM, A16 | Separation must be chunked and write each chunk to disk. Measure available memory during the first test |
| Swift Playgrounds | No debugger, profiler or XCTest; probably Swift only; few Info.plist keys and capabilities; SDK can lag behind the OS |
| Running inside Playgrounds | Background processing (`BGContinuedProcessingTask`) is probably unavailable: separation runs in the foreground with the screen kept awake |
| Input formats | Whatever AVFoundation reads (mp4, m4a, mov, wav, mp3, aiff…). No MKV/WebM |
| Source audio | iPhone camera audio is AAC. What it discarded can't be recovered. The pipeline adds no further loss with lossless export |
| Storage | About 0.7 GB per 4-minute 4K cover, about the same again per export, plus about 90 MB per separated track |
| Development | Code is written in a Linux cloud session and run on the iPad. Errors and results are reported back by hand |

## Dependencies

| Dependency | Licence | Used for |
|---|---|---|
| SwiftUI, AVFoundation, AVKit, MediaToolbox, Core ML, Accelerate, PhotosUI | iPadOS | UI, media, tap, model, DSP |
| HTDemucs weights (Core ML conversion) | MIT | Separation. Prefer a conversion checked against the PyTorch output |
| Airwindows / DaisySP algorithms | MIT (LGPL parts acceptable for personal use) | Reference designs for reverb, chorus and saturation, ported to Swift |

## First test (feasibility gate)

A single small app with one screen per check. Pass criteria:

| # | Check | Pass if |
|---|---|---|
| 1 | Swift DSP speed in a Playgrounds build | 2 tracks × 6 effects use < 30 % of one core, in real time |
| 2 | Core ML on this iPad | The model compiles from Files and a 4-minute song separates without being killed. Record the time (target ≤ 2× song length) and peak memory |
| 3 | Preview tap | Moving a slider is audible in < 100 ms. No clicks when seeking |
| 4 | Export round trip | Bypassed export nulls below −120 dBFS (24-bit). A clap test shows A/V offset ≤ 10 ms. Rotation, HDR and metadata are preserved |
| 5 | Playgrounds limits | Record whether C sources compile, which capabilities are available (Photos add), and whether background tasks are allowed |
