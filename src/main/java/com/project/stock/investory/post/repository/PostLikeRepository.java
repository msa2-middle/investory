package com.project.stock.investory.post.repository;

import com.project.stock.investory.post.entitiy.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

//    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);
    Optional<PostLike> findByUser_UserIdAndPost_PostId(Long userId, Long postId);

    long countByPost_PostId(Long postId);

//    void deleteByUserIdAndPostId(Long userId, Long postId);

}
