package com.project.stock.investory.comment.repository;

import com.project.stock.investory.comment.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostPostId(Long postId);

    Optional<Comment> findByPostPostIdAndCommentId(Long postId, Long commentId);

    Optional<Comment> findByCommentId(Long commentId);

    // 내 댓글 전체 조회
    List<Comment> findByUser_UserId(Long userId);
}
