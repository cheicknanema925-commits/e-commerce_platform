package com.boutique.agent;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Garde-fous de l'agent, réglables par configuration (boutique.agent.*)
@Validated
@ConfigurationProperties("boutique.agent")
public record AgentProperties(@Min(1) @Max(20) int maxToolCalls, @Min(1) @Max(10) int maxCallsPerTool,
		@Min(2) @Max(100) int memoryMessages, @NotNull Duration proposalTtl, @Min(1) int messagesPerMinute) {
}
