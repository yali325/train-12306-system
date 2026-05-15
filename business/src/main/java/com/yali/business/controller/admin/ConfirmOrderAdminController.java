package com.yali.business.controller.admin;


import com.yali.business.req.ConfirmOrderDoReq;
import com.yali.business.req.ConfirmOrderQueryReq;
import com.yali.business.resp.ConfirmOrderQueryResp;
import com.yali.business.service.ConfirmOrderService;
import com.yali.business.req.ConfirmOrderDoReq;
import com.yali.business.req.ConfirmOrderQueryReq;
import com.yali.business.resp.ConfirmOrderQueryResp;
import com.yali.business.service.ConfirmOrderService;
import com.yali.common.resp.CommonResp;
import com.yali.common.resp.PageResp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/confirm-order")
public class ConfirmOrderAdminController {

    @Resource
    private ConfirmOrderService confirmOrderService;

    @PostMapping("/save")
    public CommonResp<Object> save(@Valid @RequestBody ConfirmOrderDoReq req) {
        confirmOrderService.save(req);
        return new CommonResp<>();
    }

    @GetMapping("/query-list")
    public CommonResp<PageResp<ConfirmOrderQueryResp>> queryList(@Valid ConfirmOrderQueryReq req) {
        PageResp<ConfirmOrderQueryResp> list = confirmOrderService.queryList(req);
        return new CommonResp<>(list);
    }

    @DeleteMapping("/delete/{id}")
    public CommonResp<Object> delete(@PathVariable Long id) {
        confirmOrderService.delete(id);
        return new CommonResp<>();
    }

}
