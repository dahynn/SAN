import type { ButtonHTMLAttributes, ReactNode } from 'react';

type ButtonTone = 'primary' | 'subtle' | 'ghost';
type ButtonSize = 'sm' | 'md' | 'lg';

interface CurvedButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode;
  leadingIcon?: ReactNode;
  trailingIcon?: ReactNode;
  fullWidth?: boolean;
  tone?: ButtonTone;
  size?: ButtonSize;
}

const buttonToneStyles: Record<ButtonTone, string> = {
  primary:
    'bg-gradient-to-r from-action-accent to-surface-container text-text-primary shadow-neon hover:opacity-95 hover:shadow-neon active:shadow-neon-sm',

  subtle:
    'border border-action-accent/20 bg-action-accent/10 text-action-accent shadow-neon-sm hover:bg-action-accent/15 hover:shadow-neon',

  ghost:
    'text-text-secondary hover:bg-surface-container/40 hover:shadow-neon',
};

const buttonSizeStyles: Record<ButtonSize, string> = {
  sm: 'px-4 py-2 text-body-sm',
  md: 'px-5 py-3 text-body-main',
  lg: 'px-6 py-4 text-body-lg',
};

export function CurvedButton({
  children,
  leadingIcon,
  trailingIcon,
  fullWidth = false,
  tone = 'primary',
  size = 'md',
  type = 'button',
  className = '',
  disabled,
  ...props
}: CurvedButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled}
      className={[
        'inline-flex items-center justify-center gap-2 whitespace-nowrap',
        'rounded-leaf',
        'font-bold leading-none',
        'transition-all duration-150 ease-out',
        'active:translate-y-px active:scale-95',
        'disabled:cursor-not-allowed disabled:opacity-60',
        fullWidth ? 'w-full' : 'w-fit',
        buttonToneStyles[tone],
        buttonSizeStyles[size],
        className,
      ].join(' ')}
      {...props}
    >
      {leadingIcon && (
        <span className="flex shrink-0 items-center justify-center">
          {leadingIcon}
        </span>
      )}

      <span className="block whitespace-nowrap">{children}</span>

      {trailingIcon && (
        <span className="flex shrink-0 items-center justify-center">
          {trailingIcon}
        </span>
      )}
    </button>
  );
}
