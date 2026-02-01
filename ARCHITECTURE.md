# CPR Assist - Android Application Architecture

## Overview

CPR Assist is an emergency-response Android application designed for cardiac arrest management. It operates in two distinct modes optimized for different user types.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer                                 │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  Mode Selection │  │  CPR Guidance   │  │  Professional   │ │
│  │    Activity     │  │    Fragment     │  │    Fragment     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                       ViewModel Layer                            │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │ CPRGuidance     │  │ Professional    │  │ EventLog        │ │
│  │ ViewModel       │  │ ViewModel       │  │ ViewModel       │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Domain/Service Layer                        │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │  Metronome   │ │    TTS       │ │   Timer      │            │
│  │   Service    │ │   Manager    │ │   Manager    │            │
│  └──────────────┘ └──────────────┘ └──────────────┘            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│  │    Event     │ │   Vibration  │ │    Audio     │            │
│  │   Logger     │ │   Manager    │ │   Manager    │            │
│  └──────────────┘ └──────────────┘ └──────────────┘            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    EventRepository                        │  │
│  │         (In-memory + Optional Local Persistence)          │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## Design Patterns

### MVVM (Model-View-ViewModel)
- **View**: Activities and Fragments handle UI rendering
- **ViewModel**: Business logic, state management, survives configuration changes
- **Model**: Data classes representing events, timers, and application state

### Repository Pattern
- Single source of truth for event data
- Abstracts data storage implementation

### Service Pattern
- Background services for metronome and timers
- Foreground service for reliable operation during emergency

## Key Components

### 1. Metronome Service
- Generates precise audio clicks at 100-120 BPM
- Uses AudioTrack for low-latency audio
- Runs as foreground service to prevent system killing it

### 2. Timer Manager
- Master arrest timer (total duration)
- Cycle timer (2-minute intervals)
- ROSC tracking with re-arrest support

### 3. Event Logger
- Timestamped event recording
- Categories: Airway, Rhythms, Interventions, Outcomes
- Export to text/JSON format

### 4. TTS Manager
- Queued voice prompts
- Priority system for critical announcements
- Configurable voice parameters

## Data Flow

```
User Action → View → ViewModel → Service/Repository → State Update → View Update
```

## State Management

Using Kotlin StateFlow for reactive state updates:

```kotlin
sealed class ArrestState {
    object Idle : ArrestState()
    data class Active(
        val startTime: Long,
        val currentCycle: Int,
        val events: List<TimestampedEvent>
    ) : ArrestState()
    data class ROSC(
        val roscTime: Long,
        val totalArrestDuration: Long
    ) : ArrestState()
}
```

## UI/UX Flow

### Launch Screen
```
┌─────────────────────────────┐
│                             │
│    ⚕️ CPR ASSIST           │
│                             │
│  ┌───────────────────────┐  │
│  │                       │  │
│  │   CPR GUIDANCE        │  │
│  │   (For Bystanders)    │  │
│  │                       │  │
│  └───────────────────────┘  │
│                             │
│  ┌───────────────────────┐  │
│  │                       │  │
│  │   PROFESSIONAL MODE   │  │
│  │   (For Responders)    │  │
│  │                       │  │
│  └───────────────────────┘  │
│                             │
│  [Medical Disclaimer]       │
└─────────────────────────────┘
```

### CPR Guidance Mode
```
┌─────────────────────────────┐
│ CALL 911 NOW                │
├─────────────────────────────┤
│                             │
│     PUSH HARD & FAST        │
│                             │
│    ┌─────────────────┐      │
│    │                 │      │
│    │   ● ● ● ● ●    │      │ ← Visual metronome
│    │                 │      │
│    └─────────────────┘      │
│                             │
│    100-120 per minute       │
│                             │
│    COMPRESSIONS: 47         │
│    TIME: 1:23               │
│                             │
│  ┌───────────────────────┐  │
│  │    SWITCH RESCUER     │  │ ← Flashes at 2 min
│  └───────────────────────┘  │
│                             │
│  [Stop] [AED Instructions]  │
└─────────────────────────────┘
```

### Professional Mode - Entry
```
┌─────────────────────────────┐
│                             │
│                             │
│                             │
│  ┌───────────────────────┐  │
│  │                       │  │
│  │                       │  │
│  │   CONFIRM CARDIAC     │  │
│  │      ARREST           │  │
│  │                       │  │
│  │                       │  │
│  └───────────────────────┘  │
│                             │
│                             │
│                             │
└─────────────────────────────┘
```

### Professional Mode - Active
```
┌─────────────────────────────┐
│ ARREST: 04:32    CYCLE: 2   │
├─────────────────────────────┤
│ [iGel] [Tube] [Cannula] [IO]│
├─────────────────────────────┤
│ [VF] [VT] [Asystole] [PEA]  │
├─────────────────────────────┤
│ [Shock] [Adrenaline] [Amio] │
│ [Narcan] [Fluids] [TXA]     │
│ [Atropine]                  │
├─────────────────────────────┤
│                             │
│        [  ROSC  ]           │
│                             │
├─────────────────────────────┤
│ Last: Adrenaline @ 03:45    │
│ Events: 7                   │
│                             │
│ [Pause] [View Log] [End]    │
└─────────────────────────────┘
```

## Required Permissions

```xml
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Technology Stack

- **Language**: Kotlin 1.9+
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: MVVM with StateFlow
- **Audio**: AudioTrack for low-latency metronome
- **TTS**: Android TextToSpeech API
- **UI**: Material Design 3 with custom high-contrast theme
