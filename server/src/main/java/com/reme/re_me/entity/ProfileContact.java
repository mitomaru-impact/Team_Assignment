package com.reme.re_me.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "profile_contacts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_profile_contact_owner_contact",
                columnNames = {"owner_user_id", "contact_user_id"}))
public class ProfileContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "contact_user_id", nullable = false)
    private Long contactUserId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    public ProfileContact() {}

    public ProfileContact(Long ownerUserId, Long contactUserId, Long profileId) {
        this.ownerUserId = ownerUserId;
        this.contactUserId = contactUserId;
        this.profileId = profileId;
    }

    public Long getId() { return id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public Long getContactUserId() { return contactUserId; }
    public Long getProfileId() { return profileId; }

    public void setProfileId(Long profileId) { this.profileId = profileId; }
}
