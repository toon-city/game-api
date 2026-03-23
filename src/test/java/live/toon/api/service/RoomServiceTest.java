package live.toon.api.service;

import live.toon.api.dto.ChatMessageDto;
import live.toon.api.dto.RoomDto;
import live.toon.api.entity.ChatMessage;
import live.toon.api.entity.Room;
import live.toon.api.entity.RoomType;
import live.toon.api.repository.ChatMessageRepository;
import live.toon.api.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock RoomRepository        roomRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @InjectMocks RoomService    roomService;

    private Room publicRoom;

    @BeforeEach
    void setUp() {
        publicRoom = Room.builder()
                .id(1L).name("Jardin").type(RoomType.PUBLIC)
                .houseData("<map/>").maxUsers(50).userCount(3)
                .build();
    }

    // ── getPublicRooms ────────────────────────────────────────────────────────

    @Test
    void getPublicRooms_returnsAllPublicRooms() {
        when(roomRepository.findAllByType(RoomType.PUBLIC)).thenReturn(List.of(publicRoom));

        List<RoomDto> result = roomService.getPublicRooms();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Jardin");
        assertThat(result.get(0).getType()).isEqualTo("PUBLIC");
        assertThat(result.get(0).getUserCount()).isEqualTo(3);
    }

    @Test
    void getPublicRooms_returnsEmptyListWhenNoneExist() {
        when(roomRepository.findAllByType(RoomType.PUBLIC)).thenReturn(List.of());

        List<RoomDto> result = roomService.getPublicRooms();

        assertThat(result).isEmpty();
    }

    // ── getPublicRoomsPaged ───────────────────────────────────────────────────

    @Test
    void getPublicRoomsPaged_delegatesToRepository() {
        when(roomRepository.findByTypeFiltered(eq(RoomType.PUBLIC), eq(""), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicRoom)));

        Page<RoomDto> result = roomService.getPublicRoomsPaged("", 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getPublicRoomsPaged_trimsSearchQuery() {
        when(roomRepository.findByTypeFiltered(eq(RoomType.PUBLIC), eq("jardin"), eq(false), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publicRoom)));

        Page<RoomDto> result = roomService.getPublicRoomsPaged("  jardin  ", 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── getRoom ───────────────────────────────────────────────────────────────

    @Test
    void getRoom_returnsRoomById() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(publicRoom));

        RoomDto result = roomService.getRoom(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getHouseData()).isEqualTo("<map/>");
        assertThat(result.getMaxUsers()).isEqualTo(50);
    }

    @Test
    void getRoom_throwsWhenNotFound() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoom(99L))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getChatHistory ────────────────────────────────────────────────────────

    @Test
    void getChatHistory_returnsMessagesInChronologicalOrder() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(publicRoom));

        ChatMessage m1 = buildChatMessage(1L, 1L, "alice", "Salut !");
        ChatMessage m2 = buildChatMessage(2L, 1L, "bob",   "Hey !");
        // Repository retourne en DESC → service doit inverser
        when(chatMessageRepository.findByRoomIdOrderBySentAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(m2, m1));

        List<ChatMessageDto> result = roomService.getChatHistory(1L, 50);

        assertThat(result).hasSize(2);
        // Après reversed(), m1 doit être en premier
        assertThat(result.get(0).getUsername()).isEqualTo("alice");
        assertThat(result.get(1).getUsername()).isEqualTo("bob");
    }

    @Test
    void getChatHistory_throwsWhenRoomNotFound() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getChatHistory(99L, 50))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ChatMessage buildChatMessage(Long id, Long roomId, String username, String message) {
        ChatMessage m = new ChatMessage();
        m.setId(id);
        m.setRoomId(roomId);
        m.setUserId(UUID.randomUUID());
        m.setUsername(username);
        m.setMessage(message);
        m.setSentAt(OffsetDateTime.now());
        return m;
    }
}
