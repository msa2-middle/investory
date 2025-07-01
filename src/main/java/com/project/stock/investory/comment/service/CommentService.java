package com.project.stock.investory.comment.service;

import com.project.stock.investory.alarm.dto.AlarmRequestDTO;
import com.project.stock.investory.alarm.entity.AlarmType;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.comment.dto.CommentRequestDTO;
import com.project.stock.investory.comment.dto.CommentResponseDTO;
import com.project.stock.investory.comment.exception.AuthenticationRequiredException;
import com.project.stock.investory.comment.exception.CommentAccessDeniedException;
import com.project.stock.investory.comment.exception.CommentNotFoundException;
import com.project.stock.investory.comment.exception.UserNotFoundException;
import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.comment.repository.CommentRepository;
import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.exception.PostNotFoundException;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final AlarmService alarmService;

    //  댓글 생성
    @Transactional
    public CommentResponseDTO create(CommentRequestDTO request, CustomUserDetails userDetails, Long postId) {

        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        // 댓글 작성자
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new UserNotFoundException()); // 예외처리

        // 게시글
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException()); // 예외처리

        System.out.println("3");
        // 게시글 작성자
        User userPost = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new UserNotFoundException()); // 예외처리


        Comment comment =
                Comment
                        .builder()
                        .user(user)
                        .post(post)
                        .content(request.getContent())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

        Comment savedComment = commentRepository.save(comment);

        if (!userPost.getUserId().equals(comment.getUser().getUserId())) {
            AlarmRequestDTO alarmRequest = AlarmRequestDTO
                    .builder()
                    .content(userPost.getName()
                            + "님의 "
                            + post.getTitle()
                            + " 게시글에 "
                            + user.getName()
                            + " 님이 댓글을 남겼습니다.")
                    .type(AlarmType.COMMENT)
                    .build();

            alarmService.createAlarm(alarmRequest, userPost.getUserId()); // 게시글 작성자에게 보내기
        }

        return CommentResponseDTO
                .builder()
                .commentId(savedComment.getCommentId())
                .userId(savedComment.getUser().getUserId())
                .postId(savedComment.getPost().getPostId())
                .content(savedComment.getContent())
                .likeCount(savedComment.getLikeCount())
                .createdAt(savedComment.getCreatedAt())
                .userName(savedComment.getUser().getName())
                .build();

    }

    // 댓글 전체 조회
    public List<CommentResponseDTO> getPostComments(Long postId) {

        List<Comment> comments = commentRepository.findByPostPostId(postId);

        if (comments.isEmpty()) {
            throw new CommentNotFoundException();
        }

        return comments.stream()
                .map(comment -> CommentResponseDTO.builder()
                        .commentId(comment.getCommentId())
                        .userId(comment.getUser().getUserId())
                        .postId(comment.getPost().getPostId())
                        .content(comment.getContent())
                        .userName(comment.getUser().getName())
                        .likeCount(comment.getLikeCount())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 댓글 상세 조회
    public CommentResponseDTO getPostComment(Long postId, Long commentId) {

        Comment comment =
                commentRepository.findByPostPostIdAndCommentId(postId, commentId)
                        .orElseThrow(() -> new CommentNotFoundException()); // 예외처리

        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .userId(comment.getUser().getUserId())
                .postId(comment.getPost().getPostId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .userName(comment.getUser().getName())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    // 댓글 수정 // userId는 검증할 때
    public CommentResponseDTO updateComment(
            CustomUserDetails userDetails, Long postId, Long commentId, CommentRequestDTO request
    ) {

        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        Comment comment =
                commentRepository.findByPostPostIdAndCommentId(postId, commentId)
                        .orElseThrow(() -> new CommentNotFoundException()); // 예외처리

        if (!userDetails.getUserId().equals(comment.getUser().getUserId())) {
            throw new CommentAccessDeniedException();
        }

        // 엔티티 내부 메서드로 상태 변경 (유효성 검증 포함)
        comment.updateComment(request.getContent());

        commentRepository.save(comment);

        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .userId(userDetails.getUserId())
                .postId(comment.getPost().getPostId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .userName(comment.getUser().getName())
                .build();
    }

    // 댓글 삭제 // userId는 검증할 때
    public CommentResponseDTO deleteComment(
            CustomUserDetails userDetails, Long postId, Long commentId
    ) {

        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        Comment comment =
                commentRepository.findByPostPostIdAndCommentId(postId, commentId)
                        .orElseThrow(() -> new CommentNotFoundException()); // 예외처리


        if (!userDetails.getUserId().equals(comment.getUser().getUserId())) {
            throw new CommentAccessDeniedException();
        }

        CommentResponseDTO deleteComment =
                CommentResponseDTO.builder()
                        .commentId(comment.getCommentId())
                        .userId(userDetails.getUserId())
                        .postId(comment.getPost().getPostId())
                        .content(comment.getContent())
                        .build();

        commentRepository.delete(comment);

        return deleteComment;
    }

}
