# Environment setup

## Set `$SKILL_ROOT`

Set up `$SKILL_ROOT` once per session.

Every file this skill references - workflow markdown, reference docs, helper
scripts, and downloaded dependencies - is written in the form `$SKILL_ROOT/...`,
relative to the **skill root** (the directory holding this skill's `SKILL.md`).
Set it once to the absolute path of the directory you loaded `SKILL.md` from:

```sh
# Substitute with the directory the SKILL.md lives in.
export SKILL_ROOT="/absolute/path/to/skills/profilers/android-profiler"
```
