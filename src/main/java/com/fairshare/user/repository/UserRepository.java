package com.fairshare.user.repository;

import com.fairshare.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query("SELECT COALESCE(u.onboardingStep, 'welcome'), COUNT(u) FROM User u GROUP BY COALESCE(u.onboardingStep, 'welcome')")
    List<Object[]> countUsersByOnboardingStep();
}
