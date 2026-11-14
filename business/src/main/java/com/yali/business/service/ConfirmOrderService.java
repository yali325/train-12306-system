package com.yali.business.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yali.business.domain.*;
import com.yali.business.dto.ConfirmOrderMQDto;
import com.yali.business.enums.ConfirmOrderStatusEnum;
import com.yali.business.enums.RedisKeyPreEnum;
import com.yali.business.enums.SeatColEnum;
import com.yali.business.enums.SeatTypeEnum;
import com.yali.business.mapper.ConfirmOrderMapper;
import com.yali.business.req.ConfirmOrderDoReq;
import com.yali.business.req.ConfirmOrderQueryReq;
import com.yali.business.req.ConfirmOrderTicketReq;
import com.yali.business.resp.ConfirmOrderQueryResp;
import com.yali.common.exception.BusinessException;
import com.yali.common.exception.BusinessExceptionEnum;
import com.yali.common.context.LoginMemberContext;
import com.yali.common.resp.PageResp;
import com.yali.common.util.SnowUtil;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class ConfirmOrderService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfirmOrderService.class);

    @Resource
    private ConfirmOrderMapper confirmOrderMapper;
    @Resource
    private DailyTrainTicketService dailyTrainTicketService;
    @Resource
    private DailyTrainCarriageService dailyTrainCarriageService;
    @Resource
    private DailyTrainSeatService dailyTrainSeatService;
    @Resource
    private AfterConfirmOrderService afterConfirmOrderService;
    @Resource
    private RedissonClient redissonClient;
    @Resource private OrderTerminalService orderTerminalService;

    public void save(ConfirmOrderDoReq req) {
        DateTime now = DateTime.now();
        ConfirmOrder confirmOrder = BeanUtil.copyProperties(req, ConfirmOrder.class);
        if (ObjectUtil.isNull(confirmOrder.getId())) {
            confirmOrder.setId(SnowUtil.getSnowflakeNextId());
            confirmOrder.setCreateTime(now);
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.insert(confirmOrder);
        } else {
            confirmOrder.setUpdateTime(now);
            confirmOrderMapper.updateByPrimaryKey(confirmOrder);
        }
    }

    public PageResp<ConfirmOrderQueryResp> queryList(ConfirmOrderQueryReq req) {
        ConfirmOrderExample confirmOrderExample = new ConfirmOrderExample();
        confirmOrderExample.setOrderByClause("id desc");
        ConfirmOrderExample.Criteria criteria = confirmOrderExample.createCriteria();

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<ConfirmOrder> confirmOrderList = confirmOrderMapper.selectByExample(confirmOrderExample);

        PageInfo<ConfirmOrder> pageInfo = new PageInfo<>(confirmOrderList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<ConfirmOrderQueryResp> list = BeanUtil.copyToList(confirmOrderList, ConfirmOrderQueryResp.class);

        PageResp<ConfirmOrderQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        confirmOrderMapper.deleteByPrimaryKey(id);
    }


    @SentinelResource(value = "doConfirm", blockHandler = "doConfirmBlock")
    public void doConfirm(ConfirmOrderMQDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("购票消息不能为空");
        }
        if (dto.getOrderId() == null) {
            // 只接受新版事件。抛出异常让 MQ 重试并最终进入死信，便于核对旧消息。
            throw new IllegalArgumentException("购票消息缺少 orderId，eventId=" + dto.getEventId());
        }
        doConfirmOne(dto.getOrderId());
    }

    private void doConfirmOne(Long orderId) {
        ConfirmOrder order = confirmOrderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            // 理论上订单与 Outbox 同事务提交；若暂时不可见，交给 MQ 重投核对。
            throw new IllegalStateException("消息对应的订单不存在，orderId=" + orderId);
        }
        String lockKey = RedisKeyPreEnum.CONFIRM_ORDER.getCode() + "-"
                + DateUtil.formatDate(order.getDate()) + "-" + order.getTrainCode();
        RLock lock = redissonClient.getLock(lockKey);
        long lockWaitStartedNs = System.nanoTime();
        try {
            // 等待同车次正在处理的订单，避免把正常锁竞争变成 MQ 故障重试。
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待同车次出票锁时被中断，orderId=" + orderId, e);
        }
        long lockAcquiredNs = System.nanoTime();
        try {
            // 拿锁前读到的状态可能已变化，必须在锁内重新查。
            order = confirmOrderMapper.selectByPrimaryKey(orderId);
            if (order == null) throw new IllegalStateException("订单在处理期间消失，orderId=" + orderId);
            String status = order.getStatus();
            if (ConfirmOrderStatusEnum.SUCCESS.getCode().equals(status)
                    || ConfirmOrderStatusEnum.EMPTY.getCode().equals(status)
                    || ConfirmOrderStatusEnum.CANCEL.getCode().equals(status)
                    || ConfirmOrderStatusEnum.FAILURE.getCode().equals(status)) {
                return; // 重复消息：终态直接确认，不重复选座和扣库存。
            }
            if (!ConfirmOrderStatusEnum.INIT.getCode().equals(status)) {
                // 旧版已提交的 PENDING 可能含部分写入，不可盲目重置成 INIT。
                throw new IllegalStateException("订单处于非终态且不能安全重试，orderId=" + orderId + ", status=" + status);
            }
            try {
                sell(order);
            } catch (BusinessException e) {
                if (e.getE() == BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR) {
                    markEmptyIfInit(orderId);
                    return;
                }
                throw e; // 本地事务回滚后，交给 MQ 重试。
            }
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
            LOG.info("出票锁耗时，orderId={}，等锁={}ms，持锁={}ms", orderId,
                    TimeUnit.NANOSECONDS.toMillis(lockAcquiredNs - lockWaitStartedNs),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lockAcquiredNs));
        }
    }

    private int markEmptyIfInit(Long orderId) {
        return orderTerminalService.markEmpty(orderId);
    }


    /**
     * 售票
     * @param confirmOrder
     */
    private void sell(ConfirmOrder confirmOrder) {
        // 构造ConfirmOrderDoReq
        ConfirmOrderDoReq req = new ConfirmOrderDoReq();
        req.setMemberId(confirmOrder.getMemberId());
        req.setDate(confirmOrder.getDate());
        req.setTrainCode(confirmOrder.getTrainCode());
        req.setStart(confirmOrder.getStart());
        req.setEnd(confirmOrder.getEnd());
        req.setDailyTrainTicketId(confirmOrder.getDailyTrainTicketId());
        req.setTickets(JSON.parseArray(confirmOrder.getTickets(), ConfirmOrderTicketReq.class));
        req.setImageCode("");
        req.setImageCodeToken("");
        req.setLogId("");

        // 省略业务数据校验，如：车次是否存在，余票是否存在，车次是否在有效期内，tickets条数>0，同乘客同车次是否已买过

        // INIT 条件抢占移入 AfterConfirmOrderService 的本地事务；失败时连同库存一起回滚。

        Date date = req.getDate();
        String trainCode = req.getTrainCode();
        String start = req.getStart();
        String end = req.getEnd();
        List<ConfirmOrderTicketReq> tickets = req.getTickets();

        long routeStartedNs = System.nanoTime();
        // 查出余票记录，需要得到真实的库存
        DailyTrainTicket dailyTrainTicket = dailyTrainTicketService.selectByUnique(date, trainCode, start, end);
        LOG.debug("查出余票记录：{}", dailyTrainTicket);

        // 预扣减余票数量，并判断余票是否足够
        reduceTickets(req, dailyTrainTicket);
        long selectionStartedNs = System.nanoTime();

        // 最终的选座结果
        List<DailyTrainSeat> finalSeatList = new ArrayList<>();
        Map<Long, String> originalSell = new HashMap<>();
        // 计算相对第一个座位的偏移值
        // 比如选择的是C1,D2，则偏移值是：[0,5]
        // 比如选择的是A1,B1,C1，则偏移值是：[0,1,2]
        ConfirmOrderTicketReq ticketReq0 = tickets.get(0);
        if (StrUtil.isNotBlank(ticketReq0.getSeat())) {
            LOG.info("本次购票有选座");
            // 查出本次选座的座位类型都有哪些列，用于计算所选座位与第一个座位的偏离值
            List<SeatColEnum> colEnumList = SeatColEnum.getColsByType(ticketReq0.getSeatTypeCode());
            LOG.info("本次选座的座位类型包含的列：{}", colEnumList);

            // 组成和前端两排选座一样的列表，用于作参照的座位列表，例：referSeatList = {A1, C1, D1, F1, A2, C2, D2, F2}
            List<String> referSeatList = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                for (SeatColEnum seatColEnum : colEnumList) {
                    referSeatList.add(seatColEnum.getCode() + i);
                }
            }
            LOG.info("用于作参照的两排座位：{}", referSeatList);

            List<Integer> offsetList = new ArrayList<>();
            // 绝对偏移值，即：在参照座位列表中的位置
            List<Integer> aboluteOffsetList = new ArrayList<>();
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                int index = referSeatList.indexOf(ticketReq.getSeat());
                aboluteOffsetList.add(index);
            }
            LOG.info("计算得到所有座位的绝对偏移值：{}", aboluteOffsetList);
            for (Integer index : aboluteOffsetList) {
                int offset = index - aboluteOffsetList.get(0);
                offsetList.add(offset);
            }
            LOG.info("计算得到所有座位的相对第一个座位的偏移值：{}", offsetList);

            getSeat(finalSeatList,
                    date,
                    trainCode,
                    ticketReq0.getSeatTypeCode(),
                    ticketReq0.getSeat().split("")[0], // 从A1得到A
                    offsetList,
                    dailyTrainTicket.getStartIndex(),
                    dailyTrainTicket.getEndIndex(), originalSell
            );

        } else {
            LOG.info("本次购票没有选座");
            for (ConfirmOrderTicketReq ticketReq : tickets) {
                getSeat(finalSeatList,
                        date,
                        trainCode,
                        ticketReq.getSeatTypeCode(),
                        null,
                        null,
                        dailyTrainTicket.getStartIndex(),
                        dailyTrainTicket.getEndIndex(), originalSell
                );
            }
        }

        LOG.debug("最终选座：{}", finalSeatList);
        if (finalSeatList.size() != tickets.size()) {
            LOG.info("最终选座数量{}与购票数量{}不一致，判定为余票不足", finalSeatList.size(), tickets.size());
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
        }

        // 选中座位后事务处理：
        // 座位表修改售卖情况sell；
        // 余票详情表修改余票；
        // 为会员增加购票记录
        // 更新确认订单为成功
        long persistenceStartedNs = System.nanoTime();
        try {
            if (!afterConfirmOrderService.afterDoConfirm(
                    dailyTrainTicket, finalSeatList, originalSell, tickets, confirmOrder)) {
                throw new IllegalStateException("订单状态抢占失败，orderId=" + confirmOrder.getId());
            }
        } catch (Exception e) {
            LOG.error("保存购票信息失败", e);
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_EXCEPTION);
        } finally {
            LOG.info("出票阶段耗时，orderId={}，余票读取与预检={}ms，选座={}ms，事务写入={}ms",
                    confirmOrder.getId(),
                    TimeUnit.NANOSECONDS.toMillis(selectionStartedNs - routeStartedNs),
                    TimeUnit.NANOSECONDS.toMillis(persistenceStartedNs - selectionStartedNs),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - persistenceStartedNs));
        }
    }

    private void getSeat(List<DailyTrainSeat> finalSeatList, Date date, String trainCode, String seatType, String column, List<Integer> offsetList, Integer startIndex, Integer endIndex, Map<Long, String> originalSell) {
        List<DailyTrainSeat> getSeatList = new ArrayList<>();
        List<DailyTrainCarriage> carriageList = dailyTrainCarriageService.selectBySeatType(date, trainCode, seatType);
        LOG.debug("共查出{}个符合条件的车厢", carriageList.size());

        // 一个车箱一个车箱的获取座位数据
        for (DailyTrainCarriage dailyTrainCarriage : carriageList) {
            LOG.debug("开始从车厢{}选座", dailyTrainCarriage.getIndex());
            getSeatList = new ArrayList<>();
            List<DailyTrainSeat> seatList = dailyTrainSeatService.selectByCarriage(date, trainCode, dailyTrainCarriage.getIndex());
            LOG.debug("车厢{}的座位数：{}", dailyTrainCarriage.getIndex(), seatList.size());
            for (int i = 0; i < seatList.size(); i++) {
                DailyTrainSeat dailyTrainSeat = seatList.get(i);
                Integer seatIndex = dailyTrainSeat.getCarriageSeatIndex();
                String col = dailyTrainSeat.getCol();

                // 判断当前座位不能被选中过
                boolean alreadyChooseFlag = false;
                for (DailyTrainSeat finalSeat : finalSeatList){
                    if (finalSeat.getId().equals(dailyTrainSeat.getId())) {
                        alreadyChooseFlag = true;
                        break;
                    }
                }
                if (alreadyChooseFlag) {
                    LOG.debug("座位{}被选中过，不能重复选中，继续判断下一个座位", seatIndex);
                    continue;
                }

                // 判断column，有值的话要比对列号
                if (StrUtil.isBlank(column)) {
                    LOG.debug("无选座");
                } else {
                    if (!column.equals(col)) {
                        LOG.debug("座位{}列值不对，继续判断下一个座位，当前列值：{}，目标列值：{}", seatIndex, col, column);
                        continue;
                    }
                }

                // 只修改候选副本，避免一组相邻座位失败后污染原始 seatList。
                DailyTrainSeat seatCandidate = BeanUtil.copyProperties(dailyTrainSeat, DailyTrainSeat.class);
                boolean isChoose = calSell(seatCandidate, startIndex, endIndex, originalSell);
                if (isChoose) {
                    LOG.debug("选中座位");
                    getSeatList.add(seatCandidate);
                } else {
                    continue;
                }

                // 根据offset选剩下的座位
                boolean isGetAllOffsetSeat = true;
                if (CollUtil.isNotEmpty(offsetList)) {
                    LOG.debug("有偏移值：{}，校验偏移的座位是否可选", offsetList);
                    // 从索引1开始，索引0就是当前已选中的票
                    for (int j = 1; j < offsetList.size(); j++) {
                        Integer offset = offsetList.get(j);
                        // 座位在库的索引是从1开始
                        // int nextIndex = seatIndex + offset - 1;
                        int nextIndex = i + offset;

                        // 有选座时，一定是在同一个车箱
                        if (nextIndex >= seatList.size()) {
                            LOG.debug("座位{}不可选，偏移后的索引超出了这个车箱的座位数", nextIndex);
                            isGetAllOffsetSeat = false;
                            break;
                        }

                        DailyTrainSeat nextDailyTrainSeat = BeanUtil.copyProperties(seatList.get(nextIndex), DailyTrainSeat.class);
                        boolean isChooseNext = calSell(nextDailyTrainSeat, startIndex, endIndex, originalSell);
                        if (isChooseNext) {
                            LOG.debug("座位{}被选中", nextDailyTrainSeat.getCarriageSeatIndex());
                            getSeatList.add(nextDailyTrainSeat);
                        } else {
                            LOG.debug("座位{}不可选", nextDailyTrainSeat.getCarriageSeatIndex());
                            isGetAllOffsetSeat = false;
                            break;
                        }
                    }
                }
                if (!isGetAllOffsetSeat) {
                    getSeatList = new ArrayList<>();
                    continue;
                }

                // 保存选好的座位
                finalSeatList.addAll(getSeatList);
                return;
            }
        }
    }

    /**
     * 计算某座位在区间内是否可卖
     * 例：sell=10001，本次购买区间站1~4，则区间已售000
     * 全部是0，表示这个区间可买；只要有1，就表示区间内已售过票
     *
     * 选中后，要计算购票后的sell，比如原来是10001，本次购买区间站1~4
     * 方案：构造本次购票造成的售卖信息01110，和原sell 10001按位与，最终得到11111
     */
    private boolean calSell(DailyTrainSeat dailyTrainSeat, Integer startIndex, Integer endIndex, Map<Long, String> originalSell) {
        // 00001, 00000
        String sell = dailyTrainSeat.getSell();
        //  000, 000
        String sellPart = sell.substring(startIndex, endIndex);
        if (Integer.parseInt(sellPart) > 0) {
            LOG.debug("座位{}在本次车站区间{}~{}已售过票，不可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            return false;
        } else {
            LOG.debug("座位{}在本次车站区间{}~{}未售过票，可选中该座位", dailyTrainSeat.getCarriageSeatIndex(), startIndex, endIndex);
            //  111,   111
            String curSell = sellPart.replace('0', '1');
            // 0111,  0111
            curSell = StrUtil.fillBefore(curSell, '0', endIndex);
            // 01110, 01110
            curSell = StrUtil.fillAfter(curSell, '0', sell.length());

            // 当前区间售票信息curSell 01110与库里的已售信息sell 00001按位与，即可得到该座位卖出此票后的售票详情
            // 15(01111), 14(01110 = 01110|00000)
            int newSellInt = NumberUtil.binaryToInt(curSell) | NumberUtil.binaryToInt(sell);
            //  1111,  1110
            String newSell = NumberUtil.getBinaryStr(newSellInt);
            // 01111, 01110
            newSell = StrUtil.fillBefore(newSell, '0', sell.length());
            LOG.debug("座位{}被选中，原售票信息：{}，车站区间：{}~{}，即：{}，最终售票信息：{}"
                    , dailyTrainSeat.getCarriageSeatIndex(), sell, startIndex, endIndex, curSell, newSell);
            originalSell.putIfAbsent(dailyTrainSeat.getId(), sell);
            dailyTrainSeat.setSell(newSell);
            return true;

        }
    }

    private static void reduceTickets(ConfirmOrderDoReq req, DailyTrainTicket dailyTrainTicket) {
        for(ConfirmOrderTicketReq ticketReq : req.getTickets()) {
            String seatTypeCode = ticketReq.getSeatTypeCode();
            SeatTypeEnum seatTypeEnum = EnumUtil.getBy(SeatTypeEnum::getCode, seatTypeCode);
            switch (seatTypeEnum){
                case YDZ -> {
                    int countLeft = dailyTrainTicket.getYdz() - 1;
                    if(countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYdz(countLeft);
                }
                case EDZ -> {
                    int countLeft = dailyTrainTicket.getEdz() - 1;
                    if(countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setEdz(countLeft);
                }
                case RW -> {
                    int countLeft = dailyTrainTicket.getRw() - 1;
                    if(countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setRw(countLeft);
                }
                case YW -> {
                    int countLeft = dailyTrainTicket.getYw() - 1;
                    if(countLeft < 0) {
                        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_TICKET_COUNT_ERROR);
                    }
                    dailyTrainTicket.setYw(countLeft);
                }
            }
        }
    }
    /**
     * 降级方法，需包含限流方法的所有参数和BlockException参数
     * @param dto
     * @param e
     */
    public void doConfirmBlock(ConfirmOrderMQDto dto, BlockException e) {
        LOG.info("出票消费被限流，orderId={}", dto == null ? null : dto.getOrderId());
        throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_FLOW_EXCEPTION);
    }
    /**
     * 查询前面有几个人在排队
     * @param id
     */
    public Integer queryLineCount(Long id) {
        ConfirmOrder confirmOrder = confirmOrderMapper.selectByPrimaryKey(id);
        Long memberId = LoginMemberContext.getId();
        if (confirmOrder == null || !memberId.equals(confirmOrder.getMemberId())) {
            throw new BusinessException(BusinessExceptionEnum.CONFIRM_ORDER_NOT_FOUND);
        }
        ConfirmOrderStatusEnum statusEnum = EnumUtil.getBy(ConfirmOrderStatusEnum::getCode, confirmOrder.getStatus());
        int result = switch (statusEnum) {
            case PENDING -> 0; // 排队0
            case SUCCESS -> -1; // 成功
            case FAILURE -> -2; // 失败
            case EMPTY -> -3; // 无票
            case CANCEL -> -4; // 取消
            case INIT -> 1; // 仅表示等待中，不承诺精确排位
        };

        return result;
    }

    /**
     * 取消排队，只有I状态才能取消排队，所以按状态更新
     * @param id
     */
    public Integer cancel(Long id) {
        return orderTerminalService.cancel(id, LoginMemberContext.getId());
    }
}
