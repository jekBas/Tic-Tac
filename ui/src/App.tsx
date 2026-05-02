import { useCallback, useEffect, useRef, useState } from 'react';
import { createSession, createPlayerVsComputerSession, simulateSession, submitHumanMove } from './api/sessionApi';
import { subscribeToSession } from './api/sessionSocket';
import { Board } from './components/Board';
import { ErrorMessage } from './components/ErrorMessage';
import { MoveHistory } from './components/MoveHistory';
import { StatusPanel } from './components/StatusPanel';
import { CellValue, GameState, MoveRecord, Player, SessionEvent, SessionMode, SessionStatus } from './types/session';

const EMPTY_BOARD: CellValue[] = Array(9).fill(null);

export function App() {
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [sessionStatus, setSessionStatus] = useState<SessionStatus>('CREATED');
  const [sessionMode, setSessionMode] = useState<SessionMode | null>(null);
  const [humanPlayer, setHumanPlayer] = useState<Player | null>(null);
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [displayBoard, setDisplayBoard] = useState<CellValue[]>(EMPTY_BOARD);
  const [moves, setMoves] = useState<MoveRecord[]>([]);
  const [currentMoveIndex, setCurrentMoveIndex] = useState(-1);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showPlayerChoice, setShowPlayerChoice] = useState(false);
  const [pendingMode, setPendingMode] = useState<'PLAYER_VS_COMPUTER' | 'PLAYER_VS_STUPID_COMPUTER' | null>(null);
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

  function resetState() {
    setError(null);
    setSessionId(null);
    setSessionStatus('CREATED');
    setSessionMode(null);
    setHumanPlayer(null);
    setGameState(null);
    setDisplayBoard(EMPTY_BOARD);
    setMoves([]);
    setCurrentMoveIndex(-1);
    setShowPlayerChoice(false);
    setPendingMode(null);
    cleanup();
  }

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
    resetState();
    setIsLoading(true);
    setSessionMode('SIMULATION');

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

  function handlePlayAgainstComputer() {
    resetState();
    setPendingMode('PLAYER_VS_COMPUTER');
    setShowPlayerChoice(true);
  }

  function handlePlayAgainstStupidComputer() {
    resetState();
    setPendingMode('PLAYER_VS_STUPID_COMPUTER');
    setShowPlayerChoice(true);
  }

  async function handlePlayerChoice(player: Player) {
    setShowPlayerChoice(false);
    setIsLoading(true);
    const mode = pendingMode ?? 'PLAYER_VS_COMPUTER';
    setSessionMode(mode);
    setHumanPlayer(player);

    try {
      const created = await createPlayerVsComputerSession(
        player, mode === 'PLAYER_VS_STUPID_COMPUTER' ? 'STUPID' : 'SMART');
      const id = created['session-id'];
      setSessionId(id);
      setSessionStatus(created.status);
      setGameState(created['current-game-state']);
      setMoves(created['move-history']);

      if (created['current-game-state']) {
        setDisplayBoard(created['current-game-state'].board);
      }
      if (created['move-history'].length > 0) {
        setCurrentMoveIndex(created['move-history'].length - 1);
      }

      unsubscribeRef.current = subscribeToSession(
        id,
        handleSessionEvent,
        (wsError) => setError(wsError),
      );

      setIsLoading(false);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'An unexpected error occurred';
      setError(message);
      setIsLoading(false);
    }
  }

  async function handleCellClick(index: number) {
    if (!sessionId || isLoading) return;
    if (sessionStatus !== 'IN_PROGRESS') return;

    setIsLoading(true);
    setError(null);

    try {
      const response = await submitHumanMove(sessionId, index);
      setSessionStatus(response.status);
      setGameState(response['current-game-state']);
      setMoves(response['move-history']);

      if (response['current-game-state']) {
        setDisplayBoard(response['current-game-state'].board);
      }
      if (response['move-history'].length > 0) {
        setCurrentMoveIndex(response['move-history'].length - 1);
      }

      setIsLoading(false);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Move failed';
      setError(message);
      setIsLoading(false);
    }
  }

  const isHumanTurn = (sessionMode === 'PLAYER_VS_COMPUTER' || sessionMode === 'PLAYER_VS_STUPID_COMPUTER')
    && sessionStatus === 'IN_PROGRESS'
    && !isLoading
    && gameState?.['next-player'] === humanPlayer;

  return (
    <div className="app">
      <h1>Tic Tac Toe</h1>

      <div className="button-row">
        <button
          className="start-button"
          onClick={handleStartSimulation}
          disabled={isLoading}
        >
          {isLoading && sessionMode === 'SIMULATION' ? 'Simulating...' : 'Start Simulation'}
        </button>
        <button
          className="start-button pvc-button"
          onClick={handlePlayAgainstComputer}
          disabled={isLoading}
        >
          Play against Computer
        </button>
        <button
          className="start-button pvc-button"
          onClick={handlePlayAgainstStupidComputer}
          disabled={isLoading}
        >
          Play with Stupid Computer
        </button>
      </div>

      {showPlayerChoice && (
        <div className="player-choice">
          <p>Choose your side:</p>
          <div className="choice-buttons">
            <button className="choice-button choice-x" onClick={() => handlePlayerChoice('X')}>
              Play as X (first)
            </button>
            <button className="choice-button choice-o" onClick={() => handlePlayerChoice('O')}>
              Play as O (second)
            </button>
          </div>
        </div>
      )}

      {error && <ErrorMessage message={error} onDismiss={() => setError(null)} />}

      {sessionId && (
        <>
          <StatusPanel
            sessionStatus={sessionStatus}
            gameStatus={gameState?.status ?? null}
            winner={gameState?.winner ?? null}
            nextPlayer={gameState?.['next-player'] ?? null}
          />
          <Board
            board={displayBoard}
            interactive={isHumanTurn}
            onCellClick={handleCellClick}
          />
          <MoveHistory moves={moves} currentMoveIndex={currentMoveIndex} />
        </>
      )}
    </div>
  );
}