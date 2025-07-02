package com.project.stock.investory.alarm.helper;

import com.project.stock.investory.alarm.dto.AlarmRequestDTO;
import com.project.stock.investory.alarm.entity.AlarmType;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.comment.exception.CommentNotFoundException;
import com.project.stock.investory.comment.model.Comment;
import com.project.stock.investory.comment.repository.CommentRepository;
import com.project.stock.investory.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmHelper {

    private final AlarmService alarmService;
    private final CommentRepository commentRepository;

    public void createPostCommentAlarm(Long postId, User commenter, Long postAuthorId, String postTitle) {
        String content = String.format("%s님이 '%s' 게시글에 댓글을 달았습니다.",
                commenter.getName(), postTitle);
        String targetUrl = "/community/posts/" + postId;

        AlarmRequestDTO dto = AlarmRequestDTO.forPost(
                AlarmType.POST_COMMENT, content, targetUrl, postId, commenter);

        alarmService.createAlarm(dto, postAuthorId);
    }

    public void createPostLikeAlarm(Long postId, User liker, Long postAuthorId, String postTitle) {
        String content = String.format("%s님이 '%s' 게시글에 좋아요를 눌렀습니다.",
                liker.getName(), postTitle);
        String targetUrl = "/community/posts/" + postId;

        AlarmRequestDTO dto = AlarmRequestDTO.forPost(
                AlarmType.POST_LIKE, content, targetUrl, postId, liker);

        alarmService.createAlarm(dto, postAuthorId);
    }

    public void createCommentLikeAlarm(Long commentId, User liker, Long commentAuthorId, Long postId) {
        String content = String.format("%s님이 회원님의 댓글에 좋아요를 눌렀습니다.",
                liker.getName());

        Comment comment =
                commentRepository.findByPostPostIdAndCommentId(postId, commentId)
                        .orElseThrow(() -> new CommentNotFoundException()); // 예외처리

        String targetUrl = "/community/posts/" + comment.getPost().getPostId();

        AlarmRequestDTO dto = AlarmRequestDTO.forComment(
                AlarmType.COMMENT_LIKE, content, targetUrl, commentId, liker);

        alarmService.createAlarm(dto, commentAuthorId);
    }

    public void createStockPriceAlarm(String stockId, User user, int targetPrice, int currentPrice, String stockName, String conditionText) {  // String 타입으로 변경
//        String content = String.format("%s이(가) 목표가 %.0f원에 도달했습니다. (현재가: %.0f원)",
//                stockName, targetPrice, currentPrice);
        String content = String.format("[주식 알림] %s님, %s 주식이 목표가 %,d원 %s에 도달했습니다. (현재가: %,d원)",
                user.getName(), stockName, targetPrice, conditionText, currentPrice);
        String targetUrl = "/stock/" + stockId + "/stock-info/product-info" ;  // stockCode 대신 stockId 사용

        AlarmRequestDTO dto = AlarmRequestDTO.forStock(
                AlarmType.STOCK_PRICE, content, targetUrl, stockId);

        alarmService.createAlarm(dto, user.getUserId());
    }
}