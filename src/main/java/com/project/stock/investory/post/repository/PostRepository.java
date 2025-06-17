package com.project.stock.investory.post.repository;

import com.project.stock.investory.post.entitiy.Board;
import com.project.stock.investory.post.entitiy.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // Board로 조회
    List<Post> findByBoard(Board board);

    // Board가 Post에서 @ManyToOne으로 연결되어 있으므로 Board 엔티티 내부의 stockId 사용 가능
    Optional<Post> findByPostIdAndBoard_StockId(Long postId, String stockId);
}

