package com.example.PieJuega.repository;

import com.example.PieJuega.model.ChatRoomMember;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    @EntityGraph(attributePaths = {"room"})
    List<ChatRoomMember> findByUser_IdOrderByRoom_UpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<ChatRoomMember> findByRoom_IdOrderByJoinedAtAsc(Long roomId);

    Optional<ChatRoomMember> findByRoom_IdAndUser_Id(Long roomId, Long userId);

    boolean existsByRoom_IdAndUser_Id(Long roomId, Long userId);

    long countByRoom_Id(Long roomId);
}
