import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "..");
const web = resolve(root, "web");
const manifest = JSON.parse(await readFile(resolve(web, "manifest.webmanifest"), "utf8"));
const serviceWorker = await readFile(resolve(web, "sw.js"), "utf8");
const app = await readFile(resolve(web, "js/app.js"), "utf8");

assert.equal(manifest.start_url, "./");
assert.equal(manifest.scope, "./");
assert.equal(manifest.display, "standalone");
assert.match(serviceWorker, /__FREEFERBOOK_BUILD__/);
assert.match(app, /serviceWorker\.register/);

const expectedShellFiles = [
    "index.html", "styles.css", "manifest.webmanifest",
    "js/app.js", "js/db.js", "js/archive.js", "js/markdown.js",
    "js/formatting.js", "js/revision.js", "js/diff.js"
];

for (const relative of expectedShellFiles) {
    await access(resolve(web, relative));
    assert.ok(serviceWorker.includes(`./${relative}`), `${relative} não está no cache shell`);
}

assert.match(serviceWorker, /update\.json/);
assert.match(serviceWorker, /web-version\.json/);
assert.match(serviceWorker, /freeferbook-test\.apk/);

console.log(`web_pwa=ok shell=${expectedShellFiles.length} build-placeholder=present`);
