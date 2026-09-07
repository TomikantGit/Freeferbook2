param(
    [string]$Repositorio = "https://github.com/TomikantGit/Freeferbook2.git",
    [string]$Branch = "main"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
Set-Location $projectRoot

if (Test-Path ".git\HEAD") {
    Write-Host "Git já está configurado nesta pasta." -ForegroundColor Green
    & git status --short --branch
    exit $LASTEXITCODE
}

Write-Host "Inicializando metadados Git sem alterar os arquivos de trabalho..." -ForegroundColor Cyan
& git init
if ($LASTEXITCODE -ne 0) { throw "Falha no git init." }

$origin = (& git remote) -contains "origin"
if ($origin) {
    & git remote set-url origin $Repositorio
} else {
    & git remote add origin $Repositorio
}
if ($LASTEXITCODE -ne 0) { throw "Falha ao configurar origin." }

Write-Host "Buscando histórico remoto..." -ForegroundColor Cyan
& git fetch origin $Branch
if ($LASTEXITCODE -ne 0) {
    throw "Não foi possível buscar origin/$Branch. Nenhum arquivo local foi substituído."
}

# O reset mixed conecta HEAD/index ao histórico remoto sem substituir arquivos locais.
& git reset --mixed "origin/$Branch"
if ($LASTEXITCODE -ne 0) {
    throw "Falha ao conectar o índice ao histórico remoto."
}

& git branch -M $Branch
if ($LASTEXITCODE -ne 0) { throw "Falha ao renomear a branch local." }

& git branch --set-upstream-to="origin/$Branch" $Branch
if ($LASTEXITCODE -ne 0) {
    Write-Host "Aviso: não foi possível configurar upstream automaticamente." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Git restaurado. Os arquivos locais foram preservados." -ForegroundColor Green
Write-Host "Revise agora as diferenças antes de publicar:"
& git status --short --branch
