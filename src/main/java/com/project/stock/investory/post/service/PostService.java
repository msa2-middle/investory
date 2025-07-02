package com.project.stock.investory.post.service;

import com.project.stock.investory.post.dto.PostDto;
import com.project.stock.investory.post.dto.PostRequestDto;
import com.project.stock.investory.post.dto.PostWithAuthorDto;
import com.project.stock.investory.post.entity.Board;
import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.exception.AuthenticationRequiredException;
import com.project.stock.investory.post.exception.PostAccessDeniedException;
import com.project.stock.investory.post.exception.PostNotFoundException;
import com.project.stock.investory.post.repository.BoardRepository;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;

    /**
     * [Create]
     * stock_id에 해당하는 Board가 없다면 자동 생성(board_id와 stock_id)
     */
    @Transactional // @Transactional 있어야 Board save & Post save 함께 처리됨 (롤백 안전)
    public PostDto createPost(String stockId, PostRequestDto request, CustomUserDetails userDetails) {

        // 1. stockId에 해당하는 Board 찾기 (없으면 새로 생성)
        Board board = boardRepository.findByStockId(stockId)
                .orElseGet(() -> boardRepository.save(Board.builder()
                        .stockId(stockId)
                        .build()));

        // 2. Post Entity 생성
        Post post = Post.builder()
                .userId(userDetails.getUserId())
                .board(board)  // FK 값(Board)은 엔티티 자체로 넣어야 함 (Long boardId 불가, JPA의 FK 연관관계)
                .title(request.getTitle())
                .content(request.getContent())
                .viewCount(0)
                .likeCount(0)
                .createdAt(LocalDateTime.now()) // timestamp 자동 생성
                .updatedAt(LocalDateTime.now())
                .build();

        // 3. Entity 저장
        Post savedPost = postRepository.save(post);

        // 4. Entity -> DTO 변환 후 반환
        return PostDto.builder()
                .postId(savedPost.getPostId())
                .userId(savedPost.getUserId())
                .boardId(savedPost.getBoard().getBoardId())
                .title(savedPost.getTitle())
                .content(savedPost.getContent())
                .viewCount(savedPost.getViewCount())
                .likeCount(savedPost.getLikeCount())
                .createdAt(savedPost.getCreatedAt())
                .updatedAt(savedPost.getUpdatedAt())
                .build();
    }


    /**
     * [Read]
     * 1. stock_id에 해당하는 모든 post를 read
     * 2. post_id의 개별 post 조회(1번 메서드를 통해 화면 상 특정 stock_id의 글들만 나타남)
     * 3. 게시글 상세 조회
     * 4. like_count(게시글 좋아요) 조회
     * 5. view_count(조회수) 조회
     */
    // 1. stock_id에 해당하는 모든 post를 read
    @Transactional(readOnly = true)
    public List<PostDto> getPostsByStockId(String stockId) {
        Board board = boardRepository.findByStockId(stockId)
                .orElseThrow(PostNotFoundException::new);
        return postRepository.findByBoard(board).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 2. post_id의 개별 post 조회(1번 메서드를 통해 화면 상 특정 stock_id의 글들만 나타남)
    @Transactional(readOnly = true)
    public PostDto getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
        return convertToDto(post);
    }

    // 3. 게시글 상세 조회 (작성자 이름 포함)
    @Transactional(readOnly = true)
    public PostWithAuthorDto getPostWithAuthor(Long postId) {
        return postRepository.findPostWithAuthorById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    // 4. like_count(게시글 좋아요) 조회
    public long getLikeCount(Long postId) {
        return postRepository.countLikesByPostId(postId);
    }

    // 5. view_count(조회수) 조회
    public long getViewCount(Long postId) {
        return postRepository.getViewCountByPostId(postId);
    }

    // mapping util method
    private PostDto convertToDto(Post post) {
        return PostDto.builder()
                .postId(post.getPostId())
                .userId(post.getUserId())
                .boardId(post.getBoard().getBoardId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }


    /**
     * [Update]
     * 1. 게시글 수정
     * 2. 조회수 늘리기
     */
    @Transactional
    public PostDto updatePost(Long postId, PostRequestDto request, CustomUserDetails userDetails) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 작성자 확인
        if (!post.getUserId().equals(userDetails.getUserId())) {
            throw new PostAccessDeniedException();
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setUpdatedAt(LocalDateTime.now());

        return PostDto.builder()
                .postId(post.getPostId())
                .userId(post.getUserId())
                .boardId(post.getBoard().getBoardId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // 조회수 늘리기
    @Transactional
    public Post getPostAndIncreaseView(Long postId) {
        // 1. 조회수 증가
        postRepository.incrementViewCount(postId);

        // 2. 게시글 조회
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    /**
     * [Delete]
     */
    @Transactional
    public void deletePost(Long postId, CustomUserDetails userDetails) {

        // userId null 체크
        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        // 게시글 생성 및 예외처리
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 작성자 확인
        if (!post.getUserId().equals(userDetails.getUserId())) {
            throw new PostAccessDeniedException();
        }

        postRepository.delete(post);
    }


}




