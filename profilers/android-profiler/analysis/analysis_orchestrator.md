Use the guidelines below to find the right analysis path to follow based on the
user intent (for example, analyzing a system trace or a heap dump).

## Understand the Goal

Identify the explicit artifact and the analysis goal the user wants (for
example, analyzing a system trace or a heap dump). Before proceeding, ensure you
have an answer to the following question: "User intends to
{analyze \| query \| investigate} {target artifact and goal}".

## Workflow Discovery and Execution Planning

If the user's request involves multiple distinct analysis goals (for example,
analyzing a trace for jank AND checking for memory leaks or GPU issues), do not
merge them:

1. Use your file search tools (for example, `grep_search`) to recursively scan the `$SKILL_ROOT/analysis/workflows/` directory for workflow entry points (markdown files defining a top-level `name:` key in their frontmatter, ignoring internal `references/` subdirectories). Find all the workflows that match the user's target artifact and goal.
2. Propose all the matched workflows as options to the user, along with simple details on why they apply. Work with the user and converge on an execution plan.
3. Continue with the user selection.

> The user may ask to execute one or more workflows - sequentially or in
> parallel, or stitch two analysis workflows together. When this happens,
> perform a quick scan of these workflows to ensure they are compatible with
> each other, and clarify with the user before proceeding.