# Morphe patches with LM Studio

This repository includes portable Markdown skills for
`huihui-qwen3.8-27b-abliterated`. They teach repository conventions; they do not
change model weights or guarantee correct patches. No particular context length,
quantization, sampling parameters, or tool-call compatibility is assumed.

## Continue in VS Code: recommended setup for this workspace

Here Continue supplies the coding tools and instructions, while LM Studio serves
the model over its API. Configure project instructions in Continue; do not assume
LM Studio chat presets or chat plugins carry into API requests from Continue.
You do not need an LM Studio skills plugin or an extra filesystem MCP server for
Continue's built-in read/edit/terminal workflow.

### 1. Open the workspace

Open `E:\Morphe\Morphe.code-workspace` in VS Code, or open `E:\Morphe` as the
folder. This includes the repository, original inputs, and inspection artifacts.
The workspace rule is already created at
`E:\Morphe\.continue\rules\morphe-patches.md`.
It routes the agent to the canonical skills without copying them.

If you instead open only `E:\Morphe\santodan-patches`, copy that rule into
`santodan-patches/.continue/rules/` for that workspace and ensure the agent can
still inspect originals outside the repository. Use one rule location for the
active workspace. The root rule is outside the patch repository's Git tracking;
keep a copy if moving to another machine.

The existing root rule `testcmd.md` asks for short output and forbids documentation
changes. It has been preserved. Explicit user requests to update documentation
take precedence; the new rule states that explicitly.

### 2. Start LM Studio's local server

Load `huihui-qwen3.8-27b-abliterated`, then start the local API server from LM
Studio's server/developer controls. The examples assume the server is on port 1234
with authentication disabled for the local connection. If you use a different port
or enable authentication, configure Continue to match it.

Keep the model's supplied chat template. Start with its default sampling settings;
do not add guessed thinking/template overrides. Context length is limited by the
model and your available RAM/VRAM. As a practical starting point, try 16K tokens
if supported and memory permits; increase to 32K if tasks run out of context and
performance remains usable. These are starting suggestions, not tested requirements
for this specific model. Avoid loading entire decompiled apps into the prompt.

In a VS Code PowerShell terminal, confirm the server and get the exact model ID:

```powershell
(Invoke-RestMethod -Uri 'http://localhost:1234/v1/models').data |
    Select-Object -ExpandProperty id
```

Use the returned ID for your intended model, not its friendly name or an assumed
Hugging Face repository name. The endpoint can list unloaded downloaded models
when just-in-time loading is enabled.
[LM Studio model-list endpoint](https://lmstudio.ai/docs/developer/openai-compat/models).

### 3. Configure Continue

Open Continue's sidebar (`Ctrl+L`), then use the agent/configuration selector and
its gear icon to edit the selected local configuration. On this machine the usual
global file is `C:\Users\Danny\.continue\config.yaml`; use the UI to confirm
which configuration is active. Its contents have not been modified by this setup.

Use [continue-config.example.yaml](../lmstudio/continue-config.example.yaml) as
a complete minimal example, or merge just its model entry into your existing
`models:` list. Do not overwrite other models, rules, or MCP configuration.
Replace `REPLACE_WITH_LM_STUDIO_MODEL_ID` with the ID returned above, save, and
select **Morphe - local Qwen** in Continue.

The native provider is `lmstudio`, with `apiBase: http://localhost:1234/v1`.
[Continue LM Studio configuration](https://docs.continue.dev/customize/model-providers/top-level/lmstudio).
The example assigns chat/edit/apply roles; autocomplete is optional and is not
needed for patches. The `tool_use` declaration makes Continue aware of native
tool support, but does not teach the model to call tools. Actual compatibility
must pass the smoke test below.
[Model capabilities](https://docs.continue.dev/customize/deep-dives/model-capabilities).

If the LM Studio provider fails while the server is working, try changing the
model entry's provider to `openai`, keeping the same model ID and `/v1` API base.
Continue supports OpenAI-compatible endpoints. This is an integration fallback,
not a way to fix invalid tool calls from the model.
[Configuration reference](https://docs.continue.dev/reference).

### 4. Confirm the rule and tools

Reload the VS Code window if the rule does not appear. Check Continue's rules UI
(the rules/pen control described in its documentation) for **Morphe patch workflow**.
The rule uses `alwaysApply: true`, with its body scoped to patch-repository tasks.
[Continue rules](https://docs.continue.dev/customize/deep-dives/rules).

Select **Agent** mode when you want edits/builds. **Plan** mode provides read-only
tools and **Chat** mode provides conversation without tools. Start with the default
tool approval policy; approve expected repository reads and commands when shown.
No extra MCP setup is required for the basic workflow.
[Agent mode](https://docs.continue.dev/ide-extensions/agent/quick-start).

Start a new Agent conversation with this read-only smoke test:

```text
Do not modify files. Work in E:\Morphe\santodan-patches.
Read AI_Guide.md's Start here section and
lmstudio/skills/morphe-inspect-app/SKILL.md.
Run git status --short from the repository. List original NuvioTV inputs in
E:\Morphe\OriginalApps and tell me which version you would inspect first.
Show the tool evidence and report any access failures. Do not read .env.
```

Confirm real tool calls and results appear. The model should identify beta.5 from
the currently listed original input, then say manifest metadata must confirm it.
A prose answer claiming to read files is not evidence of tool access.

### 5. Check the build environment

Run these yourself in a VS Code PowerShell terminal before a first patch task:

```powershell
Set-Location -LiteralPath 'E:\Morphe\santodan-patches'
java -version
.\build-local.ps1 :patches:tasks --all
```

Use Java 21. If necessary, set it for this terminal session using the existing JDK:

```powershell
$env:JAVA_HOME = 'E:\Morphe\jdk-21.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

Agent command terminals may use a different session; have the agent check its own
`java -version` and set its environment if needed. SDK configuration must resolve
Android platform 36. `build-local.ps1` requires the ignored `.env` with package-registry
credentials; do not paste its contents into chat. Keep verification artifacts out
of source directories. See AI_Guide.md for the actual build and feature tasks.

### 6. Request a patch

Use the example request below, including precise expected behavior. For a substantial
feature, first use Plan mode to inspect the native flow, then switch to Agent mode
to implement the agreed scope. Keep a single feature per conversation.

Require the agent to inspect the original APK, implement the patch, build the `.mpp`,
and run relevant real-input/runtime/bundle checks. Inspect its diff and actual command
results. Do not accept compilation alone as proof of correct app behavior. Device
checks remain separate, and commits/pushes require your explicit request.

### Troubleshooting

| Symptom | Check/action |
| --- | --- |
| Connection refused | Start LM Studio's server; check the port and API base. |
| Model not found | Copy the exact ID from `/v1/models`; load the intended model. |
| Agent mode unavailable | Select the configured chat model and check `tool_use`; then test native tool compatibility. |
| Tool calls printed as text or invalid arguments | Check the supplied model template, LM Studio logs, and Continue output. Capability flags cannot repair model/tool-format incompatibility. |
| Rule ignored | Open the correct workspace, confirm the rule in the rules UI, reload, and start a fresh conversation. |
| Context overflow or severe slowdown | Read narrower file ranges; keep a compact evidence note; reduce context or start a fresh task with that note. |
| Invented native names | Require descriptors and instruction evidence from the exact target DEX before editing. |
| Build fails on credentials/SDK/Java | Resolve the reported environment prerequisite; keep failed checks marked as failed. |

The pack has not been exercised through this model's live Continue session. The
smoke test establishes integration; a small real patch with native verification
is the next check of whether the model is reliable enough for your workflow.

## Setup

1. Load your model in LM Studio. Create a chat preset with the contents of
   [SYSTEM_PROMPT.md](../lmstudio/SYSTEM_PROMPT.md) as its system prompt.
2. Give the chat filesystem tools for `E:\Morphe` and a command tool that can run
   PowerShell in `E:\Morphe\santodan-patches`. Reads/searches must cover original
   inputs and inspection artifacts as well as the repo. Patch creation also requires
   file edits; verification requires shell execution. Skills alone provide none of these.
   LM Studio supports connecting MCP servers through its `mcp.json` configuration:
   [official MCP setup](https://lmstudio.ai/docs/app/mcp).
3. If your chosen skills plugin accepts a skills root, point it at
   `E:\Morphe\santodan-patches\lmstudio\skills`. The folders use conventional
   `SKILL.md` files with YAML `name` and `description`; plugin discovery and loading
   depend on that plugin. This pack does not assume or install a particular plugin.
4. Without a skills plugin, use filesystem tools to read each relevant `SKILL.md`
   explicitly. Without filesystem tools, paste the system prompt, the guide's
   "Start here" section, relevant skills, and targeted source/DEX excerpts into chat.
   In that mode the model can propose diffs, while you apply them and run checks.
5. Smoke-test access: ask the model to report `git status --short`, read the guide's
   first section, and list original inputs without modifying files. Confirm that
   actual tool results appear. If tool calls fail, resolve host/model integration
   before asking it to implement a patch.

Use the model's supplied chat template. Keep enough context for the guide, selected
skills, native contract notes, and changed source. Load one task at a time and read
targeted ranges. If context fills, preserve a short progress note and resume with
that note plus the relevant skill. Avoid attaching entire decompiled apps.

## Example request

```text
Work in E:\Morphe\santodan-patches. Read AI_Guide.md's Start here section,
then load morphe-inspect-app and morphe-create-patch from lmstudio/skills.
Create a patch for [app] that [specific desired behavior]. Target only the latest
original app version available in this workspace. Inspect its actual DEX before
choosing hooks, preserve existing older support, and do not commit or push.
Load morphe-runtime-extension if needed, then morphe-verify-patch for validation.
Report the target version, changed files, executed checks, bundle path, and
device checks that remain.
```

Describe how the feature should behave when disabled, which native screen/flow it
affects, and any independence requirements relative to existing patches. Review the
diff and verification output before using a generated app. A successful build alone
does not establish runtime correctness.
