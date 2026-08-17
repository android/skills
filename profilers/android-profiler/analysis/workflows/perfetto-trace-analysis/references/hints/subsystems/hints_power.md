---
subsystem: power
description: >
  Expert hints for battery drain, power rails, network traffic, and wake locks.
keywords:
  - power
  - battery
  - wake lock
  - suspend
  - network
  - modem
  - bluetooth
  - drain
---

# Power & Network Expert Hints

- When investigating overall battery drain, start by querying `power_rails`
  track and summing energy consumed (`power_ma * duration`) for each rail.
- To check if a device is sleeping correctly during screen-off periods, query
  the `suspend_state` track; a lack of time in "suspended" state indicates a
  wakefulness problem.
- To find the root cause of the device failing to suspend, query the
  `kernel_wakelock` track and aggregate total duration for each wake lock.
- If a top kernel wake lock name contains "bt_" or "bcm" (for example,
  `bt_host_wake`), cross-reference its timing with events in the
  `bluetooth_scan_results` track.
- If the modem rail in `power_rails` shows high consumption, query the
  `network_packets` table and aggregate traffic volume by UID to identify app.
- When analyzing network traffic from a shared UID, use the `socket_tag`
  associated with network packets for granular attribution.
- Convert impact into a common energy unit like milliwatt-hours (mWh) to
  compare the severity of different issues.
