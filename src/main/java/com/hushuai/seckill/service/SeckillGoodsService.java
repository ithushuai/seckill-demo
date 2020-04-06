package com.hushuai.seckill.service;

import com.hushuai.seckill.pojo.TbSeckillGoods;
import com.hushuai.seckill.utils.Result;

import java.util.List;

/**
 * created by it_hushuai
 * 2020/1/8 21:47
 */
public interface SeckillGoodsService {
    List<TbSeckillGoods> findAll();

    TbSeckillGoods findOne(Long id);

    Result saveOrder(Long id, String userId);

    Result saveOrder2(Long id, String userId);
}
