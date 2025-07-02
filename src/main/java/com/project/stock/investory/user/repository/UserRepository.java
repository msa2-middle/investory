package com.project.stock.investory.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.project.stock.investory.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 회원 조회
    Optional<User> findByEmail(String email);

}
