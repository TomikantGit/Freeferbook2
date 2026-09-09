export const DiffLineType = Object.freeze({
    UNCHANGED: "unchanged",
    ADDED: "added",
    REMOVED: "removed"
});

export const DiffSpanType = DiffLineType;

function myersOperations(oldItems, newItems) {
    const n = oldItems.length;
    const m = newItems.length;
    if (!n) return newItems.map(value => ({ type: DiffLineType.ADDED, value }));
    if (!m) return oldItems.map(value => ({ type: DiffLineType.REMOVED, value }));

    let frontier = new Map([[1, 0]]);
    const trace = [];

    for (let d = 0; d <= n + m; d++) {
        trace.push(new Map(frontier));
        for (let k = -d; k <= d; k += 2) {
            const left = frontier.get(k - 1) ?? Number.NEGATIVE_INFINITY;
            const right = frontier.get(k + 1) ?? Number.NEGATIVE_INFINITY;
            let x = (k === -d || (k !== d && left < right))
                ? (frontier.get(k + 1) ?? 0)
                : (frontier.get(k - 1) ?? 0) + 1;
            let y = x - k;

            while (x < n && y < m && oldItems[x] === newItems[y]) {
                x++;
                y++;
            }
            frontier.set(k, x);

            if (x >= n && y >= m) {
                return backtrack(trace, oldItems, newItems);
            }
        }
    }
    return [];
}

function backtrack(trace, oldItems, newItems) {
    let x = oldItems.length;
    let y = newItems.length;
    const result = [];

    for (let d = trace.length - 1; d >= 0; d--) {
        const frontier = trace[d];
        const k = x - y;
        const left = frontier.get(k - 1) ?? Number.NEGATIVE_INFINITY;
        const right = frontier.get(k + 1) ?? Number.NEGATIVE_INFINITY;
        const previousK = (k === -d || (k !== d && left < right)) ? k + 1 : k - 1;
        const previousX = frontier.get(previousK) ?? 0;
        const previousY = previousX - previousK;

        while (x > previousX && y > previousY) {
            result.push({ type: DiffLineType.UNCHANGED, value: oldItems[x - 1] });
            x--;
            y--;
        }

        if (d === 0) break;
        if (x === previousX) {
            result.push({ type: DiffLineType.ADDED, value: newItems[y - 1] });
            y--;
        } else {
            result.push({ type: DiffLineType.REMOVED, value: oldItems[x - 1] });
            x--;
        }
    }

    return result.reverse();
}

function tokenize(line) {
    return String(line).match(/\s+|[\p{L}\p{N}'’-]+|[^\s\p{L}\p{N}'’-]+/gu) ?? [];
}

function mergeSpans(spans) {
    const merged = [];
    for (const span of spans) {
        if (!span.text) continue;
        const last = merged.at(-1);
        if (last?.type === span.type) last.text += span.text;
        else merged.push({ ...span });
    }
    return merged.length ? merged : [{ type: DiffSpanType.UNCHANGED, text: "" }];
}

function inlineSpans(oldLine, newLine) {
    const operations = myersOperations(tokenize(oldLine), tokenize(newLine));
    return {
        oldSpans: mergeSpans(operations
            .filter(op => op.type !== DiffLineType.ADDED)
            .map(op => ({ type: op.type === DiffLineType.REMOVED ? DiffSpanType.REMOVED : DiffSpanType.UNCHANGED, text: op.value }))),
        newSpans: mergeSpans(operations
            .filter(op => op.type !== DiffLineType.REMOVED)
            .map(op => ({ type: op.type === DiffLineType.ADDED ? DiffSpanType.ADDED : DiffSpanType.UNCHANGED, text: op.value })))
    };
}

function splitLines(text) {
    const normalized = String(text ?? "").replaceAll("\r\n", "\n");
    return normalized === "" ? [] : normalized.split("\n");
}

export function compareText(oldText, newText) {
    const operations = myersOperations(splitLines(oldText), splitLines(newText));
    const lines = [];

    for (let index = 0; index < operations.length;) {
        const operation = operations[index];
        if (operation.type === DiffLineType.UNCHANGED) {
            lines.push({
                type: DiffLineType.UNCHANGED,
                prefix: " ",
                spans: [{ type: DiffSpanType.UNCHANGED, text: operation.value }]
            });
            index++;
            continue;
        }

        const block = [];
        while (index < operations.length && operations[index].type !== DiffLineType.UNCHANGED) {
            block.push(operations[index++]);
        }
        const removed = block.filter(op => op.type === DiffLineType.REMOVED).map(op => op.value);
        const added = block.filter(op => op.type === DiffLineType.ADDED).map(op => op.value);
        const paired = Math.min(removed.length, added.length);

        for (let i = 0; i < paired; i++) {
            const { oldSpans, newSpans } = inlineSpans(removed[i], added[i]);
            lines.push({ type: DiffLineType.REMOVED, prefix: "-", spans: oldSpans });
            lines.push({ type: DiffLineType.ADDED, prefix: "+", spans: newSpans });
        }
        for (let i = paired; i < removed.length; i++) {
            lines.push({ type: DiffLineType.REMOVED, prefix: "-", spans: [{ type: DiffSpanType.REMOVED, text: removed[i] }] });
        }
        for (let i = paired; i < added.length; i++) {
            lines.push({ type: DiffLineType.ADDED, prefix: "+", spans: [{ type: DiffSpanType.ADDED, text: added[i] }] });
        }
    }

    return lines;
}

export function diffSummary(lines) {
    return lines.reduce((summary, line) => {
        summary[line.type]++;
        return summary;
    }, { unchanged: 0, added: 0, removed: 0 });
}
