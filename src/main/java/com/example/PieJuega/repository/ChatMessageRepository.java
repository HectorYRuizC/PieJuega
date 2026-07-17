package com.example.PieJuega.repository;

import com.example.PieJuega.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @EntityGraph(attributePaths = {"sender"})
    Page<ChatMessage> findByRoom_IdOrderByIdDesc(Long roomId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    Page<ChatMessage> findByRoom_IdAndIdLessThanOrderByIdDesc(
            Long roomId,
            Long beforeId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"sender"})
    Optional<ChatMessage> findFirstByRoom_IdOrderByIdDesc(Long roomId);

    long countByRoom_IdAndIdGreaterThan(Long roomId, Long lastReadMessageId);

    boolean existsByRoom_Id(Long roomId);
}
