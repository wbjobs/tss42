package com.example.orderinventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderinventory.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo} AND deleted = 0 FOR UPDATE")
    Order selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

    @Select("SELECT * FROM t_order WHERE order_no = #{orderNo} AND deleted = 0")
    Order selectByOrderNoDirect(@Param("orderNo") String orderNo);

    @Update("UPDATE t_order SET status = #{targetStatus}, update_time = #{updateTime}, version = version + 1 " +
            "WHERE id = #{id} AND status = #{expectStatus} AND version = #{version} AND deleted = 0")
    int updateStatusWithVersion(@Param("id") Long id,
                                @Param("expectStatus") Integer expectStatus,
                                @Param("targetStatus") Integer targetStatus,
                                @Param("version") Integer version,
                                @Param("updateTime") LocalDateTime updateTime);

    @Select("SELECT * FROM t_order WHERE status = #{status} AND create_time < #{beforeTime} AND deleted = 0 " +
            "ORDER BY create_time ASC LIMIT #{limit}")
    List<Order> selectTimeoutPendingOrders(@Param("status") Integer status,
                                           @Param("beforeTime") LocalDateTime beforeTime,
                                           @Param("limit") int limit);
}
