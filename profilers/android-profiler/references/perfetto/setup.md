# Getting `trace_processor` working

> **Prerequisite:** Ensure `$SKILL_ROOT` is initialized per
> `$SKILL_ROOT/references/env_setup.md`.

## Put `trace_processor` on the `PATH`

If the workspace does not already have a pre-downloaded `trace_processor`
wrapper, download it from <https://get.perfetto.dev/trace_processor> to
`$SKILL_ROOT/bin/trace_processor` and make it invocable for this session:

```sh
chmod +x "$SKILL_ROOT/bin/trace_processor"  # ensure exec bit is set
export PATH="$SKILL_ROOT/bin:$PATH"
trace_processor --version  # smoke test (ensure > v57.0, e.g. v57.1+)
```

After this, every bare `trace_processor ...` command in this skill works
verbatim. On Windows, skip the `PATH` setup and invoke it as
`python "$SKILL_ROOT/bin/trace_processor" ...` instead.

Notes:

- The first invocation downloads the prebuilt native binary (picking the
  right one for the host platform) into `~/.local/share/perfetto/prebuilts/`
  and caches it; only the first call pays the download cost.
- If the user's environment has its own mandatory `trace_processor`
  (Google-internal, OEM build environments, CI images), prefer that
  team-specific setup instead.
