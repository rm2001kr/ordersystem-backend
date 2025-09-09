package com.beyond.ordersystem.member.controller;

import com.beyond.ordersystem.common.auth.JwtTokenProvider;
import com.beyond.ordersystem.common.dto.CommonSuccessDto;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.dto.*;
import com.beyond.ordersystem.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/create")
    public ResponseEntity<?> memberCreate(@RequestBody @Valid MemberCreateDto dto) {
        Long id = memberService.save(dto);
        return new ResponseEntity<>(new CommonSuccessDto(id, HttpStatus.CREATED.value(), "회원가입 성공"), HttpStatus.CREATED);
    }

    @PostMapping("/doLogin")
    public ResponseEntity<?> memberDoLogin(@RequestBody @Valid LoginReqDto dto) {
        Member member = memberService.doLogin(dto);
        String accessToken = jwtTokenProvider.createAtToken(member); // at 토큰 생성
        String refreshToken = jwtTokenProvider.createRtToken(member); // rt 토큰 생성
        return new ResponseEntity<>(new CommonSuccessDto(new LoginResDto(accessToken, refreshToken), HttpStatus.OK.value(), "로그인 성공"), HttpStatus.OK);
    }


    @PostMapping("/refresh-at")
    public ResponseEntity<?> generateNewAt(@RequestBody RefreshTokenDto dto) {  // rt를 통한 at 갱신 요청
        Member member = jwtTokenProvider.validateRt(dto.getRefreshToken()); // rt 검증 로직
        String accessToken = jwtTokenProvider.createAtToken(member); // at 신규 생성 로직
        return new ResponseEntity<>(new CommonSuccessDto(new LoginResDto(accessToken, accessToken), HttpStatus.OK.value(), "at 갱신 성공"), HttpStatus.OK);
    }

    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> memberList() {
        List<MemberResDto> memberList = memberService.findAll();
        return new ResponseEntity<>(new CommonSuccessDto(memberList, HttpStatus.OK.value(), "사용자 목록 조회 성공"), HttpStatus.OK);
    }

    @GetMapping("/myInfo")
    public ResponseEntity<?> memberMyInfo() {
        MemberResDto memberResDto = memberService.findMyInfo();
        return new ResponseEntity<>(new CommonSuccessDto(memberResDto, HttpStatus.OK.value(), "마이페이지 조회 성공"), HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> memberDelete() {
        Long deleteMemberId = memberService.memberDelete();
        return new ResponseEntity<>(new CommonSuccessDto(deleteMemberId, HttpStatus.OK.value(), "회원탈퇴 성공"), HttpStatus.OK);
    }

}
