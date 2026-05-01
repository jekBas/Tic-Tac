package com.example.tictac.session.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Component("gameEngineRetryListener")
public class GameEngineRetryListener implements RetryListener {

	private static final Logger log = LoggerFactory.getLogger(GameEngineRetryListener.class);

	@Override
	public <T, E extends Throwable> void onError(RetryContext context,
																							 RetryCallback<T, E> callback,
																							 Throwable throwable) {
		log.warn("Game Engine call failed (attempt {}/{}): {}",
				context.getRetryCount(), 3, throwable.getMessage());
	}

	@Override
	public <T, E extends Throwable> void close(RetryContext context,
																						 RetryCallback<T, E> callback,
																						 Throwable throwable) {
		if (throwable != null) {
			log.error("All retry attempts exhausted for Game Engine: {}", throwable.getMessage(), throwable);
		}
	}
}
