import assert from "node:assert/strict";
import { applyAllSafeRevision, applyRevisionIssue, reviewText, RevisionIssueType } from "../web/js/revision.js";

const sample = "Ela  chegou , cedo. muito muito bem.";
const result = reviewText(sample);
assert.equal(result.wordCount, 6);
assert.equal(result.sentenceCount, 2);
assert.equal(result.paragraphCount, 1);
assert.equal(result.autoFixableCount, 3);
assert.deepEqual(result.issues.map(issue => issue.type), [
    RevisionIssueType.DUPLICATE_SPACE,
    RevisionIssueType.SPACE_BEFORE_PUNCTUATION,
    RevisionIssueType.REPEATED_WORD
]);
assert.equal(applyAllSafeRevision(sample, result), "Ela chegou, cedo. muito bem.");

const repeated = result.issues.find(issue => issue.type === RevisionIssueType.REPEATED_WORD);
assert.equal(applyRevisionIssue(sample, repeated), "Ela  chegou , cedo. muito bem.");

const longSentence = Array.from({ length: 35 }, (_, i) => `palavra${i + 1}`).join(" ") + ".";
const longResult = reviewText(longSentence);
assert.equal(longResult.issues.length, 1);
assert.equal(longResult.issues[0].type, RevisionIssueType.LONG_SENTENCE);
assert.equal(longResult.issues[0].isAutoFixable, false);
assert.equal(longResult.autoFixableCount, 0);

assert.deepEqual(reviewText(""), {
    issues: [], wordCount: 0, sentenceCount: 0, paragraphCount: 0, autoFixableCount: 0
});

console.log("web_revision=ok issues=duplicate-space,punctuation,repeated-word,long-sentence");
