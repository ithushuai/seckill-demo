package com.hushuai.seckill.thread;

import com.hushuai.seckill.mapper.TbSeckillGoodsMapper;
import com.hushuai.seckill.pojo.TbSeckillGoods;
import com.hushuai.seckill.pojo.TbSeckillOrder;
import com.hushuai.seckill.utils.IdWorker;
import com.hushuai.seckill.utils.OrderRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 仅仅是进行下单处理，减库存操作已在下单中完成
 *
 * created by it_hushuai
 * 2020/4/1 22:41
 */
@Component
public class OrderCreateThread2 implements Runnable{
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private IdWorker idWorker;
    @Resource
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Override
    public void run() {
        //从redis中记录的订单列表读取订单信息
        OrderRecord orderRecord = (OrderRecord) redisTemplate
                .boundListOps(OrderRecord.class.getSimpleName()).rightPop();
        //根据商品id去缓存中查询商品详细信息
        TbSeckillGoods seckillGood = (TbSeckillGoods) redisTemplate
                .boundHashOps(TbSeckillGoods.class.getSimpleName()).get(orderRecord.getId());
        //生成订单保存到缓存中
        TbSeckillOrder seckillOrder = new TbSeckillOrder();
        seckillOrder.setUserId(orderRecord.getUserId());
        seckillOrder.setSeckillId(idWorker.nextId());
        seckillOrder.setSellerId(seckillGood.getSellerId());
        seckillOrder.setMoney(seckillGood.getCostPrice());
        seckillOrder.setStatus("0");//未支付
        seckillOrder.setCreateTime(new Date());
        redisTemplate.boundHashOps(TbSeckillOrder.class.getSimpleName()).put(orderRecord.getUserId(), seckillOrder);
    }
}
