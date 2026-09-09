const CUSTOM_MARKERS = ["•", "→", "★", "✓", "◆"];

export function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

export function renderInlineMarkdown(value) {
    let text = escapeHtml(value);
    text = text.replace(/`([^`]+)`/g, "<code>$1</code>");
    text = text.replace(/~~(.+?)~~/g, "<del>$1</del>");
    text = text.replace(/\+\+(.+?)\+\+/g, "<u>$1</u>");
    text = text.replace(/==(.+?)==/g, "<mark>$1</mark>");
    text = text.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>");
    text = text.replace(/__(.+?)__/g, "<strong>$1</strong>");
    text = text.replace(/(?<!\*)\*([^*\n]+)\*(?!\*)/g, "<em>$1</em>");
    text = text.replace(/(?<!_)_([^_\n]+)_(?!_)/g, "<em>$1</em>");
    return text;
}

export function renderMarkdown(markdown) {
    const lines = String(markdown ?? "").replaceAll("\r\n", "\n").split("\n");
    const out = [];
    let listType = null;

    const closeList = () => {
        if (listType) out.push(`</${listType}>`);
        listType = null;
    };

    const openList = type => {
        if (listType === type) return;
        closeList();
        listType = type;
        out.push(`<${type}>`);
    };

    for (const rawLine of lines) {
        const line = rawLine.trimEnd();
        if (!line.trim()) {
            closeList();
            out.push("<p><br></p>");
            continue;
        }

        const checklist = line.match(/^\s*-\s*\[([ xX])\]\s+(.*)$/);
        if (checklist) {
            openList("ul");
            const checked = checklist[1].toLowerCase() === "x";
            out.push(`<li class="check-item">${checked ? "☑" : "☐"} ${renderInlineMarkdown(checklist[2])}</li>`);
            continue;
        }

        const unordered = line.match(/^\s*[-*+]\s+(.*)$/);
        if (unordered) {
            openList("ul");
            out.push(`<li>${renderInlineMarkdown(unordered[1])}</li>`);
            continue;
        }

        const ordered = line.match(/^\s*\d+[.)]\s+(.*)$/);
        if (ordered) {
            openList("ol");
            out.push(`<li>${renderInlineMarkdown(ordered[1])}</li>`);
            continue;
        }

        const custom = line.match(new RegExp(`^\\s*([${CUSTOM_MARKERS.join("")}])\\s+(.*)$`));
        if (custom) {
            closeList();
            out.push(`<p>${escapeHtml(custom[1])} ${renderInlineMarkdown(custom[2])}</p>`);
            continue;
        }

        closeList();
        const h3 = line.match(/^\s*###\s+(.*)$/);
        if (h3) { out.push(`<h3>${renderInlineMarkdown(h3[1])}</h3>`); continue; }
        const h2 = line.match(/^\s*##\s+(.*)$/);
        if (h2) { out.push(`<h2>${renderInlineMarkdown(h2[1])}</h2>`); continue; }
        const h1 = line.match(/^\s*#\s+(.*)$/);
        if (h1) { out.push(`<h1>${renderInlineMarkdown(h1[1])}</h1>`); continue; }
        const quote = line.match(/^\s*>\s?(.*)$/);
        if (quote) { out.push(`<blockquote>${renderInlineMarkdown(quote[1])}</blockquote>`); continue; }
        out.push(`<p>${renderInlineMarkdown(line)}</p>`);
    }

    closeList();
    return out.join("\n");
}

export function textStats(text) {
    const value = String(text ?? "");
    const words = value.trim() ? value.trim().split(/\s+/).length : 0;
    const lines = value ? value.split(/\r?\n/).length : 0;
    return { words, lines, chars: value.length };
}
