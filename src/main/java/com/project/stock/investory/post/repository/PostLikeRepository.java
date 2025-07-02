package com.project.stock.investory.post.repository;

import com.project.stock.investory.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByUser_UserIdAndPost_PostId(Long userId, Long postId);

    long countByPost_PostId(Long postId);

//    void deleteByUserIdAndPostId(Long userId, Long postId);

    // 특정 사용자의 좋아요 리스트 전부 조회
    List<PostLike> findByUser_UserId(Long userId);

}
