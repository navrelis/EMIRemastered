<#
.SYNOPSIS
  Runs the EMI dev-client screenshot tour unattended and prints its summary.

.DESCRIPTION
  Starts `gradlew runClientTour` (dev client, joins run/saves/devworld, waits for EMI, performs the tour
  steps, takes screenshots, quits by itself), then prints run/tour-out/<tour>/summary.json in a readable
  form: per-step status, EMI reload time, and every EMI related WARN/ERROR log line.
  Exit code: 0 = all steps ok, 1 = a step failed / timeout, 2 = no summary written.

.PARAMETER Tour
  Tour name (run/tour/<name>.json or built-in resource, default: "default") or a path to a tour json.

.PARAMETER SummaryOnly
  Do not start the game; just print the summary of the last run of that tour.

.EXAMPLE
  scripts\tour.ps1
  scripts\tour.ps1 -Tour my-tour
  scripts\tour.ps1 -SummaryOnly
#>
param(
	[string]$Tour = "default",
	[switch]$SummaryOnly
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

# the tour name is the file name without extension when a path was given
$tourName = $Tour
if ($Tour -match '[\\/]' -or $Tour.EndsWith(".json")) {
	$tourName = [System.IO.Path]::GetFileNameWithoutExtension($Tour)
}
$outDir = Join-Path $root "run\tour-out\$tourName"
$summaryFile = Join-Path $outDir "summary.json"

if (-not $SummaryOnly) {
	if (Test-Path $summaryFile) { Remove-Item $summaryFile -Force }
	Write-Host "== running tour '$Tour' (gradlew runClientTour) =="
	& "$root\gradlew.bat" runClientTour "-Ptour=$Tour" --console=plain
	Write-Host "== gradle exit code $LASTEXITCODE =="
}

if (-not (Test-Path $summaryFile)) {
	Write-Host "No summary written: $summaryFile" -ForegroundColor Red
	exit 2
}

$s = Get-Content $summaryFile -Raw | ConvertFrom-Json
Write-Host ""
Write-Host "================ TOUR SUMMARY: $($s.tour) ================"
$color = if ($s.status -eq "ok") { "Green" } else { "Red" }
Write-Host ("status      : {0}{1}" -f $s.status, $(if ($s.reason) { " ($($s.reason))" } else { "" })) -ForegroundColor $color
Write-Host ("duration    : {0} ms, steps ok/failed/total: {1}/{2}/{3}" -f $s.durationMs, $s.stepsOk, $s.stepsFailed, $s.stepsTotal)
Write-Host ("EMI         : loaded={0} status={1} reloadMs={2}" -f $s.emi.loaded, $s.emi.status, $s.emi.reloadMs)
if ($s.emi.pluginReloadMs) {
	$plugins = ($s.emi.pluginReloadMs.PSObject.Properties | ForEach-Object { "$($_.Name)=$($_.Value)ms" }) -join ", "
	Write-Host "plugin reg. : $plugins"
}
Write-Host "output      : $($s.outputDir)"
Write-Host ""
Write-Host "---- steps ----"
foreach ($st in $s.steps) {
	$line = "{0,2}. {1,-16} {2}" -f $st.index, $st.type, $st.status
	$detail = @()
	foreach ($key in "item", "text", "name", "id", "key", "n", "index") {
		if ($st.spec.PSObject.Properties[$key]) { $detail += "$key=$($st.spec.$key)" }
	}
	if ($detail) { $line += "  [" + ($detail -join " ") + "]" }
	if ($st.screenshot) { $line += "  -> $($st.screenshot)" }
	$c = if ($st.status -eq "ok") { "Gray" } else { "Red" }
	Write-Host $line -ForegroundColor $c
	if ($st.info) { Write-Host "      info : $($st.info)" -ForegroundColor DarkGray }
	if ($st.error) { Write-Host "      error: $($st.error)" -ForegroundColor Red }
}

Write-Host ""
Write-Host "---- EMI-related WARN/ERROR log lines: $($s.logWarningCount) ----"
foreach ($w in $s.logWarnings) {
	Write-Host ("[{0}] {1}: {2}" -f $w.level, $w.logger, $w.message) -ForegroundColor Yellow
	if ($w.thrown) { Write-Host "       $($w.thrown)" -ForegroundColor DarkYellow }
}
if ($s.emi.reloadWarningCount -gt 0) {
	Write-Host ""
	Write-Host "---- EMI reload log ($($s.emi.reloadWarningCount) warnings) ----"
	$s.emi.reloadLog | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
}
Write-Host ""
Write-Host "screenshots:"
Get-ChildItem $outDir -Filter *.png | ForEach-Object { Write-Host "  $($_.FullName)" }

if ($s.status -eq "ok") { exit 0 } else { exit 1 }
