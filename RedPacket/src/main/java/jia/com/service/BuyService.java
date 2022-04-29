package jia.com.service;

import jia.com.common.config.JedisPoolUtil;

import org.springframework.stereotype.Service;
import jia.com.util.RedPacketUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lonel on 2016/12/21.
 */
@Service
public class BuyService {

    private ArrayList<Integer> redPackets;

    public Object get(String key, String uid) {
        Jedis jedis= JedisPoolUtil.getJedisPoolInstance().getResource();
        //监视ck
        String ck=key+"ck";
        String mk=key+"mk";
        String uk=key+"uk";
        jedis.watch(ck);

        //判断用户是否已经抢过
        if(jedis.sismember(uk,uid)) {
            System.out.println( uid + "------已经抢过红包------");
            jedis.close();
            return false;
        }

        //判断是否有剩余
        String count=jedis.get(ck);
        String money=jedis.get(mk);
        if(Integer.parseInt(count)<=0){
            System.out.println("红包已经抢完***************");
            jedis.close();
            return false;
        }
        //开启事务
        Transaction ts= jedis.multi();
        int m= RedPacketUtil.splitRedPacket(Integer.parseInt(money),Integer.parseInt(count));
        ts.decr(ck);
        ts.decrBy(mk,m);
        ts.sadd(uk,uid);
        List<Object> results=ts.exec();
        if(results==null||results.size()==0){
            System.out.println(uid+"抢红包失败!!!!!");
            jedis.close();
            return false;
        }

        System.out.println(uid+"抢到红包------"+m);
        jedis.close();
        return true;
    }

    public Object getByScript(String key,String uid)
    {
         Jedis jedis= JedisPoolUtil.getJedisPoolInstance().getResource();
         String secKillScript =
                 "local key=KEYS[1];\r\n" +
                 "local uid=KEYS[2];\r\n" +
                 "local ck=key..'ck';\r\n" +
                 "local mk=key..'mk';\r\n" +
                 "local uk=key..'uk';\r\n" +
                 "local mls=key..'mls';\r\n" +
                 "local userExists=redis.call(\"sismember\",uk,uid);\r\n" +
                 "if tonumber(userExists)==1 then \r\n" +
                 "   return 2;\r\n" +
                 "end\r\n" +
                 "local count= redis.call(\"get\" ,ck);\r\n" +
                "if tonumber(count)<=0 then \r\n" +
                "   return 0;\r\n" +
                "else \r\n" +
                "   redis.call(\"decr\",ck);\r\n" +
                "   local money=tonumber(redis.call(\"lpop\",mls));\r\n" +
                "   redis.call(\"decrby\",mk,money);\r\n" +
                "   redis.call(\"sadd\",uk,uid);\r\n" +
                "end\r\n" +
                "return 1" ;

        String sha=  jedis.scriptLoad(secKillScript);

        Object result= jedis.evalsha(sha, 2,key,uid);

        String reString=String.valueOf(result);
        if ("0".equals( reString )  ) {
            System.err.println("*****红包已经发完！！");
        }else if("1".equals( reString )){
            System.err.println(uid+"恭喜你抢到了红包");
        }else if("2".equals( reString )  )  {
            System.err.println("该用户已抢过！！");
        }else{
            System.out.println(".....抢红包异常.......");
        }
        jedis.close();
        return true;

    }
    //发红包
    public void send(String uid,int totalMoney,int totalCont){
        Jedis jedis= JedisPoolUtil.getJedisPoolInstance().getResource();
        String ck=uid+"ck";//数量
        String mk=uid+"mk";//金额
        String mls=uid+"mls";//红包列表
        redPackets= (ArrayList<Integer>) RedPacketUtil.splitRedPackets(totalMoney,totalCont);

        jedis.set(ck,totalCont+"");
        jedis.set(mk,totalMoney+"");
        for(Integer i: redPackets)
              jedis.lpush(mls,i.toString());
        jedis.close();
    }


}
