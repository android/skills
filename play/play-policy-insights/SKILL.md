---
name: play-policy-insights
description: Automated auditor designed to verify Android applications against Google Play Policy domains. It cross-references static code analysis with Play Store declarations to generate deterministic compliance reports, identifying undeclared data collection, architectural risks, and missing disclosures across Permissions and APIs Hygiene, User Account and Identity, and Data Safety and Privacy domains.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-07-13'
  keywords:
  - account deletion
  - accessibility api
  - all files access
  - audio recording
  - audit
  - compliance
  - contacts access
  - data disclosure
  - data safety
  - data safety label
  - data transmission
  - demo credentials
  - exact alarm
  - foreground services
  - location access
  - login credentials
  - manifest hygiene
  - package visibility
  - permissions hygiene
  - photo and video access
  - photopicker
  - play policy
  - pre-submission audit
  - privacy policy
  - prominent disclosure
  - restricted permissions
  - scoped storage
  - sms and call log
  - static analysis
  - target sdk
  - user consent
---

# Play Policy Insights: data safety, login credentials, and restricted permissions

You must audit Android apps for three specific policy domains. You must check
data safety, demo login credentials, and restricted permissions.

## Path Resolution

*   **repo_root**: Absolute path to the directory containing this `SKILL.md`.
*   **app_dir**:: Absolute path to the directory containing app's code.
*   **temp_dir**: Absolute path to the scratch directory at the workspace root.
    It is located at `.scratch/play_policy_insights_<uuid>`. **Containment
    Mandate**: You must confine all file system writes, intermediate artifacts,
    and logs strictly to this directory. This ensures the skill remains portable
    and safe across diverse execution environments, including local harnesses
    and CI/CD pipelines, by avoiding reliance on system-level temporary paths or
    user home directories.

## Critical mandates

-   **Execution Mode Awareness** Before starting Phase 2, evaluate if your
    execution environment provides a tool to spawn or delegate tasks to
    general-purpose sub-agents (e.g., tools often named `invoke_agent`,
    `delegate_task`, or `spawn_worker`, using generic agent profiles like
    'generalist' or 'coding_agent').

-   If **YES**, you MUST use **Mode A (Delegation)**.

-   If **NO**, use **Mode B (Sequential Self-Execution)**. You must read the
    prompt files intended for the subagents, follow their instructions, and
    write the expected output files to disk.

-   **Sub-agents orchestration:**

    -   If you use "Mode A (Delegation)", wait for "SUCCESS" confirmation from
        sub-agents to know when they are done.

    -   **Idempotency & Timeout Safeguard**: If a sub-agent fails or times out,
        you MUST verify the presence and integrity of its target output file
        (e.g., `<temp_dir>/worker_<goal_name>.json`) before retrying. If the
        file exists and contains valid JSON, treat the execution as **SUCCESS**
        and proceed. Otherwise, retry up to three times.

-   **Fail-fast mandate:** The automated audit in Phase 1 is the source of
    truth. If `orchestrator.py` fails, you must stop immediately with an
    explanation of failure. Do not use manual auditing as a fallback.

## The two-phase protocol

### Phase 1: Fact gathering and triage

1.  **Initialize and triage**:
    -   Run `python3 <repo_root>/scripts/orchestrator.py init <app_dir>`.
    -   This will create the scratch environment, perform static analysis, map
        the codebase, identify audit goals, and produce prompts for subagents
        for each audit goal and prompts for designated critic and aggregator
        subagents.
    -   You must wait (up to 5 minutes) for the script to finish.
2.  **Capture environment**: Note values of the `temp_dir`, and
    `activated_goals` from the JSON output. You will need them in Phase 2.
3.  **Evaluate goals**: If `activated_goals` is empty, skip to step 3 of Phase 2
    (Aggregation). Otherwise, proceed to step 1 of Phase 2 (Detailed analysis).

### Phase 2: Goal-oriented audit

Determine your execution capabilities and proceed with either Mode A OR Mode B.

#### Mode A: Orchestrator WITH Delegation Capabilities (Parallel)

1.  **Detailed analysis**: For each goal in `activated_goals` (e.g.,
    `permissions_and_apis`, `data_safety_part_1`, `data_safety_part_2`),
    delegate to a sub-agent. **Concurrency Limit:** You must not spawn more than
    3 sub-agents simultaneously. Spawn the first batch of up to 3, wait for
    their completions, and then spawn the next batch. Repeat until all goals are
    complete. Pass the prompt: `"Read your instructions from
    <temp_dir>/prompt_worker_<goal_name>.md and execute. MANDATORY: You must
    use your file-writing capabilities to save your final JSON findings directly
    to the file system at <temp_dir>/worker_<goal_name>.json. You are strictly
    forbidden from outputting the JSON in your chat response. To minimize
    context usage, your final response must be exactly 'SUCCESS' and nothing
    else."` **Validate**: Confirm every
    `<temp_dir>/worker_<goal_name>.json` exists and contains valid JSON. If a
    sub-agent fails or times out, but the valid JSON output file is already
    present on disk, do NOT retry; proceed normally. Only retry the
    corresponding worker (up to three times) if the file is missing or invalid.
2.  **Aggregate Findings**: Execute the python aggregation command:
   `python3 <repo_root>/scripts/orchestrator.py aggregate <temp_dir>`. This
   produces `aggregated_findings.json` and returns a JSON object containing
   `critic_chunks` representing the number of chunks to verify (e.g.,
   `{"temp_dir": "...", "critic_chunks": 2}`).
3.  **Parallel Critic review**: For each chunk index `i` from 1 to
   `critic_chunks`, delegate to a sub-agent. **Concurrency Limit:** You must not
   spawn more than 3 critic sub-agents simultaneously. Batch them in groups of 3
   as above. Pass the prompt:
   `"Read your instructions from <temp_dir>/prompt_critic_<i>.md and execute. MANDATORY: You must use your file-writing capabilities to save your final JSON findings directly to the file system at <temp_dir>/critic_output_<i>.json. You are strictly forbidden from outputting the JSON in your chat response. To minimize context usage, your final response must be exactly 'SUCCESS' and nothing else."`
   **Validate**: Confirm each `<temp_dir>/critic_output_<i>.json` exists and
   contains valid JSON before proceeding. If it failed or timed out, but the
   valid JSON file is present, proceed normally. Otherwise, retry that specific
   critic chunk.
4.  **Proceed to Finalization** (Step 4 below)

#### Mode B: Orchestrator WITHOUT Delegation Capabilities (Sequential)

1.  **Detailed Analysis**: For each goal in `activated_goals`, sequentially:
    -   Read the contents of `<temp_dir>/prompt_worker_<goal_name>.md`.
    -   Execute the instructions contained within that file yourself.
    -   **CRITICAL**: You MUST format your findings exactly as requested in the
        prompt and save them to `<temp_dir>/worker_<goal_name>.json`. **Do not**
        summarize findings in your thoughts or chat; move to the next task.
    -   **Validate**: Confirm `<temp_dir>/worker_<goal_name>.json` exists before
        moving to the next goal.
2.  **Aggregate Findings**: Execute the python aggregation command:
   `python3 <repo_root>/scripts/orchestrator.py --aggregate <temp_dir>`.
   This produces `aggregated_findings.json` and returns a JSON object containing
   `critic_chunks` representing the number of chunks to verify.
3.  **Sequential Critic review**: For each chunk index `i` from 1 to
   `critic_chunks`, sequentially:
    -   Read the contents of `<temp_dir>/prompt_critic_<i>.md`.
    -   Execute the steps yourself and save your findings to
      `<temp_dir>/critic_output_<i>.json`.
    -   **Validate**: Confirm `<temp_dir>/critic_output_<i>.json` exists before
      moving to the next chunk.
4.  **Proceed to Finalization** (Step 4 below)

#### Finalization (Both Modes)

4.  **Present findings**: Run `python3 <repo_root>/scripts/generate_report.py <temp_dir>`. 
    It will produce `<temp_dir>/compliance_report.md`. Present this output file to user.
5.  **STOP**: The audit is complete. Await further instructions.
