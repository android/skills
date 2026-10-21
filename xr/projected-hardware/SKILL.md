---
name: projected-hardware
description: Provides instructions and workflows for accessing and managing hardware
  on Android XR projected devices, such as audio glasses and display glasses. Use
  this skill when building experiences for intelligent eyewear in an Android app and
  you need to access device hardware like the camera and microphone, handle glasses-specific
  runtime permissions, or manage connection lifecycles using projected context.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-17'
  keywords:
  - Android XR
  - intelligent eyewear
  - audio glasses
  - display glasses
  - glasses hardware
  - ProjectedContext
  - projected context
  - runtime permissions
  - CameraX
  - audio recording
---

## Prerequisites

- You must use a `compileSdk` of 36 or higher.

## Connection and lifecycle management

Intelligent eyewear hardware is only accessible while the glasses are connected
to the host phone. Monitor connection state and manage resource lifecycles using
`ProjectedContext`. You must read
[Understand projected activities and projected contexts](references/android/develop/xr/jetpack-xr-sdk/glasses/support-different-types.md) to understand
how to get a projected context and [how to manage its lifecycle](references/android/develop/xr/jetpack-xr-sdk/glasses/check-availability.md).

## Request glasses hardware permissions

- **Hardware permissions and opt-in** : You must read [Request hardware permissions](references/android/develop/xr/jetpack-xr-sdk/request-hardware-permissions.md) to understand how to declare camera and microphone runtime permissions in the Android Manifest file, request runtime permissions in different scenarios, and handle permission results.

## Glasses hardware access and capabilities

- **Camera capture and CameraX** : You must read [Capture an image with the glasses' camera](references/android/develop/xr/jetpack-xr-sdk/access-hardware-projected-context.md) to understand the implementation patterns for capturing a photo with the glasses' camera using a `ProjectedContext`.
