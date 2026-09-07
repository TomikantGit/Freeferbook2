# Automação de APK de teste — Freeferbook

O projeto possui um fluxo simples para gerar e publicar APKs de teste no GitHub.

## Primeira configuração desta cópia

Se a pasta `.git` estiver vazia ou incompleta, execute:

```powershell
.\scripts\restaurar-git.ps1
```

O script busca `TomikantGit/Freeferbook2` e reconecta o histórico sem substituir os arquivos de trabalho existentes.

## Publicar um teste

Depois da configuração inicial, execute:

```powershell
.\scripts\publicar-teste.ps1 -Mensagem "teste: descrição da alteração"
```

O script:

1. roda `assembleDebug --offline` localmente;
2. adiciona ao Git apenas caminhos conhecidos do projeto;
3. cria um commit se houver alterações;
4. faz `git push origin main`.

O push dispara `.github/workflows/android-test-release.yml`. O GitHub Actions compila o APK, recria a prerelease `test-latest` e prepara o canal público consumido pelo atualizador interno do app.

## Link fixo do APK

O APK mais recente fica sempre em:

`https://github.com/TomikantGit/Freeferbook2/releases/download/test-latest/freeferbook-test.apk`

## Assinatura das builds de teste

As builds automatizadas usam uma keystore dedicada de teste. A keystore e suas credenciais ficam empacotadas exclusivamente no GitHub Actions Secret `TEST_SIGNING_BUNDLE`. Nenhuma chave de assinatura é armazenada no código, em Releases ou em caches compartilhados do repositório.

O workflow desempacota esse Secret apenas no diretório temporário do runner, mascara as credenciais nos logs e o Gradle as lê por variáveis de ambiente. Builds locais continuam usando a assinatura debug padrão quando essas variáveis não existem.

Uma build local pode ter assinatura diferente da build automatizada. Nesse caso, a primeira instalação do canal automatizado pode exigir remover a build local anterior; depois disso, as builds do GitHub usam a mesma assinatura de teste.

## Atualização dentro do app

Em `Configurações > Atualizações de teste`, o Freeferbook consulta:

`https://tomikantgit.github.io/Freeferbook2/update.json`

Esse manifesto aponta para o APK publicado pelo GitHub Pages e inclui `versionCode`, `versionName` e SHA-256. O app só baixa quando o usuário solicita, valida o hash e então abre o instalador do Android.

O GitHub Pages publica apenas os arquivos de `update-site` gerados pelo workflow (APK, `update.json` e uma página mínima). O repositório pode ser público sem expor credenciais, pois os dados de assinatura permanecem nos Secrets do GitHub Actions.
