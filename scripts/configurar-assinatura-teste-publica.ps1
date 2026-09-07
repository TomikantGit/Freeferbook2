param(
    [string]$Repositorio = "TomikantGit/Freeferbook2"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$privateDir = Join-Path $projectRoot ".private"
$keystorePath = Join-Path $privateDir "public-test-signing.p12"
$bundlePath = Join-Path $privateDir "public-test-signing-bundle.txt"

New-Item -ItemType Directory -Force -Path $privateDir | Out-Null

function New-StrongPassword {
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $bytes = New-Object byte[] 32
        $rng.GetBytes($bytes)
        return ([Convert]::ToBase64String($bytes))
            .Replace("+", "A")
            .Replace("/", "B")
            .Replace("=", "C")
    }
    finally {
        $rng.Dispose()
    }
}

function Resolve-Gh {
    $command = Get-Command gh -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }

    $knownPath = "C:\Program Files\GitHub CLI\gh.exe"
    if (Test-Path -LiteralPath $knownPath) { return $knownPath }

    throw "GitHub CLI (gh) nÃ£o encontrado. Instale-o antes de continuar."
}

if (-not (Test-Path -LiteralPath $bundlePath)) {
    $password = New-StrongPassword
    $alias = "freeferbook-public-test"

    & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair `
        -keystore $keystorePath `
        -storetype PKCS12 `
        -storepass $password `
        -alias $alias `
        -keypass $password `
        -dname "CN=Freeferbook Public Test,O=Freeferbook,C=BR" `
        -keyalg RSA `
        -keysize 3072 `
        -sigalg SHA384withRSA `
        -validity 10000

    if ($LASTEXITCODE -ne 0) {
        throw "Falha ao gerar a chave de assinatura."
    }

    $bundleObject = [ordered]@{
        keystore      = [Convert]::ToBase64String([IO.File]::ReadAllBytes($keystorePath))
        storePassword = $password
        keyAlias      = $alias
        keyPassword   = $password
    }

    $bundleJson = $bundleObject | ConvertTo-Json -Compress
    $bundle = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($bundleJson))
    [IO.File]::WriteAllText($bundlePath, $bundle, [Text.UTF8Encoding]::new($false))
}

$gh = Resolve-Gh
& $gh auth status | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "GitHub CLI nÃ£o estÃ¡ autenticado. Execute 'gh auth login' e tente novamente."
}

$bundleValue = [IO.File]::ReadAllText($bundlePath).Trim()
if ([string]::IsNullOrWhiteSpace($bundleValue)) {
    throw "O bundle local de assinatura estÃ¡ vazio."
}

$bundleValue | & $gh secret set TEST_SIGNING_BUNDLE --repo $Repositorio
if ($LASTEXITCODE -ne 0) {
    throw "NÃ£o foi possÃ­vel cadastrar TEST_SIGNING_BUNDLE no GitHub."
}

Write-Host ""
Write-Host "Assinatura pÃºblica de teste configurada com sucesso." -ForegroundColor Green
Write-Host "O material privado ficou somente em .private/ e nÃ£o deve ser enviado ao Git."
Write-Host "Agora execute o workflow Android Test Release ou faÃ§a um novo push em main."

