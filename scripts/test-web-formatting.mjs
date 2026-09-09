import assert from "node:assert/strict";
import {
    applyCustomMarker,
    applyMarkdownFormat,
    CUSTOM_MARKERS
} from "../web/js/formatting.js";

function state(text, start, end, format) {
    return applyMarkdownFormat(text, start, end, format);
}

{
    const result = state("texto", 0, 5, "bold");
    assert.deepEqual(result, { text: "**texto**", start: 2, end: 7 });
    assert.deepEqual(state(result.text, result.start, result.end, "bold"), { text: "texto", start: 0, end: 5 });
}

{
    const result = state("um\ndois", 0, 7, "numbered");
    assert.equal(result.text, "1. um\n2. dois");
    const converted = state(result.text, result.start, result.end, "checklist");
    assert.equal(converted.text, "- [ ] um\n- [ ] dois");
    const bulleted = state(converted.text, converted.start, converted.end, "bulleted");
    assert.equal(bulleted.text, "- um\n- dois");
}

{
    const result = state("linha", 0, 5, "heading");
    assert.equal(result.text, "### linha");
    assert.equal(state(result.text, result.start, result.end, "heading").text, "linha");
}

{
    const result = state("linha", 0, 5, "quote");
    assert.equal(result.text, "> linha");
}

{
    for (const marker of CUSTOM_MARKERS) {
        const result = applyCustomMarker("a\nb", 0, 3, marker);
        assert.equal(result.text, `${marker} a\n${marker} b`);
        const removed = applyCustomMarker(result.text, result.start, result.end, marker);
        assert.equal(removed.text, "a\nb");
    }
}

{
    const invalid = applyCustomMarker("texto", 0, 5, "@");
    assert.equal(invalid.text, "texto");
}

{
    assert.equal(state("texto", 2, 2, "italic").text, "texto");
    const underline = state("texto", 0, 5, "underline");
    assert.equal(underline.text, "++texto++");
    const highlight = state("texto", 0, 5, "highlight");
    assert.equal(highlight.text, "==texto==");
    const strike = state("texto", 0, 5, "strike");
    assert.equal(strike.text, "~~texto~~");
}

console.log(`web_formatting=ok markers=${CUSTOM_MARKERS.join("")}`);
