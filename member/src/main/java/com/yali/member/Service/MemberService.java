package com.yali.member.Service;

import cn.hutool.core.collection.CollUtil;
import com.yali.common.util.SnowUtil;
import com.yali.dto.req.MemberRegisterReq;
import com.yali.member.entity.Member;
import com.yali.member.entity.MemberExample;
import com.yali.member.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MemberService {
    @Autowired
    private MemberMapper memberMapper;

    //返回long的意义是什么？
    public long register(MemberRegisterReq req) {
        String mobile = req.getMobile();
        MemberExample example = new MemberExample();
        example.createCriteria().andMobileEqualTo(mobile);
        List<Member> list = memberMapper.selectByExample(example);

        if(CollUtil.isNotEmpty(list)){
            /* 带验证码的注册可以用这种方式，有带验证码的注册可以用这种方式,
            有验码,说明手机号是本人用,原来注册过的,
            就不用再保存了,直接把数据库返回。
            这个接口做成即可以是注册,也可以是登录
             */
            throw new RuntimeException("手机号已注册");
        }
        Member member = new Member();
        member.setId(SnowUtil.getSnowflakeNextId());
        member.setMobile(mobile);
        memberMapper.insert(member);
        return member.getId();
    }
}
