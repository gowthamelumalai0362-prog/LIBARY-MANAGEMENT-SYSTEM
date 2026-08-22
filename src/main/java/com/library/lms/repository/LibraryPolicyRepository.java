package com.library.lms.repository;

import com.library.lms.entity.LibraryPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LibraryPolicyRepository extends JpaRepository<LibraryPolicy, Long> {
    Optional<LibraryPolicy> findFirstByOrderByIdAsc();
}
