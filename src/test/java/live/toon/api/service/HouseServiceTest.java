package live.toon.api.service;

import jakarta.persistence.EntityNotFoundException;
import live.toon.api.dto.HouseDto;
import live.toon.api.dto.HouseRequest;
import live.toon.api.dto.HouseSchemaDto;
import live.toon.api.entity.*;
import live.toon.api.repository.HouseSchemaRepository;
import live.toon.api.repository.RoomRepository;
import live.toon.api.repository.UserRepository;
import live.toon.api.security.JwtPrincipal;
import live.toon.api.security.UserRank;
import live.toon.api.security.policy.HousePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HouseServiceTest {

    @Mock RoomRepository         roomRepository;
    @Mock HouseSchemaRepository  schemaRepository;
    @Mock UserRepository         userRepository;
    @Mock PasswordEncoder        passwordEncoder;
    @Mock HousePolicy            housePolicy;
    @InjectMocks HouseService    houseService;

    private static final UUID OWNER_ID = UUID.randomUUID();
    private User  owner;
    private HouseSchema schema;
    private Room  privateRoom;
    private JwtPrincipal ownerActor;
    private JwtPrincipal userActor;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(OWNER_ID).username("alice").build();
        schema = HouseSchema.builder().id(1L).name("Chalet").description("Un chalet").houseData("<house/>").build();
        privateRoom = Room.builder()
                .id(10L).name("Chez Alice").type(RoomType.PRIVATE)
                .access(HouseAccess.OPEN).owner(owner).schema(schema)
                .houseData("<house/>").maxUsers(20).userCount(0)
                .build();
        ownerActor = new JwtPrincipal(OWNER_ID, "alice", UserRank.ROLE_USER);
        userActor  = new JwtPrincipal(UUID.randomUUID(), "bob", UserRank.ROLE_USER);
    }

    // ── listSchemas ───────────────────────────────────────────────────────────

    @Test
    void listSchemas_returnsMappedDtos() {
        when(schemaRepository.findAll()).thenReturn(List.of(schema));

        List<HouseSchemaDto> result = houseService.listSchemas();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Chalet");
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    // ── createHouse ───────────────────────────────────────────────────────────

    @Test
    void createHouse_createsOpenHouseSuccessfully() {
        HouseRequest req = buildRequest("Mon chalet", HouseAccess.OPEN, null);
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(schemaRepository.findById(1L)).thenReturn(Optional.of(schema));
        when(roomRepository.save(any())).thenReturn(privateRoom);

        HouseDto result = houseService.createHouse(OWNER_ID, req);

        assertThat(result.getName()).isEqualTo("Chez Alice");
        assertThat(result.getOwnerUsername()).isEqualTo("alice");
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void createHouse_hashesPasswordWhenPasswordAccess() {
        HouseRequest req = buildRequest("Ma maison", HouseAccess.PASSWORD, "secret");
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner));
        when(schemaRepository.findById(1L)).thenReturn(Optional.of(schema));
        when(passwordEncoder.encode("secret")).thenReturn("$2a$hash");
        when(roomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HouseDto result = houseService.createHouse(OWNER_ID, req);

        assertThat(result.isHasPassword()).isTrue();
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void createHouse_throwsWhenNoSchemaId() {
        HouseRequest req = new HouseRequest();
        req.setName("Test");
        req.setAccess(HouseAccess.OPEN);
        req.setSchemaId(null);

        assertThatThrownBy(() -> houseService.createHouse(OWNER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("schéma de maison est obligatoire");
    }

    @Test
    void createHouse_throwsWhenPasswordAccessWithoutPassword() {
        HouseRequest req = buildRequest("Test", HouseAccess.PASSWORD, null);

        assertThatThrownBy(() -> houseService.createHouse(OWNER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mot de passe est requis");
    }

    // ── deleteHouse ───────────────────────────────────────────────────────────

    @Test
    void deleteHouse_deletesSuccessfully() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(privateRoom));

        houseService.deleteHouse(10L);

        verify(roomRepository).delete(privateRoom);
    }

    @Test
    void deleteHouse_throwsWhenHouseNotFound() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> houseService.deleteHouse(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── validateAccess ────────────────────────────────────────────────────────

    @Test
    void validateAccess_allowsEntryWhenOpen() {
        when(roomRepository.findById(10L)).thenReturn(Optional.of(privateRoom));
        when(housePolicy.canBypassRestrictions(ownerActor, privateRoom)).thenReturn(false);

        assertThatCode(() -> houseService.validateAccess(ownerActor, 10L, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAccess_allowsOwnerToBypassClosed() {
        privateRoom.setAccess(HouseAccess.CLOSED);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(privateRoom));
        when(housePolicy.canBypassRestrictions(ownerActor, privateRoom)).thenReturn(true);

        assertThatCode(() -> houseService.validateAccess(ownerActor, 10L, null))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAccess_throwsForClosedHouseWhenNotOwner() {
        privateRoom.setAccess(HouseAccess.CLOSED);
        when(roomRepository.findById(10L)).thenReturn(Optional.of(privateRoom));
        when(housePolicy.canBypassRestrictions(userActor, privateRoom)).thenReturn(false);

        assertThatThrownBy(() -> houseService.validateAccess(userActor, 10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fermée");
    }

    @Test
    void validateAccess_allowsCorrectPassword() {
        privateRoom.setAccess(HouseAccess.PASSWORD);
        privateRoom.setPasswordHash("$2a$hash");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(privateRoom));
        when(housePolicy.canBypassRestrictions(userActor, privateRoom)).thenReturn(false);
        when(passwordEncoder.matches("secret", "$2a$hash")).thenReturn(true);

        assertThatCode(() -> houseService.validateAccess(userActor, 10L, "secret"))
                .doesNotThrowAnyException();
    }

    @Test
    void validateAccess_throwsForWrongPassword() {
        privateRoom.setAccess(HouseAccess.PASSWORD);
        privateRoom.setPasswordHash("$2a$hash");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(privateRoom));
        when(housePolicy.canBypassRestrictions(userActor, privateRoom)).thenReturn(false);
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> houseService.validateAccess(userActor, 10L, "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Mot de passe incorrect");
    }

    @Test
    void validateAccess_throwsWhenPasswordMissingForPasswordHouse() {
        privateRoom.setAccess(HouseAccess.PASSWORD);
        privateRoom.setPasswordHash("$2a$hash");
        when(roomRepository.findById(10L)).thenReturn(Optional.of(privateRoom));
        when(housePolicy.canBypassRestrictions(userActor, privateRoom)).thenReturn(false);

        assertThatThrownBy(() -> houseService.validateAccess(userActor, 10L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mot de passe est requis");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private HouseRequest buildRequest(String name, HouseAccess access, String password) {
        HouseRequest req = new HouseRequest();
        req.setName(name);
        req.setSchemaId(1L);
        req.setAccess(access);
        req.setPassword(password);
        return req;
    }
}
