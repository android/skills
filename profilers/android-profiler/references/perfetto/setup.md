# Getting `trace_processor` working

> **Prerequisite:** Ensure `$SKILL_ROOT` is initialized per
> `$SKILL_ROOT/references/env_setup.md`.

## Put the bundled `trace_processor` on the `PATH`

The skill ships a `trace_processor` wrapper at
`$SKILL_ROOT/references/perfetto/bin/trace_processor`. Make it invokable for
this session:

```sh
# some installs lose the exec bit
chmod +x "$SKILL_ROOT/references/perfetto/bin/trace_processor"
export PATH="$SKILL_ROOT/references/perfetto/bin:$PATH"
trace_processor --version                   # smoke test
```

After this, every bare `trace_processor ...` command in this skill works
verbatim. If each command runs in a fresh shell, repeat the `export` line in
that command. On Windows, skip the `PATH` setup and invoke it as
`python "$SKILL_ROOT/references/perfetto/bin/trace_processor" ...` instead.

Notes:

- The first invocation downloads the prebuilt native binary (picking the
  right one for the host platform) into `~/.local/share/perfetto/prebuilts/`
  and caches it; only the first call pays the download cost.
