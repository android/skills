## Android skills

Android skills are a dedicated repository of AI-optimised, modular instructions and resources, to
help LLMs better understand and execute specific patterns that follow the best practices and
guidance on Android development from [developer.android.com](https://developer.android.com).

Android skills follow the [open-standard agent skills](https://agentskills.io/home) - markdown
files (SKILL.md) that provide a technical specification of a task, and ground LLMs with information
on specialized domains and workflows.

To learn more, read the official documentation:

- [Android skills](https://developer.android.com/tools/agents/android-skills)
- [Android CLI](https://developer.android.com/tools/agents/android-cli)
- [Android Studio](https://developer.android.com/studio/gemini/skills)

### Install Android skills

Use Android CLI to install Android skills to directories for all detected agents:

```
android skills add [--all] [--agent=<agent-name>] [--skill=<skill-name>]
```

If you don't have any existing agent directories and don't specify particular agents, the skills
will be installed for Gemini and Antigravity at `~/.gemini/antigravity/skills`.

**Options:**

- `--all` - Add all Android skills. If omitted (and `--skill` isn't specified), only the
  `android-cli` skill will be installed.
- `--agent` - A comma-separated list of agents to install the skill for. If omitted, the skill will
  be installed for all detected agents.
- `--skill` - Specific skill that you want to install. If omitted (and `--all` isn't specified),
  only the `android-cli` skill will be installed.

## Disclaimer

AI can make mistakes, so always double-check the results.

## Contributing

Submit a GitHub issue to provide feedback, report issues, or make new skill requests and changes.

Public contributions are not accepted at this time.

## License

Android Skills is licensed under the [Apache License 2.0](LICENSE.txt). See the `LICENSE.txt` file
for
details.

## Review our community guidelines

This project follows
[Google's Open Source Community Guidelines](https://opensource.google/conduct/).