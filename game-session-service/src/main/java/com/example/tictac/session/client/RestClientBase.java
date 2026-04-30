package com.example.tictac.session.client;

import com.example.tictac.session.exception.GameEngineCommunicationException;
import java.io.IOException;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

public abstract class RestClientBase {

	protected final RestClient restClient;

	protected RestClientBase(RestClient restClient) {
		this.restClient = restClient;
	}

	protected <T> T execute(String description, RestCall<T> call) {
		try {
			T response = call.invoke();
			if (response == null) {
				throw new GameEngineCommunicationException(
						serviceName() + " returned an empty body for " + description);
			}
			return response;
		} catch (GameEngineCommunicationException ex) {
			throw ex;
		} catch (ResourceAccessException ex) {
			throw new GameEngineCommunicationException(
					serviceName() + " is unreachable while calling " + description + ": " + ex.getMessage(), ex);
		} catch (RestClientResponseException ex) {
			throw new GameEngineCommunicationException(
					serviceName() + " returned " + ex.getStatusCode().value()
							+ " while calling " + description + ": " + ex.getResponseBodyAsString(), ex);
		} catch (RuntimeException ex) {
			throw new GameEngineCommunicationException(
					"Failed to call " + serviceName() + ": " + description + " (" + ex.getMessage() + ")", ex);
		}
	}

	protected void onErrorResponse(HttpRequest request, ClientHttpResponse response) throws IOException {
		HttpStatusCode status = response.getStatusCode();
		String body = new String(response.getBody().readAllBytes());
		throw new GameEngineCommunicationException(
				serviceName() + " returned " + status.value() + (body.isBlank() ? "" : ": " + body));
	}

	protected abstract String serviceName();

	@FunctionalInterface
	protected interface RestCall<T> {
		T invoke();
	}
}
