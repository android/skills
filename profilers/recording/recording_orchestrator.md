# Recording Orchestrator

Use the guidelines below to orchestrate between recording workflows after
disambiguating the user intent (for example, recording a system trace or a
heap dump). The guidelines below ensure pre-flight checks are always performed
and domain-specific profiling flags are applied accurately.

## Workflow Selection

At this point, you should have an answer to the following question: "User
intends to record a _____". For example, "User intends to record a system
trace".

## Handling Composite Requests

If the user's request involves multiple distinct recording goals (for example,
setting up a specialized environment AND recording a generic trace), do not
execute them simultaneously.

1. Break down the request and propose a sequential execution plan to the user.
2. Ask the user for confirmation to start the first step.
3. Do not proceed to the next workflow until the current one is completed.

## Workflow Discovery and Routing

This skill supports multiple specialized recording workflows. To determine the
right workflows to use:

1. Use your file search tools (for example, `grep_search`) to recursively scan
   the `$SKILL_ROOT/recording/workflows/` directory for workflow entrypoints
   (markdown files defining a top-level `name:` key in their frontmatter,
   ignoring internal `references/` subdirectories).
2. Compare the user's request and intent against the `name:`, `description:`,
   and `keywords:` fields to identify matching workflows. If multiple are
   found, present them as options to the user and proceed with the
   user's selection.