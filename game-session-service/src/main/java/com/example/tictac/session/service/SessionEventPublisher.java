package com.example.tictac.session.service;

import com.example.tictac.session.dto.SessionEventDto;
import com.example.tictac.session.model.GameSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class SessionEventPublisher {
	private static final Logger log = LoggerFactory.getLogger(SessionEventPublisher.class);

	private final SimpMessagingTemplate messagingTemplate;

	public SessionEventPublisher(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	public void publishUpdate(GameSession session) {
		String destination = "/topic/sessions/" + session.getSessionId();
		SessionEventDto event = SessionEventDto.from(session);
		messagingTemplate.convertAndSend(destination, event);
		log.debug("Published session event to {} (status={})", destination, event.sessionStatus());
	}
}