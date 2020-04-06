package com.hushuai.seckill.controller;

import com.hushuai.seckill.pojo.TbSeckillGoods;
import com.hushuai.seckill.service.SeckillGoodsService;
import com.hushuai.seckill.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * created by it_hushuai
 * 2020/1/8 21:40
 */
@RestController
@RequestMapping("/seckillGoods")
public class SeckillGoodsController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @RequestMapping("/findAll")
    public List<TbSeckillGoods> findAll(){
        final List<TbSeckillGoods> all = seckillGoodsService.findAll();
        return seckillGoodsService.findAll();
    }

    @RequestMapping("/findOne/{id}")
    public TbSeckillGoods findOne(@PathVariable("id") Long id){
        return seckillGoodsService.findOne(id);
    }

    @RequestMapping("/saveOrder/{id}")
    public Result saveOrder(@PathVariable("id") Long id){
        String userId = UUID.randomUUID().toString();
//        String userId = "hushuai";
        return seckillGoodsService.saveOrder(id, userId);
//        return seckillGoodsService.saveOrder2(id, userId);
    }
}
