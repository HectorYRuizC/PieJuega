package com.example.PieJuega.repository;

import com.example.PieJuega.model.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByToken(String token);

    List<PushDevice> findByUser_Id(Long userId);

    long deleteByTokenAndUser_Id(String token, Long userId);

    @Modifying
    @Query("DELETE FROM PushDevice device WHERE device.token IN :tokens")
    int deleteByTokenIn(Collection<String> tokens);
}
