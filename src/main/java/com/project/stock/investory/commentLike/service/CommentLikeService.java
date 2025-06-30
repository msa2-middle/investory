package com.project.stock.investory.commentLike.service;

import com.project.stock.investory.alarm.dto.AlarmRequestDTO;
import com.project.stock.investory.alarm.entity.AlarmType;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.comment.exception.CommentNotFoundException;
import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.comment.repository.CommentRepository;
import com.project.stock.investory.commentLike.dto.CommentLikeResponseDTO;
import com.project.stock.investory.commentLike.exception.UserNotFoundException;
import com.project.stock.investory.commentLike.model.CommentLike;
import com.project.stock.investory.commentLike.repository.CommentLikeRepository;
import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final AlarmService alarmService;

    // 댓글 좋아요 토글 (좋아요/좋아요 취소)
    @Transactional
    public CommentLikeResponseDTO toggleCommentLike(CustomUserDetails userDetails, Long commentId) {
        Comment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new CommentNotFoundException());

        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException());

        Optional<CommentLike> existingLike =
                commentLikeRepository.findByUserUserIdAndCommentCommentId(user.getUserId(), comment.getCommentId());

        boolean isLiked;
        int newLikeCount;

        if (existingLike.isPresent()) {
            // 좋아요 취소
            commentLikeRepository.delete(existingLike.get());
            newLikeCount = Math.max(comment.getLikeCount() - 1, 0);
            comment.updateCommentLike(newLikeCount);
            isLiked = false;
        } else {
            // 좋아요 추가
            CommentLike newCommentLike = CommentLike.builder()
                    .user(user)
                    .comment(comment)
                    .createdAt(LocalDateTime.now())
                    .build();
            commentLikeRepository.save(newCommentLike);
            newLikeCount = comment.getLikeCount() + 1;
            comment.updateCommentLike(newLikeCount);
            isLiked = true;

            if (!user.getUserId().equals(comment.getUser().getUserId())) {
                AlarmRequestDTO alarmRequest = AlarmRequestDTO
                        .builder()
                        .content(comment.getUser().getName()
                                + "님의 "
                                + comment.getContent()
                                + " 댓글에 "
                                + user.getName()
                                + " 님이 좋아요를 남겼습니다.")
                        .type(AlarmType.COMMENT)
                        .build();

                alarmService.createAlarm(alarmRequest, comment.getUser().getUserId()); // 댓글 작성자에게 보내기
            }
        }

        commentRepository.save(comment);

        return CommentLikeResponseDTO.builder()
                .isLiked(isLiked)
                .likeCount(newLikeCount)
                .build();
    }

    // 사용자의 댓글 좋아요 상태 확인
    public boolean hasUserLikedComment(CustomUserDetails userDetails, Long commentId) {
        if (userDetails == null) {
            return false;
        }

        return commentLikeRepository.findByUserUserIdAndCommentCommentId(
                userDetails.getUserId(), commentId).isPresent();
    }

    // 댓글의 총 좋아요 수 조회
    public int getCommentLikeCount(Long commentId) {
        Comment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new CommentNotFoundException());
        return comment.getLikeCount();
    }

}