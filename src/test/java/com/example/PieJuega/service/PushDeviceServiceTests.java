package com.example.PieJuega.service;

import com.example.PieJuega.model.PushDevice;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.PushDeviceRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.DevicePlatform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushDeviceServiceTests {

    @Mock
    private PushDeviceRepository pushDeviceRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void registersANewDeviceForTheAuthenticatedUser() {
        User user = User.builder().id(7L).build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(pushDeviceRepository.findByToken("token-1")).thenReturn(Optional.empty());
        PushDeviceService service = new PushDeviceService(
                pushDeviceRepository,
                userRepository
        );

        service.register(7L, " token-1 ", DevicePlatform.IOS, false, true);

        ArgumentCaptor<PushDevice> captor = ArgumentCaptor.forClass(PushDevice.class);
        verify(pushDeviceRepository).save(captor.capture());
        assertSame(user, captor.getValue().getUser());
        assertEquals("token-1", captor.getValue().getToken());
        assertEquals(DevicePlatform.IOS, captor.getValue().getPlatform());
        assertEquals(false, captor.getValue().isSoundEnabled());
        assertEquals(true, captor.getValue().isVibrationEnabled());
    }

    @Test
    void reassignsARefreshedTokenToTheCurrentUser() {
        User previousUser = User.builder().id(3L).build();
        User currentUser = User.builder().id(7L).build();
        PushDevice existing = PushDevice.builder()
                .id(11L)
                .user(previousUser)
                .token("token-1")
                .platform(DevicePlatform.ANDROID)
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(currentUser));
        when(pushDeviceRepository.findByToken("token-1"))
                .thenReturn(Optional.of(existing));
        PushDeviceService service = new PushDeviceService(
                pushDeviceRepository,
                userRepository
        );

        service.register(7L, "token-1", DevicePlatform.IOS, true, false);

        assertSame(currentUser, existing.getUser());
        assertEquals(DevicePlatform.IOS, existing.getPlatform());
        assertEquals(true, existing.isSoundEnabled());
        assertEquals(false, existing.isVibrationEnabled());
        verify(pushDeviceRepository).save(existing);
    }

    @Test
    void unregistersOnlyTheCurrentUsersDevice() {
        PushDeviceService service = new PushDeviceService(
                pushDeviceRepository,
                userRepository
        );

        service.unregister(7L, " token-1 ");

        verify(pushDeviceRepository).deleteByTokenAndUser_Id("token-1", 7L);
    }
}
