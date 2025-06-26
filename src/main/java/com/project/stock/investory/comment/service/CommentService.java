package com.project.stock.investory.comment.service;

import com.project.stock.investory.alarm.dto.AlarmRequestDTO;
import com.project.stock.investory.alarm.entity.AlarmType;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.comment.dto.CommentRequestDTO;
import com.project.stock.investory.comment.dto.CommentResponseDTO;
import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.comment.repository.CommentRepository;

import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.security.CustomUserDetails;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 댓글 작성자
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        // 게시글
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        System.out.println("3");
        // 게시글 작성자
        User userPost = userRepository.findById(post.getUserId())
                .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        
        Comment comment =
                    Comment
                        .builder()
                        .user(user)
                        .post(post)
                        .content(request.getContent())
                        .build();

        Comment savedComment = commentRepository.save(comment);

        AlarmRequestDTO alarmRequest = AlarmRequestDTO
                .builder()
                .content(userPost.getName() + "님의 " + post.getTitle() +" 게시글에 " + user.getName() + " 님이 댓글을 남겼습니다.")
//                .content(post.getUser().getName() + "님의 " + post.getContent().substring(0, 2) + "..."  +" 게시글에 " + user.getName() + " 님이 댓글을 남겼습니다.")
                .type(AlarmType.COMMENT)
                .build();

        alarmService.createAlarm(alarmRequest, user.getUserId());

        return CommentResponseDTO
                .builder()
                .userId(savedComment.getUser().getUserId())
                .postId(savedComment.getPost().getPostId())
                .content(savedComment.getContent())
                .build();

    }

    // 댓글 전체 조회
    public List<CommentResponseDTO> getPostComments(Long postId) {

        List<Comment> comments = commentRepository.findByPostPostId(postId);

        if (comments.isEmpty()) {
            throw new EntityNotFoundException();
        }

        return comments.stream()
                .map(comment -> CommentResponseDTO.builder()
                        .userId(comment.getUser().getUserId())
                        .postId(comment.getPost().getPostId())
                        .content(comment.getContent())
                        .build())
                .collect(Collectors.toList());
    }

    // 댓글 상세 조회
    public CommentResponseDTO getPostComment(Long postId, Long commentId) {

        Comment comment =
                commentRepository.findByPostPostIdAndCommentId(postId, commentId)
                        .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        return CommentResponseDTO.builder()
                .userId(comment.getUser().getUserId())
                .postId(comment.getPost().getPostId())
                .content(comment.getContent())
                .build();
    }

    // 댓글 수정 // userId는 검증할 때
    public CommentResponseDTO updateComment(
            CustomUserDetails userDetails, Long postId, Long commentId, CommentRequestDTO request
    ) {
        Comment comment =
                commentRepository.findByPostPostIdAndCommentId(postId, commentId)
                        .orElseThrow(() -> new EntityNotFoundException()); // 예외처리

        if (!userDetails.getUserId().equals(comment.getUser().getUserId())) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        // 엔티티 내부 메서드로 상태 변경 (유효성 검증 포함)
        comment.updateComment(request.getContent());

        commentRepository.save(comment);

        return CommentResponseDTO.builder()
                .userId(userDetails.getUserId())
                .postId(comment.getPost().getPostId())
                .content(comment.getContent())
                .build();
    }

    // 댓글 삭제 // userId는 검증할 때
    public CommentResponseDTO deleteComment(
            CustomUserDetails userDetails, Long postId, Long commentId
    ) {
        Comment comment =
                commentRepository.findByPostPostIdAndCommentId(postId, commentId)
                        .orElseThrow(() -> new EntityNotFoundException()); // 예외처리


        if (!userDetails.getUserId().equals(comment.getUser().getUserId())) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        CommentResponseDTO deleteComment =
                CommentResponseDTO.builder()
                        .userId(userDetails.getUserId())
                        .postId(comment.getPost().getPostId())
                        .content(comment.getContent())
                        .build();

        commentRepository.delete(comment);

        return deleteComment;
    }

}
