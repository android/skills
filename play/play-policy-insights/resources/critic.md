# Review of specialized policy findings (Chunked Critic)

Review a specific chunk of identified policy findings to identify potential
false positives, exaggerated claims, or inaccuracies due to lack of evidence.

### Provided context files

The prompt provides absolute paths to files and directories:
- `{{TEMP_DIR}}`: The temporary scratch directory for this audit.
- `input_file`: `{{TEMP_DIR}}/input_critic_{{CHUNK_INDEX}}.json` contains the
  chunk of findings to verify.

### Instructions

1.  **Read input chunk**: Read the JSON object in
   `{{TEMP_DIR}}/input_critic_{{CHUNK_INDEX}}.json`. The keys (e.g. `"1"`,
   `"2"`, `"3"`) correspond to the finding IDs.
2.  **Verify each finding**:
    -   For each finding, analyze its evidence against the source files in
      `{{APP_DIR}}`.
    -   Determine the verdict:
        -   `"VERIFIED"`: True Positive. The codebase confirms the policy
          violation.
        -   `"MANUAL_REVIEW"`: Ambiguous code or abstract logic where automatic
          verification is impossible.
        -   `"PRUNED"`: False Positive. The codebase is compliant, or the
          finding is not supported by actual evidence.
    -   Provide your verification details in the output JSON.
3.  **Optional Editorial Overrides**: If (and only if) you need to edit, refine,
   or moderate the worker finding's text, you may include one or more of these
   optional keys to your decision object. **If you agree with the worker's text,
   you MUST omit these keys entirely from your JSON.**
    -   `"issue_summary"`: Write a more accurate, tailored summary.
    -   `"severity"`: Set to `"CRITICAL"`, `"IMPORTANT"`, or `"SUGGESTION"` to
      override.
    -   `"recommendation"`: Write a tailored, codebase-specific remediation
      step.
4.  **Save Results**: Save your final "Thin JSON" mapping to
   `{{TEMP_DIR}}/critic_output_{{CHUNK_INDEX}}.json`.

### Audit principles for false positive detection

-   **Environment detection**: **Do not** flag emulator or root detection as
  violations unless evidence shows malicious intent or review evasion.
-   **Speculative collection**: Prune claims based solely on permission
  presence. Require evidence of actual data access and transfer (whether
  off-device network egress or on-device sharing to a third-party app).
-   **Standard patterns**: **Do not** flag standard Android architectural
  patterns unless used maliciously.
-   **Surgical Evidence Standard**: Prune or downgrade any finding where the
  worker has speculated on a transmission pathway that cannot be directly
  verified in the immediate source files, or where the finding fails to cite a
  concrete file and line number containing the direct policy violation.
-   **Data Safety Flag Verification**: If a finding includes Data Safety flags
  (e.g., `user_initiated`, `is_third_party`), strictly verify them. If the
  worker claims `user_initiated: true`, ensure there is undeniable evidence of
  explicit user interaction triggering the transfer. If the worker claims
  `is_third_party: true`, verify the sink is definitively outside the
  developer's control (e.g., Android Share Sheet, Social Media SDK). If evidence
  is lacking, downgrade the finding or use `"issue_summary"` to correct the
  claim.
-   **Gatekeeper Validation**: If a worker claims a finding is compliant because
  a disclosure exists (`prominent_disclosure_status: "DISCLOSED"`), you MUST
  verify that the UI acts as a strict gatekeeper. If data collection begins
  before the user taps "Accept", or if they can dismiss it and continue,
  override the `"severity"` to `"CRITICAL"` and explicitly state the gatekeeper
  is invalid.

### Output JSON format

**Important**: The output must be a pure JSON object mapping the sequential
finding IDs from your input file to their decisions. If there are no findings in
the chunk, return `{}`.

```json
{
  "1": {
    "action": "VERIFIED | MANUAL_REVIEW | PRUNED",
    "confidence": "High | Medium | Low",
    "critic_justification": "A concise, technical explanation of your decision based on codebase evidence.",
    "issue_summary": "OPTIONAL: Overridden issue summary text",
    "severity": "OPTIONAL: CRITICAL | IMPORTANT | SUGGESTION",
    "recommendation": "OPTIONAL: Overridden recommendation text"
  }
}
```
