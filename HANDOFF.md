# LivroHub — Handoff para continuar em outra conta

## O que é o LivroHub

App Android offline, em Kotlin + Jetpack Compose, que funciona como um "GitHub para livros". Permite criar livros/manuscritos, organizar por capítulos, editar texto com highlight Markdown, salvar versões com histórico imutável, comparar diferenças por palavra (diff), restaurar versões anteriores, catalogar personagens e exportar conteúdo.

## Stack

- Kotlin + Jetpack Compose + Material 3
- MVVM + Repository pattern
- Room/SQLite (banco local, 100% offline)
- DataStore Preferences (configurações do app)
- java-diff-utils (`io.github.java-diff-utils:java-diff-utils`)
- Coroutines + StateFlow
- Coil Compose (imagens de personagens)
- `minSdk = 24`, `targetSdk = 35`

## Estado atual do código

### Funcionalidades implementadas

1. **Biblioteca de livros** — criar, renomear, excluir livros com estatísticas (capítulos, linhas, palavras, caracteres)
2. **Workspace de livro** — abas de capítulos e personagens com NavigationBar
3. **Capítulos** — criar, renomear, excluir capítulos dentro de um livro
4. **Editor de texto** — campo grande com highlight Markdown (bold, italic, H1, H2), números de linha opcionais, tamanho de fonte configurável
5. **Versionamento** — salvar versão com mensagem opcional, cada salvamento cria nova entrada preservando histórico completo
6. **Diff por palavra** — comparar texto atual com última versão salva, com inline highlighting por palavra (verde/vermelho)
7. **Histórico** — lista cronológica de versões com restauração (cria nova versão sem apagar histórico)
8. **Comparação entre versões** — selecionar duas versões salvas e ver diff entre elas
9. **Exportação** — exportar qualquer versão como .txt ou .md via SAF
10. **Personagens** — catálogo com nome, sobrenomes, capítulos, foto (via Photo Picker)
11. **Configurações** — tema (sistema/claro/escuro), tamanho da fonte, números de linha

### Código refatorado (Módulo 8)

- Imports FQN corrigidos em AppContainer
- Regex compilados como companion object (OfflineChapterRepository, MarkdownVisualTransformation, TextDiffEngine)
- DiffRowGenerator movido para companion object (thread-safe)
- Cores de diff com suporte dark/light mode (objeto DiffColors)
- Auto-wrap removido do EditorViewModel (Compose faz wrap visual)
- TextStyle memoizado com remember no EditorScreen
- APIs deprecated substituídas (Divider → HorizontalDivider, values() → entries)
- CharacterEntity com @ColumnInfo snake_case consistente
- TopAppBar com botão voltar adicionado ao BookWorkspaceScreen
- KDoc em todos os 35+ arquivos Kotlin
- Import não utilizado removido (withTransaction em OfflineBookRepository)

## Estrutura de pastas

```
com.livrohub/
├── LivroHubApp.kt              # Application
├── MainActivity.kt             # Activity principal
├── di/AppContainer.kt          # DI manual
├── data/local/                 # Entities + DAOs + Database
├── data/repository/            # Repository implementations
├── domain/model/               # Data classes de domínio
├── domain/repository/          # Repository interfaces
├── domain/diff/                # Motor de diff (TextDiffEngine)
└── ui/                         # Screens + ViewModels
    ├── LivroHubAppRoot.kt
    ├── home/
    ├── library/
    ├── workspace/
    ├── chapters/
    ├── characters/
    ├── editor/
    ├── diff/
    ├── history/
    └── settings/
```

## Banco de dados — Schema v4

| Tabela | Descrição |
|--------|-----------|
| `books` | Livros (id, title, created_at) |
| `chapters` | Capítulos (id, book_id FK, title, order_index, created_at) |
| `chapter_versions` | Versões (id, chapter_id FK, content, created_at, message, sequence_number, word/char/line_count) |
| `characters` | Personagens (id, book_id FK, name, surnames, chapters, image_uri) |

Todas as FKs usam CASCADE para exclusão.

## Documentação disponível

| Arquivo | Conteúdo |
|---------|----------|
| `ARCHITECTURE.md` | Arquitetura completa com diagramas, schema, fluxos, padrões |
| `PROGRESS.md` | Registro cronológico de implementação, decisões, pendências |
| `SETUP.md` | Como abrir no Android Studio |
| `TOOLS_SETUP.md` | Instalação de Java/Gradle/Android SDK |
| `HANDOFF.md` | Este arquivo |

## Como continuar

1. Copiar/clonar o projeto para a nova máquina/conta
2. Abrir no Android Studio (sincronizar Gradle baixará dependências)
3. Ler `ARCHITECTURE.md` para entender o sistema
4. Ler `PROGRESS.md` antes de editar código
5. Manter etapas pequenas e atualizar a documentação

## Regras para a próxima IA

- **Não recomece o projeto do zero**
- Leia `ARCHITECTURE.md` e `PROGRESS.md` antes de editar
- Mantenha o padrão MVVM + Repository
- Mantenha o histórico imutável (nunca deletar/editar versões existentes)
- Atualize `PROGRESS.md` ao finalizar cada módulo
- Adicione KDoc em código novo
- Compile com `./gradlew assembleDebug` para validar

## Próximos passos sugeridos

1. Testar compilação e execução no Android Studio
2. Implementar importação de livros/manuscritos
3. Reordenação de capítulos (drag & drop)
4. Busca textual dentro dos capítulos
5. Migração adequada do banco (substituir `fallbackToDestructiveMigration()`)
6. Testes automatizados expandidos
