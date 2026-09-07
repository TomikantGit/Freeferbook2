# Instalacao de Java e Gradle

## O que foi tentado nesta sessao

Foram testadas tres rotas:

- download portatil do JDK 17 via Adoptium;
- download portatil do Gradle 8.7 via `services.gradle.org`;
- uso do `winget`.

Todas falharam dentro do ambiente Codex:

- downloads HTTPS retornaram erro TLS/credenciais do Windows;
- `winget` foi encontrado, mas o Windows bloqueou sua execucao neste ambiente.

## Caminho recomendado fora do Codex

Abra um PowerShell normal do Windows e rode:

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Gradle.Gradle
```

Depois feche e abra novamente o terminal, entao confira:

```powershell
java -version
gradle -v
```

## Alternativa pelo Android Studio

Se voce usa Android Studio, ele ja inclui um Java proprio.

O mais importante para este projeto e:

1. Abrir esta pasta no Android Studio.
2. Deixar o Android Studio instalar/configurar o Android SDK.
3. Gerar o Gradle Wrapper caso ele reclame do arquivo ausente:

```powershell
gradle wrapper --gradle-version 8.7
```

## Arquivo ainda ausente

O projeto ja tem:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.properties`

Ainda falta:

- `gradle/wrapper/gradle-wrapper.jar`

Esse arquivo pode ser gerado automaticamente pelo comando `gradle wrapper --gradle-version 8.7`.

## Se o Gradle falhar escrevendo no cache global `~\.gradle`

Use o script local do projeto. Ele define `GRADLE_USER_HOME` dentro de `work\.gradle-home`, evitando o cache global do Windows:

```powershell
cd "<caminho-do-projeto>\papel-voc-um-a-desenvolvedor-a"
.\run-gradle-local.bat wrapper --gradle-version 8.7
```

Depois:

```powershell
.\run-gradle-local.bat --version
```

## Erro mais recente: espaco insuficiente no disco

O Gradle avancou ate tentar baixar dependencias do Maven, mas falhou com:

```text
Espaco insuficiente no disco
```

Isso aconteceu gravando dentro de:

```text
work\.gradle-home
```

Antes de tentar novamente:

1. Libere espaco no disco `C:`. Recomendacao: pelo menos 5 GB livres, idealmente 10 GB.
2. Apague o cache parcial:

```powershell
cd "<caminho-do-projeto>\papel-voc-um-a-desenvolvedor-a"
Remove-Item -Recurse -Force .\work\.gradle-home
```

3. Tente novamente:

```powershell
.\run-gradle-local.bat wrapper --gradle-version 8.7
```
