import { listProjects, saveProject, removeProject, newProject, newChapter } from "./db.js";
import { renderMarkdown, textStats } from "./markdown.js";
import { importFreeferbookArchive, exportFreeferbookArchive, downloadBlob } from "./archive.js";
import { applyCustomMarker, applyMarkdownFormat } from "./formatting.js";

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
    lineNumberGutter: $("#lineNumberGutter"),
    formattingToolbar: $("#formattingToolbar"),
    formattingHint: $("#formattingHint"),
    customMarkerButton: $("#customMarkerButton"),
    customMarkerPicker: $("#customMarkerPicker"),
    editorStats: $("#editorStats"),
    draftStatus: $("#draftStatus"),
    previewContent: $("#previewContent"),
    historyList: $("#historyList"),
    editPane: $("#editPane"),
    previewPane: $("#previewPane"),
    historyPane: $("#historyPane"),
    saveVersionButton: $("#saveVersionButton"),
    chapterMenu: $("#chapterMenu"),
    workspaceCollectionLabel: $("#workspaceCollectionLabel"),
    newWorkspaceItemButton: $("#newWorkspaceItemButton"),
    emptyWorkspaceTitle: $("#emptyWorkspaceTitle"),
    emptyWorkspaceText: $("#emptyWorkspaceText"),
    emptyNewWorkspaceItemButton: $("#emptyNewWorkspaceItemButton"),
    worldbuildingEditorView: $("#worldbuildingEditorView"),
    worldbuildingKindLabel: $("#worldbuildingKindLabel"),
    worldbuildingTitle: $("#worldbuildingTitle"),
    worldbuildingForm: $("#worldbuildingForm"),
    worldbuildingName: $("#worldbuildingName"),
    worldbuildingSurnames: $("#worldbuildingSurnames"),
    worldbuildingDescription: $("#worldbuildingDescription"),
    worldbuildingChapters: $("#worldbuildingChapters"),
    worldbuildingImageUri: $("#worldbuildingImageUri"),
    characterSurnamesField: $("#characterSurnamesField"),
    locationDescriptionField: $("#locationDescriptionField"),
    deleteWorldbuildingButton: $("#deleteWorldbuildingButton"),
    worldbuildingSaveState: $("#worldbuildingSaveState"),
    importInput: $("#importBookInput"),
    toast: $("#toast"),
    textDialog: $("#textDialog"),
    textDialogTitle: $("#textDialogTitle"),
    textDialogLabel: $("#textDialogLabel"),
    textDialogInput: $("#textDialogInput"),
    textDialogHint: $("#textDialogHint"),
    confirmDialog: $("#confirmDialog"),
    confirmDialogTitle: $("#confirmDialogTitle"),
    confirmDialogText: $("#confirmDialogText"),
    settingsDialog: $("#settingsDialog"),
    webTheme: $("#webTheme"),
    editorFontSize: $("#editorFontSize"),
    editorFontSizeValue: $("#editorFontSizeValue"),
    editorLineHeight: $("#editorLineHeight"),
    editorLineHeightValue: $("#editorLineHeightValue"),
    showLineNumbers: $("#showLineNumbers"),
    showCharactersTab: $("#showCharactersTab"),
    showLocationsTab: $("#showLocationsTab"),
    reducedMotion: $("#reducedMotion"),
    settingsWebVersion: $("#settingsWebVersion"),
    settingsAndroidVersion: $("#settingsAndroidVersion")
};

const WEB_SETTINGS_KEY = "freeferbook-web-settings-v1";
const DEFAULT_WEB_SETTINGS = Object.freeze({
    theme: "system",
    editorFontSize: 17,
    editorLineHeight: 1.7,
    showLineNumbers: true,
    showCharactersTab: true,
    showLocationsTab: true,
    reducedMotion: false
});

let projects = [];
let currentProject = null;
let selectedChapterId = null;
let selectedWorldbuildingId = null;
let workspaceMode = "chapters";
let viewMode = "edit";
let persistTimer = null;
let toastTimer = null;
let webSettings = loadWebSettings();
let editorSelection = { start: 0, end: 0 };

function loadWebSettings() {
    try {
        const parsed = JSON.parse(localStorage.getItem(WEB_SETTINGS_KEY) ?? "null");
        return { ...DEFAULT_WEB_SETTINGS, ...(parsed && typeof parsed === "object" ? parsed : {}) };
    } catch (_) {
        return { ...DEFAULT_WEB_SETTINGS };
    }
}

function storeWebSettings() {
    try { localStorage.setItem(WEB_SETTINGS_KEY, JSON.stringify(webSettings)); }
    catch (_) { /* preferir funcionamento da UI mesmo com storage restrito */ }
}

function syncSettingsControls() {
    elements.webTheme.value = webSettings.theme;
    elements.editorFontSize.value = String(webSettings.editorFontSize);
    elements.editorFontSizeValue.textContent = `${webSettings.editorFontSize}px`;
    elements.editorLineHeight.value = String(webSettings.editorLineHeight);
    elements.editorLineHeightValue.textContent = Number(webSettings.editorLineHeight).toFixed(1);
    elements.showLineNumbers.checked = Boolean(webSettings.showLineNumbers);
    elements.showCharactersTab.checked = Boolean(webSettings.showCharactersTab);
    elements.showLocationsTab.checked = Boolean(webSettings.showLocationsTab);
    elements.reducedMotion.checked = Boolean(webSettings.reducedMotion);
}

function applyWebSettings({ rerenderIfNeeded = true } = {}) {
    const root = document.documentElement;
    if (webSettings.theme === "system") delete root.dataset.theme;
    else root.dataset.theme = webSettings.theme;
    root.dataset.reducedMotion = String(Boolean(webSettings.reducedMotion));
    root.style.setProperty("--editor-font-size", `${Number(webSettings.editorFontSize) || 17}px`);
    root.style.setProperty("--editor-line-height", String(Number(webSettings.editorLineHeight) || 1.7));

    document.querySelector('[data-workspace-mode="characters"]')?.classList.toggle("hidden", !webSettings.showCharactersTab);
    document.querySelector('[data-workspace-mode="locations"]')?.classList.toggle("hidden", !webSettings.showLocationsTab);
    elements.lineNumberGutter.classList.toggle("hidden", !webSettings.showLineNumbers);
    syncSettingsControls();
    updateLineNumbers();

    const hiddenCurrentMode =
        (workspaceMode === "characters" && !webSettings.showCharactersTab) ||
        (workspaceMode === "locations" && !webSettings.showLocationsTab);
    if (hiddenCurrentMode) {
        workspaceMode = "chapters";
        selectedWorldbuildingId = null;
        if (rerenderIfNeeded && currentProject) renderWorkspace();
    }
}

function updateWebSetting(key, value) {
    webSettings = { ...webSettings, [key]: value };
    storeWebSettings();
    applyWebSettings();
}

function captureEditorSelection() {
    if (!elements.editor) return editorSelection;
    editorSelection = {
        start: elements.editor.selectionStart ?? 0,
        end: elements.editor.selectionEnd ?? 0
    };
    syncFormattingToolbar();
    return editorSelection;
}

function syncFormattingToolbar() {
    const selectedLength = Math.abs(editorSelection.end - editorSelection.start);
    const disabled = workspaceMode !== "chapters" || !currentChapter() || selectedLength === 0;
    elements.formattingToolbar?.querySelectorAll("[data-format]").forEach(button => {
        button.disabled = disabled;
    });
    if (elements.customMarkerButton) elements.customMarkerButton.disabled = disabled;
    if (elements.formattingHint) {
        elements.formattingHint.textContent = disabled
            ? "Selecione um trecho para formatar."
            : `${selectedLength} caractere${selectedLength === 1 ? "" : "s"} selecionado${selectedLength === 1 ? "" : "s"}.`;
    }
    if (disabled) elements.customMarkerPicker?.classList.add("hidden");
}

function applyFormattingResult(result) {
    const chapter = currentChapter();
    if (!chapter || !result) return;
    chapter.draftContent = result.text;
    elements.editor.value = result.text;
    editorSelection = { start: result.start, end: result.end };
    elements.editor.focus();
    elements.editor.setSelectionRange(result.start, result.end);
    updateEditorMeta();
    schedulePersist();
    syncFormattingToolbar();
}

function applyEditorFormat(format) {
    const chapter = currentChapter();
    if (!chapter) return;
    const { start, end } = editorSelection;
    applyFormattingResult(applyMarkdownFormat(chapter.draftContent ?? "", start, end, format));
}

function applyEditorCustomMarker(marker) {
    const chapter = currentChapter();
    if (!chapter) return;
    const { start, end } = editorSelection;
    applyFormattingResult(applyCustomMarker(chapter.draftContent ?? "", start, end, marker));
    elements.customMarkerPicker.classList.add("hidden");
}

function currentChapter() {
    return currentProject?.chapters?.find(chapter => chapter.id === selectedChapterId) ?? null;
}

function currentWorldbuildingItem() {
    if (!currentProject || workspaceMode === "chapters") return null;
    return (currentProject[workspaceMode] ?? []).find(item => item.id === selectedWorldbuildingId) ?? null;
}

function workspaceConfig() {
    if (workspaceMode === "characters") {
        return {
            singular: "personagem",
            singularTitle: "Personagem",
            plural: "Personagens",
            emptyTitle: "Nenhum personagem selecionado",
            emptyText: "Crie ou escolha um personagem na lista ao lado."
        };
    }
    if (workspaceMode === "locations") {
        return {
            singular: "local",
            singularTitle: "Local",
            plural: "Locais",
            emptyTitle: "Nenhum local selecionado",
            emptyText: "Crie ou escolha um local na lista ao lado."
        };
    }
    return {
        singular: "capítulo",
        singularTitle: "Capítulo",
        plural: "Capítulos",
        emptyTitle: "Nenhum capítulo selecionado",
        emptyText: "Crie ou escolha um capítulo na lista ao lado."
    };
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

function renderWorldbuildingList() {
    elements.chapterList.replaceChildren();
    if (!currentProject || workspaceMode === "chapters") return;
    const items = currentProject[workspaceMode] ?? [];
    const config = workspaceConfig();

    if (!items.length) {
        const empty = document.createElement("div");
        empty.className = "muted small";
        empty.textContent = `Nenhum ${config.singular}.`;
        elements.chapterList.append(empty);
        return;
    }

    for (const item of items) {
        const button = document.createElement("button");
        button.className = `chapter-item${selectedWorldbuildingId === item.id ? " active" : ""}`;
        button.innerHTML = `<span class="chapter-item-title"></span><span class="chapter-item-meta"></span>`;
        button.querySelector(".chapter-item-title").textContent = item.name || `Sem nome`;
        const detail = workspaceMode === "characters"
            ? [item.surnames, item.chapters ? `cap. ${item.chapters}` : ""].filter(Boolean).join(" • ")
            : [item.description, item.chapters ? `cap. ${item.chapters}` : ""].filter(Boolean).join(" • ");
        button.querySelector(".chapter-item-meta").textContent = detail || config.singularTitle;
        button.addEventListener("click", () => {
            selectedWorldbuildingId = item.id;
            renderWorkspace();
        });
        elements.chapterList.append(button);
    }
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
            selectedWorldbuildingId = null;
            workspaceMode = "chapters";
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
    const config = workspaceConfig();
    elements.workspaceCollectionLabel.textContent = config.plural;
    elements.newWorkspaceItemButton.title = `Novo ${config.singular}`;
    elements.newWorkspaceItemButton.setAttribute("aria-label", `Novo ${config.singular}`);
    elements.emptyWorkspaceTitle.textContent = config.emptyTitle;
    elements.emptyWorkspaceText.textContent = config.emptyText;
    elements.emptyNewWorkspaceItemButton.textContent = `Criar ${config.singular}`;
    document.querySelectorAll("[data-workspace-mode]").forEach(button => {
        button.classList.toggle("active", button.dataset.workspaceMode === workspaceMode);
    });

    if (workspaceMode === "chapters") {
        renderChapterList();
        const chapter = currentChapter();
        elements.noChapterView.classList.toggle("hidden", Boolean(chapter));
        elements.chapterEditorView.classList.toggle("hidden", !chapter);
        elements.worldbuildingEditorView.classList.add("hidden");
        if (!chapter) {
            editorSelection = { start: 0, end: 0 };
            syncFormattingToolbar();
            return;
        }

        if (elements.editor.value !== chapter.draftContent) {
            elements.editor.value = chapter.draftContent ?? "";
            editorSelection = { start: 0, end: 0 };
            elements.editor.setSelectionRange(0, 0);
        }
        updateEditorMeta();
        captureEditorSelection();
        switchViewMode(viewMode, false);
        return;
    }

    renderWorldbuildingList();
    const item = currentWorldbuildingItem();
    elements.noChapterView.classList.toggle("hidden", Boolean(item));
    elements.chapterEditorView.classList.add("hidden");
    elements.worldbuildingEditorView.classList.toggle("hidden", !item);
    editorSelection = { start: 0, end: 0 };
    syncFormattingToolbar();
    if (item) renderWorldbuildingEditor(item);
}

function renderAll() {
    renderBookList();
    renderTopbar();
    renderWorkspace();
}

async function selectProject(id) {
    await persistNow();
    currentProject = projects.find(project => project.id === id) ?? null;
    workspaceMode = "chapters";
    selectedChapterId = currentProject?.chapters?.slice().sort((a, b) => a.orderIndex - b.orderIndex)[0]?.id ?? null;
    selectedWorldbuildingId = null;
    viewMode = "edit";
    elements.sidebar.classList.remove("open");
    renderAll();
}

function selectChapter(id) {
    workspaceMode = "chapters";
    selectedChapterId = id;
    selectedWorldbuildingId = null;
    viewMode = "edit";
    renderWorkspace();
}

function switchWorkspaceMode(mode) {
    if (!currentProject || !["chapters", "characters", "locations"].includes(mode)) return;
    if (mode === "characters" && !webSettings.showCharactersTab) return;
    if (mode === "locations" && !webSettings.showLocationsTab) return;
    workspaceMode = mode;
    viewMode = "edit";
    if (mode === "chapters") {
        selectedChapterId ??= currentProject.chapters?.slice().sort((a, b) => a.orderIndex - b.orderIndex)[0]?.id ?? null;
        selectedWorldbuildingId = null;
    } else {
        const items = currentProject[mode] ?? [];
        selectedWorldbuildingId = items.some(item => item.id === selectedWorldbuildingId)
            ? selectedWorldbuildingId
            : items[0]?.id ?? null;
    }
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
    updateLineNumbers();
}

function updateLineNumbers() {
    if (!elements.lineNumberGutter) return;
    const chapter = currentChapter();
    const count = Math.max(1, (chapter?.draftContent ?? "").split(/\r?\n/).length);
    elements.lineNumberGutter.textContent = Array.from({ length: count }, (_, index) => index + 1).join("\n");
    elements.lineNumberGutter.scrollTop = elements.editor?.scrollTop ?? 0;
}

function renderWorldbuildingEditor(item) {
    const isCharacter = workspaceMode === "characters";
    elements.worldbuildingKindLabel.textContent = isCharacter ? "Personagem" : "Local";
    elements.worldbuildingTitle.textContent = item.name || (isCharacter ? "Novo personagem" : "Novo local");
    elements.characterSurnamesField.classList.toggle("hidden", !isCharacter);
    elements.locationDescriptionField.classList.toggle("hidden", isCharacter);

    if (elements.worldbuildingName.value !== (item.name ?? "")) elements.worldbuildingName.value = item.name ?? "";
    if (elements.worldbuildingSurnames.value !== (item.surnames ?? "")) elements.worldbuildingSurnames.value = item.surnames ?? "";
    if (elements.worldbuildingDescription.value !== (item.description ?? "")) elements.worldbuildingDescription.value = item.description ?? "";
    if (elements.worldbuildingChapters.value !== (item.chapters ?? "")) elements.worldbuildingChapters.value = item.chapters ?? "";
    if (elements.worldbuildingImageUri.value !== (item.imageUri ?? "")) elements.worldbuildingImageUri.value = item.imageUri ?? "";
    elements.worldbuildingSaveState.textContent = "Salvo automaticamente neste navegador.";
}

function updateWorldbuildingItemFromForm() {
    const item = currentWorldbuildingItem();
    if (!item) return;

    item.name = elements.worldbuildingName.value;
    item.chapters = elements.worldbuildingChapters.value;
    const nextImageUri = elements.worldbuildingImageUri.value.trim() || null;
    if ((item.imageUri ?? null) !== nextImageUri) item.mediaId = null;
    item.imageUri = nextImageUri;

    if (workspaceMode === "characters") {
        item.surnames = elements.worldbuildingSurnames.value;
    } else {
        item.description = elements.worldbuildingDescription.value;
    }

    elements.worldbuildingTitle.textContent = item.name.trim() || workspaceConfig().singularTitle;
    elements.worldbuildingSaveState.textContent = "Salvando…";
    renderWorldbuildingList();
    schedulePersist();
    clearTimeout(updateWorldbuildingItemFromForm.saveStateTimer);
    updateWorldbuildingItemFromForm.saveStateTimer = setTimeout(() => {
        elements.worldbuildingSaveState.textContent = "Salvo automaticamente neste navegador.";
    }, 700);
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
    workspaceMode = "chapters";
    selectedChapterId = null;
    selectedWorldbuildingId = null;
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

async function createWorkspaceItem() {
    if (workspaceMode === "chapters") {
        await createChapter();
        return;
    }
    await createWorldbuildingItem();
}

async function createWorldbuildingItem() {
    if (!currentProject || workspaceMode === "chapters") return;
    const config = workspaceConfig();
    const name = await promptText({
        title: `Novo ${config.singular}`,
        label: "Nome",
        hint: `O ${config.singular} ficará salvo neste livro e será incluído no backup.`
    });
    if (!name?.trim()) return;

    const item = workspaceMode === "characters"
        ? {
            id: crypto.randomUUID(),
            name: name.trim(),
            surnames: "",
            chapters: "",
            imageUri: null,
            mediaId: null
        }
        : {
            id: crypto.randomUUID(),
            name: name.trim(),
            description: "",
            chapters: "",
            imageUri: null,
            mediaId: null
        };

    currentProject[workspaceMode] ??= [];
    currentProject[workspaceMode].push(item);
    selectedWorldbuildingId = item.id;
    await persistNow();
    renderWorkspace();
    elements.worldbuildingName.focus();
}

async function deleteCurrentWorldbuildingItem() {
    const item = currentWorldbuildingItem();
    if (!item || workspaceMode === "chapters") return;
    const config = workspaceConfig();
    if (!await confirmAction(
        `Excluir ${config.singular}`,
        `Excluir “${item.name || config.singularTitle}” deste livro?`
    )) return;

    currentProject[workspaceMode] = (currentProject[workspaceMode] ?? []).filter(candidate => candidate.id !== item.id);
    selectedWorldbuildingId = currentProject[workspaceMode][0]?.id ?? null;
    await persistNow();
    renderWorkspace();
    showToast(`${config.singularTitle} excluído.`);
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
        workspaceMode = "chapters";
        selectedChapterId = project.chapters?.slice().sort((a, b) => a.orderIndex - b.orderIndex)[0]?.id ?? null;
        selectedWorldbuildingId = null;
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
    elements.settingsWebVersion.textContent = web?.shortSha ? `${webVersion} • ${web.shortSha}` : webVersion;
    elements.settingsAndroidVersion.textContent = androidVersion;
}

function bindEvents() {
    $("#newBookButton").addEventListener("click", createBook);
    $("#welcomeNewBookButton").addEventListener("click", createBook);
    $("#importBookButton").addEventListener("click", openImportPicker);
    $("#welcomeImportBookButton").addEventListener("click", openImportPicker);
    elements.importInput.addEventListener("change", event => event.target.files?.[0] && importSelectedBook(event.target.files[0]));
    elements.newWorkspaceItemButton.addEventListener("click", createWorkspaceItem);
    elements.emptyNewWorkspaceItemButton.addEventListener("click", createWorkspaceItem);
    elements.saveVersionButton.addEventListener("click", () => saveCurrentVersion());

    document.querySelectorAll("[data-workspace-mode]").forEach(button => {
        button.addEventListener("click", () => switchWorkspaceMode(button.dataset.workspaceMode));
    });

    elements.worldbuildingForm.addEventListener("input", updateWorldbuildingItemFromForm);
    elements.worldbuildingForm.addEventListener("submit", event => event.preventDefault());
    elements.deleteWorldbuildingButton.addEventListener("click", deleteCurrentWorldbuildingItem);

    elements.editor.addEventListener("input", event => {
        const chapter = currentChapter();
        if (!chapter) return;
        chapter.draftContent = event.target.value;
        captureEditorSelection();
        updateEditorMeta();
        schedulePersist();
    });
    elements.editor.addEventListener("scroll", () => {
        elements.lineNumberGutter.scrollTop = elements.editor.scrollTop;
    });
    elements.editor.addEventListener("select", captureEditorSelection);
    elements.editor.addEventListener("keyup", captureEditorSelection);
    elements.editor.addEventListener("click", captureEditorSelection);
    elements.editor.addEventListener("keydown", event => {
        if (!(event.ctrlKey || event.metaKey) || event.altKey) return;
        const key = event.key.toLowerCase();
        const shortcut = key === "b" ? "bold" : key === "i" ? "italic" : key === "u" ? "underline" : null;
        if (!shortcut) return;
        event.preventDefault();
        captureEditorSelection();
        applyEditorFormat(shortcut);
    });

    elements.formattingToolbar.addEventListener("mousedown", event => {
        if (event.target.closest("button")) event.preventDefault();
    });
    elements.formattingToolbar.addEventListener("click", event => {
        const format = event.target.closest("[data-format]")?.dataset.format;
        if (format) {
            applyEditorFormat(format);
            return;
        }
        if (event.target.closest("#customMarkerButton")) {
            event.stopPropagation();
            if (!elements.customMarkerButton.disabled) elements.customMarkerPicker.classList.toggle("hidden");
        }
    });
    elements.customMarkerPicker.addEventListener("click", event => {
        event.stopPropagation();
        const marker = event.target.closest("[data-custom-marker]")?.dataset.customMarker;
        if (marker) applyEditorCustomMarker(marker);
    });

    $("#settingsButton").addEventListener("click", () => {
        syncSettingsControls();
        elements.settingsDialog.showModal();
    });
    elements.webTheme.addEventListener("change", event => updateWebSetting("theme", event.target.value));
    elements.editorFontSize.addEventListener("input", event => updateWebSetting("editorFontSize", Number(event.target.value)));
    elements.editorLineHeight.addEventListener("input", event => updateWebSetting("editorLineHeight", Number(event.target.value)));
    elements.showLineNumbers.addEventListener("change", event => updateWebSetting("showLineNumbers", event.target.checked));
    elements.showCharactersTab.addEventListener("change", event => updateWebSetting("showCharactersTab", event.target.checked));
    elements.showLocationsTab.addEventListener("change", event => updateWebSetting("showLocationsTab", event.target.checked));
    elements.reducedMotion.addEventListener("change", event => updateWebSetting("reducedMotion", event.target.checked));

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
    document.addEventListener("click", () => {
        elements.chapterMenu.classList.add("hidden");
        elements.customMarkerPicker.classList.add("hidden");
    });

    $("#openSidebarButton").addEventListener("click", () => elements.sidebar.classList.add("open"));
    $("#closeSidebarButton").addEventListener("click", () => elements.sidebar.classList.remove("open"));
    window.addEventListener("beforeunload", () => currentProject && saveProject(currentProject));
}

async function init() {
    applyWebSettings({ rerenderIfNeeded: false });
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
