"""Inspect the original TFD Companion 1.1.5 APKS without patching it.

Uses only Python's standard library and the workspace Android/JDK tools.
All generated evidence is placed in a new output directory.
"""

import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import zipfile


WORKSPACE = Path(__file__).resolve().parents[5]
EXPECTED_SHA256 = "61b48408873dd7cc8f75c9ab65b21419e4e3577371443f1ac3f9359c71325f4d"
CLASSES = {
    "Ngsm": "com.nexon.ngsm.Ngsm",
    "Ngsm-dialog": "com.nexon.ngsm.Ngsm$1",
    "Ngsm-dialog-click": "com.nexon.ngsm.Ngsm$1$1",
    "Ngsm-exit": "com.nexon.ngsm.Ngsm$2",
    "NXPNgsm": "kr.co.nexon.npaccount.security.NXPNgsm",
    "TCIntroActivity": "com.nexon.tfdc.activity.TCIntroActivity",
    "TCMainActivity": "com.nexon.tfdc.activity.TCMainActivity",
    "TCSdkActivity": "com.nexon.tfdc.activity.base.TCSdkActivity",
    "TCSdkActivity-token": "com.nexon.tfdc.activity.base.TCSdkActivity$getNgsmToken$2",
}


def capture(command, destination):
    result = subprocess.run(command, capture_output=True, check=True)
    destination.write_bytes(result.stdout)
    return result.stdout.decode("utf-8", errors="replace")


def dex_strings(data):
    """Read the DEX string table; skip the ULEB128 UTF-16 length prefix."""
    import struct

    if data[:4] != b"dex\n":
        raise ValueError("Expected an original DEX file")
    count, offset = struct.unpack_from("<II", data, 56)
    for index in range(count):
        start = struct.unpack_from("<I", data, offset + index * 4)[0]
        while data[start] & 0x80:
            start += 1
        start += 1
        end = data.index(b"\0", start)
        # The searched identifiers are ASCII; MUTF-8 differences do not affect them.
        yield data[start:end].decode("utf-8", errors="replace")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=WORKSPACE / "OriginalApps/The-First-Descendant-Companion_1.1.5.apks")
    parser.add_argument("--output", type=Path, required=True, help="New evidence directory; existing directories are rejected")
    args = parser.parse_args()
    raw = args.input.read_bytes()
    digest = hashlib.sha256(raw).hexdigest()
    if digest != EXPECTED_SHA256:
        raise ValueError("Input differs from the inspected original 1.1.5 APKS")
    aapt = WORKSPACE / "SDK/build-tools/36.0.0/aapt.exe"
    java = WORKSPACE / "jdk-21.0.2/bin/java.exe"
    analyzer = WORKSPACE / ".android-sdk/cmdline-tools/lib/apkanalyzer-classpath.jar"
    strings = WORKSPACE / "SDK/ndk/29.0.14206865/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-strings.exe"
    for tool in (aapt, java, analyzer, strings):
        if not tool.is_file():
            raise FileNotFoundError(tool)
    args.output.mkdir(parents=True, exist_ok=False)
    report = {"input": str(args.input.resolve()), "sha256": digest, "splits": [], "shizuku_dex_strings": [], "native_strings": {}}
    with zipfile.ZipFile(io.BytesIO(raw)) as outer:
        for entry in outer.infolist():
            if not entry.filename.endswith(".apk"):
                continue
            if Path(entry.filename).name != entry.filename:
                raise ValueError("Unexpected nested APK path")
            apk_bytes = outer.read(entry)
            apk_path = args.output / entry.filename
            apk_path.write_bytes(apk_bytes)
            report["splits"].append({"name": entry.filename, "sha256": hashlib.sha256(apk_bytes).hexdigest()})
            capture([str(aapt), "dump", "badging", str(apk_path)], args.output / (entry.filename + "-badging.txt"))
            with zipfile.ZipFile(io.BytesIO(apk_bytes)) as apk:
                for name in apk.namelist():
                    if re.fullmatch(r"classes\d*\.dex", name):
                        for value in dex_strings(apk.read(name)):
                            if "shizuku" in value.lower():
                                report["shizuku_dex_strings"].append({"apk": entry.filename, "dex": name, "string": value})
                    if name.startswith("lib/") and name.endswith(".so"):
                        target = args.output / "native" / entry.filename / name
                        target.parent.mkdir(parents=True, exist_ok=True)
                        target.write_bytes(apk.read(name))
                        output = capture([str(strings), str(target)], target.with_suffix(".strings.txt"))
                        report["native_strings"][entry.filename + "/" + name] = {
                            "sha256": hashlib.sha256(target.read_bytes()).hexdigest(),
                            "shizuku_lines": [line for line in output.splitlines() if "shizuku" in line.lower()],
                            "detector_type_names": [line for line in output.splitlines() if line in ("4CADB", "8CRooting", "12CHacktoolDex", "13CHacktoolFile", "13CEngineDetect")],
                        }
    base = args.output / "base.apk"
    badging = (args.output / "base.apk-badging.txt").read_text(encoding="utf-8")
    if not re.search(r"package: name='com\.nexon\.tfdc' versionCode='29' versionName='1\.1\.5'", badging):
        raise ValueError("Unexpected base manifest package/version")
    capture([str(aapt), "dump", "--values", "resources", str(base)], args.output / "resources.txt")
    analyzer_command = [str(java), f"-Dcom.android.sdklib.toolsdir={WORKSPACE / 'SDK/cmdline-tools/latest'}", "-classpath", str(analyzer), "com.android.tools.apk.analyzer.ApkAnalyzerCli"]
    for label, name in CLASSES.items():
        capture(analyzer_command + ["dex", "code", "--class", name, str(base)], args.output / (label + ".smali"))
    (args.output / "report.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    print(f"Evidence created: {args.output.resolve()}")
    print(f"Shizuku DEX strings: {len(report['shizuku_dex_strings'])}")
    print("No APK or patch bundle was modified or built.")


if __name__ == "__main__":
    main()
