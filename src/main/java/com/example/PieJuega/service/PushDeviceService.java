package com.example.PieJuega.service;

import com.example.PieJuega.exception.ResourceNotFoundException;
import com.example.PieJuega.model.PushDevice;
import com.example.PieJuega.model.User;
import com.example.PieJuega.repository.PushDeviceRepository;
import com.example.PieJuega.repository.UserRepository;
import com.example.PieJuega.util.DevicePlatform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushDeviceService {

    private final PushDeviceRepository pushDeviceRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(
            Long userId,
            String rawToken,
            DevicePlatform platform,
            Boolean soundEnabled,
            Boolean vibrationEnabled
    ) {
        String token = rawToken.trim();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        PushDevice device = pushDeviceRepository.findByToken(token)
                .orElseGet(PushDevice::new);
        device.setUser(user);
        device.setToken(token);
        device.setPlatform(platform);
        device.setSoundEnabled(soundEnabled == null || soundEnabled);
        device.setVibrationEnabled(vibrationEnabled == null || vibrationEnabled);
        pushDeviceRepository.save(device);
    }

    @Transactional
    public void unregister(Long userId, String rawToken) {
        pushDeviceRepository.deleteByTokenAndUser_Id(rawToken.trim(), userId);
    }
}
