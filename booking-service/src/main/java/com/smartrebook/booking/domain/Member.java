package com.smartrebook.booking.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_reference", nullable = false, unique = true, length = 50)
    private String memberReference;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "membership_type", nullable = false, length = 50)
    private String membershipType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Member() {
    }

    public Member(String memberReference, String firstName, String lastName, String membershipType) {
        this.memberReference = memberReference;
        this.firstName = firstName;
        this.lastName = lastName;
        this.membershipType = membershipType;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getMemberReference() {
        return memberReference;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getMembershipType() {
        return membershipType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
