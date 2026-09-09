export const RevisionIssueType = Object.freeze({
    DUPLICATE_SPACE: "DUPLICATE_SPACE",
    SPACE_BEFORE_PUNCTUATION: "SPACE_BEFORE_PUNCTUATION",
    REPEATED_WORD: "REPEATED_WORD",
    LONG_SENTENCE: "LONG_SENTENCE"
});

const duplicateSpaceRegex = /(?<=[\p{L}\p{N}])[\t ]{2,}(?=[\p{L}\p{N}])/gu;
const spaceBeforePunctuationRegex = /[\t ]+([,.;:!?])/gu;
const repeatedWordRegex = /\b([\p{L}][\p{L}'’-]*)\b([\t ]+)\1\b/giu;
const wordRegex = /[\p{L}\p{N}][\p{L}\p{N}'’-]*/gu;
const sentenceRegex = /[^.!?\n]+(?:[.!?]+|$)/gu;

function makeIssue(text, { type, start, endExclusive, title, description, replacement = null }) {
    const contextStart = Math.max(0, start - 28);
    const contextEnd = Math.min(text.length, endExclusive + 28);
    const raw = text.slice(contextStart, contextEnd).replaceAll("\n", " ").trim();
    return {
        type,
        start,
        endExclusive,
        title,
        description,
        excerpt: `${contextStart > 0 ? "…" : ""}${raw}${contextEnd < text.length ? "…" : ""}`,
        replacement,
        isAutoFixable: replacement !== null
    };
}

export function reviewText(input) {
    const text = String(input ?? "");
    if (!text.trim()) {
        return { issues: [], wordCount: 0, sentenceCount: 0, paragraphCount: 0, autoFixableCount: 0 };
    }

    const mechanical = [];

    for (const match of text.matchAll(duplicateSpaceRegex)) {
        mechanical.push(makeIssue(text, {
            type: RevisionIssueType.DUPLICATE_SPACE,
            start: match.index,
            endExclusive: match.index + match[0].length,
            title: "Espaçamento duplicado",
            description: "Há mais de um espaço entre elementos do texto.",
            replacement: " "
        }));
    }

    for (const match of text.matchAll(spaceBeforePunctuationRegex)) {
        mechanical.push(makeIssue(text, {
            type: RevisionIssueType.SPACE_BEFORE_PUNCTUATION,
            start: match.index,
            endExclusive: match.index + match[0].length,
            title: "Espaço antes da pontuação",
            description: "A pontuação deve ficar junto da palavra anterior.",
            replacement: match[1]
        }));
    }

    for (const match of text.matchAll(repeatedWordRegex)) {
        mechanical.push(makeIssue(text, {
            type: RevisionIssueType.REPEATED_WORD,
            start: match.index,
            endExclusive: match.index + match[0].length,
            title: "Palavra repetida",
            description: `A palavra “${match[1]}” aparece duas vezes seguidas.`,
            replacement: match[1]
        }));
    }

    const acceptedMechanical = [];
    for (const candidate of mechanical.sort((a, b) => a.start - b.start || b.endExclusive - a.endExclusive)) {
        const overlaps = acceptedMechanical.some(issue => candidate.start < issue.endExclusive && candidate.endExclusive > issue.start);
        if (!overlaps) acceptedMechanical.push(candidate);
    }

    const styleIssues = [];
    const sentenceMatches = [...text.matchAll(sentenceRegex)];
    for (const match of sentenceMatches) {
        const sentence = match[0].trim();
        const words = [...sentence.matchAll(wordRegex)].length;
        if (words < 35) continue;
        styleIssues.push(makeIssue(text, {
            type: RevisionIssueType.LONG_SENTENCE,
            start: match.index,
            endExclusive: match.index + match[0].length,
            title: "Frase longa",
            description: `Esta frase tem ${words} palavras. Considere dividi-la para melhorar a leitura.`
        }));
    }

    const issues = [...acceptedMechanical, ...styleIssues].sort((a, b) => a.start - b.start);
    const paragraphCount = text.split(/\n\s*\n/u).filter(paragraph => paragraph.trim()).length;
    const sentenceCount = sentenceMatches.map(match => match[0].trim()).filter(Boolean).length;
    const wordCount = [...text.matchAll(wordRegex)].length;

    return {
        issues,
        wordCount,
        sentenceCount,
        paragraphCount,
        autoFixableCount: issues.filter(issue => issue.isAutoFixable).length
    };
}

export function applyRevisionIssue(input, issue) {
    const text = String(input ?? "");
    if (!issue?.isAutoFixable || issue.replacement === null) return text;
    if (!Number.isInteger(issue.start) || !Number.isInteger(issue.endExclusive)) return text;
    if (issue.start < 0 || issue.endExclusive < 0 || issue.start > issue.endExclusive || issue.endExclusive > text.length) return text;
    return text.slice(0, issue.start) + issue.replacement + text.slice(issue.endExclusive);
}

export function applyAllSafeRevision(input, result = reviewText(input)) {
    return result.issues
        .filter(issue => issue.isAutoFixable)
        .sort((a, b) => b.start - a.start)
        .reduce((current, issue) => applyRevisionIssue(current, issue), String(input ?? ""));
}
