import { MoveRecord } from '../types/session';

interface MoveHistoryProps {
  moves: MoveRecord[];
  currentMoveIndex: number;
}

const POSITION_LABELS = ['Top-Left', 'Top', 'Top-Right', 'Left', 'Center', 'Right', 'Bot-Left', 'Bottom', 'Bot-Right'];

export function MoveHistory({ moves, currentMoveIndex }: MoveHistoryProps) {
  if (moves.length === 0) return null;

  return (
    <div className="move-history">
      <h3>Move History</h3>
      <div className="move-list">
        {moves.map((move, index) => (
          <div
            key={move['move-number']}
            className={`move-entry ${index <= currentMoveIndex ? 'visible' : 'pending'}`}
          >
            <span className="move-number">#{move['move-number']}</span>
            <span className={`move-player player-${move.player.toLowerCase()}`}>{move.player}</span>
            <span className="move-position">{POSITION_LABELS[move.position]}</span>
            <span className="move-result">{move['resulting-status'].replace('_', ' ')}</span>
          </div>
        ))}
      </div>
    </div>
  );
}