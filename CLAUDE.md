# Project: Custom Android SMS App

## What this project is
A custom Android SMS app that handles real carrier text messages on the device.
The motivation: I dislike Google Messages and want full control over theming and how
messages are organized. Privacy matters — I do NOT want a third-party app reading my
texts, so I'm building my own where permissions stay on-device and nothing leaves it.

This is an SMS app, NOT a WhatsApp-style internet messenger. There is no server and no
backend. Do not steer me toward building one.

## Who I am (read this every session)
This is my first mobile app ever. I have no prior knowledge of Kotlin, Android
development, or mobile development. Teach me as we build — explain what each piece does
and why. Don't hand me code to paste blindly. Treat me as a capable beginner: I learn
fast, but assume zero Android-specific knowledge.

## How to teach me
- Before writing code for a new concept, explain the concept in plain language first.
- The first time we touch something new, walk through the code block by block.
- Tell me what to click in Android Studio — I've never used it.
- Flag common beginner mistakes for each step so I avoid them.
- Go one step at a time. Confirm the current step works on my end before moving on.
  Don't race ahead or dump large code blocks without explanation.
- If I push back with "explain this first," stop and explain before continuing.

## Skills I'm building (don't let me offload these)
Kotlin fundamentals, Jetpack Compose state management, Android lifecycle,
Room/SQL queries. Make me attempt these before solving.

## Stack
- Kotlin
- Jetpack Compose (UI)
- Room (my own organization layer on top of the system messages)
- Android Telephony / SMS APIs
If you'd recommend something different for a beginner, tell me why before we commit.

## Build plan — phased, in order
- **Phase 0 — Read-only viewer:** request READ_SMS, query the SMS content provider,
  render my real conversation threads in a custom-themed UI. No default-app takeover yet.
  This is where I nail theming and message organization first.
- **Phase 1 — Become the default SMS app:** add the components Android requires (incoming
  SMS receiver, incoming MMS receiver, send activity, respond-via-message service) so I
  can send and reply. Plain SMS only for now — skip MMS/group messaging to keep it light.
- **Phase 2+ — Organization features:** labels, smart folders, tags, search, per-contact
  themes, scheduled send. We spec these together when we get there.

## Testing
I test on a real Android phone, not the emulator (the point is my actual texts).

## Current status
<!-- Update this line as we progress. -->
Phase 0 in progress. Done: blank Compose app runs on device (Moto Razr 2025); READ_SMS declared in manifest + runtime permission request working (button + state-driven status). Next: query the SMS content provider and display real messages.