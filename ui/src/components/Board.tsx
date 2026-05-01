import { CellValue } from '../types/session';

interface BoardProps {
  board: CellValue[];
}

export function Board({ board }: BoardProps) {
  return (
    <div className="board">
      {board.map((cell, index) => (
        <div key={index} className={`cell ${cell ? `cell-${cell.toLowerCase()}` : ''}`}>
          {cell ?? ''}
        </div>
      ))}
    </div>
  );
}