import { CellValue } from '../types/session';

interface BoardProps {
  board: CellValue[];
  interactive?: boolean;
  onCellClick?: (index: number) => void;
}

export function Board({ board, interactive, onCellClick }: BoardProps) {
  return (
    <div className="board">
      {board.map((cell, index) => (
        <div
          key={index}
          className={`cell ${cell ? `cell-${cell.toLowerCase()}` : ''} ${interactive && !cell ? 'cell-clickable' : ''}`}
          onClick={() => interactive && !cell && onCellClick?.(index)}
        >
          {cell ?? ''}
        </div>
      ))}
    </div>
  );
}