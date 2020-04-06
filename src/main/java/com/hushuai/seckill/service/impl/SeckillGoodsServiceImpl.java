package com.hushuai.seckill.service.impl;

import com.hushuai.seckill.mapper.TbSeckillGoodsMapper;
import com.hushuai.seckill.pojo.TbSeckillGoods;
import com.hushuai.seckill.pojo.TbSeckillOrder;
import com.hushuai.seckill.service.SeckillGoodsService;
import com.hushuai.seckill.thread.OrderCreateThread;
import com.hushuai.seckill.thread.OrderCreateThread2;
import com.hushuai.seckill.utils.IdWorker;
import com.hushuai.seckill.utils.OrderRecord;
import com.hushuai.seckill.utils.Result;
import com.hushuai.seckill.utils.SystemConst;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.hushuai.seckill.utils.SystemConst.CONST_SECKILLGOODS_ID_PREFIX;
import static com.hushuai.seckill.utils.SystemConst.CONST_USER_ID_PREFIX;

/**
 * created by it_hushuai
 * 2020/1/8 21:47
 */
@Service
@Transactional
public class SeckillGoodsServiceImpl implements SeckillGoodsService {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private IdWorker idWorker;
    @Resource
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Resource
    private ThreadPoolTaskExecutor executor;
    @Resource
    private OrderCreateThread orderCreateThread;
    @Resource
    private OrderCreateThread2 orderCreateThread2;

    @Override
    public List<TbSeckillGoods> findAll() {
        Set keys = redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).keys();
        return redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).values();
    }

    @Override
    public TbSeckillGoods findOne(Long id) {
        return (TbSeckillGoods) redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id);
    }

    /**
     * redis列表元素弹出方式下单，可以防止超卖，且效率高
     * @param id
     * @param userId
     * @return
     */
    @Override
    public Result saveOrder(Long id, String userId) {
        /*//根据商品id从redis中查出商品
        TbSeckillGoods seckillGood = (TbSeckillGoods) redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id);
        //如果缓存中秒杀商品不存在或者库存为空，则提示已售罄
        if(seckillGood == null || seckillGood.getStockCount() <= 0){
            return new Result(false, "已售罄");
        }*/
        //判断用户是否已经抢购过该商品
        Boolean member = redisTemplate.boundSetOps(CONST_USER_ID_PREFIX + id).isMember(userId);
        if(member){
            return new Result(false, "您已抢购，无法再次抢购");
        }
        //从商品队列中取商品id
        id = (Long)redisTemplate.boundListOps(CONST_SECKILLGOODS_ID_PREFIX + id).rightPop();
        if(id == null){//商品不存在
            return new Result(false, "已售罄");
        }
        //根据商品id从redis中查出商品
        TbSeckillGoods seckillGood = (TbSeckillGoods) redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id);
        //如果时间已截止，提示秒杀时间已结束
        if(seckillGood.getEndTime().getTime() < System.currentTimeMillis()){
            return new Result(false, "活动已结束");
        }
        //已成功抢购,在redis中记录某商品有哪些用户已抢购过的集合中
        redisTemplate.boundSetOps(CONST_USER_ID_PREFIX + id).add(userId);
        //生成下单信息，也保存在redis中，供多线程处理
        redisTemplate.boundListOps(OrderRecord.class.getSimpleName()).leftPush(new OrderRecord(id, userId));
        //多线程处理下单
        executor.execute(orderCreateThread);
        return new Result(true, "恭喜您抢购到商品，请尽快支付");
    }

    /**
     * 对下单过程加锁，能够防止超卖，但是效率低
     * @param id
     * @param userId
     * @return
     */
    @Override
    public Result saveOrder2(Long id, String userId) {
        //判断用户是否已经抢购过该商品
        Boolean member = redisTemplate.boundSetOps(CONST_USER_ID_PREFIX + id).isMember(userId);
        if(member){
            return new Result(false, "您已抢购，无法再次抢购");
        }
        synchronized (SeckillGoodsServiceImpl.class){
            //从缓存中取出商品
            TbSeckillGoods seckillGood = (TbSeckillGoods) redisTemplate
                    .boundHashOps(TbSeckillGoods.class.getSimpleName()).get(id);
            if(seckillGood == null || seckillGood.getStockCount() <= 0){
                return new Result(false, "已售罄");
            }
            //已成功抢购,在redis中记录某商品有哪些用户已抢购过的集合中
            redisTemplate.boundSetOps(CONST_USER_ID_PREFIX + id).add(userId);
            //生成下单信息，也保存在redis中，供多线程处理
            redisTemplate.boundListOps(OrderRecord.class.getSimpleName()).leftPush(new OrderRecord(id, userId));
            //减库存
            seckillGood.setStockCount(seckillGood.getStockCount() - 1);
            //判断减完之后redis中商品的库存是否大于0，大于0则更新缓存，否则删除该秒杀商品的缓存，并更新到数据库
            if(seckillGood.getStockCount() > 0){
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(seckillGood.getGoodsId(), seckillGood);
            }else {
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).delete(seckillGood.getGoodsId());
                seckillGoodsMapper.updateByPrimaryKey(seckillGood);
            }
        }
        //多线程处理下单
        executor.execute(orderCreateThread2);
        return new Result(true, "恭喜您抢购到商品，请尽快支付");
    }
}
