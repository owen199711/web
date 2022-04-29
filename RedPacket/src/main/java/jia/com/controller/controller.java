package jia.com.controller;

import jia.com.service.BuyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

@RestController
public class controller {
    @Autowired
    private BuyService bs;
    @GetMapping("/get")
    public String start(@RequestParam("packetName") String packetName,@RequestParam("uid")String uid) {
        Random r=new Random();
        int i=r.nextInt(500);
        //使用乐观锁watch,存在红包遗留问题
//        bs.get(packetName,uid+i);
        //使用lua脚本。
        bs.getByScript(packetName,uid+i);
        return "success";
    }

    @GetMapping("/send")
    public String send(@RequestParam("packetName") String packetName) {
        bs.send(packetName,1000,50);
        return "success";
    }

}
