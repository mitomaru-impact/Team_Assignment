package com.reme.re_me.repository;

import com.reme.re_me.entity.ProfileContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ProfileContactRepository extends JpaRepository<ProfileContact, Long> {
    List<ProfileContact> findAllByOwnerUserIdAndProfileId(Long ownerUserId, Long profileId);
    Optional<ProfileContact> findByOwnerUserIdAndContactUserId(Long ownerUserId, Long contactUserId);
    List<ProfileContact> findAllByOwnerUserId(Long ownerUserId);

    @Transactional
    void deleteByOwnerUserIdAndContactUserId(Long ownerUserId, Long contactUserId);

    @Transactional
    void deleteAllByProfileId(Long profileId);
}
