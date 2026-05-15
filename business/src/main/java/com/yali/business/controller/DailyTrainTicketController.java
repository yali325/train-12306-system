package com.yali.business.controller;

import com.yali.business.req.DailyTrainTicketQueryReq;
import com.yali.business.resp.DailyTrainTicketQueryResp;
import com.yali.business.service.DailyTrainTicketService;
import com.yali.business.req.DailyTrainTicketQueryReq;
import com.yali.business.resp.DailyTrainTicketQueryResp;
import com.yali.business.service.DailyTrainTicketService;
import com.yali.common.resp.CommonResp;
import com.yali.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/daily-train-ticket")
public class DailyTrainTicketController {

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainTicketQueryResp>> queryList(@Valid DailyTrainTicketQueryReq req) {
        PageResp<DailyTrainTicketQueryResp> list = dailyTrainTicketService.queryList(req);
        return new CommonResp<>(list);
    }

}
