import type { DragEvent, MouseEvent } from 'react';
import { useState } from 'react';
import { CloudUpload, RotateCcw, Sparkles } from 'lucide-react';
import { CurvedButton } from '@san/ui/components/Button/CurvedButton';
import type { PendingScrap } from '@extension/types';
import { ToastMessage } from '../feedback/ToastMessage';

interface DropZoneProps {
  pendingScrap: PendingScrap | null;
  onTextDrop: (text: string) => void | Promise<void>;
  onImageDrop: (file: File) => void | Promise<void>;
  onSave: () => void | Promise<void>;
  onClear: () => void;
  isSaving?: boolean;
  savingLabel?: string;
  saveLabel?: string;
  saveError?: string | null;
  saveNotice?: string | null;
  canSave?: boolean;
  authNotice?: string | null;
  onLogin?: () => void;
}

const MIN_TEXT_LENGTH = 10;

export const DropZone = ({
  pendingScrap,
  onTextDrop,
  onImageDrop,
  onSave,
  onClear,
  isSaving = false,
  savingLabel = '저장 중...',
  saveLabel = 'Save',
  saveError = null,
  saveNotice = null,
  canSave = true,
  authNotice = null,
  onLogin,
}: DropZoneProps) => {
  const [isOver, setIsOver] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [manualText, setManualText] = useState('');
  const [error, setError] = useState<string | null>(null);

  const captureText = async (text: string) => {
    const nextText = text.trim();

    if (nextText.length < MIN_TEXT_LENGTH) {
      setError('10자 이상의 텍스트나 링크를 입력해 주세요.');
      return;
    }

    await onTextDrop(nextText);
    setManualText('');
    setIsEditing(false);
  };

  const handleDrop = async (event: DragEvent<HTMLElement>) => {
    event.preventDefault();
    setIsOver(false);
    setError(null);

    const imageFile = Array.from(event.dataTransfer.files).find((file) =>
      file.type.startsWith('image/')
    );

    if (imageFile) {
      await onImageDrop(imageFile);
      setIsEditing(false);
      return;
    }

    const droppedText = event.dataTransfer.getData('text/plain');
    await captureText(droppedText);
  };

  const handleManualSubmit = async (event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    setError(null);
    await captureText(manualText);
  };

  const handleCancelEdit = (event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    setManualText('');
    setError(null);
    setIsEditing(false);
  };

  const previewContent = pendingScrap?.raw_content ?? pendingScrap?.title ?? '';
  const imagePreviewUrl = pendingScrap?.image_preview_url ?? undefined;
  const isImageCapture = Boolean(imagePreviewUrl);

  return (
    <div className="flex flex-col gap-3">
      <section
        data-tour-id="capture-drop-zone"
        onDragOver={(event) => {
          event.preventDefault();
          setIsOver(true);
        }}
        onDragLeave={() => setIsOver(false)}
        onDrop={handleDrop}
        className={[
          'relative flex h-[190px] w-full flex-col overflow-hidden rounded-leaf border-2 transition-all duration-300',
          isOver
            ? 'border-dashed border-action-accent bg-action-accent/10 shadow-neon glow-neon'
            : pendingScrap
              ? 'border-solid border-action-accent/40 bg-surface-container/90 shadow-neon'
              : 'border-dashed border-action-accent/30 hover:border-action-accent/50 hover:bg-surface-container/90 bg-surface-container/80',
          !pendingScrap && !isEditing ? 'cursor-text items-center justify-center' : 'cursor-default',
        ].join(' ')}
        onClick={() => !pendingScrap && setIsEditing(true)}
      >
        {isEditing ? (
          <div className="flex h-full w-full flex-col gap-3 p-5" onClick={(event) => event.stopPropagation()}>
            <textarea
              value={manualText}
              onChange={(event) => setManualText(event.target.value)}
              autoFocus
              placeholder="여기에 직접 입력하거나 텍스트, 이미지, 링크를 드래그하세요."
              className="w-full flex-1 resize-none bg-transparent text-body-sm leading-6 text-text-primary outline-none placeholder:text-text-secondary/45"
            />

            <div className="flex items-center justify-end gap-3 shrink-0">
              <button
                type="button"
                onClick={handleCancelEdit}
                className="rounded-full px-3 py-1.5 text-body-sm-bold text-text-secondary transition hover:bg-surface-lowest/80 bg-surface-lowest/70 hover:text-text-primary"
              >
                취소
              </button>
              <button
                type="button"
                onClick={handleManualSubmit}
                className="rounded-full border border-action-accent/35 bg-action-accent/10 px-4 py-1.5 text-body-sm-bold text-action-accent transition hover:border-action-accent/70 hover:bg-action-accent/15 active:translate-y-px"
              >
                완료
              </button>
            </div>
          </div>
        ) : pendingScrap ? (
          <div className="flex h-full flex-col p-5">
            <div className="mb-3 flex items-center justify-between gap-3 shrink-0">
              <div className="flex items-center gap-2">
                <Sparkles size={18} className="text-action-accent" aria-hidden="true" />
                <span className="text-caption-bold uppercase tracking-wider text-text-secondary">
                  Captured
                </span>
              </div>

              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); onClear(); }}
                className="inline-flex items-center gap-1.5 text-caption-bold uppercase tracking-wider text-action-accent transition hover:text-text-primary active:translate-y-px"
              >
                <RotateCcw size={12} aria-hidden="true" />
                Clear
              </button>
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto rounded-xl border border-primary-signal/10 bg-background/40 p-4 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
              {isImageCapture ? (
                <img
                  src={imagePreviewUrl}
                  alt={pendingScrap.title}
                  className="max-h-24 w-full rounded-lg object-cover mb-3"
                />
              ) : null}

              {previewContent ? (
                <p className="whitespace-pre-wrap text-body-sm leading-6 text-text-secondary">
                  {previewContent}
                </p>
              ) : null}
            </div>

            <div className="mt-4 flex justify-center shrink-0">
              {canSave ? (
                <CurvedButton
                  onClick={(e) => { e.stopPropagation(); onSave(); }}
                  disabled={isSaving}
                  size="md"
                  data-tour-id="capture-save-button"
                  className="px-8 !rounded-xl !shadow-none"
                >
                  {isSaving ? savingLabel : saveLabel}
                </CurvedButton>
              ) : authNotice ? (
                <div className="rounded-leaf border border-action-accent/20 bg-background/70 p-3 text-center">
                  <p className="text-caption text-text-secondary">{authNotice}</p>
                  {onLogin && (
                    <CurvedButton
                      onClick={(e) => {
                        e.stopPropagation();
                        onLogin();
                      }}
                      fullWidth
                      size="md"
                      className="mt-2"
                    >
                      로그인
                    </CurvedButton>
                  )}
                </div>
              ) : null}
            </div>
          </div>
        ) : (
          <div className="flex flex-col items-center gap-3 p-5 text-center">
            <CloudUpload size={34} className="text-action-accent" aria-hidden="true" />
            <p className="text-body-main font-medium text-text-primary">
              {isOver ? '여기에 놓아 지식 심기' : '여기로 드래그하여 지식 심기'}
            </p>
            <p className="text-caption text-text-secondary">
              Drag text, image or link to archive
            </p>
          </div>
        )}
      </section>

      {(error || saveError) && (
        <ToastMessage tone="error" message={error ?? saveError ?? ''} />
      )}

      {saveNotice && (
        <ToastMessage tone="success" message={saveNotice} />
      )}
    </div>
  );
};
