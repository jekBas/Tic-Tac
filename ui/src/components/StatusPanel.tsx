import { GameStatus, Player, SessionStatus } from '../types/session';

interface StatusPanelProps {
  sessionStatus: SessionStatus;
  gameStatus: GameStatus | null;
  winner: Player | null;
  nextPlayer: Player | null;
}

const SESSION_LABELS: Record<SessionStatus, string> = {
  CREATED: 'Created',
  SIMULATING: 'Simulating...',
  COMPLETED: 'Completed',
  FAILED: 'Failed',
};

const GAME_LABELS: Record<GameStatus, string> = {
  NEW: 'New',
  IN_PROGRESS: 'In Progress',
  X_WON: 'X Won!',
  O_WON: 'O Won!',
  DRAW: 'Draw',
};

export function StatusPanel({ sessionStatus, gameStatus, winner, nextPlayer }: StatusPanelProps) {
  return (
    <div className="status-panel">
      <div className="status-row">
        <span className="status-label">Session:</span>
        <span className={`status-value session-${sessionStatus.toLowerCase()}`}>
          {SESSION_LABELS[sessionStatus]}
        </span>
      </div>
      {gameStatus && (
        <div className="status-row">
          <span className="status-label">Game:</span>
          <span className="status-value">{GAME_LABELS[gameStatus]}</span>
        </div>
      )}
      {winner && (
        <div className="status-row">
          <span className="status-label">Winner:</span>
          <span className="status-value winner">{winner}</span>
        </div>
      )}
      {nextPlayer && !winner && gameStatus === 'IN_PROGRESS' && (
        <div className="status-row">
          <span className="status-label">Next Player:</span>
          <span className="status-value">{nextPlayer}</span>
        </div>
      )}
    </div>
  );
}