package com.boutique.agent;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolCallLogRepository extends JpaRepository<ToolCallLog, Long> {

	List<ToolCallLog> findByUserIdOrderByIdAsc(Long userId);

}
