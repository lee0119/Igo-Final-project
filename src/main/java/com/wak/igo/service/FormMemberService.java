package com.wak.igo.service;

import com.wak.igo.domain.*;
import com.wak.igo.dto.request.LoginRequestDto;
import com.wak.igo.dto.request.MemberRequestDto;
import com.wak.igo.dto.request.TokenDto;
import com.wak.igo.dto.response.MemberResponseDto;
import com.wak.igo.dto.response.ResponseDto;
import com.wak.igo.jwt.TokenProvider;
import com.wak.igo.repository.*;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FormMemberService {
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;
    private final HeartRepository heartRepository;
    private final CommentRepository commentRepository;
    private final MyPostRepository myPostRepository;
    private final StoryRepository storyRepository;
    private final NotificationRepository notificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final MyPostService myPostService;
    private final LoadingImgRepository loadingImgRepository;

    //일반 회원가입
    @Transactional
    public ResponseDto<?> createMember(MemberRequestDto requestDto) {
        if (null != isPresentMember(requestDto.getMemberId())) {  //아이디 중복여부 확인
            return ResponseDto.fail("DUPLICATED_MEMBER_ID",
                    "중복된 아이디 입니다.");
        }
        if (null != isPresentMember(requestDto.getNickname())) {  //닉네임 중복여부 확인
            return ResponseDto.fail("DUPLICATED_NICKNAME",
                    "중복된 닉네임 입니다.");
        }
        if (!requestDto.getPassword().equals(requestDto.getPasswordConfirm())) {  //비밀번호 일치여부 확인
            return ResponseDto.fail("PASSWORDS_NOT_MATCHED",
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        Member member = Member.builder()  //유저 생성
                .memberId(requestDto.getMemberId())
                .nickname(requestDto.getNickname())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .build();
        memberRepository.save(member);

        return ResponseDto.success("회원가입에 성공했습니다");
    }

    //로그인(Form Login)
    @Transactional
    public ResponseDto<?> login(LoginRequestDto requestDto, HttpServletResponse response) {
        Member member = isPresentMember(requestDto.getMemberId());
        if (null == member) {
            return ResponseDto.fail("MEMBER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
        }
        if (!member.validatePassword(passwordEncoder, requestDto.getPassword())) {
            return ResponseDto.fail("INVALID_PASSWORD", "비밀번호가 틀렸습니다.");
        }
        UserDetails userDetails = new UserDetailsImpl(member);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()); // 사용자 인증
        SecurityContextHolder.getContext().setAuthentication(authentication); //security가 인증한 사용자로 등록

        UserDetailsImpl userDetails1 = (UserDetailsImpl) authentication.getPrincipal(); // UserDetails를 구현한 현재 사용자 정보 가져오기
        TokenDto tokenDto = tokenProvider.generateTokenDto(userDetails1);


        response.addHeader("Authorization", "BEARER" + " " + tokenDto.getAccessToken());
        response.addHeader("RefreshToken", tokenDto.getRefreshToken());
        response.addHeader("Access-Token-Expire-Time", tokenDto.getAccessTokenExpiresIn().toString());
        myPostService.loginNotification(userDetails1);
        return ResponseDto.success(
                MemberResponseDto.builder()
                        .nickname(member.getNickname())
                        .profileImage(member.getProfileImage())
                        .interested(member.getInterested())
                        .build());
    }

    //회원 탈퇴
    @Transactional
    public ResponseDto<?> withdrawal(Long id, UserDetailsImpl userDetails)throws IOException {
        if (null == userDetails.getAuthorities()) {
            ResponseDto.fail("MEMBER_NOT_FOUND",
                    "사용자를 찾을 수 없습니다.");
        }
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(" 가입되지 않은 회원입니다."));
        if (!userDetails.getId().equals(member.getId()))
            return ResponseDto.fail("BAD_REQUEST", "본인이 아닙니다.");;

        commentRepository.deleteAllByMember(member);
        myPostRepository.deleteAllByMember(member);
        reportRepository.deleteAllByMember(member);
        refreshTokenRepository.deleteAllByMember(member);
        postRepository.deleteAllByMember(member);
        storyRepository.deleteAllByMember(member);
        notificationRepository.deleteAllByReceiver(member);
        memberRepository.delete(member);

        return ResponseDto.success("탈퇴 완료");
    }

    // 로딩 이미지 불러오기
    public String loadingImage() {
        Optional<LoadingImage> image = loadingImgRepository.findById(1);
        return image.get().getImage();
    }

    @Transactional(readOnly = true)
    public Member isPresentMember(String membername) {
        Optional<Member> optionalMember = memberRepository.findByMemberId(membername);
        return optionalMember.orElse(null);
    }
}

