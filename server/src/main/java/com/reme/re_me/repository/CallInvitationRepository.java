package com.reme.re_me.repository;

import com.reme.re_me.entity.CallInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import jakarta.persistence.LockModeType;

public interface CallInvitationRepository extends JpaRepository<CallInvitation, String> {
    void deleteByStatusInAndExpiresAtBefore(List<String> statuses, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT call FROM CallInvitation call WHERE call.id = :callId")
    java.util.Optional<CallInvitation> findByIdForUpdate(@Param("callId") String callId);
}
