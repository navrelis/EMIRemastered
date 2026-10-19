<#
.SYNOPSIS
  Read-only proof that a dev client never takes the Windows foreground (docs/DEVTOUR.md).

.DESCRIPTION
  Polls the foreground window (user32 GetForegroundWindow, GetWindowThreadProcessId, GetWindowText) and the visible
  top-level windows of the dev game JVM(s) of a worktree. Only reads: no input, no focus change, no window change.
  Game processes are java processes whose command line contains the worktree path and "devlaunchinjector" (the Loom
  run configs), or the PIDs given with -GamePid.

  One line per poll: time, foreground PID + process name (+ title only if it is a game window or -ShowTitles),
  game PIDs, number of visible game windows. Exit code 0 when the foreground never belonged to a game PID (and, with
  -ExpectHidden, no game window was ever visible), 1 otherwise, 2 when no game process was ever seen.

.EXAMPLE
  powershell -File scripts\foreground-check.ps1 -Seconds 300 -StopWhenGameExits -ExpectHidden
  (start it right before `gradlew runClientTour`, from the worktree root)
#>
param(
	[string]$Worktree = (Get-Location).Path,
	[int[]]$GamePid = @(),
	[int]$Seconds = 120,
	[int]$IntervalMs = 2000,
	[switch]$StopWhenGameExits,
	[switch]$ExpectHidden,
	[switch]$ShowTitles
)

Add-Type -Namespace EmiDev -Name User32 -MemberDefinition @'
[DllImport("user32.dll")] public static extern System.IntPtr GetForegroundWindow();
[DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(System.IntPtr hWnd, out uint pid);
[DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern int GetWindowText(System.IntPtr hWnd, System.Text.StringBuilder s, int max);
[DllImport("user32.dll")] public static extern bool IsWindowVisible(System.IntPtr hWnd);
public delegate bool EnumProc(System.IntPtr hWnd, System.IntPtr lParam);
[DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc cb, System.IntPtr lParam);
'@

function Get-WindowTitle([IntPtr]$h) {
	$sb = New-Object System.Text.StringBuilder 256
	[void][EmiDev.User32]::GetWindowText($h, $sb, 256)
	$sb.ToString()
}

function Get-GamePids {
	if ($GamePid.Count -gt 0) {
		return @($GamePid | Where-Object { Get-Process -Id $_ -ErrorAction SilentlyContinue })
	}
	$wt = $Worktree.TrimEnd('\', '/').Replace('/', '\')
	@(Get-CimInstance Win32_Process -Filter "Name like 'java%'" -ErrorAction SilentlyContinue |
		Where-Object { $_.CommandLine -and $_.CommandLine.Contains($wt) -and $_.CommandLine.Contains('devlaunchinjector') } |
		ForEach-Object { [int]$_.ProcessId })
}

function Get-VisibleWindows([int[]]$pids) {
	$found = New-Object System.Collections.Generic.List[string]
	if ($pids.Count -eq 0) { return $found }
	$cb = [EmiDev.User32+EnumProc]{
		param($h, $l)
		$p = [uint32]0
		[void][EmiDev.User32]::GetWindowThreadProcessId($h, [ref]$p)
		if ($pids -contains [int]$p -and [EmiDev.User32]::IsWindowVisible($h)) {
			$found.Add("$p '" + (Get-WindowTitle $h) + "'")
		}
		$true
	}
	[void][EmiDev.User32]::EnumWindows($cb, [IntPtr]::Zero)
	$found
}

$end = (Get-Date).AddSeconds($Seconds)
$polls = 0; $fgHits = 0; $visHits = 0; $seen = @{}
Write-Output "foreground-check: worktree=$Worktree interval=${IntervalMs}ms max=${Seconds}s expectHidden=$ExpectHidden"
while ((Get-Date) -lt $end) {
	$pids = Get-GamePids
	foreach ($p in $pids) { $seen[$p] = $true }
	$h = [EmiDev.User32]::GetForegroundWindow()
	$fp = [uint32]0
	[void][EmiDev.User32]::GetWindowThreadProcessId($h, [ref]$fp)
	$name = (Get-Process -Id $fp -ErrorAction SilentlyContinue).ProcessName
	$isGame = $pids -contains [int]$fp
	$title = if ($isGame -or $ShowTitles) { " title='" + (Get-WindowTitle $h) + "'" } else { "" }
	$vis = Get-VisibleWindows $pids
	$polls++
	if ($isGame) { $fgHits++ }
	if ($vis.Count -gt 0) { $visHits++ }
	$flag = if ($isGame) { " GAME-FOREGROUND" } else { "" }
	Write-Output ("{0:HH:mm:ss} fg pid={1} ({2}){3} game={4} visibleGameWindows={5}{6}{7}" -f (Get-Date), $fp, $name,
		$title, ($pids -join ','), $vis.Count, ($(if ($vis.Count -gt 0) { " [" + ($vis -join '; ') + "]" } else { "" })), $flag)
	if ($StopWhenGameExits -and $seen.Count -gt 0 -and $pids.Count -eq 0) { break }
	Start-Sleep -Milliseconds $IntervalMs
}
Write-Output ("foreground-check result: polls={0} gamePidsSeen={1} gameForeground={2} pollsWithVisibleGameWindow={3}" -f
	$polls, (($seen.Keys | Sort-Object) -join ','), $fgHits, $visHits)
if ($seen.Count -eq 0) { exit 2 }
if ($fgHits -gt 0 -or ($ExpectHidden -and $visHits -gt 0)) { exit 1 }
exit 0
