import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { fileURLToPath, pathToFileURL } from "node:url";
import { dirname, resolve } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const root = resolve(here, "..");
const fixturePath = resolve(root, "contracts/book-backup-v1.sample.json");
const androidPath = resolve(root, "app/src/main/java/com/livrohub/data/archive/BookArchiveManager.kt");
const webArchivePath = resolve(root, "web/js/archive.js");
const desktopArchivePath = resolve(root, "desktopApp/src/main/kotlin/com/livrohub/desktop/DesktopArchiveManager.kt");

const fixture = JSON.parse(await readFile(fixturePath, "utf8"));
const androidSource = await readFile(androidPath, "utf8");
const webSource = await readFile(webArchivePath, "utf8");
const desktopSource = await readFile(desktopArchivePath, "utf8");

assert.equal(fixture.format, "freeferbook-book-backup");
assert.equal(fixture.schemaVersion, 1);
assert.ok(fixture.book?.title);
assert.ok(Array.isArray(fixture.chapters));
assert.ok(Array.isArray(fixture.characters));
assert.ok(Array.isArray(fixture.locations));
assert.ok(Array.isArray(fixture.images));

const androidFormat = androidSource.match(/FORMAT_ID\s*=\s*"([^"]+)"/)?.[1];
const androidSchema = Number(androidSource.match(/SCHEMA_VERSION\s*=\s*(\d+)/)?.[1]);
const webFormat = webSource.match(/FORMAT_ID\s*=\s*"([^"]+)"/)?.[1];
const webSchema = Number(webSource.match(/SCHEMA_VERSION\s*=\s*(\d+)/)?.[1]);
const desktopFormat = desktopSource.match(/FORMAT_ID\s*=\s*"([^"]+)"/)?.[1];
const desktopSchema = Number(desktopSource.match(/SCHEMA_VERSION\s*=\s*(\d+)/)?.[1]);

assert.equal(androidFormat, fixture.format, "Android e fixture usam FORMAT_ID diferentes");
assert.equal(webFormat, fixture.format, "Web e fixture usam FORMAT_ID diferentes");
assert.equal(desktopFormat, fixture.format, "Desktop e fixture usam FORMAT_ID diferentes");
assert.equal(androidSchema, fixture.schemaVersion, "Android e fixture usam schemaVersion diferentes");
assert.equal(webSchema, fixture.schemaVersion, "Web e fixture usam schemaVersion diferentes");
assert.equal(desktopSchema, fixture.schemaVersion, "Desktop e fixture usam schemaVersion diferentes");

globalThis.window ??= globalThis;
const { exportFreeferbookArchive, importFreeferbookArchive } = await import(pathToFileURL(webArchivePath));

const project = {
    id: crypto.randomUUID(),
    title: fixture.book.title,
    createdAt: fixture.book.createdAt,
    updatedAt: fixture.exportedAt,
    chapters: fixture.chapters.map((chapter, index) => ({
        id: crypto.randomUUID(),
        title: chapter.title,
        orderIndex: chapter.orderIndex ?? index,
        createdAt: chapter.createdAt,
        draftContent: chapter.versions.at(-1)?.content ?? "",
        versions: chapter.versions.map(version => ({ id: crypto.randomUUID(), ...version }))
    })),
    characters: fixture.characters.map(item => ({ id: crypto.randomUUID(), ...item, mediaId: null })),
    locations: fixture.locations.map(item => ({ id: crypto.randomUUID(), ...item, mediaId: null })),
    images: fixture.images.map(item => ({ id: crypto.randomUUID(), ...item, mediaId: null })),
    media: []
};

const embeddedMediaId = crypto.randomUUID();
const embeddedBytes = new Uint8Array([137, 80, 78, 71, 13, 10, 26, 10]);
project.media.push({
    id: embeddedMediaId,
    entryName: "media/contract-sample.png",
    blob: new Blob([embeddedBytes], { type: "image/png" })
});
project.images.push({
    id: crypto.randomUUID(),
    url: "contract-sample.png",
    description: "Imagem incorporada do contrato",
    createdAt: fixture.exportedAt,
    mediaId: embeddedMediaId
});

const { blob, fileName } = await exportFreeferbookArchive(project);
const file = new File([blob], fileName, { type: "application/zip" });
const imported = await importFreeferbookArchive(file);

assert.equal(imported.title, fixture.book.title);
assert.equal(imported.chapters.length, fixture.chapters.length);
assert.equal(imported.chapters[0].versions[0].content, fixture.chapters[0].versions[0].content);
assert.equal(imported.characters[0].name, fixture.characters[0].name);
assert.equal(imported.locations[0].name, fixture.locations[0].name);
const embeddedImage = imported.images.find(image => image.description === "Imagem incorporada do contrato");
assert.ok(embeddedImage?.mediaId, "Imagem incorporada perdeu a associação com mediaId");
const embeddedMedia = imported.media.find(media => media.id === embeddedImage.mediaId);
assert.equal(embeddedMedia?.blob?.size, embeddedBytes.length, "Blob incorporado mudou de tamanho no round-trip");

console.log(`backup_contract=ok format=${fixture.format} schema=${fixture.schemaVersion} roundtrip=${fileName} media=${embeddedBytes.length}B`);
