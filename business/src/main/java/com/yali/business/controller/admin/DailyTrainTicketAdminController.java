package com.yali.business.controller.admin;

import com.yali.business.req.DailyTrainTicketQueryReq;
import com.yali.business.req.DailyTrainTicketSaveReq;
import com.yali.business.resp.DailyTrainTicketQueryResp;
import com.yali.business.service.DailyTrainTicketService;
import com.yali.business.req.DailyTrainTicketQueryReq;
import com.yali.business.req.DailyTrainTicketSaveReq;
import com.yali.business.resp.DailyTrainTicketQueryResp;
import com.yali.business.service.DailyTrainTicketService;
import com.yali.common.resp.CommonResp;
import com.yali.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/daily-train-ticket")
public class DailyTrainTicketAdminController {

    @Resource
    private DailyTrainTicketService dailyTrainTicketService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody DailyTrainTicketSaveReq req) {
        dailyTrainTicketService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<DailyTrainTicketQueryResp>> queryList(@Valid DailyTrainTicketQueryReq req) {
        PageResp<DailyTrainTicketQueryResp> list = dailyTrainTicketService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        dailyTrainTicketService.delete(id);
        return new CommonResp<>();
    }

}
