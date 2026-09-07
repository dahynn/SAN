import { useEffect, useState } from 'react';
import { authTokenStorage } from '@dashboard/api/client';
import { FeedbackDialog } from './FeedbackDialog';
import feedbackMascotUrl from '@ui/assets/icons/feedback_full.png';

export function FloatingFeedbackButton() {
  const [isOpen, setIsOpen] = useState(false);
  const [hasToken, setHasToken] = useState(false);

  useEffect(() => {
    let ignore = false;

    authTokenStorage.getToken()
      .then((token) => {
        if (!ignore) setHasToken(Boolean(token));
      })
      .catch(() => {
        if (!ignore) setHasToken(false);
      });

    return () => {
      ignore = true;
    };
  }, []);

  if (!hasToken) return null;

  return (
    <>
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        aria-label="Send feedback"
        className="san-feedback-float fixed bottom-6 right-6 z-40 h-[112px] w-[118px] rounded-[28px] text-text-primary transition hover:-translate-y-0.5 focus:outline-none focus-visible:ring-2 focus-visible:ring-action-accent/70"
      >
        <span className="san-feedback-mascot" aria-hidden="true">
          <span className="san-feedback-mascot__image-wrap">
            <img src={feedbackMascotUrl} alt="" className="san-feedback-mascot__image" />
          </span>
          <span className="san-feedback-bubble">?</span>
        </span>
      </button>
      <FeedbackDialog open={isOpen} onClose={() => setIsOpen(false)} />
    </>
  );
}
