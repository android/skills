# Analysis Orchestrator

Use the guidelines below to find the right analysis workflow to execute based
on the user intent (for example, analyzing a system trace or a heap dump).

Ensure you can answer the following: "User intends to
[analyze/query/investigate] ______". For example,
"User intends to analyze a heap dump to investigate a memory leak".

## Handling Composite Requests

If the user's request involves multiple distinct analysis goals (for example,
analyzing a trace for jank AND checking for memory leaks or GPU issues),
do not execute them simultaneously.

1. Break down the request and propose a sequential execution plan to the user.
2. Ask the user for confirmation to start the first step.
3. Do not proceed to the next workflow until the current one is completed.

## Workflow Discovery and Routing

This skill supports multiple specialized analysis workflows. To determine the
right workflows to use:

1. Use your file search tools (for example, `grep_search`) to recursively scan
   the `$SKILL_ROOT/analysis/workflows/` directory for workflow entrypoints
   (markdown files defining a top-level `name:` key in their frontmatter,
   ignoring internal `references/` subdirectories).
2. Compare the user's request and intent against the `name:`, `description:`,
   and `keywords:` fields to identify matching workflows. If multiple are
   found, present them as options to the user and proceed with the
   user's selection.