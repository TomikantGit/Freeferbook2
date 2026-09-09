import assert from "node:assert/strict";
import { compareText, diffSummary, DiffLineType } from "../web/js/diff.js";

const lines = compareText("A casa azul\nlinha igual", "A casa verde\nlinha igual\nnova linha");
assert.deepEqual(lines.map(line => line.type), [
    DiffLineType.REMOVED,
    DiffLineType.ADDED,
    DiffLineType.UNCHANGED,
    DiffLineType.ADDED
]);
assert.ok(lines[0].spans.some(span => span.type === DiffLineType.REMOVED && span.text.includes("azul")));
assert.ok(lines[1].spans.some(span => span.type === DiffLineType.ADDED && span.text.includes("verde")));
assert.equal(lines[3].spans.map(span => span.text).join(""), "nova linha");
assert.deepEqual(diffSummary(lines), { unchanged: 1, added: 2, removed: 1 });

assert.deepEqual(compareText("", "nova").map(line => line.type), [DiffLineType.ADDED]);
assert.deepEqual(compareText("antiga", "").map(line => line.type), [DiffLineType.REMOVED]);
assert.deepEqual(compareText("igual", "igual").map(line => line.type), [DiffLineType.UNCHANGED]);

console.log("web_diff=ok line-and-inline");
