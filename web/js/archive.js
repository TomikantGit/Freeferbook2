const FORMAT_ID = "freeferbook-book-backup";
const SCHEMA_VERSION = 1;
const MAX_ARCHIVE_BYTES = 1024 * 1024 * 1024;
const MAX_MANIFEST_BYTES = 20 * 1024 * 1024;
const MAX_MEDIA_BYTES = 100 * 1024 * 1024;

const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder("utf-8");

function randomId() {
    return crypto.randomUUID();
}

function sanitizeFileName(title) {
    const safe = String(title ?? "")
        .trim()
        .replace(/[^A-Za-z0-9À-ÿ_-]+/g, "_")
        .replace(/^_+|_+$/g, "")
        .slice(0, 80) || "livro";
    return `${safe}-freeferbook.zip`;
}

function extensionMime(name) {
    const ext = name.split(".").pop()?.toLowerCase();
    return ({
        png: "image/png", jpg: "image/jpeg", jpeg: "image/jpeg", webp: "image/webp",
        gif: "image/gif", svg: "image/svg+xml", bmp: "image/bmp"
    })[ext] ?? "application/octet-stream";
}

function safeMediaEntry(name) {
    return typeof name === "string" && name.startsWith("media/") && !name.includes("..") && !name.includes("\\");
}

function crc32(bytes) {
    let crc = 0xffffffff;
    for (const byte of bytes) {
        crc ^= byte;
        for (let i = 0; i < 8; i++) crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
    return (crc ^ 0xffffffff) >>> 0;
}

function concatBytes(parts) {
    const size = parts.reduce((sum, p) => sum + p.length, 0);
    const out = new Uint8Array(size);
    let offset = 0;
    for (const part of parts) {
        out.set(part, offset);
        offset += part.length;
    }
    return out;
}

function writeU16(view, offset, value) { view.setUint16(offset, value, true); }
function writeU32(view, offset, value) { view.setUint32(offset, value >>> 0, true); }

async function createStoredZip(entries) {
    const localParts = [];
    const centralParts = [];
    let localOffset = 0;

    for (const entry of entries) {
        const nameBytes = textEncoder.encode(entry.name);
        const data = entry.data instanceof Uint8Array ? entry.data : new Uint8Array(entry.data);
        const crc = crc32(data);

        const local = new Uint8Array(30 + nameBytes.length);
        const lv = new DataView(local.buffer);
        writeU32(lv, 0, 0x04034b50);
        writeU16(lv, 4, 20);
        writeU16(lv, 6, 0x0800);
        writeU16(lv, 8, 0);
        writeU32(lv, 14, crc);
        writeU32(lv, 18, data.length);
        writeU32(lv, 22, data.length);
        writeU16(lv, 26, nameBytes.length);
        local.set(nameBytes, 30);
        localParts.push(local, data);

        const central = new Uint8Array(46 + nameBytes.length);
        const cv = new DataView(central.buffer);
        writeU32(cv, 0, 0x02014b50);
        writeU16(cv, 4, 20);
        writeU16(cv, 6, 20);
        writeU16(cv, 8, 0x0800);
        writeU16(cv, 10, 0);
        writeU32(cv, 16, crc);
        writeU32(cv, 20, data.length);
        writeU32(cv, 24, data.length);
        writeU16(cv, 28, nameBytes.length);
        writeU32(cv, 42, localOffset);
        central.set(nameBytes, 46);
        centralParts.push(central);

        localOffset += local.length + data.length;
    }

    const centralOffset = localOffset;
    const centralSize = centralParts.reduce((sum, p) => sum + p.length, 0);
    const end = new Uint8Array(22);
    const ev = new DataView(end.buffer);
    writeU32(ev, 0, 0x06054b50);
    writeU16(ev, 8, entries.length);
    writeU16(ev, 10, entries.length);
    writeU32(ev, 12, centralSize);
    writeU32(ev, 16, centralOffset);
    return concatBytes([...localParts, ...centralParts, end]);
}

function findEndOfCentral(bytes) {
    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const min = Math.max(0, bytes.length - 65557);
    for (let i = bytes.length - 22; i >= min; i--) {
        if (view.getUint32(i, true) === 0x06054b50) return i;
    }
    throw new Error("ZIP inválido: diretório central não encontrado.");
}

function parseZipDirectory(bytes) {
    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const eocd = findEndOfCentral(bytes);
    const count = view.getUint16(eocd + 10, true);
    let offset = view.getUint32(eocd + 16, true);
    const entries = new Map();

    for (let i = 0; i < count; i++) {
        if (view.getUint32(offset, true) !== 0x02014b50) throw new Error("ZIP inválido: entrada central corrompida.");
        const flags = view.getUint16(offset + 8, true);
        const method = view.getUint16(offset + 10, true);
        const compressedSize = view.getUint32(offset + 20, true);
        const uncompressedSize = view.getUint32(offset + 24, true);
        const nameLength = view.getUint16(offset + 28, true);
        const extraLength = view.getUint16(offset + 30, true);
        const commentLength = view.getUint16(offset + 32, true);
        const localOffset = view.getUint32(offset + 42, true);
        const nameBytes = bytes.slice(offset + 46, offset + 46 + nameLength);
        const name = new TextDecoder((flags & 0x0800) ? "utf-8" : "utf-8").decode(nameBytes);
        entries.set(name, { name, method, compressedSize, uncompressedSize, localOffset });
        offset += 46 + nameLength + extraLength + commentLength;
    }
    return entries;
}

async function inflateRaw(bytes) {
    if (!("DecompressionStream" in window)) {
        throw new Error("Este navegador não suporta a descompressão necessária para abrir backups Android. Atualize o navegador.");
    }
    let stream;
    try {
        stream = new DecompressionStream("deflate-raw");
    } catch (_) {
        stream = new DecompressionStream("deflate");
    }
    const response = new Response(new Blob([bytes]).stream().pipeThrough(stream));
    return new Uint8Array(await response.arrayBuffer());
}

async function readZipEntry(bytes, entry, maxBytes) {
    if (!entry) throw new Error("Entrada ZIP ausente.");
    if (entry.uncompressedSize > maxBytes) throw new Error("Entrada do backup excede o limite permitido.");
    const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
    const offset = entry.localOffset;
    if (view.getUint32(offset, true) !== 0x04034b50) throw new Error("ZIP inválido: entrada local corrompida.");
    const nameLength = view.getUint16(offset + 26, true);
    const extraLength = view.getUint16(offset + 28, true);
    const start = offset + 30 + nameLength + extraLength;
    const compressed = bytes.slice(start, start + entry.compressedSize);
    let data;
    if (entry.method === 0) data = compressed;
    else if (entry.method === 8) data = await inflateRaw(compressed);
    else throw new Error(`Método ZIP ${entry.method} não suportado.`);
    if (data.length > maxBytes) throw new Error("Entrada descompactada excede o limite permitido.");
    return data;
}

function archiveSignatureMessage(bytes) {
    const has = values => values.every((v, i) => bytes[i] === v);
    if (bytes.length >= 8 && (has([0x52,0x61,0x72,0x21,0x1a,0x07,0x00]) || has([0x52,0x61,0x72,0x21,0x1a,0x07,0x01,0x00]))) {
        return "RAR ainda não é suportado. Use o backup ZIP gerado pelo Freeferbook.";
    }
    if (bytes.length >= 6 && has([0x37,0x7a,0xbc,0xaf,0x27,0x1c])) {
        return "7z ainda não é suportado. Use o backup ZIP gerado pelo Freeferbook.";
    }
    return null;
}

function normalizeVersions(chapter) {
    const versions = Array.isArray(chapter.versions) ? chapter.versions : [];
    versions.sort((a, b) => (a.sequenceNumber ?? 0) - (b.sequenceNumber ?? 0));
    return versions;
}

export async function importFreeferbookArchive(file) {
    if (!file) throw new Error("Nenhum arquivo selecionado.");
    if (file.size > MAX_ARCHIVE_BYTES) throw new Error("O backup excede o limite de 1 GB.");
    const bytes = new Uint8Array(await file.arrayBuffer());
    const signatureError = archiveSignatureMessage(bytes);
    if (signatureError) throw new Error(signatureError);
    const directory = parseZipDirectory(bytes);
    const manifestEntry = directory.get("manifest.json");
    if (!manifestEntry) throw new Error("Este ZIP não contém um backup Freeferbook reconhecível.");
    const manifestBytes = await readZipEntry(bytes, manifestEntry, MAX_MANIFEST_BYTES);
    let manifest;
    try { manifest = JSON.parse(textDecoder.decode(manifestBytes)); }
    catch (_) { throw new Error("O manifest.json do backup está corrompido."); }

    if (manifest.format !== FORMAT_ID) throw new Error("Este arquivo não é um backup de livro do Freeferbook.");
    const schema = Number(manifest.schemaVersion ?? 0);
    if (!schema) throw new Error("O backup não informa uma versão de formato válida.");
    if (schema > SCHEMA_VERSION) throw new Error("Este backup foi criado por uma versão mais nova do Freeferbook Web.");
    if (!manifest.book?.title) throw new Error("O backup não contém os dados do livro.");

    const mediaNames = new Set();
    for (const item of manifest.characters ?? []) if (safeMediaEntry(item.imageEntry)) mediaNames.add(item.imageEntry);
    for (const item of manifest.locations ?? []) if (safeMediaEntry(item.imageEntry)) mediaNames.add(item.imageEntry);
    for (const item of manifest.images ?? []) if (safeMediaEntry(item.mediaEntry)) mediaNames.add(item.mediaEntry);

    const media = [];
    const mediaIdByEntry = new Map();
    for (const entryName of mediaNames) {
        const entry = directory.get(entryName);
        if (!entry) continue;
        const data = await readZipEntry(bytes, entry, MAX_MEDIA_BYTES);
        const id = randomId();
        mediaIdByEntry.set(entryName, id);
        media.push({ id, entryName, blob: new Blob([data], { type: extensionMime(entryName) }) });
    }

    const chapters = (manifest.chapters ?? []).map((chapter, index) => {
        const versions = normalizeVersions({
            versions: (chapter.versions ?? []).map((version, versionIndex) => {
                const content = String(version.content ?? "");
                const words = content.trim() ? content.trim().split(/\s+/).length : 0;
                return {
                    id: randomId(),
                    content,
                    createdAt: Number(version.createdAt ?? Date.now()),
                    message: version.message ?? null,
                    sequenceNumber: Number(version.sequenceNumber ?? versionIndex + 1),
                    wordCount: Number(version.wordCount ?? words),
                    charCount: Number(version.charCount ?? content.length),
                    lineCount: Number(version.lineCount ?? (content ? content.split(/\r?\n/).length : 0))
                };
            })
        });
        return {
            id: randomId(),
            title: String(chapter.title ?? `Capítulo ${index + 1}`),
            orderIndex: Number(chapter.orderIndex ?? index),
            createdAt: Number(chapter.createdAt ?? Date.now()),
            draftContent: versions.at(-1)?.content ?? "",
            versions
        };
    });

    const projectId = randomId();
    for (const chapter of chapters) chapter.bookId = projectId;
    return {
        id: projectId,
        title: String(manifest.book.title).trim() || "Livro importado",
        createdAt: Number(manifest.book.createdAt ?? Date.now()),
        updatedAt: Date.now(),
        chapters,
        characters: (manifest.characters ?? []).map(item => ({
            id: randomId(), name: String(item.name ?? "Personagem"), surnames: String(item.surnames ?? ""),
            chapters: String(item.chapters ?? ""), imageUri: item.imageUri ?? null,
            mediaId: mediaIdByEntry.get(item.imageEntry) ?? null
        })),
        locations: (manifest.locations ?? []).map(item => ({
            id: randomId(), name: String(item.name ?? "Local"), description: String(item.description ?? ""),
            chapters: String(item.chapters ?? ""), imageUri: item.imageUri ?? null,
            mediaId: mediaIdByEntry.get(item.imageEntry) ?? null
        })),
        images: (manifest.images ?? []).map(item => ({
            id: randomId(), url: String(item.url ?? ""), description: String(item.description ?? ""),
            createdAt: Number(item.createdAt ?? Date.now()), mediaId: mediaIdByEntry.get(item.mediaEntry) ?? null
        })),
        media
    };
}

function nextMediaEntry(media, usedEntries) {
    const original = safeMediaEntry(media.entryName) ? media.entryName : null;
    if (original && !usedEntries.has(original)) {
        usedEntries.add(original);
        return original;
    }
    const ext = media.blob?.type?.split("/")[1]?.replace("jpeg", "jpg")?.replace(/[^a-z0-9]/g, "") || "bin";
    let index = usedEntries.size;
    let name;
    do { name = `media/${String(index++).padStart(4, "0")}.${ext}`; } while (usedEntries.has(name));
    usedEntries.add(name);
    return name;
}

export async function exportFreeferbookArchive(project) {
    const mediaById = new Map((project.media ?? []).map(item => [item.id, item]));
    const usedEntries = new Set();
    const entryByMediaId = new Map();
    const zipEntries = [];

    for (const media of project.media ?? []) {
        if (!media?.id || !(media.blob instanceof Blob) || media.blob.size > MAX_MEDIA_BYTES) continue;
        const entryName = nextMediaEntry(media, usedEntries);
        entryByMediaId.set(media.id, entryName);
        zipEntries.push({ name: entryName, data: new Uint8Array(await media.blob.arrayBuffer()) });
    }

    const manifest = {
        format: FORMAT_ID,
        schemaVersion: SCHEMA_VERSION,
        exportedAt: Date.now(),
        book: { title: project.title, createdAt: project.createdAt },
        chapters: [...(project.chapters ?? [])]
            .sort((a, b) => a.orderIndex - b.orderIndex)
            .map((chapter, chapterIndex) => ({
                title: chapter.title || `Capítulo ${chapterIndex + 1}`,
                orderIndex: chapter.orderIndex ?? chapterIndex,
                createdAt: chapter.createdAt ?? Date.now(),
                versions: normalizeVersions(chapter).map(version => ({
                    content: String(version.content ?? ""),
                    createdAt: version.createdAt ?? Date.now(),
                    message: version.message ?? null,
                    sequenceNumber: version.sequenceNumber,
                    wordCount: version.wordCount,
                    charCount: version.charCount,
                    lineCount: version.lineCount
                }))
            })),
        characters: (project.characters ?? []).map(item => ({
            name: item.name ?? "Personagem", surnames: item.surnames ?? "", chapters: item.chapters ?? "",
            imageUri: item.imageUri ?? null, imageEntry: entryByMediaId.get(item.mediaId) ?? null
        })),
        locations: (project.locations ?? []).map(item => ({
            name: item.name ?? "Local", description: item.description ?? "", chapters: item.chapters ?? "",
            imageUri: item.imageUri ?? null, imageEntry: entryByMediaId.get(item.mediaId) ?? null
        })),
        images: (project.images ?? []).map(item => ({
            url: item.url ?? "", description: item.description ?? "", createdAt: item.createdAt ?? Date.now(),
            mediaEntry: entryByMediaId.get(item.mediaId) ?? null
        }))
    };

    zipEntries.unshift({ name: "manifest.json", data: textEncoder.encode(JSON.stringify(manifest, null, 2)) });
    const zipBytes = await createStoredZip(zipEntries);
    return { blob: new Blob([zipBytes], { type: "application/zip" }), fileName: sanitizeFileName(project.title) };
}

export function downloadBlob(blob, fileName) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1500);
}
