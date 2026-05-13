package com.yali.member.Service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.yali.common.exception.BusinessException;
import com.yali.common.exception.BusinessExceptionEnum;
import com.yali.common.util.SnowUtil;
import com.yali.member.dto.req.MemberLoginReq;
import com.yali.member.dto.req.MemberRegisterReq;
import com.yali.member.dto.resp.MemberLoginResp;
import com.yali.member.entity.Member;
import com.yali.member.entity.MemberExample;
import com.yali.member.mapper.MemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MemberService {
    @Autowired
    private MemberMapper memberMapper;

    //返回long的意义是什么？
    public long register(MemberRegisterReq req) {
        String mobile = req.getMobile();
        Member list = selectByMobile(mobile);

        if(ObjectUtil.isNotNull(list)){
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

    public void sendCode(MemberRegisterReq req) {
        String mobile = req.getMobile();
        Member list = selectByMobile(mobile);

        // 如果手机号不存在，则插入一条记录
        if (ObjectUtil.isNull(list)) {
            Member member = new Member();
            member.setId(SnowUtil.getSnowflakeNextId());
            member.setMobile(mobile);
            memberMapper.insert(member);
        }

        // 生成验证码
//         String code = RandomUtil.randomString(4);
        String code = "8888";
        log.info("短信验证码为：{}",code);
        // 保存短信记录表：手机号，短信验证码，有效期，是否已使用，业务类型，发送时间，使用时间


        // 对接短信通道，发送短信
    }

    public MemberLoginResp login(MemberLoginReq req) {
        String mobile = req.getMobile();
        String code = req.getCode();
        Member list = selectByMobile(mobile);

        // 如果手机号不存在，则插入一条记录
        if (ObjectUtil.isNull(list)) {
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_NOT_EXIST);
        }

        // 生成验证码
        if(!"8888".equals(code)){
            throw new BusinessException(BusinessExceptionEnum.MEMBER_MOBILE_CODE_ERROR);
        }
        MemberLoginResp memberLoginResp = BeanUtil.copyProperties(list, MemberLoginResp.class);
        return memberLoginResp;
    }

    private Member selectByMobile(String mobile) {
        MemberExample example = new MemberExample();
        example.createCriteria().andMobileEqualTo(mobile);
        List<Member> list = memberMapper.selectByExample(example);
        if (CollUtil.isEmpty(list)) {
            return null;
        }else{
            return list.get(0);
        }
    }
}
