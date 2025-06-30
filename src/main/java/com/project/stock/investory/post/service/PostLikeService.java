package com.project.stock.investory.post.service;

import com.project.stock.investory.alarm.dto.AlarmRequestDTO;
import com.project.stock.investory.alarm.entity.AlarmType;
import com.project.stock.investory.alarm.service.AlarmService;
import com.project.stock.investory.post.entity.Post;
import com.project.stock.investory.post.entity.PostLike;
import com.project.stock.investory.post.exception.AuthenticationRequiredException;
import com.project.stock.investory.post.exception.PostLikeDuplicatedException;
import com.project.stock.investory.post.exception.PostNotFoundException;
import com.project.stock.investory.post.exception.UserNotFoundException;
import com.project.stock.investory.post.repository.PostLikeRepository;
import com.project.stock.investory.post.repository.PostRepository;
import com.project.stock.investory.security.CustomUserDetails;
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
    private final AlarmService alarmService;

    // 좋아요 표시
    @Transactional
    public void likePost(CustomUserDetails userDetails, Long postId) {

        // 좋아요 눌렀는지 체크
        if (postLikeRepository.findByUser_UserIdAndPost_PostId(userDetails.getUserId(), postId).isPresent()) {
            throw new PostLikeDuplicatedException();
        }

        // Option<Post> 객체 생성 및 postId 유효성 검사
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // Option<User> 객체 생성 및 userId 유효성 검사
        User user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(UserNotFoundException::new);

        // 게시글 작성자
        User userPost = userRepository.findById(post.getUserId())
                .orElseThrow(UserNotFoundException::new); // 예외처리

        // PostLike 객체 생성
        PostLike postLike = new PostLike(post, user);

        // post 테이블의 like_count +1
        postRepository.incrementLikeCount(postId);

        // PostLike 저장
        postLikeRepository.save(postLike);

        // 알람보내기
        AlarmRequestDTO alarmRequest = AlarmRequestDTO
                .builder()
                .content(userPost.getName()
                        + "님의 "
                        + post.getTitle()
                        + " 게시글에 "
                        + user.getName()
                        + " 님이 좋아요를 눌렀습니다.")
                .type(AlarmType.COMMENT)
                .build();

        alarmService.createAlarm(alarmRequest, user.getUserId());
    }

    // 좋아요 해제
    @Transactional
    public void unlikePost(CustomUserDetails userDetails, Long postId) {

        /*@AuthenticationPrincipal을 사용해 컨트롤러에서 userId를 가져오는 경우,
        Spring Security 필터 체인 덕분에 이 userId가 null인 경우는 거의 없음
        하지만 방어적인 코드로써 이렇게 명시적으로 체크*/
        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        // Option<Post> 객체 생성 및 postId 유효성 검사
        Post post = postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);

        // 반환 타입이 Optional<PostLike>인 건, 해당 사용자가 해당 게시글에 좋아요를 누르지 않았을 수도 있기 때문
        Optional<PostLike> postLikeOpt = postLikeRepository.findByUser_UserIdAndPost_PostId(userDetails.getUserId(), postId);

        /*Optional 객체에서 ifPresent() 메서드는
        postLikeOpt 안에 PostLike 객체가 실제로 존재할 때만 -> 유효성 자동 처리
        괄호 안의 동작(postLikeRepository::delete)을 실행*/
        postLikeOpt.ifPresent(postLikeRepository::delete);

        // post테이블의 like_count -1(음수방지처리)
        postRepository.decrementLikeCount(postId);
    }


    // 좋아요 개수세기 -> 실제로 사용하지않고 Post 에있는 필드로 클라이언트 화면에 표시
    public long countLikes(Long postId) {

        // 1. postId 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        return postLikeRepository.countByPost_PostId(postId);
    }


    public boolean hasUserLiked(CustomUserDetails userDetails, Long postId) {

        // userId null 체크
        if (userDetails.getUserId() == null) {
            throw new AuthenticationRequiredException();
        }

        // 1. postId 존재 여부 확인
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException();
        }

        // 2. 좋아요 여부 확인
        return postLikeRepository.findByUser_UserIdAndPost_PostId(userDetails.getUserId(), postId).isPresent();
    }
}
