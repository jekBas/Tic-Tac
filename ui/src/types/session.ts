export type Player = 'X' | 'O';

export type GameStatus = 'NEW' | 'IN_PROGRESS' | 'X_WON' | 'O_WON' | 'DRAW';

export type SessionStatus = 'CREATED' | 'IN_PROGRESS' | 'SIMULATING' | 'COMPLETED' | 'FAILED';

export type SessionMode = 'SIMULATION' | 'PLAYER_VS_COMPUTER' | 'PLAYER_VS_STUPID_COMPUTER';

export type CellValue = 'X' | 'O' | null;

export interface GameState {
  'game-id': string;
  board: CellValue[];
  status: GameStatus;
  winner: Player | null;
  'next-player': Player | null;
}

export interface MoveRecord {
  'move-number': number;
  player: Player;
  position: number;
  'resulting-status': GameStatus;
  'board-after-move': CellValue[];
  'created-at': string;
}

export interface SessionResponse {
  'session-id': string;
  'game-id': string | null;
  status: SessionStatus;
  mode: SessionMode | null;
  'human-player': Player | null;
  'current-game-state': GameState | null;
  'move-history': MoveRecord[];
  'created-at': string;
  'updated-at': string;
  'failure-reason': string | null;
}

export interface SessionEvent {
  'session-id': string;
  'game-id': string | null;
  'session-status': SessionStatus;
  'current-game-state': GameState | null;
  'latest-move': MoveRecord | null;
  'move-history': MoveRecord[];
  'failure-reason': string | null;
}