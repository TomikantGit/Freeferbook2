import { listProjects, saveProject, removeProject, newProject, newChapter } from "./db.js";
import { renderMarkdown, textStats } from "./markdown.js";
import { importFreeferbookArchive, exportFreeferbookArchive, downloadBlob } from "./archive.js";

const $ = selector => document.querySelector(selector);
const elements = {
    sidebar: $("#sidebar"),
    bookList: $("#bookList"),
    chapterList: $("#chapterList"),
    welcomeView: $("#welcomeView"),
    workspaceView: $("#workspaceView"),
    topbarTitle: $("#topbarTitle"),
    topbarSubtitle: $("#topbarSubtitle"),
    topbarActions: $("#topbarActions"),
    bookTitleInWorkspace: $("#bookTitleInWorkspace"),
    noChapterView: $("#noChapterView"),
    chapterEditorView: $("#chapterEditorView"),
    editor: $("#editor"),
    editorStats: $("#editorStats"),
    draftStatus: $("#draftStatus"),
    previewContent: $("#previewContent"),
    historyList: $("#historyList"),
    editPane: $("#editPane"),
    previewPane: $("#previewPane"),
    historyPane: $("#historyPane"),
    saveVersionButton: $("#saveVersionButton"),
    chapterMenu: $("#chapterMenu"),
    importInput: $("#importBookInput"),
    toast: $("#toast"),
    textDialog: $("#textDialog"),
    textDialogTitle: $("#textDialogTitle"),
    textDialogLabel: $("#textDialogLabel"),
    textDialogInput: $("#textDialogInput"),
    textDialogHint: $("#textDialogHint"),
    confirmDialog: $("#confirmDialog"),
    confirmDialogTitle: $("#confirmDialogTitle"),
    confirmDialogText: $("#confirmDialogText")
};

let projects = [];
let currentProject = null;
let selectedChapterId = null;
let viewMode = "edit";
let persistTimer = null;
let toastTimer = null;

function currentChapter() {
    return currentProject?.chapters?.find(chapter => chapter.id === selectedChapterId) ?? null;
}

function formatDate(timestamp) {
    try {
        return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(new Date(timestamp));
    } catch (_) {
        return "—";
    }
}

function countProjectWords(project) {
    return (project.chapters ?? []).reduce((total, chapter) => total + textStats(chapter.draftContent ?? "").words, 0);
}

function showToast(message, duration = 3200) {
    clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.classList.remove("hidden");
    toastTimer = setTimeout(() => elements.toast.classList.add("hidden"), duration);
}

function schedulePersist() {
    clearTimeout(persistTimer);
    persistTimer = setTimeout(async () => {
        if (!currentProject) return;
        await saveProject(currentProject);
        const index = projects.findIndex(project => project.id === currentProject.id);
        if (index >= 0) projects[index] = currentProject;
        renderBookList();
    }, 450);
}

async function persistNow() {
    clearTimeout(persistTimer);
    if (!currentProject) return;
    await saveProject(currentProject);
    const index = projects.findIndex(project => project.id === currentProject.id);
    if (index >= 0) projects[index] = currentProject;
}

function latestVersion(chapter) {
    if (!chapter?.versions?.length) return null;
    return [...chapter.versions].sort((a, b) => a.sequenceNumber - b.sequenceNumber).at(-1);
}

function hasUnsavedChanges(chapter) {
    return (chapter?.draftContent ?? "") !== (latestVersion(chapter)?.content ?? "");
}

function renderBookList() {
    elements.bookList.replaceChildren();
    if (!projects.length) {
        const empty = document.createElement("div");
        empty.className = "muted small";
        empty.textContent = "Nenhum livro neste navegador.";
        elements.bookList.append(empty);
        return;
    }

    for (const project of projects) {
        const button = document.createElement("button");
        button.className = `book-item${currentProject?.id === project.id ? " active" : ""}`;
        button.innerHTML = `
            <span class="book-item-title"></span>
            <span class="book-item-meta"></span>
        `;
        button.querySelector(".book-item-title").textContent = project.title;
        button.querySelector(".book-item-meta").textContent = `${project.chapters?.length ?? 0} capítulos • ${countProjectWords(project)} palavras`;
        button.addEventListener("click", () => selectProject(project.id));
        elements.bookList.append(button);
    }
}

function renderChapterList() {
    elements.chapterList.replaceChildren();
    if (!currentProject) return;
    const chapters = [...(currentProject.chapters ?? [])].sort((a, b) => a.orderIndex - b.orderIndex);
    if (!chapters.length) {
        const empty = document.createElement("div");
        empty.className = "muted small";
        empty.textContent = "Nenhum capítulo.";
        elements.chapterList.append(empty);
        return;
    }

    chapters.forEach((chapter, index) => {
        const button = document.createElement("button");
        button.className = `chapter-item${selectedChapterId === chapter.id ? " active" : ""}`;
        button.innerHTML = `<span class="chapter-item-title"></span><span class="chapter-item-meta"></span>`;
        button.querySelector(".chapter-item-title").textContent = chapter.title;
        const version = latestVersion(chapter);
        button.querySelector(".chapter-item-meta").textContent = `${index + 1} • ${version ? `versão #${version.sequenceNumber}` : "sem versão"}${hasUnsavedChanges(chapter) ? " • rascunho" : ""}`;
        button.addEventListener("click", () => selectChapter(chapter.id));
        elements.chapterList.append(button);
    });
}

function renderTopbar() {
    elements.topbarActions.replaceChildren();
    if (!currentProject) {
        elements.topbarTitle.textContent = "Freeferbook Web";
        elements.topbarSubtitle.textContent = "Seus textos ficam salvos neste navegador.";
        return;
    }

    elements.topbarTitle.textContent = currentProject.title;
    elements.topbarSubtitle.textContent = `${currentProject.chapters?.length ?? 0} capítulos • atualizado ${formatDate(currentProject.updatedAt ?? currentProject.createdAt)}`;

    const rename = document.createElement("button");
    rename.className = "secondary-button compact";
    rename.textContent = "Renomear";
    rename.addEventListener("click", renameCurrentBook);

    const exportButton = document.createElement("button");
    exportButton.className = "primary-button compact";
    exportButton.textContent = "Exportar backup";
    exportButton.addEventListener("click", exportCurrentBook);

    const remove = document.createElement("button");
    remove.className = "icon-button";
    remove.textContent = "⋮";
    remove.title = "Mais opções";
    remove.addEventListener("click", async () => {
        if (await confirmAction("Excluir livro", `Excluir “${currentProject.title}” deste navegador? Exporte um backup antes se quiser preservar o conteúdo.`)) {
            const id = currentProject.id;
            await removeProject(id);
            projects = projects.filter(project => project.id !== id);
            currentProject = null;
            selectedChapterId = null;
            renderAll();
            showToast("Livro excluído deste navegador.");
        }
    });

    elements.topbarActions.append(rename, exportButton, remove);
}

function renderWorkspace() {
    const hasProject = Boolean(currentProject);
    elements.welcomeView.classList.toggle("hidden", hasProject);
    elements.workspaceView.classList.toggle("hidden", !hasProject);
    if (!hasProject) return;

    elements.bookTitleInWorkspace.textContent = currentProject.title;
    renderChapterList();
    const chapter = currentChapter();
    elements.noChapterView.classList.toggle("hidden", Boolean(chapter));
    elements.chapterEditorView.classList.toggle("hidden", !chapter);
    if (!chapter) return;

    if (elements.editor.value !== chapter.draftContent) elements.editor.value = chapter.draftContent ?? "";
    updateEditorMeta();
    switchViewMode(viewMode, false);
}

function renderAll() {
    renderBookList();
    renderTopbar();
    renderWorkspace();
}

async function selectProject(id) {
    await persistNow();
    currentProject = projects.find(project => project.id === id) ?? null;
    selectedChapterId = currentProject?.chapters?.slice().sort((a, b) => a.orderIndex - b.orderIndex)[0]?.id ?? null;
    viewMode = "edit";
    elements.sidebar.classList.remove("open");
    renderAll();
}

function selectChapter(id) {
    selectedChapterId = id;
    viewMode = "edit";
    renderWorkspace();
}

function updateEditorMeta() {
    const chapter = currentChapter();
    if (!chapter) return;
    const stats = textStats(chapter.draftContent ?? "");
    elements.editorStats.textContent = `${stats.words} palavras • ${stats.lines} linhas • ${stats.chars} caracteres`;
    const dirty = hasUnsavedChanges(chapter);
    elements.draftStatus.textContent = dirty ? "Alterações não salvas" : "Versão salva";
    elements.draftStatus.style.color = dirty ? "var(--primary)" : "var(--muted)";
}

function renderPreview() {
    const chapter = currentChapter();
    elements.previewContent.innerHTML = renderMarkdown(chapter?.draftContent ?? "");
}

function renderHistory() {
    const chapter = currentChapter();
    elements.historyList.replaceChildren();
    if (!chapter?.versions?.length) {
        const empty = document.createElement("div");
        empty.className = "muted";
        empty.textContent = "Nenhuma versão salva ainda.";
        elements.historyList.append(empty);
        return;
    }

    const versions = [...chapter.versions].sort((a, b) => b.sequenceNumber - a.sequenceNumber);
    for (const version of versions) {
        const item = document.createElement("div");
        item.className = "history-item";
        const main = document.createElement("div");
        main.innerHTML = `<div class="history-title"></div><div class="muted small"></div><div class="history-snippet"></div>`;
        main.querySelector(".history-title").textContent = `Versão #${version.sequenceNumber}${version.message ? ` — ${version.message}` : ""}`;
        main.querySelector(".muted").textContent = `${formatDate(version.createdAt)} • ${version.wordCount} palavras`;
        main.querySelector(".history-snippet").textContent = version.content || "(versão vazia)";

        const restore = document.createElement("button");
        restore.className = "secondary-button compact";
        restore.textContent = "Restaurar";
        restore.addEventListener("click", () => restoreVersion(version));
        item.append(main, restore);
        elements.historyList.append(item);
    }
}

function switchViewMode(mode, rerender = true) {
    viewMode = mode;
    document.querySelectorAll("[data-view-mode]").forEach(button => button.classList.toggle("active", button.dataset.viewMode === mode));
    elements.editPane.classList.toggle("hidden", mode !== "edit");
    elements.previewPane.classList.toggle("hidden", mode !== "preview");
    elements.historyPane.classList.toggle("hidden", mode !== "history");
    if (mode === "preview") renderPreview();
    if (mode === "history") renderHistory();
    if (rerender) renderChapterList();
}

async function promptText({ title, label, initial = "", hint = "" }) {
    elements.textDialogTitle.textContent = title;
    elements.textDialogLabel.textContent = label;
    elements.textDialogInput.value = initial;
    elements.textDialogHint.textContent = hint;
    elements.textDialog.showModal();
    elements.textDialogInput.focus();
    elements.textDialogInput.select();
    return new Promise(resolve => {
        elements.textDialog.addEventListener("close", () => {
            if (elements.textDialog.returnValue !== "confirm") return resolve(null);
            resolve(elements.textDialogInput.value);
        }, { once: true });
    });
}

async function confirmAction(title, text) {
    elements.confirmDialogTitle.textContent = title;
    elements.confirmDialogText.textContent = text;
    elements.confirmDialog.showModal();
    return new Promise(resolve => {
        elements.confirmDialog.addEventListener("close", () => resolve(elements.confirmDialog.returnValue === "confirm"), { once: true });
    });
}

async function createBook() {
    const title = await promptText({ title: "Novo livro", label: "Título", hint: "O livro será salvo somente neste navegador até você exportar um backup." });
    if (!title?.trim()) return;
    const project = newProject(title);
    await saveProject(project);
    projects.unshift(project);
    currentProject = project;
    selectedChapterId = null;
    renderAll();
    await createChapter();
}

async function renameCurrentBook() {
    if (!currentProject) return;
    const title = await promptText({ title: "Renomear livro", label: "Título", initial: currentProject.title });
    if (!title?.trim()) return;
    currentProject.title = title.trim();
    await persistNow();
    renderAll();
}

async function createChapter() {
    if (!currentProject) return;
    const title = await promptText({ title: "Novo capítulo", label: "Título", initial: `Capítulo ${(currentProject.chapters?.length ?? 0) + 1}` });
    if (!title?.trim()) return;
    const orderIndex = currentProject.chapters?.length ?? 0;
    const chapter = newChapter(currentProject.id, title, orderIndex);
    currentProject.chapters.push(chapter);
    selectedChapterId = chapter.id;
    viewMode = "edit";
    await persistNow();
    renderAll();
    elements.editor.focus();
}

async function renameCurrentChapter() {
    const chapter = currentChapter();
    if (!chapter) return;
    const title = await promptText({ title: "Renomear capítulo", label: "Título", initial: chapter.title });
    if (!title?.trim()) return;
    chapter.title = title.trim();
    await persistNow();
    renderAll();
}

async function deleteCurrentChapter() {
    const chapter = currentChapter();
    if (!chapter) return;
    if (!await confirmAction("Excluir capítulo", `Excluir “${chapter.title}” e todo o histórico dele?`)) return;
    currentProject.chapters = currentProject.chapters.filter(item => item.id !== chapter.id);
    currentProject.chapters.sort((a, b) => a.orderIndex - b.orderIndex).forEach((item, index) => item.orderIndex = index);
    selectedChapterId = currentProject.chapters[0]?.id ?? null;
    await persistNow();
    renderAll();
    showToast("Capítulo excluído.");
}

async function saveCurrentVersion(messageOverride = undefined) {
    const chapter = currentChapter();
    if (!chapter) return;
    let message = messageOverride;
    if (message === undefined) {
        const input = await promptText({
            title: "Salvar versão",
            label: "Mensagem opcional",
            initial: "",
            hint: "O histórico é imutável: cada salvamento cria uma nova versão."
        });
        if (input === null) return;
        message = input.trim() || null;
    }

    const nextSequence = appendVersion(chapter, message);
    await persistNow();
    updateEditorMeta();
    renderChapterList();
    if (viewMode === "history") renderHistory();
    showToast(`Versão #${nextSequence} salva.`);
}

function appendVersion(chapter, message = null) {
    const stats = textStats(chapter.draftContent ?? "");
    const nextSequence = Math.max(0, ...(chapter.versions ?? []).map(v => Number(v.sequenceNumber) || 0)) + 1;
    chapter.versions.push({
        id: crypto.randomUUID(),
        content: chapter.draftContent ?? "",
        createdAt: Date.now(),
        message: message || null,
        sequenceNumber: nextSequence,
        wordCount: stats.words,
        charCount: stats.chars,
        lineCount: stats.lines
    });
    return nextSequence;
}

async function restoreVersion(version) {
    const chapter = currentChapter();
    if (!chapter) return;
    if (!await confirmAction("Restaurar versão", `Restaurar a versão #${version.sequenceNumber}? Uma nova versão será criada e o histórico anterior será preservado.`)) return;
    chapter.draftContent = version.content;
    elements.editor.value = version.content;
    await saveCurrentVersion(`Restaurada da versão #${version.sequenceNumber}`);
    switchViewMode("edit");
}

async function exportCurrentBook() {
    if (!currentProject) return;
    const dirtyChapters = (currentProject.chapters ?? []).filter(hasUnsavedChanges);
    if (dirtyChapters.length) {
        const shouldSave = await confirmAction(
            "Salvar rascunhos antes do backup",
            `${dirtyChapters.length} capítulo(s) possuem alterações ainda sem versão. Para o backup conter o texto mais recente, o Freeferbook criará uma versão automática em cada um deles.`
        );
        if (!shouldSave) return;
        dirtyChapters.forEach(chapter => appendVersion(chapter, "Backup Web automático"));
    }
    await persistNow();
    try {
        const { blob, fileName } = await exportFreeferbookArchive(currentProject);
        downloadBlob(blob, fileName);
        showToast("Backup completo gerado.");
    } catch (error) {
        console.error(error);
        showToast(error.message || "Não foi possível exportar o livro.", 5000);
    }
}

function openImportPicker() {
    elements.importInput.value = "";
    elements.importInput.click();
}

async function importSelectedBook(file) {
    try {
        showToast("Importando backup…", 10000);
        const project = await importFreeferbookArchive(file);
        await saveProject(project);
        projects.unshift(project);
        currentProject = project;
        selectedChapterId = project.chapters?.slice().sort((a, b) => a.orderIndex - b.orderIndex)[0]?.id ?? null;
        renderAll();
        showToast(`“${project.title}” importado com sucesso.`);
    } catch (error) {
        console.error(error);
        showToast(error.message || "Não foi possível importar o backup.", 6000);
    }
}

async function loadReleaseInfo() {
    const web = await fetch("web-version.json", { cache: "no-store" }).then(r => r.ok ? r.json() : null).catch(() => null);
    const android = await fetch("update.json", { cache: "no-store" }).then(r => r.ok ? r.json() : null).catch(() => null);

    const webVersion = web?.version ?? "desenvolvimento local";
    const webCommit = web?.shortSha ? `commit ${web.shortSha}` : "sem metadados de deploy";
    const androidVersion = android?.versionName ?? "indisponível";
    const androidCommit = android?.notes ?? "sem metadados";

    $("#webBuildInfo").textContent = `Web: ${webVersion}${web?.shortSha ? ` • ${web.shortSha}` : ""}`;
    $("#androidBuildInfo").textContent = `Android: ${androidVersion}`;
    $("#welcomeWebVersion").textContent = webVersion;
    $("#welcomeWebCommit").textContent = webCommit;
    $("#welcomeAndroidVersion").textContent = androidVersion;
    $("#welcomeAndroidCommit").textContent = androidCommit;
}

function bindEvents() {
    $("#newBookButton").addEventListener("click", createBook);
    $("#welcomeNewBookButton").addEventListener("click", createBook);
    $("#importBookButton").addEventListener("click", openImportPicker);
    $("#welcomeImportBookButton").addEventListener("click", openImportPicker);
    elements.importInput.addEventListener("change", event => event.target.files?.[0] && importSelectedBook(event.target.files[0]));
    $("#newChapterButton").addEventListener("click", createChapter);
    $("#emptyNewChapterButton").addEventListener("click", createChapter);
    elements.saveVersionButton.addEventListener("click", () => saveCurrentVersion());

    elements.editor.addEventListener("input", event => {
        const chapter = currentChapter();
        if (!chapter) return;
        chapter.draftContent = event.target.value;
        updateEditorMeta();
        schedulePersist();
    });

    document.querySelectorAll("[data-view-mode]").forEach(button => button.addEventListener("click", () => switchViewMode(button.dataset.viewMode)));

    $("#chapterMenuButton").addEventListener("click", event => {
        event.stopPropagation();
        elements.chapterMenu.classList.toggle("hidden");
    });
    elements.chapterMenu.addEventListener("click", event => {
        const action = event.target.closest("[data-chapter-action]")?.dataset.chapterAction;
        elements.chapterMenu.classList.add("hidden");
        if (action === "rename") renameCurrentChapter();
        if (action === "delete") deleteCurrentChapter();
    });
    document.addEventListener("click", () => elements.chapterMenu.classList.add("hidden"));

    $("#openSidebarButton").addEventListener("click", () => elements.sidebar.classList.add("open"));
    $("#closeSidebarButton").addEventListener("click", () => elements.sidebar.classList.remove("open"));
    window.addEventListener("beforeunload", () => currentProject && saveProject(currentProject));
}

async function init() {
    bindEvents();
    try {
        projects = await listProjects();
    } catch (error) {
        console.error(error);
        showToast("O navegador bloqueou o armazenamento local. Verifique as permissões do site.", 6000);
    }
    renderAll();
    loadReleaseInfo();
}

init();
