package com.hushuai.seckill.schedule;

import com.hushuai.seckill.mapper.TbSeckillGoodsMapper;
import com.hushuai.seckill.pojo.TbSeckillGoods;
import com.hushuai.seckill.pojo.TbSeckillGoodsExample;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

import static com.hushuai.seckill.utils.SystemConst.CONST_SECKILLGOODS_ID_PREFIX;

@Component
public class SeckillGoodsToRedis {

    @Resource
    private TbSeckillGoodsMapper seckillGoodsMapper;
    @Resource
    private RedisTemplate redisTemplate;
    @Scheduled(cron = "0 */1 * * * ?")
    public void importToRedis(){
        //1.查询合法秒杀商品数据
        TbSeckillGoodsExample example = new TbSeckillGoodsExample();
        Date date = new Date();
        example.createCriteria().andStatusEqualTo("1").andStockCountGreaterThan(0)
                .andStartTimeLessThan(date).andEndTimeGreaterThan(date);
        List<TbSeckillGoods> tbSeckillGoods = seckillGoodsMapper.selectByExample(example);
        for (TbSeckillGoods seckillGood : tbSeckillGoods) {//将秒杀商品依次存入redis
            //注意如果redis中已经有的商品，则不更新,只添加之前未加入过的秒杀商品
            if(redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).get(seckillGood.getId()) == null){
                redisTemplate.boundHashOps(TbSeckillGoods.class.getSimpleName()).put(seckillGood.getId(), seckillGood);
                //为每个秒杀商品生成一个长度为库存量大小,元素值为商品id的队列，用没下一个单，队列就弹出一个元素
                createQueue(seckillGood);
            }
        }
    }

    private void createQueue(TbSeckillGoods seckillGood) {
        Long id = seckillGood.getId();
        for (int i = 0; i < seckillGood.getStockCount(); i++) {
            //所有商品统一前缀，再加以id进行区分，左进右出
            redisTemplate.boundListOps(CONST_SECKILLGOODS_ID_PREFIX + id).leftPush(id);
        }
    }
}