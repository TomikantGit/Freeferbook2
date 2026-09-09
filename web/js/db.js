const DB_NAME = "freeferbook-web";
const DB_VERSION = 1;
const PROJECT_STORE = "projects";

let dbPromise;

function openDatabase() {
    if (dbPromise) return dbPromise;
    dbPromise = new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        request.onupgradeneeded = () => {
            const db = request.result;
            if (!db.objectStoreNames.contains(PROJECT_STORE)) {
                db.createObjectStore(PROJECT_STORE, { keyPath: "id" });
            }
        };
        request.onsuccess = () => resolve(request.result);
        request.onerror = () => reject(request.error ?? new Error("Não foi possível abrir o armazenamento local."));
    });
    return dbPromise;
}

async function transaction(mode, callback) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const tx = db.transaction(PROJECT_STORE, mode);
        const store = tx.objectStore(PROJECT_STORE);
        let result;
        try {
            result = callback(store);
        } catch (error) {
            reject(error);
            return;
        }
        tx.oncomplete = () => resolve(result);
        tx.onerror = () => reject(tx.error ?? new Error("Falha ao acessar o armazenamento local."));
        tx.onabort = () => reject(tx.error ?? new Error("Operação de armazenamento cancelada."));
    });
}

export async function listProjects() {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const tx = db.transaction(PROJECT_STORE, "readonly");
        const request = tx.objectStore(PROJECT_STORE).getAll();
        request.onsuccess = () => {
            const projects = request.result ?? [];
            projects.sort((a, b) => (b.updatedAt ?? b.createdAt ?? 0) - (a.updatedAt ?? a.createdAt ?? 0));
            resolve(projects);
        };
        request.onerror = () => reject(request.error);
    });
}

export async function getProject(id) {
    const db = await openDatabase();
    return new Promise((resolve, reject) => {
        const tx = db.transaction(PROJECT_STORE, "readonly");
        const request = tx.objectStore(PROJECT_STORE).get(id);
        request.onsuccess = () => resolve(request.result ?? null);
        request.onerror = () => reject(request.error);
    });
}

export async function saveProject(project) {
    project.updatedAt = Date.now();
    await transaction("readwrite", store => store.put(project));
    return project;
}

export async function removeProject(id) {
    await transaction("readwrite", store => store.delete(id));
}

export function newProject(title) {
    const now = Date.now();
    return {
        id: crypto.randomUUID(),
        title: title.trim(),
        createdAt: now,
        updatedAt: now,
        chapters: [],
        characters: [],
        locations: [],
        images: [],
        media: []
    };
}

export function newChapter(bookId, title, orderIndex) {
    const now = Date.now();
    return {
        id: crypto.randomUUID(),
        bookId,
        title: title.trim(),
        orderIndex,
        createdAt: now,
        draftContent: "",
        versions: []
    };
}
