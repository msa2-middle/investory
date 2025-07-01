package com.project.stock.investory.commentLike.model;

import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class CommentLike {

    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY) // Oracle 12 버전부터 적용되는 코드
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "comment_like_seq")
    @SequenceGenerator(name = "comment_like_seq", sequenceName = "comment_like_seq", allocationSize = 1)
    @Column(name = "comment_like_seq_id")
    private Long commentLikeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

//    @CreatedDate
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime createdAt;

}
