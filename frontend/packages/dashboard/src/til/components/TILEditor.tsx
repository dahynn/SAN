/* eslint-disable @typescript-eslint/no-unused-vars */
import { useEffect, useRef, useState } from 'react';
import Editor, { OnMount } from '@monaco-editor/react';
import type * as monaco from 'monaco-editor';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Bold, Italic, List, Link as LinkIcon, RotateCcw, Heading, Quote, Code, ListOrdered, ListChecks, Save } from 'lucide-react';
import type { TILMode } from '@dashboard/til/components/TILModeTabs';
import type { TilResponse } from '@san/shared';
import type {
    TilGenerateMutation,
    TilGithubCommitMutation,
    TilJobStatusQuery,
    TilJobTone,
    TilUpdateMutation,
} from '@dashboard/til/types';
import { ContentEmptyState } from '@dashboard/components/shared/empty/ContentEmptyState';
import { hangulAdjacentStrongPlugin, normalizeHangulAdjacentStrong } from '@dashboard/utils/markdown';

const STATUS_MESSAGE_VISIBLE_MS = 3500;
const GENERATE_LABEL = '\uC0DD\uC131\uD558\uAE30';
const REGENERATE_LABEL = '\uB2E4\uC2DC \uC0DD\uC131\uD558\uAE30';
const GENERATING_LABEL = '\uC0DD\uC131\uD558\uB294 \uC911...';

interface TILEditorProps {
    activeTab: TILMode;
    selectedDate: string;
    title: string;
    setTitle: (value: string) => void;
    draft: string;
    selectedTil: TilResponse | null;
    isTilLoading?: boolean;
    generateMutation: TilGenerateMutation;
    updateMutation: TilUpdateMutation;
    commitMutation: TilGithubCommitMutation;
    generationStatusQuery: TilJobStatusQuery;
    commitStatusQuery: TilJobStatusQuery;
    generationTone: TilJobTone;
    commitTone: TilJobTone;
    generationMessage: string | null;
    commitMessage: string | null;
}

export function TILEditor({
                              activeTab,
                              selectedDate,
                              title,
                              setTitle,
                              draft,
                              selectedTil,
                              isTilLoading = false,
                              generateMutation,
                              updateMutation,
                              commitMutation,
                              generationStatusQuery,
                              commitStatusQuery,
                              generationTone,
                              commitTone,
    generationMessage,
    commitMessage,
                          }: TILEditorProps) {
    const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);

    const isGenerating = generateMutation.isPending || generationStatusQuery.isFetching || isRunning(generationStatusQuery.data?.status);

    const aiDraft = removeTilDateHeading(selectedTil?.content ?? '');
    const savedDraft = removeTilDateHeading(draft || aiDraft);
    const draftKey = `${selectedTil?.summaryId ?? 'empty'}:${savedDraft}`;
    const [editDraftState, setEditDraftState] = useState(() => ({
        key: draftKey,
        value: savedDraft,
    }));
    const editDraft = editDraftState.key === draftKey ? editDraftState.value : savedDraft;
    const setEditDraft = (value: string) => setEditDraftState({ key: draftKey, value });
    const isEditing = activeTab === 'edit';
    const isDrafts = activeTab === 'drafts';
    const generateLabel = selectedDate === getTodayDate() ? GENERATE_LABEL : REGENERATE_LABEL;
    const displayedDraft = activeTab === 'drafts' ? aiDraft : editDraft;
    const previewDraft = normalizeHangulAdjacentStrong(displayedDraft);
    const isEmptyTil = !isTilLoading && !selectedTil && !isGenerating;

    const handleEditorMount: OnMount = (editor) => {
        editorRef.current = editor;
    };

    const handleGenerate = () => {
        if (isGenerating) return;
        generateMutation.mutate();
    };

    const handleSave = () => {
        if (!selectedTil?.summaryId || !canSave) return;

        updateMutation.mutate({
            summaryId: selectedTil.summaryId,
            title: title.trim(),
            content: removeTilDateHeading(editDraft),
        });
    };

    const handleReset = () => {
        setTitle(selectedTil?.title ?? '');
        setEditDraft(savedDraft);
    };



    const handleFormat = (action: 'heading' | 'bold' | 'italic' | 'quote' | 'code' | 'link' | 'ordered-list' | 'list' | 'task') => {
        const editor = editorRef.current;
        if (!editor) return;

        const model = editor.getModel();
        const selection = editor.getSelection();
        if (!model || !selection) return;

        const selectedText = model.getValueInRange(selection);

        let replacement = selectedText;
        switch (action) {
            case 'heading':
                replacement = `### ${selectedText || 'Heading'}`;
                break;
            case 'bold':
                replacement = `**${selectedText || 'text'}**`;
                break;
            case 'italic':
                replacement = `*${selectedText || 'text'}*`;
                break;
            case 'quote':
                replacement = `\n> ${selectedText || 'quote'}`;
                break;
            case 'code':
                replacement = selectedText.includes('\n')
                    ? `\`\`\`\n${selectedText || 'code'}\n\`\`\``
                    : `\`${selectedText || 'code'}\``;
                break;
            case 'link':
                replacement = `[${selectedText || 'link'}](url)`;
                break;
            case 'ordered-list':
                replacement = `\n1. ${selectedText || 'item'}`;
                break;
            case 'list':
                replacement = `\n- ${selectedText || 'item'}`;
                break;
            case 'task':
                replacement = `\n- [ ] ${selectedText || 'task'}`;
                break;
        }

        editor.executeEdits('toolbar', [
            { range: selection, text: replacement, forceMoveMarkers: true },
        ]);

        setEditDraft(removeTilDateHeading(model.getValue()));
    };

    const statusMessage = generationMessage ?? commitMessage;
    const statusTone = generationMessage ? generationTone : commitTone;
    const [visibleStatusMessage, setVisibleStatusMessage] = useState<string | null>(null);
    const hasUnsavedChanges = editDraft !== savedDraft || title !== (selectedTil?.title ?? '');
    const showModeAction = isDrafts || isEditing;
    const canSave = Boolean(
        selectedTil?.summaryId &&
        title.trim() &&
        editDraft.trim() &&
        hasUnsavedChanges &&
        !updateMutation.isPending,
    );

    useEffect(() => {
        if (!statusMessage) {
            const clearId = window.setTimeout(() => setVisibleStatusMessage(null), 0);
            return () => window.clearTimeout(clearId);
        }

        const showId = window.setTimeout(() => setVisibleStatusMessage(statusMessage), 0);
        const hideId = statusTone === 'pending'
            ? undefined
            : window.setTimeout(() => setVisibleStatusMessage(null), STATUS_MESSAGE_VISIBLE_MS);

        return () => {
            window.clearTimeout(showId);
            if (hideId) window.clearTimeout(hideId);
        };
    }, [statusMessage, statusTone]);

    return (
        <div className="flex h-full min-h-0 w-full flex-col">
            <main className="relative flex min-h-0 w-full flex-1 flex-col bg-transparent">
                <div className="flex shrink-0 items-center justify-between border-b border-text-secondary/5 px-4 py-2.5">
                    <div className="flex min-w-0 flex-1 items-center gap-3 overflow-hidden text-text-secondary">
                        {/* 그룹 1: 텍스트 서식 */}
                        <div className={`${isEditing ? 'flex' : 'hidden'} items-center gap-2`}>
                            <button type="button" onClick={() => handleFormat('heading')} title="Heading" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <Heading size={13} strokeWidth={2} />
                            </button>
                            <button type="button" onClick={() => handleFormat('bold')} title="Bold" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <Bold size={13} strokeWidth={2.5} />
                            </button>
                            <button type="button" onClick={() => handleFormat('italic')} title="Italic" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <Italic size={13} strokeWidth={2.5} />
                            </button>
                            <button type="button" onClick={() => handleFormat('quote')} title="Quote" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <Quote size={13} strokeWidth={2} />
                            </button>
                            <button type="button" onClick={() => handleFormat('code')} title="Code" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <Code size={13} strokeWidth={2} />
                            </button>
                            <button type="button" onClick={() => handleFormat('link')} title="Link" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <LinkIcon size={13} strokeWidth={2} />
                            </button>
                        </div>

                        <div className={`${isEditing ? 'block' : 'hidden'} h-3 w-px bg-text-primary/10`} />

                        {/* 그룹 2: 리스트 */}
                        <div className={`${isEditing ? 'flex' : 'hidden'} items-center gap-2`}>
                            <button type="button" onClick={() => handleFormat('ordered-list')} title="Ordered List" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <ListOrdered size={13} strokeWidth={2} />
                            </button>
                            <button type="button" onClick={() => handleFormat('list')} title="Unordered List" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <List size={13} strokeWidth={2} />
                            </button>
                            <button type="button" onClick={() => handleFormat('task')} title="Task List" className="flex h-6 w-6 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary">
                                <ListChecks size={13} strokeWidth={2} />
                            </button>
                        </div>

                        <div className={`${isEditing ? 'block' : 'hidden'} h-4 w-px bg-text-primary/10`} />

                        {visibleStatusMessage ? (
                            <div
                                className={`flex min-w-0 max-w-[220px] items-center gap-2 rounded-full border px-3 py-1 md:max-w-[420px] ${
                                    statusTone === 'success'
                                        ? 'border-action-accent/30 bg-action-accent/10 text-action-accent'
                                        : statusTone === 'error'
                                            ? 'border-error/30 bg-error/10 text-error'
                                            : statusTone === 'pending'
                                                ? 'border-yellow-500/30 bg-yellow-500/10 text-yellow-500'
                                                : 'border-text-secondary/10 bg-text-primary/5 text-text-secondary'
                                }`}
                            >
                                <span className="block min-w-0 truncate whitespace-nowrap text-xs font-bold">{visibleStatusMessage}</span>
                            </div>
                        ) : null}
                    </div>

                    <div className="flex shrink-0 items-center gap-4 text-xs font-medium text-text-secondary">
                        <span>UTF-8</span>

                        {showModeAction ? <div className="h-4 w-px bg-text-primary/10" /> : null}

                        {isEditing ? (
                            <>
                                <button
                                    type="button"
                                    onClick={handleSave}
                                    disabled={!canSave}
                                className="flex h-8 items-center gap-1.5 rounded-tl-[10px] rounded-br-[10px] rounded-bl-md rounded-tr-md bg-action-accent px-3 text-xs font-bold text-background transition-colors hover:bg-action-accent-hover disabled:cursor-not-allowed disabled:bg-action-accent/60"
                                >
                                    <Save size={13} strokeWidth={2.2} />
                                    {updateMutation.isPending ? 'SAVING...' : 'SAVE'}
                                </button>

                                <div className="h-4 w-px bg-text-primary/10" />
                            </>
                        ) : null}

                        {isDrafts ? (
                            <button
                                type="button"
                                onClick={handleGenerate}
                                disabled={isGenerating}
                                className="til-light-teal-accent flex items-center gap-1.5 font-bold text-action-accent transition-colors hover:opacity-80 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                <RotateCcw size={14} className={isGenerating ? 'animate-spin' : ''} />
                                {isGenerating ? GENERATING_LABEL : generateLabel}
                            </button>
                        ) : null}

                        {isEditing ? (
                            <button
                                type="button"
                                onClick={handleReset}
                                disabled={!hasUnsavedChanges || updateMutation.isPending}
                                className="flex items-center gap-1.5 font-bold text-text-secondary transition-colors hover:text-text-primary disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                <RotateCcw size={14} />
                                초기화
                            </button>
                        ) : null}
                    </div>
                </div>

                <div className="relative min-h-0 w-full flex-1 overflow-hidden pt-2">
                    {isTilLoading ? (
                        <div className="flex h-full min-h-[500px] w-full flex-col gap-4 overflow-hidden p-6 animate-pulse">
                            <div className="h-6 w-3/4 rounded bg-text-primary/5" />
                            <div className="h-4 w-full rounded bg-text-primary/5" />
                            <div className="h-4 w-full rounded bg-text-primary/5" />
                            <div className="h-4 w-2/3 rounded bg-text-primary/5" />
                            <div className="mt-4 h-6 w-1/2 rounded bg-text-primary/5" />
                            <div className="h-4 w-full rounded bg-text-primary/5" />
                            <div className="h-4 w-5/6 rounded bg-text-primary/5" />
                        </div>
                    ) : isEmptyTil ? (
                        <div className="flex min-h-[500px] flex-col items-center justify-center gap-5">
                            <ContentEmptyState
                            variant="til"
                            title="오늘은 작성된 TIL이 없어요"
                            description={'뿌리가 튼튼하게 자리를 잡았습니다.\n새로운 지식을 수확하면 오늘의 TIL을 정리할 수 있어요.'}
                            />
                            <button
                                type="button"
                                onClick={handleGenerate}
                                disabled={isGenerating}
                                className="til-light-teal-accent flex h-10 items-center gap-2 rounded-lg border border-action-accent/30 bg-action-accent/10 px-4 text-sm font-bold text-action-accent transition hover:bg-action-accent/15 disabled:cursor-not-allowed disabled:opacity-50"
                            >
                                <RotateCcw size={15} className={isGenerating ? 'animate-spin' : ''} />
                                {isGenerating ? GENERATING_LABEL : generateLabel}
                            </button>
                        </div>
                    ) : isEditing ? (
                        <div className="til-editor-scrollbar til-editor-surface relative h-full min-h-[500px] w-full overflow-hidden rounded-lg border border-text-secondary/5 bg-surface-lowest/30">
                            <Editor
                                theme="vs-dark"
                                defaultLanguage="markdown"
                                value={editDraft}
                                onChange={(v) => setEditDraft(v ?? '')}
                                onMount={handleEditorMount}
                                loading={
                                    <div className="flex h-full w-full flex-col gap-4 p-6 animate-pulse">
                                        <div className="h-6 w-3/4 rounded bg-text-primary/5" />
                                        <div className="h-4 w-full rounded bg-text-primary/5" />
                                        <div className="h-4 w-full rounded bg-text-primary/5" />
                                        <div className="h-4 w-2/3 rounded bg-text-primary/5" />
                                        <div className="mt-4 h-6 w-1/2 rounded bg-text-primary/5" />
                                        <div className="h-4 w-full rounded bg-text-primary/5" />
                                        <div className="h-4 w-5/6 rounded bg-text-primary/5" />
                                    </div>
                                }
                                options={{
                                    fontSize: 15,
                                    fontFamily: 'Pretendard, ui-monospace, monospace',
                                    lineHeight: 26,
                                    wordWrap: 'on',
                                    minimap: { enabled: false },
                                    scrollbar: {
                                        vertical: 'auto',
                                        horizontal: 'auto',
                                        verticalScrollbarSize: 7,
                                        horizontalScrollbarSize: 7,
                                        handleMouseWheel: true,
                                        alwaysConsumeMouseWheel: false,
                                    },
                                    padding: { top: 16, bottom: 40 },
                                    lineNumbers: 'on',
                                    renderLineHighlight: 'all',
                                    quickSuggestions: false,
                                    automaticLayout: true,
                                    scrollBeyondLastLine: false,
                                    readOnly: false,
                                    overviewRulerLanes: 0,
                                    fontLigatures: true,
                                    cursorSmoothCaretAnimation: 'on',
                                    smoothScrolling: true,
                                }}
                            />
                        </div>
                    ) : (
                        <div className="til-editor-scrollbar til-editor-surface h-full min-h-[500px] overflow-y-auto px-10 pb-8 pt-4">
                            <article className="max-w-none leading-relaxed text-text-primary">
                                <ReactMarkdown
                                    remarkPlugins={[remarkGfm, hangulAdjacentStrongPlugin]}
                                    components={{
                                        h1: ({ children }) => (
                                            <h1 className="mb-6 border-b border-surface-highest/30 pb-4 text-3xl font-bold leading-tight text-text-primary">
                                                {children}
                                            </h1>
                                        ),
                                        h2: ({ children }) => (
                                            <h2 className="mb-4 mt-8 text-2xl font-bold leading-tight text-text-primary">
                                                {children}
                                            </h2>
                                        ),
                                        h3: ({ children }) => (
                                            <h3 className="mb-3 mt-6 text-xl font-semibold leading-snug text-text-primary">
                                                {children}
                                            </h3>
                                        ),
                                        h4: ({ children }) => (
                                            <h4 className="mb-2 mt-5 text-lg font-semibold leading-snug text-text-primary">
                                                {children}
                                            </h4>
                                        ),
                                        p: ({ children }) => (
                                            <p className="mb-4 leading-7 text-text-primary/90">{children}</p>
                                        ),
                                        ul: ({ children }) => (
                                            <ul className="mb-5 list-disc space-y-2 pl-6 text-text-primary/90">
                                                {children}
                                            </ul>
                                        ),
                                        ol: ({ children }) => (
                                            <ol className="mb-5 list-decimal space-y-2 pl-6 text-text-primary/90">
                                                {children}
                                            </ol>
                                        ),
                                        li: ({ children }) => (
                                            <li className="pl-1 leading-7 marker:text-action-accent">{children}</li>
                                        ),
                                        strong: ({ children }) => (
                                            <strong className="font-bold text-text-primary">{children}</strong>
                                        ),
                                        em: ({ children }) => (
                                            <em className="text-text-secondary">{children}</em>
                                        ),
                                        blockquote: ({ children }) => (
                                            <blockquote className="mb-5 border-l-2 border-action-accent/50 bg-action-accent/5 py-3 pl-4 text-text-secondary">
                                                {children}
                                            </blockquote>
                                        ),
                                        code: ({ children }) => (
                                            <code className="rounded bg-surface-highest px-1.5 py-0.5 font-mono text-sm text-action-accent">
                                                {children}
                                            </code>
                                        ),
                                        pre: ({ children }) => (
                                            <pre className="mb-5 overflow-x-auto rounded-[8px] border border-action-accent/10 bg-surface-low p-4 text-sm leading-6">
                        {children}
                      </pre>
                                        ),
                                        a: ({ href, children }) => (
                                            <a
                                                href={href}
                                                target="_blank"
                                                rel="noreferrer"
                                                className="text-action-accent underline decoration-action-accent/40 underline-offset-4"
                                            >
                                                {children}
                                            </a>
                                        ),
                                        hr: () => <hr className="my-8 border-action-accent/20" />,
                                    }}
                                >
                                    {previewDraft}
                                </ReactMarkdown>
                            </article>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}

function isRunning(status?: string) {
    return status === 'PENDING' || status === 'PROCESSING';
}

function getTodayDate() {
    const date = new Date();
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

function removeTilDateHeading(content: string) {
    return content
        .replace(/^\s*#{1,6}\s*TIL\s*[-–—]\s*\d{4}[./-]\d{1,2}[./-]\d{1,2}\s*\n+/i, '')
        .replace(/^\s*TIL\s*[-–—]\s*\d{4}[./-]\d{1,2}[./-]\d{1,2}\s*\n+/i, '')
        .trimStart();
}
