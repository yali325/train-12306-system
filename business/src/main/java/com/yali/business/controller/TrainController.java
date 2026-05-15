package com.yali.business.controller;

import com.yali.business.resp.TrainQueryResp;
import com.yali.business.service.TrainService;
import com.yali.common.resp.CommonResp;
import com.yali.business.resp.TrainQueryResp;
import com.yali.business.service.TrainService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/train")
public class TrainController {

    @Resource
    private TrainService trainService;

    @GetMapping("/query-all")
    public CommonResp<List<TrainQueryResp>> queryList() {
        List<TrainQueryResp> list = trainService.queryAll();
        return new CommonResp<>(list);
    }

}
