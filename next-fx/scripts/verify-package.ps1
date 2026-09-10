param(
  [Parameter(Mandatory = $true)]
  [string]$Path
)

if (-not (Test-Path $Path)) {
  Write-Error "Missing package path: $Path"
  exit 1
}

Write-Host "Windows package verification is implemented with the Windows app-image in P7."
exit 2
