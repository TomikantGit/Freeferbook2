export const CUSTOM_MARKERS = Object.freeze(["•", "→", "★", "✓", "◆"]);

function normalizeSelection(text, start, end) {
    const length = String(text ?? "").length;
    const a = Math.max(0, Math.min(Number(start) || 0, length));
    const b = Math.max(0, Math.min(Number(end) || 0, length));
    return { start: Math.min(a, b), end: Math.max(a, b) };
}

function unchanged(text, start, end) {
    const selection = normalizeSelection(text, start, end);
    return { text: String(text ?? ""), ...selection };
}

export function toggleInlineFormat(text, start, end, prefix, suffix = prefix) {
    const value = String(text ?? "");
    const selection = normalizeSelection(value, start, end);
    if (selection.start === selection.end || !value) return unchanged(value, start, end);

    const hasOuterMarkers =
        selection.start >= prefix.length &&
        selection.end + suffix.length <= value.length &&
        value.slice(selection.start - prefix.length, selection.start) === prefix &&
        value.slice(selection.end, selection.end + suffix.length) === suffix;

    if (hasOuterMarkers) {
        const markerStart = selection.start - prefix.length;
        const content = value.slice(selection.start, selection.end);
        return {
            text: value.slice(0, markerStart) + content + value.slice(selection.end + suffix.length),
            start: markerStart,
            end: markerStart + content.length
        };
    }

    const content = value.slice(selection.start, selection.end);
    const contentStart = selection.start + prefix.length;
    return {
        text: value.slice(0, selection.start) + prefix + content + suffix + value.slice(selection.end),
        start: contentStart,
        end: contentStart + content.length
    };
}

function selectedLineRange(text, start, end) {
    const selection = normalizeSelection(text, start, end);
    const lineStartBreak = text.lastIndexOf("\n", selection.start - 1);
    const lineStart = lineStartBreak === -1 ? 0 : lineStartBreak + 1;
    const lineEndBreak = text.indexOf("\n", selection.end);
    const lineEnd = lineEndBreak === -1 ? text.length : lineEndBreak;
    return { ...selection, lineStart, lineEnd };
}

export function toggleLinePrefix(text, start, end, prefix) {
    const value = String(text ?? "");
    const range = selectedLineRange(value, start, end);
    if (range.start === range.end || !value) return unchanged(value, start, end);

    const lines = value.slice(range.lineStart, range.lineEnd).split("\n");
    const removePrefix = lines.every(line => line.startsWith(prefix));
    let selectionStartDelta = 0;
    let selectionEndDelta = 0;
    let runningOffset = range.lineStart;

    const transformedLines = lines.map((line, index) => {
        const originalStart = runningOffset;
        const originalEnd = originalStart + line.length;
        const affectsStart = range.start >= originalStart && range.start <= originalEnd;
        const affectsEnd = range.end >= originalStart && range.end <= originalEnd;

        let transformed;
        if (removePrefix) {
            if (affectsStart) selectionStartDelta -= Math.min(prefix.length, range.start - originalStart);
            if (originalStart < range.end || affectsEnd) selectionEndDelta -= prefix.length;
            transformed = line.startsWith(prefix) ? line.slice(prefix.length) : line;
        } else {
            if (affectsStart) selectionStartDelta += prefix.length;
            if (originalStart < range.end || affectsEnd) selectionEndDelta += prefix.length;
            transformed = prefix + line;
        }

        runningOffset = originalEnd + (index < lines.length - 1 ? 1 : 0);
        return transformed;
    });

    const transformedBlock = transformedLines.join("\n");
    const newText = value.slice(0, range.lineStart) + transformedBlock + value.slice(range.lineEnd);
    const newStart = Math.max(0, Math.min(range.start + selectionStartDelta, newText.length));
    const newEnd = Math.max(newStart, Math.min(range.end + selectionEndDelta, newText.length));
    return { text: newText, start: newStart, end: newEnd };
}

function parseListPrefix(line) {
    if (/^- \[[ xX]\] /.test(line)) return { kind: "checklist", length: 6 };
    if (line.startsWith("- ")) return { kind: "bulleted", length: 2 };
    const numbered = line.match(/^\d+\. /);
    if (numbered) return { kind: "numbered", length: numbered[0].length };
    const marker = CUSTOM_MARKERS.find(candidate => line.startsWith(`${candidate} `));
    if (marker) return { kind: "custom", marker, length: marker.length + 1 };
    return null;
}

function isTargetPrefix(prefix, target) {
    if (!prefix) return false;
    if (target.kind === "custom") return prefix.kind === "custom" && prefix.marker === target.marker;
    return prefix.kind === target.kind;
}

function targetPrefix(target, index) {
    switch (target.kind) {
        case "bulleted": return "- ";
        case "numbered": return `${index + 1}. `;
        case "checklist": return "- [ ] ";
        case "custom": return `${target.marker} `;
        default: throw new Error(`Estilo de lista desconhecido: ${target.kind}`);
    }
}

export function toggleListStyle(text, start, end, target) {
    const value = String(text ?? "");
    const range = selectedLineRange(value, start, end);
    if (range.start === range.end || !value) return unchanged(value, start, end);
    if (target?.kind === "custom" && !CUSTOM_MARKERS.includes(target.marker)) return unchanged(value, start, end);

    const lines = value.slice(range.lineStart, range.lineEnd).split("\n");
    const parsed = lines.map(parseListPrefix);
    const allAlreadyTarget = parsed.every(prefix => isTargetPrefix(prefix, target));

    let oldAbsoluteLineStart = range.lineStart;
    let newAbsoluteLineStart = range.lineStart;
    let mappedStart = range.start;
    let mappedEnd = range.end;

    const transformedLines = lines.map((line, index) => {
        const oldPrefixLength = parsed[index]?.length ?? 0;
        const content = line.slice(oldPrefixLength);
        const newPrefix = allAlreadyTarget ? "" : targetPrefix(target, index);
        const transformed = newPrefix + content;

        const mapOffset = offset => {
            const positionInOldLine = Math.max(0, Math.min(offset - oldAbsoluteLineStart, line.length));
            const contentOffset = Math.max(0, positionInOldLine - oldPrefixLength);
            return newAbsoluteLineStart + newPrefix.length + contentOffset;
        };

        const oldLineEnd = oldAbsoluteLineStart + line.length;
        if (range.start >= oldAbsoluteLineStart && range.start <= oldLineEnd) mappedStart = mapOffset(range.start);
        if (range.end >= oldAbsoluteLineStart && range.end <= oldLineEnd) mappedEnd = mapOffset(range.end);

        oldAbsoluteLineStart = oldLineEnd + (index < lines.length - 1 ? 1 : 0);
        newAbsoluteLineStart += transformed.length + (index < lines.length - 1 ? 1 : 0);
        return transformed;
    });

    const newBlock = transformedLines.join("\n");
    const newText = value.slice(0, range.lineStart) + newBlock + value.slice(range.lineEnd);
    const newStart = Math.max(0, Math.min(mappedStart, newText.length));
    const newEnd = Math.max(newStart, Math.min(mappedEnd, newText.length));
    return { text: newText, start: newStart, end: newEnd };
}

export function applyMarkdownFormat(text, start, end, format) {
    switch (format) {
        case "bold": return toggleInlineFormat(text, start, end, "**");
        case "italic": return toggleInlineFormat(text, start, end, "*");
        case "strike": return toggleInlineFormat(text, start, end, "~~");
        case "underline": return toggleInlineFormat(text, start, end, "++");
        case "highlight": return toggleInlineFormat(text, start, end, "==");
        case "heading": return toggleLinePrefix(text, start, end, "### ");
        case "quote": return toggleLinePrefix(text, start, end, "> ");
        case "bulleted": return toggleListStyle(text, start, end, { kind: "bulleted" });
        case "numbered": return toggleListStyle(text, start, end, { kind: "numbered" });
        case "checklist": return toggleListStyle(text, start, end, { kind: "checklist" });
        default: return unchanged(text, start, end);
    }
}

export function applyCustomMarker(text, start, end, marker) {
    return toggleListStyle(text, start, end, { kind: "custom", marker });
}
