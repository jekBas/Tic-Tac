package com.example.tictac.session.client;

import com.example.tictac.common.dto.GameStateDto;
import com.example.tictac.common.dto.MoveRequest;
import com.example.tictac.session.exception.GameEngineTransientException;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GameEngineClient extends RestClientBase {

	public GameEngineClient(RestClient gameEngineRestClient) {
		super(gameEngineRestClient);
	}

	@Override
	protected String serviceName() {
		return "Game Engine";
	}

	@Retryable(retryFor = GameEngineTransientException.class,
			backoff = @Backoff(delay = 500, multiplier = 2),
			listeners = "gameEngineRetryListener")
	public GameStateDto createGame() {
		return execute("POST /games", () -> restClient.post()
				.uri("/games")
				.retrieve()
				.onStatus(HttpStatusCode::isError, this::onErrorResponse)
				.body(GameStateDto.class));
	}

	@Retryable(retryFor = GameEngineTransientException.class,
			backoff = @Backoff(delay = 500, multiplier = 2),
			listeners = "gameEngineRetryListener")
	public GameStateDto applyMove(String gameId, MoveRequest moveRequest) {
		return execute("POST /games/" + gameId + "/move", () -> restClient.post()
				.uri("/games/{gameId}/move", gameId)
				.body(moveRequest)
				.retrieve()
				.onStatus(HttpStatusCode::isError, this::onErrorResponse)
				.body(GameStateDto.class));
	}

	@Retryable(retryFor = GameEngineTransientException.class,
			backoff = @Backoff(delay = 500, multiplier = 2),
			listeners = "gameEngineRetryListener")
	public GameStateDto getGame(String gameId) {
		return execute("GET /games/" + gameId, () -> restClient.get()
				.uri("/games/{gameId}", gameId)
				.retrieve()
				.onStatus(HttpStatusCode::isError, this::onErrorResponse)
				.body(GameStateDto.class));
	}
}
