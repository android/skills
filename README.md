## Android skills

**Android skills** are a dedicated repository of **AI-optimized, modular instructions** and
resources, to help agents better understand and execute specific patterns that follow the best
practices and guidance on Android development
from [developer.android.com](https://developer.android.com).

Android skills follow the [open-standard agent skills](https://agentskills.io/home) - markdown
files (SKILL.md) that provide a technical specification of a task, and **ground LLMs** with
information on specialized domains and workflows.

Our Android skill development focuses on **use cases and workflows where evaluations show LLMs
underperform**.
We aren't prioritizing well-established areas where LLMs are already proficient, such
as basic Jetpack Compose best practices.

To learn more, read the official documentation:

- [Android skills](https://developer.android.com/tools/agents/android-skills)
- [Android CLI](https://developer.android.com/tools/agents/android-cli)
- [Android Studio](https://developer.android.com/studio/gemini/skills)

### Install Android skills

Use Android CLI to install a specific skill into the current directory:

```
android skills add r8-analyzer --project=.
```

Use Android CLI to install all Android skills to directories for all detected agents:

```
android skills add --all
```

If you don't have any existing agent directories and don't specify particular agents, the skills
will be installed for Gemini and Antigravity at `~/.gemini/antigravity/skills`.

**Options:**

- `--all` - Add all Android skills. If omitted, only the `android-cli` skill will be installed.
- `--agent` - A comma-separated list of agents to install the skill for. If omitted, the skill will be installed for all detected agents.
- `--project` - Path to a project root in which to install the skills.

### Activate skills

Your agent automatically activates skills that are relevant to your task. To use a skill, prompt your agent to complete a task that is related to the skill, for example "Make my app UI edge-to-edge." The agent should automatically find and use the skill if it has been installed.

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
