package com.project.stock.investory.commentLike.service;

import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.comment.repository.CommentRepository;
import com.project.stock.investory.commentLike.model.CommentLike;
import com.project.stock.investory.commentLike.repository.CommentLikeRepository;
import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;


    // 댓글 좋아요 설정
    @Transactional
    public void addCommentLike(CustomUserDetails userDetails, Long commentId) {

        Comment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new EntityNotFoundException());

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new EntityNotFoundException());

        Optional<CommentLike> commentLike =
                commentLikeRepository.findByUserUserIdAndCommentCommentId(user.getUserId(), comment.getCommentId());


        if (commentLike.isPresent()) {
            // commentLike 는 Optinal 객체라서 delete 메서드를 바로 사용하지 못한다.
            CommentLike like = commentLike.get();
            commentLikeRepository.delete(like);

            comment.updateCommentLike(comment.getLikeCount() - 1);
            commentRepository.save(comment);

        } else {
            CommentLike newCommentLike = CommentLike
                                            .builder()
                                            .user(user)
                                            .comment(comment)
                                            .build();
            commentLikeRepository.save(newCommentLike);

            comment.updateCommentLike(comment.getLikeCount() + 1);
            commentRepository.save(comment);
        }

    }
}
