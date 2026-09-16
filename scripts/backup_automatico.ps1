$sourcePmsg = "C:\Dev\Pmsg"
$sourceBackups = "C:\Pmsg-Backups"
$destRaix = "G:\Meu Drive\Raix"

# Define destination folders
$destPmsg = Join-Path $destRaix "Pmsg"
$destBackups = Join-Path $destRaix "Pmsg-Backups"

# Create destination folders if they don't exist
if (!(Test-Path $destPmsg)) { New-Item -ItemType Directory -Force -Path $destPmsg | Out-Null }
if (!(Test-Path $destBackups)) { New-Item -ItemType Directory -Force -Path $destBackups | Out-Null }

Write-Host "Iniciando backup de C:\Dev\Pmsg para G:\Meu Drive\Raix\Pmsg"
# Backup Pmsg (excluding heavy build folders and sensitive files like google-services.json and keystores)
# Excluded dirs: .git, node_modules, build, .dart_tool, .pub-cache, Pods
# Excluded files: google-services.json, *.keystore, .env
robocopy $sourcePmsg $destPmsg /MIR /FFT /Z /NP /R:3 /W:5 /XD ".git" "node_modules" "build" ".dart_tool" "android\app\build" "ios\Pods" /XF "google-services.json" "*.keystore" ".env" "secrets.*"

Write-Host "Iniciando backup de C:\Pmsg-Backups para G:\Meu Drive\Raix\Pmsg-Backups"
# Backup Pmsg-Backups
if (Test-Path $sourceBackups) {
    robocopy $sourceBackups $destBackups /MIR /FFT /Z /NP /R:3 /W:5
} else {
    Write-Host "Aviso: Pasta C:\Pmsg-Backups não encontrada. Pulando..."
}

Write-Host "Backup concluído com sucesso."
