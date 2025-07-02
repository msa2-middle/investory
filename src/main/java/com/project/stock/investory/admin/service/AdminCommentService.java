package com.project.stock.investory.admin.service;

import com.project.stock.investory.admin.dto.AdminCommentResponseDto;
import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.comment.repository.CommentRepository;
import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommentService {

    private final CommentRepository commentRepository;

    public List<AdminCommentResponseDto> getAllComments() {
        return commentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    public AdminCommentResponseDto getCommentById(Long commentId) {
        Comment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        return toDto(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        commentRepository.delete(comment);
    }

    @Transactional
    public AdminCommentResponseDto updateComment(Long commentId, String content) {
        Comment comment = commentRepository.findByCommentId(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        comment.updateComment(content);
        return toDto(comment);
    }

    private AdminCommentResponseDto toDto(Comment comment) {
        Post post = comment.getPost();
        User user = comment.getUser();

        return AdminCommentResponseDto.builder()
                .commentId(comment.getCommentId())
                .postId(post.getPostId())
                .postTitle(post.getTitle())
                .userId(user.getUserId())
                .userName(user.getName())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
