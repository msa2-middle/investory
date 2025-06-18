package com.project.stock.investory.post.entity;

import com.project.stock.investory.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "post_like",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "post_id"})}
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "post_like_seq_generator")
    @SequenceGenerator(name = "post_like_seq_generator", sequenceName = "post_like_seq", allocationSize = 1)
    @Column(name = "post_like_id")
    private Long postLikeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Constructor
    public PostLike(Post post, User user) {
        this.post = post;
        this.user = user;
        this.createdAt = LocalDateTime.now();
    }


}
