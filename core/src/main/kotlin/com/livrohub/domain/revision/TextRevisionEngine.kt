package com.livrohub.domain.revision

/** Tipos de ocorrência encontrados pela revisão textual local. */
enum class RevisionIssueType {
    DUPLICATE_SPACE,
    SPACE_BEFORE_PUNCTUATION,
    REPEATED_WORD,
    LONG_SENTENCE
}

/**
 * Uma ocorrência detectada no texto.
 *
 * [replacement] só é preenchido quando a correção pode ser aplicada de forma
 * determinística sem reescrever o conteúdo do autor.
 */
data class RevisionIssue(
    val type: RevisionIssueType,
    val start: Int,
    val endExclusive: Int,
    val title: String,
    val description: String,
    val excerpt: String,
    val replacement: String? = null
) {
    val isAutoFixable: Boolean
        get() = replacement != null
}

/** Resumo calculado junto com as ocorrências da revisão. */
data class RevisionResult(
    val issues: List<RevisionIssue>,
    val wordCount: Int,
    val sentenceCount: Int,
    val paragraphCount: Int
) {
    val autoFixableCount: Int
        get() = issues.count(RevisionIssue::isAutoFixable)
}

/**
 * Motor de revisão editorial leve e 100% offline.
 *
 * O objetivo é detectar problemas mecânicos com baixo risco de falso positivo.
 * Questões de estilo são apenas sinalizadas e nunca reescritas automaticamente.
 */
object TextRevisionEngine {
    // Só trata espaçamento duplicado entre conteúdo alfanumérico. Assim, não
    // destrói indentação nem os dois espaços finais usados como quebra Markdown.
    private val duplicateSpaceRegex = Regex("(?<=[\\p{L}\\p{N}])[\\t ]{2,}(?=[\\p{L}\\p{N}])")
    private val spaceBeforePunctuationRegex = Regex("[\\t ]+([,.;:!?])")
    private val repeatedWordRegex = Regex(
        pattern = "(?iu)\\b([\\p{L}][\\p{L}'’-]*)\\b([\\t ]+)\\1\\b"
    )
    private val wordRegex = Regex("(?u)[\\p{L}\\p{N}][\\p{L}\\p{N}'’-]*")
    private val sentenceRegex = Regex("[^.!?\\n]+(?:[.!?]+|$)")

    fun review(text: String): RevisionResult {
        if (text.isBlank()) {
            return RevisionResult(
                issues = emptyList(),
                wordCount = 0,
                sentenceCount = 0,
                paragraphCount = 0
            )
        }

        val mechanicalIssues = buildList {
            duplicateSpaceRegex.findAll(text).forEach { match ->
                add(
                    issue(
                        text = text,
                        type = RevisionIssueType.DUPLICATE_SPACE,
                        start = match.range.first,
                        endExclusive = match.range.last + 1,
                        title = "Espaçamento duplicado",
                        description = "Há mais de um espaço entre elementos do texto.",
                        replacement = " "
                    )
                )
            }

            spaceBeforePunctuationRegex.findAll(text).forEach { match ->
                val punctuation = match.groupValues[1]
                add(
                    issue(
                        text = text,
                        type = RevisionIssueType.SPACE_BEFORE_PUNCTUATION,
                        start = match.range.first,
                        endExclusive = match.range.last + 1,
                        title = "Espaço antes da pontuação",
                        description = "A pontuação deve ficar junto da palavra anterior.",
                        replacement = punctuation
                    )
                )
            }

            repeatedWordRegex.findAll(text).forEach { match ->
                val firstWord = match.groupValues[1]
                add(
                    issue(
                        text = text,
                        type = RevisionIssueType.REPEATED_WORD,
                        start = match.range.first,
                        endExclusive = match.range.last + 1,
                        title = "Palavra repetida",
                        description = "A palavra “$firstWord” aparece duas vezes seguidas.",
                        replacement = firstWord
                    )
                )
            }
        }

        // Evita mostrar duas correções automáticas sobre exatamente a mesma faixa.
        val nonOverlappingMechanical = mechanicalIssues
            .sortedWith(compareBy<RevisionIssue> { it.start }.thenByDescending { it.endExclusive })
            .fold(mutableListOf<RevisionIssue>()) { accepted, candidate ->
                val overlaps = accepted.any {
                    candidate.start < it.endExclusive && candidate.endExclusive > it.start
                }
                if (!overlaps) accepted += candidate
                accepted
            }

        val styleIssues = sentenceRegex.findAll(text).mapNotNull { match ->
            val sentence = match.value.trim()
            val words = wordRegex.findAll(sentence).count()
            if (words < 35) return@mapNotNull null

            issue(
                text = text,
                type = RevisionIssueType.LONG_SENTENCE,
                start = match.range.first,
                endExclusive = match.range.last + 1,
                title = "Frase longa",
                description = "Esta frase tem $words palavras. Considere dividi-la para melhorar a leitura.",
                replacement = null
            )
        }.toList()

        val sentenceCount = sentenceRegex.findAll(text)
            .map { it.value.trim() }
            .count { it.isNotEmpty() }

        val paragraphCount = text
            .split(Regex("\\n\\s*\\n"))
            .count { it.isNotBlank() }

        return RevisionResult(
            issues = (nonOverlappingMechanical + styleIssues).sortedBy { it.start },
            wordCount = wordRegex.findAll(text).count(),
            sentenceCount = sentenceCount,
            paragraphCount = paragraphCount
        )
    }

    /** Aplica uma ocorrência se ela ainda corresponder à faixa analisada. */
    fun applyIssue(text: String, issue: RevisionIssue): String {
        val replacement = issue.replacement ?: return text
        if (issue.start !in 0..text.length || issue.endExclusive !in 0..text.length) return text
        if (issue.start > issue.endExclusive) return text

        return text.replaceRange(issue.start, issue.endExclusive, replacement)
    }

    /** Aplica todas as correções seguras, do fim para o início para preservar offsets. */
    fun applyAllSafe(text: String, result: RevisionResult = review(text)): String {
        return result.issues
            .filter(RevisionIssue::isAutoFixable)
            .sortedByDescending { it.start }
            .fold(text) { current, issue -> applyIssue(current, issue) }
    }

    private fun issue(
        text: String,
        type: RevisionIssueType,
        start: Int,
        endExclusive: Int,
        title: String,
        description: String,
        replacement: String?
    ): RevisionIssue {
        val contextStart = (start - 28).coerceAtLeast(0)
        val contextEnd = (endExclusive + 28).coerceAtMost(text.length)
        val raw = text.substring(contextStart, contextEnd)
            .replace('\n', ' ')
            .trim()
        val excerpt = buildString {
            if (contextStart > 0) append("…")
            append(raw)
            if (contextEnd < text.length) append("…")
        }

        return RevisionIssue(
            type = type,
            start = start,
            endExclusive = endExclusive,
            title = title,
            description = description,
            excerpt = excerpt,
            replacement = replacement
        )
    }
}
