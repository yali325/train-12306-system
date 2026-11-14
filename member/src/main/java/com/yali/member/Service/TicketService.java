package com.yali.member.Service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yali.common.req.MemberTicketReq;
import com.yali.common.dto.TicketIssuedEvent;
import com.yali.common.resp.PageResp;
import com.yali.common.util.SnowUtil;
import com.yali.member.entity.Ticket;
import com.yali.member.entity.TicketExample;
import com.yali.member.mapper.TicketMapper;
import com.yali.member.req.TicketQueryReq;
import com.yali.member.resp.TicketQueryResp;
import jakarta.annotation.Resource;
//import org.apache.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketService.class);

    @Resource
    private TicketMapper ticketMapper;

    @Resource
    private JdbcTemplate jdbcTemplate;

    /** 同一订单的所有乘客车票在 member 本地事务里一起落库。 */
    @Transactional
    public void saveIssued(TicketIssuedEvent event) {
        String sql = """
                INSERT INTO ticket (id, order_id, member_id, passenger_id, passenger_name,
                    train_date, train_code, carriage_index, seat_row, seat_col,
                    start_station, start_time, end_station, end_time, seat_type,
                    create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(3), NOW(3))
                """;
        for (MemberTicketReq req : event.getTickets()) {
            try {
                jdbcTemplate.update(sql, SnowUtil.getSnowflakeNextId(), event.getOrderId(),
                        req.getMemberId(), req.getPassengerId(), req.getPassengerName(),
                        req.getTrainDate(), req.getTrainCode(), req.getCarriageIndex(),
                        req.getSeatRow(), req.getSeatCol(), req.getStartStation(),
                        req.getStartTime(), req.getEndStation(), req.getEndTime(),
                        req.getSeatType());
            } catch (DuplicateKeyException duplicate) {
                Integer existing = jdbcTemplate.queryForObject(
                        "select count(*) from ticket where order_id = ? and passenger_id = ?",
                        Integer.class, event.getOrderId(), req.getPassengerId());
                if (existing == null || existing != 1) {
                    // 若冲突来自主键 ID 而非本订单乘客，不能误认为幂等成功。
                    throw duplicate;
                }
            }
        }
    }

    /**
     * 会员购买车票后新增保存
     *
     * @param req
     */
    public void save(MemberTicketReq req) throws Exception {
//         LOG.info("seata全局事务ID save: {}", RootContext.getXID());
        DateTime now = DateTime.now();
        Ticket ticket = BeanUtil.copyProperties(req, Ticket.class);
        ticket.setId(SnowUtil.getSnowflakeNextId());
        ticket.setCreateTime(now);
        ticket.setUpdateTime(now);
        ticketMapper.insert(ticket);
        // 模拟被调用方出现异常
        // if (1 == 1) {
        //     throw new Exception("测试异常11");
        // }
    }

    public PageResp<TicketQueryResp> queryList(TicketQueryReq req) {
        TicketExample ticketExample = new TicketExample();
        ticketExample.setOrderByClause("id desc");
        TicketExample.Criteria criteria = ticketExample.createCriteria();
        if (ObjUtil.isNotNull(req.getMemberId())) {
            criteria.andMemberIdEqualTo(req.getMemberId());
        }

        LOG.info("查询页码：{}", req.getPage());
        LOG.info("每页条数：{}", req.getSize());
        PageHelper.startPage(req.getPage(), req.getSize());
        List<Ticket> ticketList = ticketMapper.selectByExample(ticketExample);

        PageInfo<Ticket> pageInfo = new PageInfo<>(ticketList);
        LOG.info("总行数：{}", pageInfo.getTotal());
        LOG.info("总页数：{}", pageInfo.getPages());

        List<TicketQueryResp> list = BeanUtil.copyToList(ticketList, TicketQueryResp.class);

        PageResp<TicketQueryResp> pageResp = new PageResp<>();
        pageResp.setTotal(pageInfo.getTotal());
        pageResp.setList(list);
        return pageResp;
    }

    public void delete(Long id) {
        ticketMapper.deleteByPrimaryKey(id);
    }
}
