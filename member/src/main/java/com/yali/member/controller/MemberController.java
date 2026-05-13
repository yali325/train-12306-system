package com.yali.member.controller;

import com.yali.common.resp.CommonResp;
import com.yali.dto.req.MemberRegisterReq;
import com.yali.member.Service.MemberService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @PostMapping("/register")
    public CommonResp<Long> register(@Valid MemberRegisterReq req) {
        long register =  memberService.register(req);
//        CommonResp<Long> commonResp = new CommonResp<>();
//        commonResp.setContent(register);
//        return commonResp;
        return new CommonResp<>(register);
    }
}
