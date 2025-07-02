package com.project.stock.investory.commentLike.repository;

import com.project.stock.investory.commentLike.model.CommentLike;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long>  {

    Optional<CommentLike> findByUserUserIdAndCommentCommentId(Long userId, Long commentId);

    // 내가 좋아요한 댓글 전체 조회
    List<CommentLike> findByUser_UserId(Long userId);

    // 특정 댓글의 좋아요 수 조회
    @Query("SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.commentId = :commentId")
    int countByCommentId(@Param("commentId") Long commentId);

    // 사용자가 특정 댓글에 좋아요를 눌렀는지 확인
    boolean existsByUserUserIdAndCommentCommentId(Long userId, Long commentId);
}
