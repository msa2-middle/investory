package com.project.stock.investory.commentLike.repository;

import com.project.stock.investory.commentLike.model.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long>  {

    Optional<CommentLike> findByUserUserIdAndCommentCommentId(Long userId, Long commentId);

}
