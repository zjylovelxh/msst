package com.zjy.mianshist.BIPM;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.Collections;
import java.util.List;
import java.util.Map;



public class BIPUtils {

    public static BitMapBloomFilter bloomFilter= new BitMapBloomFilter(300);
    public  static Boolean IsBIP(String str){
        return bloomFilter.contains(str);
    }

    public static  void  REBUILDBIPS(String configInfo){

        if (StrUtil.isBlank(configInfo)){
            configInfo="{}";
        }

        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(configInfo, Map.class); // 将yaml格式的字符串转换为map
        List<String> blackips = (List<String>) map.get("blackips");

        synchronized (BIPUtils.class){
            if (CollectionUtil.isNotEmpty(blackips))
            {
                BitMapBloomFilter bitMapBloomFilter = new BitMapBloomFilter(958509);
                for (String blackip : blackips) {
                    bitMapBloomFilter.add(blackip);
                }
                bloomFilter=bitMapBloomFilter;
            }else {
                bloomFilter=new BitMapBloomFilter(300);
            }
        }

    }
}
