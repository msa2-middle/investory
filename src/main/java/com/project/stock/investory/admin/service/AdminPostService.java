package com.project.stock.investory.admin.service;

import com.project.stock.investory.admin.dto.AdminPostResponseDto;
import com.project.stock.investory.post.dto.PostWithAuthorDto;
import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPostService {

    private final PostRepository postRepository;

    public List<AdminPostResponseDto> getAllPosts() {
        return postRepository.findAllAdminPostsWithAuthor();
    }

    @Transactional(readOnly = true)
    public PostWithAuthorDto getPostById(Long postId) {
        return postRepository.findPostWithAuthorById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다.")); // 추후 예외클래스 추가 예정
    }

    @Transactional
    public void deletePostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        postRepository.delete(post);
    }

    @Transactional
    public PostWithAuthorDto updatePostById(Long postId, String title, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        post.setTitle(title);
        post.setContent(content);
        post.setUpdatedAt(LocalDateTime.now());

        return postRepository.findPostWithAuthorById(postId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
    }

}

