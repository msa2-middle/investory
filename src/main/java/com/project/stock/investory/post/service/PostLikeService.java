package com.project.stock.investory.post.service;

import com.project.stock.investory.post.entitiy.Post;
import com.project.stock.investory.post.entitiy.PostLike;
import com.project.stock.investory.post.repository.PostLikeRepository;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.user.entity.User;
import com.project.stock.investory.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostLikeService(PostLikeRepository postLikeRepository,
                           PostRepository postRepository,
                           UserRepository userRepository) {
        this.postLikeRepository = postLikeRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        if (postLikeRepository.findByUser_UserIdAndPost_PostId(userId, postId).isPresent()) {
            throw new IllegalStateException("이미 좋아요를 누른 게시글입니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 존재하지 않습니다."));
        PostLike postLike = new PostLike(post, user);
        postLikeRepository.save(postLike);
    }

    @Transactional
    public void unlikePost(Long userId, Long postId) {
        Optional<PostLike> postLikeOpt = postLikeRepository.findByUser_UserIdAndPost_PostId(userId, postId);
        postLikeOpt.ifPresent(postLikeRepository::delete);
    }

    public long countLikes(Long postId) {
        return postLikeRepository.countByPost_PostId(postId);
    }

    public boolean hasUserLiked(Long userId, Long postId) {
        return postLikeRepository.findByUser_UserIdAndPost_PostId(userId, postId).isPresent();
    }
}
