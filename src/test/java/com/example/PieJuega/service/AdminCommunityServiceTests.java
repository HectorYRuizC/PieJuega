package com.example.PieJuega.service;

import com.example.PieJuega.model.FootballTeam;
import com.example.PieJuega.model.Role;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.FootballTeamRepository;
import com.example.PieJuega.repository.RoleRepository;
import com.example.PieJuega.repository.TeamMemberRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.AuthProvider;
import com.example.PieJuega.util.TeamFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCommunityServiceTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private FootballTeamRepository teamRepository;
    @Mock
    private TeamMemberRepository memberRepository;

    private AdminCommunityService service;
    private User admin;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        service = new AdminCommunityService(
                userRepository,
                roleRepository,
                teamRepository,
                memberRepository
        );
        adminRole = Role.builder().id(2L).name("ROLE_ADMIN").build();
        admin = user(1L, "Admin", Set.of(adminRole));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    }

    @Test
    void preventsAdministratorFromSuspendingOwnAccount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.setUserActive(1L, 1L, false)
        );
    }

    @Test
    void suspendsAnotherUser() {
        User player = user(2L, "Jugador", Set.of());
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(userRepository.save(player)).thenReturn(player);

        var response = service.setUserActive(1L, 2L, false);

        assertFalse(response.active());
        verify(userRepository).save(player);
    }

    @Test
    void grantsAdministratorRole() {
        User player = user(2L, "Jugador", new HashSet<>());
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(player)).thenReturn(player);

        var response = service.setAdministrator(1L, 2L, true);

        assertTrue(response.roles().contains("ROLE_ADMIN"));
    }

    @Test
    void archivesTeamWithoutDeletingItsHistory() {
        FootballTeam team = FootballTeam.builder()
                .id(8L)
                .name("Atlético Norte")
                .city("Barranquilla")
                .primaryColor("#267A78")
                .secondaryColor("#ECFFFC")
                .format(TeamFormat.SEVEN)
                .formation("2-3-1")
                .owner(admin)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
        when(teamRepository.findById(8L)).thenReturn(Optional.of(team));
        when(teamRepository.save(team)).thenReturn(team);
        when(memberRepository.findByTeam_IdOrderBySquadRoleAscSlotIndexAsc(8L))
                .thenReturn(java.util.List.of());

        var response = service.setTeamActive(1L, 8L, false);

        assertFalse(response.active());
        verify(teamRepository).save(team);
    }

    private User user(Long id, String name, Set<Role> roles) {
        return User.builder()
                .id(id)
                .username(name)
                .email(name.toLowerCase() + "@example.com")
                .password("encoded")
                .authProvider(AuthProvider.LOCAL)
                .active(true)
                .roles(new HashSet<>(roles))
                .build();
    }
}
