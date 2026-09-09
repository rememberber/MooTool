param(
  [Parameter(Mandatory = $true)]
  [ValidateSet("app-image")]
  [string]$Type
)

Write-Host "Windows packaging is implemented in P7 on a Windows runner. P0 only defines this entry."
Write-Error "Run scripts/package.sh --type app-image on macOS/Linux for the current P0 app-image."
exit 2
