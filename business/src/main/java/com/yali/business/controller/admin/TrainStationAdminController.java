package com.yali.business.controller.admin;

import com.yali.business.req.TrainStationQueryReq;
import com.yali.business.req.TrainStationSaveReq;
import com.yali.business.resp.TrainStationQueryResp;
import com.yali.business.service.TrainStationService;
import com.yali.common.context.LoginMemberContext;
import com.yali.common.resp.CommonResp;
import com.yali.common.resp.PageResp;
import com.yali.business.req.TrainStationQueryReq;
import com.yali.business.req.TrainStationSaveReq;
import com.yali.business.resp.TrainStationQueryResp;
import com.yali.business.service.TrainStationService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/train-station")
public class TrainStationAdminController {

    @Resource
    private TrainStationService trainStationService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody TrainStationSaveReq req) {
        trainStationService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<TrainStationQueryResp>> queryList(@Valid TrainStationQueryReq req) {
        PageResp<TrainStationQueryResp> list = trainStationService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        trainStationService.delete(id);
        return new CommonResp<>();
    }

}
