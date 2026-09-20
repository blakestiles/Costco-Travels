package com.smartrebook.booking.repository;

import com.smartrebook.booking.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByMemberReference(String memberReference);
}
