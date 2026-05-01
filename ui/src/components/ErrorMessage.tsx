interface ErrorMessageProps {
  message: string;
  onDismiss: () => void;
}

export function ErrorMessage({ message, onDismiss }: ErrorMessageProps) {
  return (
    <div className="error-message">
      <span className="error-text">{message}</span>
      <button className="error-dismiss" onClick={onDismiss}>
        Dismiss
      </button>
    </div>
  );
}