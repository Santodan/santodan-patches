# Kilo Code with the local Qwen model

The Kilo Code VS Code extension 7.8.8 is installed. Open
`E:\Morphe\Morphe.code-workspace`, run **Developer: Reload Window**, and open
the Kilo sidebar. Select the **morphe** agent and
**Qwen 27B Abliterated - Morphe** from **LM Studio - Local Qwen** if the UI does
not select them automatically. Start a fresh chat after reloading.

## Workspace configuration

`E:\Morphe\.kilo\kilo.jsonc` configures:

- Local API: `http://localhost:1234/v1`, exact model ID
  `huihui-qwen3.8-27b-abliterated`.
- Context 16,384 tokens and output 4,096, matching the currently loaded model.
- Only the local provider enabled; auxiliary model requests use the same local model.
- Automatic file reads/edits, searches, commands, skills, and todo updates.
- Context compaction and pruning of old tool output.
- A focused `morphe` agent without subagent/agent-manager, web, or notebook tools.
- Session sharing disabled.
- Skills registered directly from `santodan-patches/lmstudio/skills`.
- Instructions loaded from `.kilo/morphe-instructions.md`.

The focused agent matters: the first live test of the default `code` agent sent
16,573 prompt tokens and exceeded the loaded 16,384-token context before beginning.
Use `morphe` for this workspace to reduce prompt/tool overhead. For longer patch
tasks, consider loading a larger context in LM Studio if memory permits and updating
`provider.morphe-local.models.huihui-qwen3.8-27b-abliterated.limit.context` to match.
Do not increase only Kilo's advertised limit while leaving LM Studio at 16K.

LM Studio reported an IQ3_S model, tool-use support, flash attention, GPU KV-cache
offload, and MTP speculation enabled. These load settings have been preserved.
No guessed sampling or chat-template overrides have been added. This setup reduces
agent overhead; it does not establish the best quantization or hardware performance.

The workspace instructions require original APK/DEX evidence, latest-version-only
work, preserving older support without extending it, and keeping original apps and
upstream source trees unchanged. Builds use Java 21 and `build-local.ps1`. No commit,
push, publishing, or device installation is authorized by a routine patch request.

The configuration is at the workspace root, outside the patch repository's Git
tracking. Keep `.kilo/` with this workspace when moving machines. Continue configuration
is preserved. The CLI bundled with the extension was used for configuration checks;
no separate CLI installation is required for the sidebar.

## Validation results

- Extension installation completed successfully.
- LM Studio API confirmed the exact model ID, loaded context, and tool-use support.
- `kilo config check` returned `No config warnings.`
- Runtime skill discovery found all four Morphe skills at their canonical paths.
- Resolved configuration confirmed the `morphe` agent, local model, 16K limit,
  and `bash: allow` policy.
- The default agent's live test failed on context size. The focused-agent test
  reached generation; one attempt was interrupted by model unloading, and a retry
  was stopped after producing no tool results for several minutes. End-to-end
  skill/file/command execution has not been verified. No patch was implemented.

After reloading VS Code, test the selected agent with this before a patch request:

```text
Read-only setup check: run git -C E:/Morphe/santodan-patches status --short
using the command tool and report its actual output. Do not modify files or
read credentials. Keep the answer brief.
```

If even this does not return tool results, inspect LM Studio's generation status
and Kilo's output for template/tool-format errors or slow inference. A configuration
check does not prove that a model can reliably execute a full coding workflow.

## First prompt

```text
Work in E:\Morphe\santodan-patches. Follow the configured Morphe workflow
and load the relevant skills.

Create a Morphe patch for Car Scanner ELM OBD2 using the latest original
version available in E:\Morphe\OriginalApps. The expected input is
Car-Scanner-ELM-OBD2_2.1.50.apk; confirm its actual package and version.

Investigate its premium entitlement checks and enable premium features
controlled locally. Identify any features requiring server authorization;
do not assume spoofing a request enables everything.

If necessary, extract/decompile into a separate inspection directory.
Preserve the original APK and all upstream source trees. Prove hooks from
real DEX/resources before editing and reject incompatible or ambiguous input.

Implement the patch, build the .mpp, and run relevant native-contract,
runtime, and bundle verification. Update relevant feature documentation and
generate patch metadata if needed. Preserve existing older-version support
without extending it and preserve unrelated user changes.

Do not commit, push, install on a device, or display credentials.
Report target package/version, changed files, checks actually executed and
their results, bundle path, features covered, and remaining device checks.
Report missing prerequisites instead of guessing.
```

## If commands still ask for approval

Reload VS Code after editing project permissions; Kilo caches project settings.
Confirm this workspace and the `morphe` agent are active. Settings → Auto Approve
should show shell commands as Allow. External-directory access and repeated failure
loops still ask; routine commands inside `E:\Morphe` are configured to run automatically.
Project instructions are guidance, while the host's permission configuration controls
approval. These configuration files do not change the host's Windows shell.

Reference: [Kilo permissions](https://kilo.ai/docs/getting-started/settings/auto-approving-actions),
[custom model limits](https://kilo.ai/docs/code-with-ai/agents/custom-models), and
[skill discovery](https://kilo.ai/docs/customize/skills).
