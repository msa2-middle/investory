package com.project.stock.investory.post.service;

import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.entity.PostLike;
import com.project.stock.investory.post.exception.AuthenticationRequiredException;
import com.project.stock.investory.post.exception.PostNotFoundException;
import com.project.stock.investory.post.exception.UserNotFoundException;
import com.project.stock.investory.post.repository.PostLikeRepository;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void likePost(Long userId, Long postId) {

        // userId null 체크
        if (userId == null) {
            throw new AuthenticationRequiredException();
        }

        // 좋아요 눌렀는지 체크
        if (postLikeRepository.findByUser_UserIdAndPost_PostId(userId, postId).isPresent()) {
            throw new IllegalStateException("이미 좋아요를 누른 게시글입니다.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());

        PostLike postLike = new PostLike(post, user);
        postLikeRepository.save(postLike);
    }

    @Transactional
    public void unlikePost(Long userId, Long postId) {

        // userId null 체크
        if (userId == null) {
            throw new AuthenticationRequiredException();
        }

        // postId 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        Optional<PostLike> postLikeOpt = postLikeRepository.findByUser_UserIdAndPost_PostId(userId, postId);
        postLikeOpt.ifPresent(postLikeRepository::delete);
    }

    public long countLikes(Long postId) {

        // 1. postId 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        return postLikeRepository.countByPost_PostId(postId);
    }

    public boolean hasUserLiked(Long userId, Long postId) {

        // userId null 체크
        if (userId == null) {
            throw new AuthenticationRequiredException();
        }

        // 1. postId 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        // 2. 좋아요 여부 확인
        return postLikeRepository.findByUser_UserIdAndPost_PostId(userId, postId).isPresent();
    }
}
