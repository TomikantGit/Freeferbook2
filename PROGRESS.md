# LivroHub - Progresso

## Implementado

- Criada a estrutura inicial de um projeto Android com modulo `app`.
- Configurado Kotlin, Jetpack Compose, Room, KSP, Coroutines e java-diff-utils no Gradle.
- Configurado o catalogo de dependencias em `gradle/libs.versions.toml`.
- Configurado `room.schemaLocation` para exportacao futura dos schemas Room.
- Definido `minSdk = 24`, conforme requisito.
- Criado `LivroHubApp` com um `AppContainer` simples para injecao manual de dependencias.
- Criado `.gitignore` basico para artefatos Android/Gradle locais.
- Criado banco local Room `LivroHubDatabase`.
- Criadas entidades:
  - `BookEntity`: representa um livro/documento.
  - `BookVersionEntity`: representa uma versao salva do conteudo completo do livro.
- Criados modelos de dominio:
  - `Book`
  - `BookVersion`
- Criados DAOs para observar livros, observar versoes, criar, renomear, excluir e buscar versoes.
- Criado contrato `BookRepository` e implementacao offline `OfflineBookRepository`.
- Ao criar um livro, o repositorio ja cria a versao inicial com `sequenceNumber = 1`.
- Ao salvar uma nova versao, o repositorio calcula o proximo numero sequencial dentro de uma transacao Room.
- Criada uma tela Compose minima apenas para validar o ponto de entrada do app.
- Adicionados arquivos textuais do Gradle Wrapper:
  - `gradlew`
  - `gradlew.bat`
  - `gradle/wrapper/gradle-wrapper.properties`
- Criado `SETUP.md` com orientacoes para abrir o projeto no Android Studio e completar o Gradle Wrapper.
- Criado `TOOLS_SETUP.md` com instrucoes para instalar Java/Gradle fora do ambiente Codex.
- Criados scripts auxiliares `run-gradle-local.bat` e `run-gradle-local.ps1` para rodar Gradle usando cache local em `work/.gradle-home`.
- Criado `HANDOFF.md` com resumo completo para continuar o projeto em outra conta/sessao.
- Modulo 2 iniciado e concluido:
  - Criado `LibraryViewModel` com `StateFlow` observando livros do repositorio.
  - Criada `LibraryScreen` em Compose para listar livros.
  - Adicionado estado vazio com acao para criar o primeiro livro.
  - Adicionado dialogo para criar livro.
  - Adicionado menu por livro com renomear e excluir.
  - Adicionado dialogo de renomear livro.
  - Adicionado dialogo de confirmacao antes de excluir livro.
  - `MainActivity` agora injeta `BookRepository` no app Compose via `LivroHubAppRoot`.
  - Adicionados icones Compose para criar livro, abrir opcoes, renomear e excluir.
- Revisao posterior do Modulo 2:
  - Confirmado que os requisitos de biblioteca estao cobertos: listar, criar, renomear e excluir livros.
  - Confirmado que a abertura/edicao de um livro deve ficar para o Modulo 3, para manter as etapas pequenas.
- Modulo 3 iniciado e concluido:
  - Criado `EditorViewModel` para observar o livro e sua ultima versao.
  - Criada `EditorScreen` em Compose com campo grande de texto.
  - A biblioteca agora abre um livro ao tocar no card.
  - `LivroHubAppRoot` alterna entre biblioteca e editor usando estado Compose simples.
  - O editor mostra titulo do livro e numero da ultima versao carregada.
  - O usuario pode editar o conteudo atual do livro.
  - O usuario pode salvar uma nova versao com mensagem opcional.
  - Cada salvamento chama `BookRepository.saveVersion`, preservando historico anterior.
  - O editor exibe estado de alteracoes nao salvas e aviso apos salvar.
- Modulo 4 iniciado e concluido parcialmente para comparacao com o texto atual:
  - Criado `TextDiffEngine` usando `java-diff-utils` para comparar texto linha a linha.
  - Criados modelos `DiffLine` e `DiffLineType`.
  - Criado `DiffViewModel` para comparar a ultima versao salva com o texto atual do editor.
  - Criada `DiffScreen` em Compose.
  - Linhas adicionadas aparecem com fundo verde e prefixo `+`.
  - Linhas removidas aparecem com fundo vermelho e prefixo `-`.
  - Linhas inalteradas aparecem sem destaque e com prefixo em branco.
  - O editor ganhou acao de comparar o texto atual com a ultima versao salva.
  - `LivroHubAppRoot` agora alterna entre biblioteca, editor e diff usando estado Compose simples.
- Modulo 5 iniciado e implementado:
  - Criado `HistoryViewModel` para observar livro e versoes salvas.
  - Criada `HistoryScreen` com lista cronologica de versoes em ordem decrescente.
  - O editor ganhou acao para abrir historico.
  - Cada versao mostra numero sequencial, data e mensagem.
  - Restaurar uma versao cria uma nova versao com o mesmo conteudo, usando `saveVersion`, sem apagar historico.
  - Exportacao via Storage Access Framework implementada com `ActivityResultContracts.CreateDocument`.
  - Cada versao pode ser exportada como `.txt` ou `.md`.
  - Versoes podem ser comparadas com a versao anterior usando a tela de diff estatica.
  - `DiffScreen` foi reaproveitada para diffs dinamicos e diffs entre versoes salvas.
- Modulo 6 iniciado e concluido:
  - Validada a compilacao local do projeto com `assembleDebug` apos as configuracoes do Gradle funcionarem.
  - Adicionada funcionalidade de "Comparar com outra versao" na `HistoryScreen`.
  - O usuario pode escolher a versao base a partir do menu, e a UI muda para o "modo de selecao" destacando a versao base.
  - Ao clicar na segunda versao (alvo), o app exibe a tela de diff entre as duas.
  - Adicionadas dependencias de teste JUnit 4 e `kotlinx-coroutines-test`.
  - Criados testes unitarios para a regra de negocio de diff em `TextDiffEngineTest`.
  - Criados testes unitarios para o salvamento de versoes em `OfflineBookRepositoryTest`.
- Modulo 7 iniciado e concluido:
  - Removido o icone de disquete duplicado no topo do `EditorScreen`.
  - Refatorado `TextDiffEngine` para usar `DiffRowGenerator`, retornando `DiffSpan` em vez de strings simples.
  - O diff agora calcula diferencas por palavras e as renderiza na mesma linha.
  - Modificado `DiffScreen` para desenhar `AnnotatedString` coloridas: fundo verde escuro para palavras adicionadas e vermelho escuro para palavras removidas.
  - Testes do `TextDiffEngine` adaptados para as estruturas de Span.
- Modulo 8 iniciado e concluido:
  - Refatoracao completa de otimizacao e documentacao do sistema.
  - Imports FQN corrigidos em `AppContainer`.
  - Regex compilados como `companion object` em `OfflineChapterRepository`, `MarkdownVisualTransformation`, `TextDiffEngine`.
  - `DiffRowGenerator` movido para `companion object` (thread-safe, evita reinstanciacao).
  - Cores de diff extraidas para objeto `DiffColors` com suporte a dark/light mode.
  - Auto-wrap removido do `EditorViewModel` (Compose faz wrap visual nativamente).
  - `TextStyle` memoizado com `remember` no `EditorScreen`.
  - APIs deprecated substituidas: `Divider` por `HorizontalDivider`, `values()` por `entries`.
  - `CharacterEntity` padronizado com `@ColumnInfo` snake_case (consistencia com demais entidades).
  - `TopAppBar` com botao voltar adicionado ao `BookWorkspaceScreen`.
  - Import nao utilizado removido (`withTransaction` em `OfflineBookRepository`).
  - KDoc adicionado em todos os 35+ arquivos Kotlin do projeto.
  - Criado `ARCHITECTURE.md` com diagramas, schema do banco, fluxos de dados e padroes.
  - `HANDOFF.md` reescrito completamente refletindo o estado atual.
  - Versao do banco atualizada para v4 (rename de colunas CharacterEntity).

## Decisoes tecnicas

- O historico nunca e sobrescrito: cada salvamento vira uma linha nova em `chapter_versions`.
- `ChapterVersionEntity` armazena o conteudo completo de cada versao. Isso simplifica restauracao, exportacao e comparacao offline. O custo de armazenamento e aceitavel para um app pessoal de manuscritos nesta fase.
- `sequenceNumber` tem indice unico por capitulo para garantir ordem estavel e evitar versoes duplicadas no mesmo numero.
- A exclusao de um livro/capitulo usa `ForeignKey.CASCADE`, removendo versoes e personagens junto.
- A injecao de dependencias foi mantida manual via `AppContainer`, para evitar adicionar Hilt antes de haver complexidade suficiente.
- Timestamps usam `System.currentTimeMillis()` por simplicidade e por serem suficientes para ordenacao e exibicao futura.
- O estado da biblioteca fica no `LibraryViewModel`; estados temporarios de dialogos ficam na composicao, pois sao detalhes locais da tela.
- A lista usa `Book.id` como chave estavel para recomposicao eficiente no Compose.
- A dependencia `material-icons-extended` foi adicionada para controles Android familiares sem criar assets manuais.
- A navegacao entre telas foi mantida com estado Compose simples em `LivroHubAppRoot`, sem Navigation Component.
- O editor sempre parte da ultima versao salva do capitulo; salvar cria uma nova versao completa.
- O diff implementado compara `ultima versao salva x texto atual do editor` e tambem entre duas versoes salvas.
- A restauracao segue a regra central do app: nunca apagar ou reescrever historico existente.
- A exportacao usa SAF para o usuario escolher o destino do arquivo, sem permissao ampla de armazenamento.
- Regex sao compilados como constantes em `companion object` para evitar recompilacao em hot paths.
- Cores de diff sao definidas em objeto `DiffColors` com paleta para dark e light mode.
- Metricas de texto (palavras, linhas, caracteres) sao pre-calculadas no salvamento, nao em queries de leitura.

## Proximos passos pendentes

1. Testar compilacao e execucao no Android Studio com rede funcional.
2. Reordenacao de capitulos (drag & drop).
3. Busca textual dentro dos capitulos.
4. Substituir `fallbackToDestructiveMigration()` por migrations adequadas.
5. Expandir testes automatizados.

## Modulo 10 — Formatacao contextual do editor (2026-09-04)

- Removida a barra de formatacao Markdown que ocupava espaco fixo abaixo do editor quando havia selecao.
- O editor agora intercepta a toolbar nativa de selecao do Compose e exibe uma toolbar flutuante junto da palavra/trecho selecionado, normalmente acionada ao pressionar e segurar o texto.
- A toolbar contextual oferece negrito, italico, riscado, titulo e citacao, mantendo tambem copiar, recortar, colar e selecionar tudo quando essas acoes estao disponiveis.
- Negrito, italico e riscado passaram a funcionar como toggle: aplicar novamente sobre o mesmo conteudo remove os marcadores Markdown.
- A selecao permanece sobre o conteudo formatado, e nao sobre os marcadores, facilitando combinar/remover formatos.
- Titulo e citacao agora atuam sobre a linha atual sem inserir quebras de linha artificiais.
- O highlight Markdown passou a reconhecer tambem riscado, heading nivel 3 e citacoes.
- Adicionado `MarkdownFormatting.kt` para concentrar a regra de formatacao e `MarkdownFormattingTest.kt` com casos de toggle e formatos de linha.
- `assembleDebug --offline` executado com sucesso apos a alteracao.
- A suite completa `testDebugUnitTest` continua bloqueada por `OfflineBookRepositoryTest`, teste legado que referencia `BookVersion`, `BookVersionDao` e `BookVersionEntity` removidos na refatoracao para capitulos. Esse erro e anterior e nao pertence ao modulo de formatacao.

## Modulo 11 — Revisao textual offline (2026-09-04)

- Adicionado botao de `Revisao` na barra superior do editor de capitulos.
- Criada `RevisionScreen`, acessivel sem salvar previamente o texto: a analise usa o conteudo atual do `EditorViewModel`, inclusive alteracoes ainda nao versionadas.
- Criado `TextRevisionEngine`, sem dependencia de rede ou IA, para detectar problemas mecanicos e sugestoes editoriais.
- A revisao identifica espacos duplicados entre palavras, espaco antes de pontuacao, palavras consecutivas repetidas e frases com 35 palavras ou mais.
- Problemas mecanicos apresentam acao `Corrigir`; frases longas aparecem apenas como sugestao para preservar a voz do autor.
- Adicionado comando para aplicar em lote as correcoes automaticas encontradas.
- A tela exibe resumo com quantidade de palavras, frases, paragrafos e pontos de revisao.
- Correcoes retornam ao mesmo `EditorViewModel` e deixam o capitulo como `alteracoes nao salvas`; nenhuma revisao cria versao automaticamente, preservando o historico imutavel.
- O detector de espacos duplicados ignora indentacao e os dois espacos finais que podem representar quebra de linha em Markdown.
- Adicionado `TextRevisionEngineTest.kt` com cenarios de espaco duplicado, palavra repetida, espaco antes de pontuacao e frase longa.
- `compileDebugKotlin --offline` e `assembleDebug --offline` executados com sucesso apos a implementacao.
- A suite completa de testes continua impedida pelo teste legado `OfflineBookRepositoryTest`, ja documentado no Modulo 10.

## Modulo 12 — Visao formatada do capitulo e toolbar compacta (2026-09-04)

- A toolbar flutuante de formatacao do editor foi compactada: os cinco controles Markdown agora usam area visual de 36dp e icones de 18dp, reduzindo a largura e a altura do popup contextual.
- Adicionado botao `Visao do capitulo` no topo do editor.
- A nova `ChapterPreviewScreen` renderiza o conteudo atual sem exibir os marcadores Markdown, mantendo negrito, italico, riscado, titulos e citacoes visiveis como formatacao.
- A visao usa o texto atual do editor, inclusive alteracoes ainda nao salvas, e informa esse estado no subtitulo.
- O `MarkdownParser` passou a reconhecer tambem heading nivel 3 (`###`) na renderizacao Compose e HTML.
- A tela de visao e somente leitura: voltar retorna ao editor sem salvar, alterar ou criar versao automaticamente.

## Modulo 13 — Automacao de APK de teste no GitHub (2026-09-05)

- Restaurada a configuracao Git desta copia local apontando para `TomikantGit/Freeferbook2`, preservando os arquivos locais por meio de `git reset --mixed origin/main`.
- Confirmado que o `origin/main` continha apenas `RELATORIO_LIVROHUB.md`; o primeiro push desta etapa passa a versionar o codigo-fonte Android completo.
- Adicionado workflow `.github/workflows/android-test-release.yml`, executado em pushes para `main` e tambem manualmente por `workflow_dispatch`.
- O workflow usa Java 17, Gradle Wrapper e `assembleDebug`, guarda o APK como artefato e publica uma prerelease fixa `test-latest` com o arquivo `freeferbook-test.apk`.
- A assinatura automatizada de teste foi posteriormente migrada para uma keystore dedicada armazenada somente em GitHub Actions Secrets.
- Adicionado `scripts/publicar-teste.ps1` para validar o build local, adicionar caminhos conhecidos do projeto, criar commit e fazer push para `main` com um unico comando.
- Adicionado `scripts/restaurar-git.ps1` para recuperar metadados Git em copias onde `.git` esteja ausente ou vazio, sem substituir os arquivos de trabalho.
- Criado `AUTOMACAO_TESTES.md` com o fluxo operacional e o link fixo da APK de teste.

## Modulo 14 — Atualizacao de builds de teste dentro do app (2026-09-05)

- Adicionada secao `Atualizacoes de teste` em Configuracoes, com exibicao da versao instalada e verificacao manual de novas builds.
- Criado `TestUpdateManager`, que consulta um manifesto `update.json`, compara `versionCode`, baixa o APK somente sob comando do usuario e valida o arquivo por SHA-256 antes de permitir a instalacao.
- O Android continua responsavel pela confirmacao final da instalacao. Em Android 8 ou superior, o app abre a tela de autorizacao de `instalar apps desconhecidos` quando necessario.
- Adicionados `INTERNET` e `REQUEST_INSTALL_PACKAGES` ao manifesto e um `FileProvider` restrito ao diretorio de cache usado pelos APKs de atualizacao.
- O Gradle aceita `testVersionCode` e `testVersionName` por propriedades, permitindo que o GitHub Actions gere codigos de versao crescentes sem alterar o arquivo do projeto a cada build.
- O workflow de teste agora gera `update.json`, `freeferbook-test.apk` e SHA-256 para um canal GitHub Pages separado do repositorio privado.
- A persistencia inicial da assinatura por cache/Release foi removida antes da publicacao do repositorio; a assinatura de teste passou a usar exclusivamente GitHub Actions Secrets.
- A primeira troca entre uma build debug local e uma build assinada pelo GitHub pode exigir desinstalar a build antiga uma unica vez; depois disso as builds automatizadas usam a mesma assinatura de teste.
- `compileDebugKotlin --offline` e `assembleDebug --offline` executados com sucesso apos a implementacao.

## Modulo 15 — Higienizacao para repositorio publico (2026-09-07)

- Removida do GitHub a Release interna que armazenava a antiga `debug.keystore` e removida tambem a `test-latest` assinada com essa chave anterior.
- Removido o cache especifico do GitHub Actions que continha a antiga chave de debug.
- Gerada uma nova keystore dedicada exclusivamente ao canal automatizado de testes, com credenciais aleatorias fortes.
- O workflow deixou de persistir material de assinatura em Release ou cache e passou a exigir somente o GitHub Actions Secret `TEST_SIGNING_BUNDLE`.
- O Secret e desempacotado somente no diretorio temporario do runner; os valores derivados sao mascarados antes de serem exportados ao ambiente do Gradle.
- Builds locais continuam funcionando sem esse Secret e usam a assinatura debug padrao do ambiente local.
- `.gitignore` passou a bloquear `.private/`, `*.jks` e `*.keystore` para reduzir risco de commit acidental de material de assinatura.
- Removidos dos documentos caminhos absolutos vinculados ao usuario do Windows e referencias aos hashes do historico privado anterior.
- Auditoria do snapshot atual nao encontrou e-mails, caminhos absolutos de perfis locais do Windows nem padroes fortes de tokens/chaves no conteudo versionado.
- `compileDebugKotlin --offline`, `assembleDebug --offline` e `signingReport --offline` executados com sucesso durante a migracao da assinatura.
- O canal automatizado de teste passou a usar `applicationId` separado (`com.livrohub.test`), preservando o app local `com.livrohub` e seus dados mesmo quando a chave publica de testes for trocada.
- Adicionado `scripts/configurar-assinatura-teste-publica.ps1`, que gera uma chave dedicada e cadastra `TEST_SIGNING_BUNDLE` via GitHub CLI sem imprimir o segredo.

## Modulo 16 — Refatoracao estrutural para extensibilidade (2026-09-07)

- Criado `AppDependencies`; `MainActivity` passou a injetar um unico grafo de dependencias em `LivroHubAppRoot`, em vez de expandir a assinatura do composable a cada novo repositorio.
- `AppContainer` passou a inicializar banco, repositorios e servicos com `lazy`, reduzindo trabalho antecipado na inicializacao do app.
- Navegacao interna de capitulos foi convertida de varios booleanos independentes para `AppNavigationState` + `ChapterScreen`, tornando as subtelas mutuamente exclusivas.
- O `Saver` da navegacao deixou de guardar conteudos grandes de diff no Bundle; textos de capitulo sao estado transitorio para evitar `TransactionTooLargeException`.
- `LivroHubAppRoot` foi dividido em composables de rota (`LibraryRoute`, `SettingsRoute`, `BookOverviewRoute`, `ChapterRoute`) e passou a compartilhar uma unica instancia de `EditorViewModel` entre editor, preview e revisao.
- Criado `ViewModelSupport.kt` com `viewModelFactory { ... }` generica e `WhileUiSubscribed`; removidas factories repetitivas e timeouts duplicados dos ViewModels.
- Fluxo de atualizacao de teste saiu de `SettingsScreen` e foi isolado em `TestUpdateSection` + `TestUpdateViewModel`, preservando estado durante mudancas de configuracao.
- Criado `ChapterMentionSynchronizer` no dominio; deteccao e sincronizacao de mencoes de personagens/locais deixaram de ser responsabilidade direta do `EditorViewModel`.
- `OfflineBookRepository` passou a depender diretamente de `BookDao`, melhorando testabilidade e reduzindo acoplamento com `LivroHubDatabase`.
- `OfflineBookRepositoryTest` legado foi reescrito para a arquitetura atual. A suite completa voltou a compilar e executar.
- Adicionados testes para `AppNavigationState` e `ChapterMentionSynchronizer`; ajustado teste de diff para refletir a ordem real de linhas alteradas do motor atual.
- Suite final: **22 testes, 0 falhas, 0 erros**.
- Icones de retorno/revisao deprecated foram migrados para variantes `AutoMirrored`.
- `ARCHITECTURE.md` foi reescrito para documentar schema v6, navegacao tipada, DI, updater isolado e regras para novas features.

## Modulo 17 — Organizacao das configuracoes e identidade visual (2026-09-07)

- O nome visivel do app passou a ser `Freeferbook` via `@string/app_name`; a Home deixou de manter `LivroHub` hardcoded e agora reutiliza o recurso de nome do aplicativo.
- A tela de Configuracoes foi reorganizada em tres categorias de alto nivel: `Aparencia`, `Funcionalidades` e `Extras`.
- `Aparencia` concentra tema/cores, tipografia, layout/componentes e animacoes/movimento.
- `Funcionalidades` concentra comportamento do editor e disponibilidade das areas de Personagens e Locais no workspace.
- `Extras` concentra os controles para reexibir tutoriais e o canal de atualizacoes de teste.
- Cada categoria usa um card proprio com descricao e subsecoes internas, deixando a tela preparada para novas configuracoes sem voltar a uma lista unica extensa.
- Removido helper de preview que ficou sem uso apos a reorganizacao.
- `compileDebugKotlin`, `testDebugUnitTest` e `assembleDebug` executados com sucesso apos a reorganizacao inicial.

## Modulo 18 — Configuracoes recolhiveis e nome unificado (2026-09-07)

- O canal publico de testes continua usando o `applicationId` separado `com.livrohub.test`, mas o nome exibido no Android passou de `Freeferbook Test` para apenas `Freeferbook`.
- `Aparencia`, `Funcionalidades` e `Extras` passaram a funcionar como nos recolhiveis, todos fechados por padrao.
- Cada no mostra somente titulo, descricao e indicador de expansao; suas opcoes internas so sao compostas apos o usuario tocar no no.
- Removido o preview global fixo do topo das configuracoes para que nenhuma opcao fique exposta antes de o usuario escolher uma categoria.

## Modulo 19 — Correcao de exportacao PDF/EPUB (2026-09-07)

- A exportacao de capitulos passou a buscar a ultima versao salva por consulta direta ao Room (`getLatestVersion`), sem depender de uma coleta temporaria de `Flow`.
- O estado do capitulo pendente de exportacao passou a usar valores `rememberSaveable` (ID e titulo), evitando perda do contexto caso o Android recrie a Activity enquanto o seletor de arquivos estiver aberto.
- PDF e EPUB agora informam explicitamente quando a ultima versao salva esta vazia e exibem Snackbar de sucesso/falha apos a tentativa de exportacao.
- Criado `MarkdownParser.parseToPlainText` para garantir que o PDF preserve todo o texto visivel ao remover apenas os marcadores Markdown suportados.
- A geracao HTML do EPUB foi refeita por blocos, com escape XHTML e suporte a titulos, citacoes, negrito, italico e riscado sem descartar o texto comum.
- `EpubExporter` agora valida stream de saida e configura corretamente o tamanho da entrada `mimetype` armazenada sem compressao.
- `PdfExporter` fecha `PdfDocument` em todos os caminhos e evita pagina sem linha renderizavel.
- Adicionado `MarkdownParserExportTest` cobrindo preservacao de texto no fluxo PDF e presenca de texto/formatacao no XHTML usado pelo EPUB.

## Modulo 20 — Formatacao avancada no editor (2026-09-07)

- O menu contextual de formatacao passou a oferecer tambem sublinhado, destaque, lista com marcadores, lista numerada e checklist.
- Sublinhado usa a sintaxe interna `++texto++` e destaque usa `==texto==`, ambos funcionando como toggle sem perder a selecao.
- Listas de varias linhas podem alternar entre `- `, numeracao sequencial (`1.`, `2.`, ...), checklist (`- [ ]`) e marcadores personalizados.
- Adicionados marcadores personalizados `•`, `→`, `★`, `✓` e `◆`, acessiveis por um seletor compacto dentro da toolbar contextual.
- Ao trocar o tipo de lista, o prefixo anterior e substituido em vez de ser acumulado, permitindo converter uma lista com marcadores em numerada/checklist/customizada diretamente.
- `MarkdownVisualTransformation` passou a destacar sublinhado e realce dentro do editor.
- `MarkdownParser` passou a preservar/renderizar os novos formatos na visao do capitulo, PDF e EPUB; listas viram `ul/ol`, checklists preservam estado visual e marcadores personalizados permanecem visiveis.
- O parser inline da visao passou a ser recursivo, permitindo combinar estilos como negrito + sublinhado ou italico + destaque no mesmo trecho.
- Testes de formatacao foram ampliados para sublinhado, listas, checklist e marcador personalizado; testes de exportacao cobrem os novos formatos.

## Modulo 21 — Barra superior compacta do editor (2026-09-07)

- Removidos da barra superior do editor os cinco icones de acao que comprimiam excessivamente a area do titulo em telas estreitas.
- A barra superior agora mantem apenas voltar, titulo/subtitulo e um menu de tres pontos.
- Salvar versao, visao do capitulo, revisao, imagens e historico passaram para o menu de overflow, mantendo os mesmos estados de habilitacao.
- O titulo do capitulo e o texto de ultima versao foram limitados a uma linha com reticencias quando necessario, evitando quebra por caractere como ocorria em aparelhos estreitos.
- `compileDebugKotlin`, `testDebugUnitTest`, `assembleDebug` e `git diff --check` executados com sucesso apos a alteracao.

## Modulo 22 — Backup/importacao de livro inteiro (2026-09-07)

- Criado `BookArchiveManager`, integrado ao `AppDependencies`, para exportar e importar um livro completo em um unico arquivo ZIP.
- O backup usa `manifest.json` versionado (`freeferbook-book-backup`, `schemaVersion = 1`) e preserva titulo/data do livro, capitulos, ordem, todo o historico de versoes, personagens, locais e imagens de referencia.
- Imagens locais acessiveis por `content://`, `file://` ou caminho local sao incorporadas em `media/`; URLs web continuam preservadas como referencia externa.
- A importacao gera novos IDs Room e reconstrói as relacoes dentro de transacao, sem sobrescrever livros existentes.
- Adicionadas consultas diretas aos DAOs para montar snapshots consistentes sem depender de `Flow` durante o backup.
- A Biblioteca ganhou menu de overflow com `Importar livro`; cada card de livro ganhou `Exportar livro (.zip)`.
- Operacoes longas exibem progresso e retornam resultado via Snackbar; criar/renomear/excluir/exportar fica bloqueado enquanto um backup esta em andamento.
- O importador valida o conteudo do arquivo, rejeita ZIPs sem manifesto Freeferbook e detecta RAR/7z com mensagem explicita de formato nao suportado.
- O leitor de ZIP nao extrai nomes de caminho fornecidos pelo arquivo: midias sao copiadas para nomes gerados pelo app e limitadas por tamanho, reduzindo risco de zip-slip/zip bomb.
- `OfflineBookRepositoryTest` foi adaptado para a nova consulta direta de `BookDao`; adicionados testes de sanitizacao do nome de arquivo de backup.

## Modulo 23 — Freeferbook Web e release multiplataforma inicial (2026-09-09)

- Criada a primeira versao funcional do Freeferbook Web em `web/`, publicada pelo mesmo GitHub Actions que gera o APK Android.
- A Web usa IndexedDB para armazenar projetos localmente no navegador, sem backend ou conta obrigatoria nesta fase.
- Implementadas Biblioteca, criacao/renomeacao/exclusao de livros, capitulos, editor, rascunho automatico, visao Markdown e historico imutavel de versoes.
- A Web importa e exporta o mesmo formato `freeferbook-book-backup`/`schemaVersion = 1` criado pelo Android, incluindo historico, personagens, locais, imagens e midias incorporadas quando presentes.
- O leitor ZIP Web suporta entradas STORE e DEFLATE; foi validado com ZIP comprimido equivalente ao gerado pelo Android e com round-trip Web -> ZIP -> Web.
- Antes de exportar um projeto com rascunhos nao versionados, a Web pede confirmacao e cria versoes automaticas para evitar perda do texto mais recente no backup.
- A pagina inicial exibe a versao/commit Web e a versao Android publicados no mesmo run, facilitando conferir se o deploy de cada plataforma corresponde ao mesmo commit.
- O workflow passou a copiar `web/` para o artefato do GitHub Pages, gerar `web-version.json`, manter `update.json`/APK na mesma raiz e foi renomeado para `Freeferbook Test Release`.
- Adicionado aviso automatico de nova atualizacao ao iniciar `com.livrohub.test`: o usuario pode escolher `Atualizar agora` ou `Depois`; falhas de rede na checagem inicial sao silenciosas.
- O fluxo automatico reaproveita `TestUpdateManager`, mantendo download, validacao SHA-256 e instalacao confirmada pelo Android.
- `compileDebugKotlin`, `testDebugUnitTest`, `assembleDebug`, `publicTestBuild`, verificacao sintatica dos modulos JavaScript e `git diff --check` foram executados com sucesso.

## Modulo 24 — Convergencia Web/Android e contrato de backup (2026-09-09)

- A Web passou a oferecer tres areas por livro: `Capitulos`, `Personagens` e `Locais`, aproximando o workspace do comportamento Android.
- Personagens podem ser criados, selecionados, editados e excluidos com nome, sobrenomes, capitulos e referencia de imagem.
- Locais podem ser criados, selecionados, editados e excluidos com nome, descricao, capitulos e referencia de imagem.
- As alteracoes de worldbuilding sao salvas automaticamente no IndexedDB e continuam entrando no mesmo backup completo ja compativel com Android.
- Ao alterar manualmente a referencia de imagem de um item importado, a associacao com a midia incorporada anterior e descartada para evitar exportar imagem divergente do novo valor.
- Criada fixture canonica `contracts/book-backup-v1.sample.json` para o contrato `freeferbook-book-backup/schemaVersion = 1`.
- Criado `scripts/validate-backup-contract.mjs`, que compara as constantes Android/Web com a fixture e executa round-trip Web -> ZIP -> Web.
- O GitHub Actions agora valida esse contrato antes de configurar Java/compilar o APK, fazendo a release falhar cedo se Android e Web divergirem.
- Smoke test em Chrome real validou criacao/edicao de livro, personagem e local usando a interface Web, IndexedDB e eventos reais do navegador.
- `node --check`, validador do contrato, `testDebugUnitTest`, `assembleDebug` e `git diff --check` passaram localmente.

## Modulo 25 — Configuracoes Web recolhiveis (2026-09-09)

- A Web ganhou uma tela de Configuracoes com os mesmos tres nos de alto nivel do Android: `Aparencia`, `Funcionalidades` e `Extras`, todos fechados por padrao.
- `Aparencia` controla tema (sistema/claro/escuro), tamanho do texto do editor e altura da linha.
- `Funcionalidades` controla numeros de linha e a visibilidade das areas de Personagens e Locais.
- `Extras` controla reducao de movimento e exibe as versoes Web/Android publicadas, alem do atalho para baixar o APK.
- Preferencias Web sao persistidas em `localStorage` (`freeferbook-web-settings-v1`) e reaplicadas no carregamento sem misturar configuracoes do navegador com os dados/backup do livro.
- O editor Web ganhou gutter de numeros de linha sincronizado com o scroll e com a quantidade de linhas logicas do rascunho.
- Tema/tipografia sao aplicados por CSS custom properties; ocultar Personagens/Locais remove a aba visual sem excluir os dados existentes.
- Smoke test em Chrome real confirmou nos inicialmente recolhidos, numeros de linha, tema escuro, tamanho de fonte, ocultacao de Personagens e persistencia das preferencias apos `reload`.

## Observacoes

- O ambiente local atual possui Java/Android SDK suficientes para `compileDebugKotlin`, `testDebugUnitTest` e `assembleDebug` em modo offline.
- O principal risco estrutural ainda pendente e `fallbackToDestructiveMigration()` no Room. Antes de evoluir o schema alem da v6 para distribuicao real, implementar migrations explicitas.
- O canal publico `com.livrohub.test` permanece separado do package local `com.livrohub` e deve ser usado para ciclos rapidos de teste/atualizacao.
