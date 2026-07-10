{{#IF_ANY data_sources, disclosure}}
{{ACTIVATE_GOAL}}
{{/IF_ANY}}
## Data Safety and Privacy Audit

### Policies to Verify

- **The Policy Spirit**: Users deserve complete transparency regarding what
  personal or sensitive information leaves their devices. Under Play Store
  rules, any off-device transmission of user data must be explicitly declared in
  the Play Store Data Safety form, and sensitive or non-obvious data collection
  requires prior prominent in-app disclosure and affirmative user consent.

- **Common Evaluation Matrix**:
  | Case | Technical Observation | `user_initiated` | `is_third_party` | Disclosure Status | Severity | Issue Summary |
  | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
  | **1** | **Not Transferred** (Local only) | N/A | N/A | `EXEMPT` | `SUGGESTION` | "Data Safety Compliant (Local-Only)" |
  | **2A** | **Transferred** + **Background** + **No Disclosure** | `false` | (Any) | `MISSING` | `CRITICAL` | "**Silent Background Transfer**": Policy violation. Data is sent without prior disclosure. |
  | **2B** | **Transferred** + **Background** + **Disclosure Found** | `false` | (Any) | `DISCLOSED` | `IMPORTANT` | "**Background Transfer (Disclosure Claimed)**": Evidence found; requires Critic verification. |
  | **3** | **Transferred** + **User-Initiated** + **First Party** | `true` | `false` | `EXEMPT` | `IMPORTANT` | "**User-Initiated Collection**": Disclosure exempt, but **must be declared** in Play Store form. |
  | **4** | **Transferred** + **User-Initiated** + **Third Party** | `true` | `true` | `EXEMPT` | `SUGGESTION` | "**Manual Sharing**": **Policy Exempt**. User triggers transfer to 3P; no disclosure or declaration needed. |

- **Exemptions Reference**:
  - **Anonymous Data**: Fully anonymized data not linked to a user is exempt.
  - **End-to-End Encryption (E2EE)**: Data E2EE where the developer cannot read
    it is exempt.
  - **Open Web WebView**: Data entered into a WebView navigating the open web is
    exempt.
  - *If any of these apply, downgrade severity to `SUGGESTION` and mark
    `EXEMPT`.*

---

### The Audit Protocol

#### Step 0: Facts orientation

- **Identify Third-Party SDKs**: If you observe network, crash-reporting, or
  tracking telemetry, check the project's build files (e.g., build.gradle,
  build.gradle.kts, libs.versions.toml) on-demand using your file-reading tools
  to verify which SDKs (Analytics, Ads, etc.) are integrated.
- **Understand Off-Device Sinks**: Use your native knowledge of standard Android
  framework components to identify transmission destinations. Focus on active
  egress vectors such as standard HTTP/REST libraries (e.g., OkHttp, Retrofit,
  Ktor, HttpURLConnection), WebViews passing data off-device, background syncing
  tasks, or analytic/telemetry frameworks (e.g., Firebase, Mixpanel, Adjust,
  AppsFlyer).
- **Identify Disclosures**: Review `disclosure` below. Use these as starting
  points for Step 2.

#### Step 1: Verify behavioral transmission (Forward Trace)

For each source in `data_sources` listed below, trace the technical signal from
the exact location provided to its transmission endpoint.
- **Mandatory Starting Point**: Navigate to the file and line number specified
  in the `data_sources` list.
- **Loop Discipline**: You must analyze the transmission path for *every* piece
  of evidence listed to verify if it reaches an off-device sink.
- **Evidence**: Provide the file/line where the data is passed to the sink and a
  brief justification of the sink's known transmission behavior (e.g., network
  upload, telemetry payload).
- **Semantic Grounding**: Use the provided `*Description*` to verify that the
  code evidence actually matches the semantic scope of the data type. If the
  variable or logic refers to unrelated data (e.g., a `fileName` that does not
  identify a user `NAME`), treat it as a false positive and follow the
  **Evidentiary Standard** to downgrade or prune the finding.
- **Sink Categorization**: Determine the destination:
  1. **First Party / Service Provider (`is_third_party: false`)**:
     Developer-controlled servers, Firebase, custom APIs, etc.
  2. **Third Party (`is_third_party: true`)**: Social SDKs, ad networks, or
     user-selected apps via Android Share Sheet (`Intent.ACTION_SEND`).
- **Ambiguity Mandate**: If the destination is ambiguous (e.g., generic URL or
  obfuscated SDK), you MUST default to `is_third_party: false` (erring on the
  side of Collection).
- **On-Device Transfer**: Treat silent data sharing to another app via
  Intents/ContentProviders as a transfer (`is_transferred: true`), even if it
  doesn't use the network.

#### Step 2: Semantic Search for Protection (UI Disclosure)

**Conditional Execution**: Execute this step **ONLY IF** `is_transferred` is
`true` AND `user_initiated` is `false`.
- **Rationale**: User-initiated actions (Case 3 and 4) are exempt from Prominent
  Disclosure. Do not search for disclosures if `user_initiated` is true.
- **Semantic Search**: Review the `disclosure` list below.
- **Gatekeeper Check**: Check if the Activity/Fragment displaying that consent
  acts as a gatekeeper (i.e., gates the initialization of the data-collection
  logic). Do not attempt to trace backwards from a low-level repository up to
  the UI.
- **Content Check**: The UI must state *what* is collected and *how* it is used.
- **Evidence**: Provide the file/layout name and the specific text found.

#### Step 3: Synthesis

- **`findings` (CRITICAL)**: You must construct and output exactly **one**
  object inside the `"findings"` array for **every** data type found in the
  `data_sources` list below, regardless of whether it is compliant or a
  violation.
- **Evaluating Keys**: For each data type object in `"findings"`, evaluate and
  populate:
  1.  **psl_constant**: Set this exactly to the uppercase **PSL Constant** of
     the data type (listed as the **Data Type** heading in the **Data Sources to
     Trace** section below, e.g., `"PRECISE_LOCATION"`, `"CONTACTS"`). This is
     critical for downstream matching.
  2.  **policy_id**: Set this exactly to either `"prominent_disclosure_policy"`
     or `"data_safety_section"` depending on the check being performed.
  3.  **is_transferred**: `true` (JSON Boolean) if the data is transferred
     off-device or on-device to a third party. `false` (JSON Boolean) if local
     only.
  4.  **user_initiated**: `true` (JSON Boolean) if the user explicitly triggers
     the transfer.
  5.  **is_third_party**: `true` (JSON Boolean) if the destination is a Third
     Party (e.g., Share Sheet, Social SDK). `false` (JSON Boolean) if it's a
     First Party/Service Provider (e.g., your own backend).
  6.  **prominent_disclosure_status**: Set strictly to one of these three
     uppercase enums: `"DISCLOSED"` (visible disclosure and user consent found),
     `"MISSING"` (no disclosure found, or is not an affirmative gatekeeper), or
     `"EXEMPT"` (exempt from prominent disclosure under policies).
  7.  **purpose**: Categorize why the data is being collected (e.g., "App
     functionality", "Analytics", "Local functionality only").
  8.  **linked_to_user**: `true` (JSON Boolean) if the data is tied to an
     identity, email, or device ID, otherwise `false` (JSON Boolean).

- **Map findings strictly to the 5 Cases:** Use the Case matrix in "Policies to
  Verify" to set the appropriate `severity` and `issue_summary` based on the
  combination of `is_transferred`, `user_initiated`, `is_third_party`, and
  `prominent_disclosure_status`.

---

### Domain-Specific Heuristics

Apply the following heuristics while strictly adhering to the Heuristics &
Extrapolation Boundaries defined in your Execution Mandates:
1. **Implicit Data Leaks (The Logger Loop)**: Inspect if any custom logging
   frameworks (e.g., Crashlytics, custom error loggers, or telemetry SDKs)
   receive sensitive variables as part of diagnostic payloads. If a user
   identifier (e.g., email, account ID) or precise location is passed to a
   logger that uploads payloads off-device, this counts as **transferred**
   (`is_transferred`: true).
2. **Third-Party SDK Siphoning**: Look at the project's build files on-demand
   (e.g., build.gradle or libs.versions.toml). If SDKs like Google Ads, or
   Firebase are initialized and have access to the context, evaluate if
   they are siphoning advertiser IDs or device identifiers automatically. If
   those libraries are loaded and the manifest requests broad network
   permissions, treat those device identifiers as **transferred**
   (`is_transferred`: true) for analytics/marketing purposes.
3. **Indirect Consent**: If you find an `AlertDialog` or disclosure, verify if
   it is an actual gatekeeper. If the app begins tracking user data *prior* to
   the user tapping "Accept", or if the user can close the dialog and continue
   using the app while data tracking remains active, flag this as a `CRITICAL`
   violation under `prominent_disclosure_policy`.

---

### Codebase Context & Evidence

{{#IF semantic_files.LEGAL}}
**Semantic Triage Starting Points (Files of Interest)**:
The following files likely contain privacy or consent logic:
- **LEGAL** related:
  {{#EACH semantic_files.LEGAL}}
  - `{{ITEM}}`
  {{/EACH}}
{{/IF}}

{{#IF data_sources}}
**Data Sources to Trace**:
{{#EACH data_sources}}
- **Data Type**: {{KEY}}
  *Description: {{VALUE.description}}*
  {{#EACH VALUE.findings}}
  - `{{ITEM}}`
  {{/EACH}}
{{/EACH}}
{{/IF}}

{{#IF disclosure}}
**UI Disclosure Starting Points**:
{{#EACH disclosure}}
- `{{ITEM}}`
{{/EACH}}
{{/IF}}

## Output schema

Save final JSON output to `{{TEMP_DIR}}/worker_{{GOAL_NAME}}.json`.

```json
{
  "domain": "Data Safety and Privacy",
  "findings": [
    {
      "psl_constant": "STRING_VALUE (The exact PSL Constant, e.g., USER_ACCOUNT)",
      "policy_id": "prominent_disclosure_policy | data_safety_section",
      "issue_summary": "STRING_VALUE",
      "severity": "CRITICAL | IMPORTANT | SUGGESTION",
      "files_involved": ["STRING_VALUE"],
      "evidence": "STRING_VALUE",
      "recommendation": "STRING_VALUE",

      # UNIFIED DATA SAFETY METADATA LOOKUP KEYS:
      "is_transferred": true | false,
      "user_initiated": true | false,
      "is_third_party": true | false,
      "prominent_disclosure_status": "DISCLOSED | MISSING | EXEMPT",
      "purpose": "STRING_VALUE",
      "linked_to_user": true | false
    }
  ]
}
```
