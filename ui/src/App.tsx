import { useCallback, useEffect, useRef, useState } from 'react';
import { createSession, simulateSession } from './api/sessionApi';
import { subscribeToSession } from './api/sessionSocket';
import { Board } from './components/Board';
import { ErrorMessage } from './components/ErrorMessage';
import { MoveHistory } from './components/MoveHistory';
import { StatusPanel } from './components/StatusPanel';
import { CellValue, GameState, MoveRecord, SessionEvent, SessionStatus } from './types/session';

const EMPTY_BOARD: CellValue[] = Array(9).fill(null);

export function App() {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [sessionStatus, setSessionStatus] = useState<SessionStatus>('CREATED');
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [displayBoard, setDisplayBoard] = useState<CellValue[]>(EMPTY_BOARD);
  const [moves, setMoves] = useState<MoveRecord[]>([]);
  const [currentMoveIndex, setCurrentMoveIndex] = useState(-1);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const unsubscribeRef = useRef<(() => void) | null>(null);

  const cleanup = useCallback(() => {
    if (unsubscribeRef.current) {
      unsubscribeRef.current();
      unsubscribeRef.current = null;
    }
  }, []);

  useEffect(() => {
    return cleanup;
  }, [cleanup]);

  function handleSessionEvent(event: SessionEvent) {
    setSessionStatus(event['session-status']);
    setGameState(event['current-game-state']);
    setMoves(event['move-history']);

    if (event['current-game-state']) {
      setDisplayBoard(event['current-game-state'].board);
    }

    if (event['move-history'].length > 0) {
      setCurrentMoveIndex(event['move-history'].length - 1);
    }

    if (event['session-status'] === 'COMPLETED' || event['session-status'] === 'FAILED') {
      setIsLoading(false);
      if (event['failure-reason']) {
        setError(event['failure-reason']);
      }
    }
  }

  async function handleStartSimulation() {
    setError(null);
    setSessionId(null);
    setSessionStatus('CREATED');
    setGameState(null);
    setDisplayBoard(EMPTY_BOARD);
    setMoves([]);
    setCurrentMoveIndex(-1);
    setIsLoading(true);
    cleanup();

    try {
      const created = await createSession();
      const id = created['session-id'];
      setSessionId(id);
      setSessionStatus(created.status);

      unsubscribeRef.current = subscribeToSession(
        id,
        handleSessionEvent,
        (wsError) => setError(wsError),
      );

      simulateSession(id).catch((err) => {
        const message = err instanceof Error ? err.message : 'Simulation request failed';
        setError(message);
        setIsLoading(false);
      });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'An unexpected error occurred';
      setError(message);
      setIsLoading(false);
    }
  }

  return (
    <div className="app">
      <h1>Tic Tac Toe Simulation</h1>

      <button
        className="start-button"
        onClick={handleStartSimulation}
        disabled={isLoading}
      >
        {isLoading ? 'Simulating...' : 'Start Simulation'}
      </button>

      {error && <ErrorMessage message={error} onDismiss={() => setError(null)} />}

      {sessionId && (
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