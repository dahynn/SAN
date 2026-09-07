import { useEffect, useMemo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import Editor, { type OnMount } from '@monaco-editor/react';
import type * as monaco from 'monaco-editor';
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  Bold,
  BookOpen,
  CheckCircle2,
  Code,
  Eye,
  FileText,
  Heading,
  Image,
  Italic,
  Link as LinkIcon,
  List,
  ListOrdered,
  Loader2,
  PenLine,
  Quote,
  RotateCcw,
  Save,
  Tags,
  Trash2,
} from 'lucide-react';
import {
  getApiErrorMessage,
  useCardDetail,
  useDeleteCard,
  useArchiveCardTagRelations,
  useUpdateRefinedContent,
  type KnowledgeCardDetailResponse,
  type ArchiveRelatedCardResponse,
} from '@san/shared';

// ... (rest of imports)
import { hangulAdjacentStrongPlugin, normalizeHangulAdjacentStrong } from '@dashboard/utils/markdown';

export type KnowledgeSourceType = 'LINK' | 'IMAGE' | 'PDF' | 'OCR' | 'TEXT';

export interface KnowledgeCardDetailData {
  cardId: string;
  source: {
    type: KnowledgeSourceType;
    url?: string | null;
    previewUrl?: string | null;
    rawContent: string;
    collectedAt?: string | null;
  };
  processedText: {
    refinedContent: string;
    updatedAt?: string | null;
  };
  finalCard: {
    title: string;
    summary: string;
    keyPoints: string[];
    categoryName: string;
    tags: string[];
    relatedKeywords: string[];
    createdAt?: string | null;
  };
  relatedCards: Array<{
    cardId: string;
    title: string;
    categoryName: string;
  }>;
}

const textWrapClass = 'min-w-0 break-words [overflow-wrap:anywhere]';
const sectionCardClass = 'min-w-0 rounded-tl-[32px] rounded-br-[32px] rounded-tr-2xl rounded-bl-2xl border border-text-secondary/5 glass-card bg-surface-container/80 p-6 shadow-md';
const panelCardClass = 'min-w-0 rounded-tl-[28px] rounded-br-[28px] rounded-tr-xl rounded-bl-xl border border-text-secondary/5 bg-surface-low p-5';
const REFINE_POLL_INTERVAL_MS = 2000;
const REFINE_POLL_TIMEOUT_MS = 30000;

const mdComponents = {
  h1: ({ children }: { children?: React.ReactNode }) => (
    <h1 className={`mb-5 border-b border-primary-signal/20 pb-3 text-2xl font-bold text-text-primary ${textWrapClass}`}>{children}</h1>
  ),
  h2: ({ children }: { children?: React.ReactNode }) => (
    <h2 className={`mb-3 mt-7 text-xl font-bold text-text-primary ${textWrapClass}`}>{children}</h2>
  ),
  h3: ({ children }: { children?: React.ReactNode }) => (
    <h3 className={`mb-2 mt-5 text-lg font-semibold text-text-primary/90 ${textWrapClass}`}>{children}</h3>
  ),
  h4: ({ children }: { children?: React.ReactNode }) => (
    <h4 className={`mb-2 mt-4 text-base font-semibold text-text-primary/90 ${textWrapClass}`}>{children}</h4>
  ),
  p: ({ children }: { children?: React.ReactNode }) => (
    <p className={`mb-4 leading-7 text-text-primary/70 ${textWrapClass}`}>{children}</p>
  ),
  ul: ({ children }: { children?: React.ReactNode }) => (
    <ul className="mb-4 list-disc space-y-1.5 pl-6 text-text-primary/70">{children}</ul>
  ),
  ol: ({ children }: { children?: React.ReactNode }) => (
    <ol className="mb-4 list-decimal space-y-1.5 pl-6 text-text-primary/70">{children}</ol>
  ),
  li: ({ children }: { children?: React.ReactNode }) => (
    <li className={`pl-1 leading-7 marker:text-primary-signal ${textWrapClass}`}>{children}</li>
  ),
  strong: ({ children }: { children?: React.ReactNode }) => (
    <strong className="font-bold text-text-primary/90">{children}</strong>
  ),
  em: ({ children }: { children?: React.ReactNode }) => (
    <em className="text-text-primary/50">{children}</em>
  ),
  blockquote: ({ children }: { children?: React.ReactNode }) => (
    <blockquote className="mb-4 border-l-2 border-primary-signal/40 bg-primary-signal/5 py-2 pl-4 text-text-primary/60">{children}</blockquote>
  ),
  code: ({ children }: { children?: React.ReactNode }) => (
    <code className={`rounded bg-text-primary/10 px-1.5 py-0.5 font-mono text-sm text-primary-signal ${textWrapClass}`}>{children}</code>
  ),
  pre: ({ children }: { children?: React.ReactNode }) => (
    <pre className="mb-4 max-w-full overflow-x-auto rounded-xl border border-text-secondary/5 bg-surface-lowest p-4 text-sm leading-6">{children}</pre>
  ),
  a: ({ href, children }: { href?: string; children?: React.ReactNode }) => (
    <a href={href} target="_blank" rel="noreferrer" className={`text-primary-signal underline decoration-primary-signal/40 underline-offset-4 ${textWrapClass}`}>{children}</a>
  ),
  hr: () => <hr className="my-6 border-text-secondary/10" />,
};

export function KnowledgeCardDetailPage() {
  const { cardId } = useParams();
  const navigate = useNavigate();
  const [refinePollStartedAt, setRefinePollStartedAt] = useState<number | null>(null);
  const [refinePollTimedOut, setRefinePollTimedOut] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [deleteErrorMessage, setDeleteErrorMessage] = useState<string | null>(null);
  const detailQuery = useCardDetail(cardId, {
    refetchInterval: (query) => {
      if (!query.state.data || refinePollTimedOut) return false;
      if (query.state.data.refinedContent?.trim()) return false;
      return REFINE_POLL_INTERVAL_MS;
    },
  });
  const relatedQuery = useArchiveCardTagRelations(cardId);
  const deleteCardMutation = useDeleteCard(cardId, {
    onSuccess: () => {
      setIsDeleteDialogOpen(false);
      navigate('/archive', { replace: true });
    },
    onError: (error) => {
      setDeleteErrorMessage(getApiErrorMessage(error, '지식카드를 삭제하지 못했습니다.'));
    },
  });
  const hasRefinedContent = Boolean(detailQuery.data?.refinedContent?.trim());

  useEffect(() => {
    setRefinePollStartedAt(null);
    setRefinePollTimedOut(false);
  }, [cardId]);

  useEffect(() => {
    if (!detailQuery.data || hasRefinedContent) return;
    setRefinePollStartedAt((current) => current ?? Date.now());
  }, [detailQuery.data, hasRefinedContent]);

  useEffect(() => {
    if (!refinePollStartedAt || hasRefinedContent) return undefined;

    const timeoutId = window.setTimeout(() => {
      setRefinePollTimedOut(true);
    }, REFINE_POLL_TIMEOUT_MS);

    return () => window.clearTimeout(timeoutId);
  }, [hasRefinedContent, refinePollStartedAt]);

  const data = useMemo(() => {
    if (!cardId || !detailQuery.data) return null;
    return toDetailData(cardId, detailQuery.data, relatedQuery.data?.relatedCards ?? []);
  }, [cardId, detailQuery.data, relatedQuery.data?.relatedCards]);

  const isCheckingRefinedContent = Boolean(detailQuery.data) && !hasRefinedContent && !refinePollTimedOut;
  const requestDeleteCard = () => {
    setDeleteErrorMessage(null);
    setIsDeleteDialogOpen(true);
  };

  const confirmDeleteCard = () => {
    if (!cardId || deleteCardMutation.isPending) return;
    deleteCardMutation.mutate();
  };

  if (!cardId) {
    return <DetailStatus tone="error" title="잘못된 카드 주소입니다." description="상세보기로 이동할 지식카드 ID가 없습니다." />;
  }

  if (detailQuery.isPending) {
    return <DetailStatus title="지식카드를 불러오는 중입니다." description="원본 데이터와 AI 정제 결과를 조회하고 있습니다." />;
  }

  if (detailQuery.isError || !data) {
    return (
      <DetailStatus
        tone="error"
        title="지식카드를 불러올 수 없습니다."
        description="카드가 삭제되었거나 접근 권한이 없을 수 있습니다."
      />
    );
  }

  return (
    <>
      <section className="flex w-full min-w-0 flex-col gap-8 py-12 text-text-primary">
        <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
          <div className="min-w-0">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="mb-5 inline-flex items-center gap-2 text-sm font-medium text-text-primary/40 transition-colors hover:text-text-primary"
            >
              <ArrowLeft size={16} aria-hidden="true" />
              돌아가기
            </button>
            <p className="text-md font-bold uppercase tracking-wide text-primary-signal">지식카드 상세보기</p>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <h1 className="max-w-4xl text-h1-bold leading-[1.25] text-text-primary md:text-[40px]">
                {data.finalCard.title}
              </h1>
              <span className="shrink-0 rounded-full border border-primary-signal/20 bg-primary-signal/10 px-3 py-1 text-sm font-semibold text-primary-signal">
                {data.finalCard.categoryName}
              </span>
            </div>
            <p className="mt-4 max-w-3xl text-base leading-7 text-text-primary/50">
              원본 데이터에서 AI 1차 정제 텍스트를 거쳐 최종 지식카드가 만들어진 흐름을 확인합니다.
            </p>
          </div>
          <button
            type="button"
            onClick={requestDeleteCard}
            disabled={deleteCardMutation.isPending}
            className="inline-flex shrink-0 items-center justify-center gap-2 rounded-tl-[14px] rounded-br-[14px] rounded-bl-md rounded-tr-md border border-red-400/20 bg-red-400/5 px-4 py-2 text-sm font-bold text-red-500 transition hover:border-red-400/40 hover:bg-red-400/10 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Trash2 size={15} strokeWidth={2.2} aria-hidden="true" />
            삭제
          </button>
        </div>

        <div className="grid min-w-0 gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
          <div className="flex min-w-0 flex-col gap-6">
            <ProcessedTextSection
              cardId={data.cardId}
              processedText={data.processedText}
              isCheckingRefinedContent={isCheckingRefinedContent}
            />
            <FinalKnowledgeCardSection finalCard={data.finalCard} />
          </div>
          <DetailMetaPanel data={data} isLoadingRelated={relatedQuery.isPending} />
        </div>
      </section>

      {isDeleteDialogOpen ? (
        <DeleteCardDialog
          title={data.finalCard.title}
          isPending={deleteCardMutation.isPending}
          errorMessage={deleteErrorMessage}
          onCancel={() => {
            if (deleteCardMutation.isPending) return;
            setIsDeleteDialogOpen(false);
          }}
          onConfirm={confirmDeleteCard}
        />
      ) : null}
    </>
  );
}


function DeleteCardDialog({
  title,
  isPending,
  errorMessage,
  onCancel,
  onConfirm,
}: {
  title: string;
  isPending: boolean;
  errorMessage: string | null;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <div
      className="fixed inset-0 z-[80] flex items-center justify-center bg-scrim/70 px-4 backdrop-blur-sm"
      onClick={onCancel}
      onKeyDown={(event) => event.key === 'Escape' && onCancel()}
      role="button"
      tabIndex={0}
    >
      <div
        className="w-full max-w-md rounded-tl-[28px] rounded-br-[28px] rounded-tr-xl rounded-bl-xl border border-red-400/15 bg-surface-container p-6 text-text-primary shadow-2xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start gap-3">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-tl-[16px] rounded-br-[16px] rounded-tr-md rounded-bl-md bg-red-400/10 text-red-500">
            <AlertTriangle size={19} aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <h2 className="text-base font-bold text-text-primary">지식카드를 삭제할까요?</h2>
            <p className={`mt-2 line-clamp-2 text-sm leading-6 text-text-primary/45 ${textWrapClass}`}>{title}</p>
          </div>
        </div>
        <p className="mt-5 text-sm leading-6 text-text-primary/45">
          삭제한 지식카드는 아카이브와 관련 카드 목록에서 사라지며 되돌릴 수 없습니다.
        </p>
        {errorMessage ? <p className="mt-3 text-sm font-medium text-red-400">{errorMessage}</p> : null}
        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            disabled={isPending}
            className="rounded-lg px-4 py-2 text-sm font-semibold text-text-primary/45 transition hover:bg-text-primary/5 hover:text-text-primary disabled:opacity-40"
          >
            취소
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={isPending}
            className="inline-flex items-center gap-2 rounded-tl-[12px] rounded-br-[12px] rounded-bl-md rounded-tr-md bg-red-500 px-4 py-2 text-sm font-bold text-white transition hover:bg-red-500/90 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {isPending ? <Loader2 size={14} className="animate-spin" /> : <Trash2 size={14} strokeWidth={2.2} />}
            {isPending ? '삭제 중...' : '삭제'}
          </button>
        </div>
      </div>
    </div>
  );
}


function ProcessedTextSection({
  cardId,
  processedText,
  isCheckingRefinedContent,
}: {
  cardId: string;
  processedText: KnowledgeCardDetailData['processedText'];
  isCheckingRefinedContent: boolean;
}) {
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState(processedText.refinedContent);
  const [saveErrorMessage, setSaveErrorMessage] = useState<string | null>(null);
  const hasRefinedContent = processedText.refinedContent.trim().length > 0;
  const normalizedEditValue = editValue.trim();
  const hasUnsavedChanges = normalizedEditValue !== processedText.refinedContent.trim();
  const previewValue = normalizeHangulAdjacentStrong(editValue);
  const updateRefinedContentMutation = useUpdateRefinedContent(cardId, {
    onSuccess: (updatedDetail) => {
      setEditValue(updatedDetail.refinedContent ?? '');
      setSaveErrorMessage(null);
      setIsEditing(false);
    },
    onError: (error) => {
      setSaveErrorMessage(getApiErrorMessage(error, '정제 텍스트를 저장하지 못했습니다.'));
    },
  });

  useEffect(() => {
    setEditValue(processedText.refinedContent);
    setSaveErrorMessage(null);
  }, [processedText.refinedContent]);

  const handleEditorMount: OnMount = (editor) => {
    editorRef.current = editor;
  };

  const handleFormat = (action: string) => {
    const editor = editorRef.current;
    if (!editor) return;
    const model = editor.getModel();
    const selection = editor.getSelection();
    if (!model || !selection) return;

    const selectedText = model.getValueInRange(selection);
    let replacement = selectedText;

    switch (action) {
      case 'heading': replacement = `### ${selectedText || 'Heading'}`; break;
      case 'bold': replacement = `**${selectedText || 'text'}**`; break;
      case 'italic': replacement = `*${selectedText || 'text'}*`; break;
      case 'quote': replacement = `\n> ${selectedText || 'quote'}`; break;
      case 'code':
        replacement = selectedText.includes('\n')
          ? `\`\`\`\n${selectedText || 'code'}\n\`\`\``
          : `\`${selectedText || 'code'}\``;
        break;
      case 'link': replacement = `[${selectedText || 'link'}](url)`; break;
      case 'ordered-list': replacement = `\n1. ${selectedText || 'item'}`; break;
      case 'list': replacement = `\n- ${selectedText || 'item'}`; break;
    }

    editor.executeEdits('toolbar', [
      { range: selection, text: replacement, forceMoveMarkers: true },
    ]);
    setEditValue(model.getValue());
  };

  const handleReset = () => {
    setEditValue(processedText.refinedContent);
    setSaveErrorMessage(null);
  };

  const handleSave = () => {
    if (!hasUnsavedChanges || updateRefinedContentMutation.isPending) return;
    if (!normalizedEditValue) {
      setSaveErrorMessage('정제 텍스트를 비워둘 수 없습니다.');
      return;
    }

    updateRefinedContentMutation.mutate({ refinedContent: normalizedEditValue });
  };

  return (
    <section className={sectionCardClass}>
      <div className="flex items-start justify-between gap-4">
        <SectionHeader
          icon={<BookOpen size={18} />}
          title="정제 텍스트"
          description="원본을 읽기 쉽게 변환한 텍스트입니다."
        />
        {hasRefinedContent && (
          <button
            type="button"
            onClick={() => setIsEditing(!isEditing)}
            className="mt-1 flex shrink-0 items-center gap-1.5 rounded-lg border border-text-secondary/10 px-3 py-1.5 text-xs font-medium text-text-primary/50 transition-colors hover:border-text-secondary/20 hover:text-text-primary"
          >
            {isEditing ? <Eye size={13} /> : <PenLine size={13} />}
            {isEditing ? '미리보기' : '편집'}
          </button>
        )}
      </div>

      {hasRefinedContent ? (
        <>
          {isEditing && (
            <div className="mt-4 flex items-center gap-1 overflow-x-auto rounded-xl border border-text-secondary/5 glass-panel bg-surface-lowest/60 px-2.5 py-2 text-text-primary/40">
              <ToolbarButton icon={<Heading size={13} />} title="Heading" onClick={() => handleFormat('heading')} />
              <ToolbarButton icon={<Bold size={13} strokeWidth={2.5} />} title="Bold" onClick={() => handleFormat('bold')} />
              <ToolbarButton icon={<Italic size={13} strokeWidth={2.5} />} title="Italic" onClick={() => handleFormat('italic')} />
              <ToolbarButton icon={<Quote size={13} />} title="Quote" onClick={() => handleFormat('quote')} />
              <ToolbarButton icon={<Code size={13} />} title="Code" onClick={() => handleFormat('code')} />
              <ToolbarButton icon={<LinkIcon size={13} />} title="Link" onClick={() => handleFormat('link')} />
              <div className="mx-1 h-4 w-px shrink-0 bg-text-primary/10" />
              <ToolbarButton icon={<ListOrdered size={13} />} title="Ordered List" onClick={() => handleFormat('ordered-list')} />
              <ToolbarButton icon={<List size={13} />} title="List" onClick={() => handleFormat('list')} />
            </div>
          )}

          {isEditing ? (
            <div className="mt-3 overflow-hidden rounded-2xl border border-text-secondary/5 bg-surface-lowest/30">
              <Editor
                height="24rem"
                theme="vs-dark"
                defaultLanguage="markdown"
                value={editValue}
                onChange={(v) => setEditValue(v ?? '')}
                onMount={handleEditorMount}
                loading={
                  <div className="flex h-96 w-full flex-col gap-4 p-6 animate-pulse">
                    <div className="h-6 w-3/4 rounded bg-text-primary/5" />
                    <div className="h-4 w-full rounded bg-text-primary/5" />
                    <div className="h-4 w-2/3 rounded bg-text-primary/5" />
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
                  },
                  padding: { top: 16, bottom: 40 },
                  lineNumbers: 'on',
                  renderLineHighlight: 'all',
                  quickSuggestions: false,
                  automaticLayout: true,
                  scrollBeyondLastLine: false,
                  overviewRulerLanes: 0,
                  smoothScrolling: true,
                }}
              />
            </div>
          ) : (
            <div className="mt-6 rounded-2xl border border-text-secondary/5 glass-panel bg-surface-lowest/60 px-4 py-5 sm:px-6">
              <article className={`max-w-none ${textWrapClass}`}>
                <ReactMarkdown remarkPlugins={[remarkGfm, hangulAdjacentStrongPlugin]} components={mdComponents}>
                  {previewValue}
                </ReactMarkdown>
              </article>
            </div>
          )}

          {isEditing && (
            <div className="mt-3 flex items-center justify-between gap-3">
              <span className={`hidden text-xs sm:block ${saveErrorMessage ? 'text-red-400' : 'text-text-primary/25'}`}>
                {saveErrorMessage ?? (hasUnsavedChanges ? '저장하지 않은 변경사항이 있어요.' : '변경사항이 없습니다.')}
              </span>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handleReset}
                  disabled={!hasUnsavedChanges || updateRefinedContentMutation.isPending}
                  className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium text-text-primary/40 transition-colors hover:text-text-primary disabled:opacity-30"
                >
                  <RotateCcw size={12} />
                  초기화
                </button>
                <button
                  type="button"
                  onClick={handleSave}
                  disabled={!hasUnsavedChanges || !normalizedEditValue || updateRefinedContentMutation.isPending}
                  className="flex items-center gap-1.5 rounded-tl-[10px] rounded-br-[10px] rounded-bl-md rounded-tr-md bg-action-accent/80 px-3 py-1.5 text-xs font-bold text-text-primary transition hover:bg-action-accent disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {updateRefinedContentMutation.isPending ? <Loader2 size={12} className="animate-spin" /> : <Save size={12} />}
                  {updateRefinedContentMutation.isPending ? '저장 중...' : '저장'}
                </button>
              </div>
            </div>
          )}

          {!isEditing && processedText.updatedAt && (
            <div className="mt-3 text-right text-xs text-text-primary/35">
              정제 일시 {formatDateTime(processedText.updatedAt)}
            </div>
          )}
        </>
      ) : isCheckingRefinedContent ? (
        <div className="mt-6 flex min-h-48 items-center justify-center rounded-2xl border border-text-secondary/5 glass-panel bg-surface-lowest/60 p-6 text-center">
          <div className="flex max-w-md flex-col items-center gap-3">
            <Loader2 size={24} className="animate-spin text-primary-signal" aria-hidden="true" />
            <p className="text-base font-semibold text-text-primary/70">1차 정제 데이터를 확인하는 중입니다.</p>
            <p className="text-sm leading-6 text-text-primary/40">
              원본 저장 직후라면 정제 작업이 아직 끝나지 않았을 수 있어요.
              <br />
              잠시 동안 자동으로 다시 확인합니다.
            </p>
          </div>
        </div>
      ) : (
        <div className="mt-6 flex min-h-48 items-center justify-center rounded-2xl border border-text-secondary/5 glass-panel bg-surface-lowest/60 p-6 text-center">
          <div className="flex max-w-md flex-col items-center gap-3">
            <BookOpen size={24} className="text-text-primary/25" aria-hidden="true" />
            <p className="text-base font-semibold text-text-primary/70">1차 정제 데이터가 없습니다.</p>
            <p className="text-sm leading-6 text-text-primary/40">
              현재 상세 API에서 정제된 텍스트가 제공되지 않아
              <br />원본 데이터와 최종 지식카드만 확인할 수 있습니다.
            </p>
          </div>
        </div>
      )}
    </section>
  );
}

function FinalKnowledgeCardSection({ finalCard }: { finalCard: KnowledgeCardDetailData['finalCard'] }) {
  return (
    <section className={sectionCardClass}>
      <SectionHeader
        icon={<CheckCircle2 size={18} />}
        title="최종 지식카드"
        description="최종 지식카드는 최초 생성 시점의 정제 텍스트를 기준으로 생성되었습니다."
      />

      <div className="mt-6">
        <div className="rounded-2xl border border-text-secondary/5 bg-surface-low p-5">
          <p className="text-sm font-bold tracking-wide text-text-primary/35">핵심 요약</p>
          <ul className="mt-4 space-y-3">
            {finalCard.keyPoints.map((point) => (
              <li key={point} className="flex min-w-0 gap-3 text-sm leading-7 text-text-primary/70">
                <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary-signal" />
                <span className={textWrapClass}>
                  <MarkdownInline>{point}</MarkdownInline>
                </span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

function MarkdownInline({ children }: { children: string }) {
  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm, hangulAdjacentStrongPlugin]}
      components={{
        p: ({ children: paragraphChildren }: { children?: React.ReactNode }) => <>{paragraphChildren}</>,
        strong: mdComponents.strong,
        em: mdComponents.em,
        code: mdComponents.code,
      }}
    >
      {normalizeHangulAdjacentStrong(children)}
    </ReactMarkdown>
  );
}

function DetailMetaPanel({ data, isLoadingRelated }: { data: KnowledgeCardDetailData; isLoadingRelated: boolean }) {
  const SourceIcon = getSourceIcon(data.source.type);

  return (
    <aside className="flex min-w-0 flex-col gap-5 lg:sticky lg:top-28 lg:self-start">
      <div className={panelCardClass}>
        <p className="text-xs font-bold uppercase tracking-wide text-primary-signal">원본 데이터</p>
        <div className="mt-4 space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-primary-signal/20 bg-primary-signal/5 px-2.5 py-1 text-xs font-bold uppercase tracking-wide text-primary-signal">
              <SourceIcon size={12} aria-hidden="true" />
              {data.source.type}
            </span>
          </div>
          <dl className="space-y-3 text-sm">
            <MetaRow label="수집 일시" value={data.source.collectedAt ? formatDateTime(data.source.collectedAt) : 'API 미제공'} />
          </dl>
          {data.source.url ? (
            <a
              href={data.source.url}
              target="_blank"
              rel="noreferrer"
              className="flex min-w-0 items-center gap-2 rounded-xl border border-text-secondary/5 bg-text-primary/[0.03] px-3 py-2.5 text-sm text-text-primary/60 transition hover:border-text-secondary/10 hover:text-text-primary"
            >
              <LinkIcon size={14} className="shrink-0 text-primary-signal" aria-hidden="true" />
              <span className="truncate">{data.source.url}</span>
            </a>
          ) : null}
          {data.source.previewUrl ? (
            <SourceImagePreview src={data.source.previewUrl} />
          ) : null}
          {data.source.type === 'TEXT' && data.source.rawContent ? (
            <ExpandableSourceText text={data.source.rawContent} />
          ) : null}
        </div>
      </div>

      <div className={panelCardClass}>
        <p className="mb-4 flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-text-primary/35">
          <Tags size={14} aria-hidden="true" />
          Tags
        </p>
        <TagList values={data.finalCard.tags} />
      </div>

      <div className={panelCardClass}>
        <p className="mb-4 text-sm font-bold tracking-wide text-text-primary/35">관련 카드</p>
        {isLoadingRelated ? (
          <p className="text-sm text-text-primary/40">관련 카드를 불러오는 중입니다.</p>
        ) : data.relatedCards.length > 0 ? (
          <div className="space-y-3">
            {data.relatedCards.map((card) => (
              <Link
                key={card.cardId}
                to={`/cards/${card.cardId}`}
                className="block rounded-xl border border-text-secondary/5 bg-text-primary/[0.03] p-4 transition hover:border-primary-signal/30 hover:bg-primary-signal/5"
              >
                <p className="text-xs font-bold text-primary-signal">{card.categoryName}</p>
                <p className={`mt-2 line-clamp-2 text-sm font-semibold leading-6 text-text-primary/70 ${textWrapClass}`}>{card.title}</p>
              </Link>
            ))}
          </div>
        ) : (
          <p className="text-sm text-text-primary/40">표시할 관련 카드가 없습니다.</p>
        )}
      </div>
    </aside>
  );
}

function DetailStatus({ title, description, tone = 'default' }: { title: string; description: string; tone?: 'default' | 'error' }) {
  const Icon = tone === 'error' ? AlertCircle : Loader2;

  return (
    <section className="grid min-h-[calc(100vh-14rem)] w-full place-items-center text-text-primary">
      <div className="flex max-w-xl flex-col items-center gap-4 rounded-tl-[32px] rounded-br-[32px] rounded-tr-2xl rounded-bl-2xl border border-text-secondary/5 glass-card bg-surface-container/80 p-8 text-center">
        <Icon className={tone === 'error' ? 'text-red-400' : 'animate-spin text-primary-signal'} size={28} aria-hidden="true" />
        <h1 className="text-xl font-bold">{title}</h1>
        <p className="text-sm leading-6 text-text-primary/45">{description}</p>
      </div>
    </section>
  );
}

function SectionHeader({ icon, title, description }: { icon: ReactNode; title: string; description: string }) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex min-w-0 items-center gap-3">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-tl-[18px] rounded-br-[18px] rounded-tr-md rounded-bl-md bg-primary-signal/10 text-primary-signal">
          {icon}
        </span>
        <h2 className="text-xl font-bold text-text-primary">{title}</h2>
      </div>
      <p className="w-full text-[16px] leading-7 text-text-primary/50">{description}</p>
    </div>
  );
}


function MetaRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex min-w-0 items-start justify-between gap-4">
      <dt className="shrink-0 text-text-primary/35">{label}</dt>
      <dd className="min-w-0 truncate text-right font-medium text-text-primary/65">{value}</dd>
    </div>
  );
}

function ExpandableSourceText({ text }: { text: string }) {
  const [expanded, setExpanded] = useState(false);
  const lineCount = text.split('\n').length;
  const isLong = text.length > 360 || lineCount > 6;

  return (
    <div className="min-w-0 rounded-xl border border-text-secondary/5 glass-panel bg-surface-lowest/60 p-3">
      <p
        className={[
          `whitespace-pre-wrap text-sm leading-6 text-text-primary/50 ${textWrapClass}`,
          !expanded && isLong ? 'line-clamp-6' : '',
        ].join(' ')}
      >
        {text}
      </p>
      {isLong && (
        <button
          type="button"
          onClick={() => setExpanded(!expanded)}
          className="mt-2 text-xs font-medium text-action-accent transition-colors hover:text-action-accent/80"
        >
          {expanded ? '접기' : '전체 보기'}
        </button>
      )}
    </div>
  );
}

function SourceImagePreview({ src }: { src: string }) {
  const [expanded, setExpanded] = useState(false);
  const [isFullImageLoaded, setIsFullImageLoaded] = useState(false);

  useEffect(() => {
    setIsFullImageLoaded(false);
  }, [src, expanded]);

  const preloadFullImage = () => {
    const image = new window.Image();
    image.src = src;
    void image.decode?.().catch(() => undefined);
  };

  return (
    <>
      <button
        type="button"
        onClick={() => setExpanded(true)}
        onMouseEnter={preloadFullImage}
        onFocus={preloadFullImage}
        className="group/img w-full rounded-xl border border-text-secondary/5 glass-panel bg-surface-lowest/60 p-1 transition hover:border-text-secondary/10"
      >
        <img
          src={src}
          alt=""
          decoding="async"
          className="max-h-48 w-full rounded-lg object-contain"
        />
        <span className="mt-1 block text-center text-[10px] text-text-primary/25 transition-colors group-hover/img:text-text-primary/40">
          클릭하여 원본 보기
        </span>
      </button>

      {expanded && (
        <div
          className="fixed inset-0 z-[80] flex items-center justify-center bg-scrim/80 px-4 pb-4 pt-[calc(var(--dashboard-nav-offset)+1rem)] backdrop-blur-sm sm:px-6 sm:pb-6"
          onClick={() => setExpanded(false)}
          onKeyDown={(e) => e.key === 'Escape' && setExpanded(false)}
          role="button"
          tabIndex={0}
        >
          {!isFullImageLoaded ? (
            <div className="absolute inset-x-4 top-[calc(var(--dashboard-nav-offset)+1rem)] bottom-4 flex items-center justify-center rounded-xl border border-text-secondary/10 bg-surface-container/70 shadow-2xl sm:inset-x-6 sm:bottom-6">
              <div className="flex flex-col items-center gap-3">
                <Loader2 size={24} className="animate-spin text-primary-signal" aria-hidden="true" />
                <span className="text-xs font-semibold text-text-primary/45">원본 이미지를 불러오는 중...</span>
              </div>
            </div>
          ) : null}
          <img
            src={src}
            alt=""
            loading="eager"
            decoding="async"
            onLoad={() => setIsFullImageLoaded(true)}
            onError={() => setIsFullImageLoaded(true)}
            onClick={(event) => event.stopPropagation()}
            className={[
              'max-h-[calc(100dvh-var(--dashboard-nav-offset)-2rem)] max-w-full rounded-xl object-contain shadow-2xl transition-opacity duration-150',
              isFullImageLoaded ? 'opacity-100' : 'opacity-0',
            ].join(' ')}
          />
        </div>
      )}
    </>
  );
}

function ToolbarButton({ icon, title, onClick }: { icon: ReactNode; title: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={title}
      className="flex h-7 w-7 shrink-0 items-center justify-center rounded transition-colors hover:bg-text-primary/10 hover:text-text-primary"
    >
      {icon}
    </button>
  );
}

function TagList({ values, subtle = false }: { values: string[]; subtle?: boolean }) {
  if (values.length === 0) {
    return <p className="text-sm text-text-primary/40">표시할 항목이 없습니다.</p>;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {values.map((value) => (
        <span
          key={value}
          className={[
            `rounded-md border px-3 py-1.5 text-sm font-medium ${textWrapClass}`,
            subtle
              ? 'border-text-secondary/5 bg-text-primary/[0.03] text-text-primary/50'
              : 'border-primary-signal/15 bg-primary-signal/5 text-text-secondary',
          ].join(' ')}
        >
          {value.startsWith('#') ? value : `#${value}`}
        </span>
      ))}
    </div>
  );
}

function toDetailData(
  cardId: string,
  detail: KnowledgeCardDetailResponse,
  relatedCards: ArchiveRelatedCardResponse[],
): KnowledgeCardDetailData {
  const sourceContent = detail.sourceContent ?? '';
  const tags = [...new Set(detail.tags ?? [])];

  return {
    cardId,
    source: {
      type: detail.sourceType,
      url: detail.sourceType === 'LINK' ? sourceContent : null,
      previewUrl: detail.sourceType === 'IMAGE' ? sourceContent : null,
      rawContent: detail.sourceType === 'TEXT' ? sourceContent : '',
      collectedAt: detail.collectedAt,
    },
    processedText: {
      refinedContent: detail.refinedContent ?? '',
      updatedAt: null,
    },
    finalCard: {
      title: detail.title,
      summary: detail.summary ?? '요약 내용이 아직 생성되지 않았습니다.',
      keyPoints: toKeyPoints(detail.summary),
      categoryName: detail.categoryName,
      tags,
      relatedKeywords: toRelatedKeywords(detail.categoryName, tags),
      createdAt: null,
    },
    relatedCards: relatedCards.map((card) => ({
      cardId: card.cardId,
      title: card.title,
      categoryName: card.categoryName,
    })),
  };
}

function toKeyPoints(summary: string | null) {
  if (!summary?.trim()) return ['요약 내용이 아직 생성되지 않았습니다.'];

  const points = summary
    .split(/(?<=[.!?。])\s+|\n+/)
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(0, 3);

  return points.length > 0 ? points : [summary.trim()];
}

function toRelatedKeywords(categoryName: string, tags: string[]) {
  return Array.from(new Set([categoryName, ...tags].filter(Boolean)));
}

function getSourceIcon(type: KnowledgeSourceType) {
  if (type === 'IMAGE' || type === 'OCR') return Image;
  if (type === 'LINK') return LinkIcon;
  return FileText;
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}
