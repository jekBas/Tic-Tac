import { useCallback, useEffect, useRef, useState } from 'react';
import { createSession, simulateSession } from './api/sessionApi';
import { Board } from './components/Board';
import { ErrorMessage } from './components/ErrorMessage';
import { MoveHistory } from './components/MoveHistory';
import { StatusPanel } from './components/StatusPanel';
import { CellValue, MoveRecord, SessionResponse, SessionStatus } from './types/session';

const EMPTY_BOARD: CellValue[] = Array(9).fill(null);
const REPLAY_DELAY_MS = 600;

export function App() {
  const [session, setSession] = useState<SessionResponse | null>(null);
  const [displayBoard, setDisplayBoard] = useState<CellValue[]>(EMPTY_BOARD);
  const [currentMoveIndex, setCurrentMoveIndex] = useState(-1);
  const [isLoading, setIsLoading] = useState(false);
  const [isReplaying, setIsReplaying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const replayTimer = useRef<number | null>(null);

  const clearReplayTimer = useCallback(() => {
    if (replayTimer.current !== null) {
      clearTimeout(replayTimer.current);
      replayTimer.current = null;
    }
  }, []);

  useEffect(() => {
    return clearReplayTimer;
  }, [clearReplayTimer]);

  function replayMoves(moves: MoveRecord[]) {
    if (moves.length === 0) return;

    setIsReplaying(true);
    setDisplayBoard(EMPTY_BOARD);
    setCurrentMoveIndex(-1);

    let step = 0;

    function nextStep() {
      if (step >= moves.length) {
        setIsReplaying(false);
        return;
      }

      setDisplayBoard(moves[step]['board-after-move']);
      setCurrentMoveIndex(step);
      step++;
      replayTimer.current = window.setTimeout(nextStep, REPLAY_DELAY_MS);
    }

    replayTimer.current = window.setTimeout(nextStep, REPLAY_DELAY_MS);
  }

  async function handleStartSimulation() {
    setError(null);
    setSession(null);
    setDisplayBoard(EMPTY_BOARD);
    setCurrentMoveIndex(-1);
    setIsLoading(true);
    clearReplayTimer();
    setIsReplaying(false);

    try {
      const created = await createSession();
      setSession(created);

      const simulated = await simulateSession(created['session-id']);
      setSession(simulated);

      const moves = simulated['move-history'];
      if (moves && moves.length > 0) {
        replayMoves(moves);
      } else if (simulated['current-game-state']) {
        setDisplayBoard(simulated['current-game-state'].board);
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'An unexpected error occurred';
      setError(message);
    } finally {
      setIsLoading(false);
    }
  }

  const sessionStatus: SessionStatus = session?.status ?? 'CREATED';
  const gameState = session?.['current-game-state'] ?? null;
  const moves = session?.['move-history'] ?? [];

  return (
    <div className="app">
      <h1>Tic Tac Toe Simulation</h1>

      <button
        className="start-button"
        onClick={handleStartSimulation}
        disabled={isLoading || isReplaying}
      >
        {isLoading ? 'Simulating...' : 'Start Simulation'}
      </button>

      {error && <ErrorMessage message={error} onDismiss={() => setError(null)} />}

      {session && (
        <>
          <StatusPanel
            sessionStatus={sessionStatus}
            gameStatus={gameState?.status ?? null}
            winner={gameState?.winner ?? null}
            nextPlayer={gameState?.['next-player'] ?? null}
          />
          <Board board={displayBoard} />
          <MoveHistory moves={moves} currentMoveIndex={currentMoveIndex} />
        </>
      )}
    </div>
  );
}