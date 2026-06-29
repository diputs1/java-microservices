package com.tungdt.microservices.background.dto;

public record DlqReplayResponse(String queue, int replayedCount) {
}
