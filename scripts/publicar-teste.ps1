param(
    [string]$Mensagem = "",
    [switch]$SemBuild
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
Set-Location $projectRoot

if ([string]::IsNullOrWhiteSpace($Mensagem)) {
    $Mensagem = "teste: " + (Get-Date -Format "yyyy-MM-dd HH:mm")
}

if (-not (Test-Path ".git\HEAD")) {
    throw @"
Os metadados Git não estão configurados nesta cópia.
Execute primeiro:

  .\scripts\restaurar-git.ps1

Esse procedimento reconecta a pasta ao repositório remoto sem substituir os arquivos locais.
"@
}

if (-not $SemBuild) {
    Write-Host "Compilando APK local antes do push..." -ForegroundColor Cyan
    & .\gradlew.bat assembleDebug --offline
    if ($LASTEXITCODE -ne 0) {
        throw "O build local falhou. O push foi cancelado."
    }
}

Write-Host "Preparando alterações conhecidas do projeto..." -ForegroundColor Cyan
$paths = @(
    "app",
    ".github",
    "scripts",
    "gradle",
    "gradlew",
    "gradlew.bat",
    ".gitignore",
    "build.gradle.kts",
    "settings.gradle.kts",
    "gradle.properties",
    "PROGRESS.md",
    "ARCHITECTURE.md",
    "HANDOFF.md",
    "RELATORIO_LIVROHUB.md",
    "SETUP.md",
    "TOOLS_SETUP.md",
    "AUTOMACAO_TESTES.md"
)

$existingPaths = $paths | Where-Object { Test-Path $_ }
if ($existingPaths.Count -gt 0) {
    & git add -- $existingPaths
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao adicionar arquivos ao Git."
    }
}

& git diff --cached --quiet
$hasStagedChanges = $LASTEXITCODE -ne 0

if ($hasStagedChanges) {
    Write-Host "Criando commit: $Mensagem" -ForegroundColor Cyan
    & git commit -m $Mensagem
    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao criar o commit."
    }
} else {
    Write-Host "Nenhuma alteração nova para commit." -ForegroundColor Yellow
}

Write-Host "Enviando main para o GitHub..." -ForegroundColor Cyan
& git push origin main
if ($LASTEXITCODE -ne 0) {
    throw "Falha no git push. Verifique autenticação e o remote origin."
}

Write-Host ""
Write-Host "Push concluído." -ForegroundColor Green
Write-Host "O GitHub Actions irá compilar e publicar a release de teste."
Write-Host "APK fixo:"
Write-Host "https://github.com/TomikantGit/Freeferbook2/releases/download/test-latest/freeferbook-test.apk"
