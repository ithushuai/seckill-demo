package com.hushuai.seckill.thread;

import com.hushuai.seckill.mapper.TbSeckillGoodsMapper;
import com.hushuai.seckill.pojo.TbSeckillGoods;
import com.hushuai.seckill.pojo.TbSeckillOrder;
import com.hushuai.seckill.utils.IdWorker;
import com.hushuai.seckill.utils.OrderRecord;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * 多线程对redis中记录的下单信息进行订单处理，并完成下单操作
 *
 * created by it_hushuai
 * 2020/1/11 21:18
 */
@Component
public class OrderCreateThread implements Runnable {
    private static String lockKey = "reduceStock";
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private IdWorker idWorker;
    @Resource
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Resource
    private Redisson redisson;
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
        //秒杀商品库存量减1
        //由于缓存中当前商品id的库存可能被其他线程修改，会产生线程安全问题，因此修改商品库存的代码需要加锁
        /*synchronized (OrderCreateThread.class){
            //再获取一次缓存中商品的信息
            seckillGood = (TbSeckillGoods) redisTemplate
                    .boundHashOps(TbSeckillGoods.class.getSimpleName()).get(orderRecord.getId());
            seckillGood.setStockCount(seckillGood.getStockCount() - 1);
            //判断减完之后redis中商品的库存是否大于0，大于0则更新缓存，否则删除该秒杀商品的缓存，并更新到数据库
            if(seckillGood.getStockCount() > 0){
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(seckillGood.getGoodsId(), seckillGood);
            }else {
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).delete(seckillGood.getGoodsId());
                seckillGoodsMapper.updateByPrimaryKey(seckillGood);
            }
        }*/

        RLock lock = redisson.getLock(lockKey);//分布式锁
        try {
            lock.lock(30, TimeUnit.SECONDS);//锁的有效期为30s，redisson会自动续期
            //再获取一次缓存中商品的信息
            seckillGood = (TbSeckillGoods) redisTemplate
                    .boundHashOps(TbSeckillGoods.class.getSimpleName()).get(orderRecord.getId());
            seckillGood.setStockCount(seckillGood.getStockCount() - 1);
            //判断减完之后redis中商品的库存是否大于0，大于0则更新缓存，否则删除该秒杀商品的缓存，并更新到数据库
            if(seckillGood.getStockCount() > 0){
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(seckillGood.getGoodsId(), seckillGood);
            }else {
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).delete(seckillGood.getGoodsId());
                seckillGoodsMapper.updateByPrimaryKey(seckillGood);
            }
        }finally {
            lock.unlock();
        }

    }
}
