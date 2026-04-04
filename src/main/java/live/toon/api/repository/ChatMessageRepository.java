package live.toon.api.repository;

import live.toon.api.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderBySentAtDesc(Long roomId, Pageable pageable);
    Page<ChatMessage> findByRoomId(Long roomId, Pageable pageable);
    Page<ChatMessage> findByUserId(UUID userId, Pageable pageable);
    Page<ChatMessage> findByRoomIdAndUserId(Long roomId, UUID userId, Pageable pageable);
    Page<ChatMessage> findByUsernameContainingIgnoreCase(String username, Pageable pageable);
    Page<ChatMessage> findByRoomIdAndUsernameContainingIgnoreCase(Long roomId, String username, Pageable pageable);
}
