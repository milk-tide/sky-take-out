package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {


    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;
    /**
     * 指定时间区间内营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end得每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 :turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end得每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> newuserList = new ArrayList<>();
        List<Integer> totaluserList = new ArrayList<>();


        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            Map map = new HashMap();
            map.put("end",endTime);
           Integer totalUser = userMapper.countByMap(map);

            map.put("begin",beginTime);
            Integer newUser = userMapper.countByMap(map);

           totaluserList.add(totalUser);
            newuserList.add(newUser);
        }


        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .totalUserList(StringUtils.join(totaluserList,","))
                .newUserList(StringUtils.join(newuserList,","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        //当前集合用于存放begin到end得每天的日期
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validorderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date,LocalTime.MAX);

            //查询每天订单总数 select count(id) where oder_time > and order_time <?
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            //查询每天的订单总数 status = 5
            Integer validorderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validorderCountList.add(validorderCount);
        }
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validorderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = 0.0;
        if (totalOrderCount!= 0 ){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }



        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(orderCountList,","))
                .validOrderCountList(StringUtils.join(validorderCountList,","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }



    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map map = new HashMap();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);

        return orderMapper.countByMap(map);
    }

    /**
     * 销量排名前十
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);

        List<GoodsSalesDTO> salestop10 = orderMapper.getSalesTop10(beginTime,endTime);

        List<String> names = salestop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names,",");
        List<Integer> numbers = salestop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers,",");




        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
