package com.hushuai.seckill.task;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

/**
 * created by it_hushuai
 * 2020/3/29 17:33
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath*:spring/applicationContext-*.xml")
public class SeckillTask {

    @Test
    public void testTask() throws IOException {
        while (true){
            System.in.read();
        }
    }
}
