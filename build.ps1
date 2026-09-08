[CmdletBinding()]
param(
    [string]$MorpheJar = '',
    [string]$JdkHome = '',
    [string]$R8Jar = '',
    [string]$TestDex = ''
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath $PSScriptRoot).Path
if (-not $MorpheJar) { $MorpheJar = Join-Path $projectRoot '../morphe-desktop-1.15.1-dev.4-all.jar' }
if (-not $JdkHome) { $JdkHome = Join-Path $projectRoot '../jdk-26.0.2' }
if (-not $R8Jar) { $R8Jar = Join-Path $projectRoot '../inspection/r8-8.6.17.jar' }
$morphePath = (Resolve-Path -LiteralPath $MorpheJar).Path
$jdkPath = (Resolve-Path -LiteralPath $JdkHome).Path
$r8Path = (Resolve-Path -LiteralPath $R8Jar).Path
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
$dex = Join-Path $runRoot 'dex'
$classesJar = Join-Path $runRoot 'patch-classes.jar'
$testClasses = Join-Path $runRoot 'test-classes'
$dist = Join-Path $projectRoot 'dist'
New-Item -ItemType Directory -Force -Path $classes, $dex, $testClasses, $dist | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& $javac --release 17 -encoding UTF-8 -cp $morphePath -d $classes @sources
if ($LASTEXITCODE -ne 0) { throw 'Patch compilation failed' }

# Morphe Manager loads patches through Android's DexClassLoader. Keep the JVM
# classes for Morphe Desktop and add classes.dex for Morphe Manager.
& $jar --create --file $classesJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'Failed to stage JVM classes for DEX compilation' }
& $java -cp $r8Path com.android.tools.r8.D8 --min-api 26 --output $dex $classesJar
if ($LASTEXITCODE -ne 0) { throw 'Android DEX compilation failed' }

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
    'Description: SantoDan app patches for Morphe Desktop and Manager',
    "Version: $($metadata.version)",
    "Timestamp: $([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())",
    'Source: https://github.com/Santodan/santodan-patches',
    "Author: $($metadata.author)",
    'Contact: https://github.com/Santodan',
    'Website: https://morphe.software/add-source?github=Santodan/santodan-patches',
    'License: GPLv3',
    "Patcher-Version: $($metadata.patcherVersion)",
    ''
) -join "`r`n"
$manifestPath = Join-Path $runRoot 'MANIFEST.MF'
[IO.File]::WriteAllText($manifestPath, $manifest + "`r`n", [Text.UTF8Encoding]::new($false))
$stagedBundle = Join-Path $runRoot "santodan-patches-$($metadata.version).mpp"
& $jar --create --file $stagedBundle --manifest $manifestPath -C $classes .
if ($LASTEXITCODE -ne 0) { throw 'MPP packaging failed' }
& $jar --update --file $stagedBundle -C $dex classes.dex
if ($LASTEXITCODE -ne 0) { throw 'Failed to add Android DEX to MPP' }

# Exercise the real Morphe loader before publishing the local build artifact.
& $java -jar $morphePath list-patches --patches $stagedBundle -p -v
if ($LASTEXITCODE -ne 0) { throw 'Morphe could not load the bundle' }
$bundle = Join-Path $dist "santodan-patches-$($metadata.version).mpp"
Copy-Item -LiteralPath $stagedBundle -Destination $bundle -Force
Write-Host "Built: $bundle"
Write-Host 'Targets: Morphe Desktop and Morphe Manager (JVM classes + Android DEX).'
