[CmdletBinding()]
param(
    [string]$MorpheJar = '',
    [string]$JdkHome = '',
    [string]$TestDex = ''
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath $PSScriptRoot).Path
if (-not $MorpheJar) { $MorpheJar = Join-Path $projectRoot '../morphe-desktop-1.15.1-dev.4-all.jar' }
if (-not $JdkHome) { $JdkHome = Join-Path $projectRoot '../jdk-26.0.2' }
$morphePath = (Resolve-Path -LiteralPath $MorpheJar).Path
$jdkPath = (Resolve-Path -LiteralPath $JdkHome).Path
$java = Join-Path $jdkPath 'bin/java.exe'
$javac = Join-Path $jdkPath 'bin/javac.exe'
$jar = Join-Path $jdkPath 'bin/jar.exe'
foreach ($tool in @($java, $javac, $jar)) {
    if (-not (Test-Path -LiteralPath $tool -PathType Leaf)) { throw "Missing JDK tool: $tool" }
}
$metadata = ConvertFrom-StringData (Get-Content -LiteralPath (Join-Path $projectRoot 'bundle.properties') -Raw)
if ($metadata.version -notmatch '^\d+\.\d+\.\d+$') { throw 'Invalid bundle version' }

# A fresh directory avoids stale classes without recursively deleting anything.
$runRoot = Join-Path $projectRoot ('build/' + [guid]::NewGuid().ToString('N'))
$classes = Join-Path $runRoot 'classes'
$testClasses = Join-Path $runRoot 'test-classes'
$dist = Join-Path $projectRoot 'dist'
New-Item -ItemType Directory -Force -Path $classes, $testClasses, $dist | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& $javac --release 17 -encoding UTF-8 -cp $morphePath -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Patch compilation failed' }

if ($TestDex) {
    $dexPath = (Resolve-Path -LiteralPath $TestDex).Path
    $testSources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/test/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
    & $javac --release 17 -encoding UTF-8 -cp "$morphePath;$classes" -d $testClasses @testSources
    if ($LASTEXITCODE -ne 0) { throw 'Verification compilation failed' }
    & $java -cp "$morphePath;$classes;$testClasses" santodan.patches.VerifyPatch $dexPath (Join-Path $runRoot 'verified.dex')
    if ($LASTEXITCODE -ne 0) { throw 'Bytecode verification failed' }
}

$manifest = @(
    'Manifest-Version: 1.0',
    "Name: $($metadata.name)",
    'Description: SantoDan app patches for Morphe Desktop',
    "Version: $($metadata.version)",
    "Author: $($metadata.author)",
    "Patcher-Version: $($metadata.patcherVersion)",
    ''
) -join "`r`n"
$manifestPath = Join-Path $runRoot 'MANIFEST.MF'
[IO.File]::WriteAllText($manifestPath, $manifest + "`r`n", [Text.UTF8Encoding]::new($false))
$stagedBundle = Join-Path $runRoot "santodan-patches-$($metadata.version).mpp"
& $jar --create --file $stagedBundle --manifest $manifestPath -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'MPP packaging failed' }

# Exercise the real Morphe loader before publishing the local build artifact.
& $java -jar $morphePath list-patches --patches $stagedBundle -p -v
if ($LASTEXITCODE -ne 0) { throw 'Morphe could not load the bundle' }
$bundle = Join-Path $dist "santodan-patches-$($metadata.version).mpp"
Copy-Item -LiteralPath $stagedBundle -Destination $bundle -Force
Write-Host "Built: $bundle"
Write-Host 'Target: Morphe Desktop. This bundle contains JVM classes, not Android DEX.'
