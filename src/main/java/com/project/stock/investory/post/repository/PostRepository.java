package com.project.stock.investory.post.repository;

import com.project.stock.investory.admin.dto.AdminPostResponseDto;
import com.project.stock.investory.post.dto.PostWithAuthorDto;
import com.project.stock.investory.post.entity.Board;
import com.project.stock.investory.post.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Board로 조회
    List<Post> findByBoard(Board board);

    // Board가 Post에서 @ManyToOne으로 연결되어 있으므로 Board 엔티티 내부의 stockId 사용 가능
    Optional<Post> findByPostIdAndBoard_StockId(Long postId, String stockId);

    // 좋아요 수 1 증가 (원자적 연산)
    @Modifying // JPA에게 데이터 변경 쿼리임을 알림
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.postId = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    // 좋아요 수 1 감소 (음수 방지)
    @Modifying
    @Query("UPDATE Post p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.postId = :postId")
    void decrementLikeCount(@Param("postId") Long postId);

    // 특정 사용자가 작성한 게시글 전체 조회
    List<Post> findByUserId(Long userId);

    // postId로 작성자 이름 가져오기
    @Query("SELECT new com.project.stock.investory.post.dto.PostWithAuthorDto(p.postId, p.title, p.content, u.name) " +
            "FROM Post p, User u WHERE p.userId = u.userId AND p.postId = :postId")
    Optional<PostWithAuthorDto> findPostWithAuthorById(@Param("postId") Long postId);

    @Query("""
    SELECT new com.project.stock.investory.post.dto.PostWithAuthorDto(
        p.postId,
        p.title,
        p.content,
        u.name
    )
    FROM Post p
    JOIN User u ON p.userId = u.userId
""")
    List<PostWithAuthorDto> findAllPostsWithAuthor();

    @Query("""
    SELECT new com.project.stock.investory.admin.dto.AdminPostResponseDto(
        p.postId,
        p.title,
        u.name,
        p.createdAt
    )
    FROM Post p
    JOIN User u ON p.userId = u.userId
""")
    List<AdminPostResponseDto> findAllAdminPostsWithAuthor();
}

