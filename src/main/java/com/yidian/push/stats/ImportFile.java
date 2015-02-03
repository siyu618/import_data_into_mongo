package com.yidian.push.stats;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.*;

import java.io.*;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Created by yidianadmin on 15-2-1.
 */
public class ImportFile implements Runnable {
    private String file = null;
    private String processingDay = null;
    private Config config = null;
    public ImportFile(String file, String processingDay, Config config) {
        this.file = file;
        this.processingDay = processingDay;
        this.config = config;
    }

    public static boolean update(String key, Object value, BasicDBObject basicDBObject) {
        String[] arr = key.split("\\.");
        BasicDBObject cur = basicDBObject;
        for (int i = 0; i < arr.length-1;i ++) {
            String subKey = arr[i];
            if (cur.containsField(subKey)) {
                cur = (BasicDBObject)cur.get(subKey);
            }
            else {
                BasicDBObject tmp = new BasicDBObject();
                cur.put(subKey, tmp);
                cur = tmp;
            }
        }
        cur.put(arr[arr.length-1], value);
        return true;
    }

    public void process() throws IOException {
        Mongo dailyMongo = new Mongo(config.getDailyStatMongoHost(), config.getDailyStatMongoPort());
        DB dailyDB = dailyMongo.getDB(config.getDailyStatMongoDB());
        DBCollection dailyStat = dailyDB.getCollection(config.getDailyStatCollection());

        Mongo newsMongo = new Mongo(config.getNewsStatMongoHost(), config.getNewsStatMongoPort());
        DB newsDB = newsMongo.getDB(config.getNewsStatMongoDB());
        DBCollection newsStat = newsDB.getCollection(config.getNewsStatCollection());


        BufferedReader reader = new BufferedReader(new FileReader(this.file));
        String line;
        int count = 0;
        System.out.println(DateUtil.dateToYYYYMMDDHHMMSS(new Date())  + " start to process file [" + file + "]");
        while ((line = reader.readLine()) != null) {
            if (line.contains("#")) {
                String[] arr = line.split("#");
                if (arr.length != 2) {continue;}
                count ++;
                String docId = arr[0];
                String docStat = arr[1];
                JSONObject json = JSON.parseObject(docStat);
                BasicDBObject update = new BasicDBObject();
                update.put("expire_at", DateUtil.incrDate(new Date(), 60));
                for (String str : json.keySet()) {
                    update.append(str, json.get(str));
                }
                BasicDBObject set = new BasicDBObject();
                set.put("$set", update);
                BasicDBObject query = new BasicDBObject();
                query.put("_id", docId);
                newsStat.update(query, set, true, false);
            }
            else { // daily stat
                count ++;
                JSONObject json = JSON.parseObject(line);
                BasicDBObject update = new BasicDBObject();
                update.put("expire_at", DateUtil.incrDate(new Date(), 60));
                for (String str : json.keySet()) {
                    update.append(str, json.get(str));
                }
                BasicDBObject set = new BasicDBObject();
                set.put("$set", update);
                BasicDBObject query = new BasicDBObject();
                query.put("_id", processingDay);
                //System.out.println(GsonFactory.getDefaultGson().toJson(set));
                dailyStat.update(query, set, true, false);
            }
        }


        dailyMongo.close();
        newsMongo.close();
        System.out.println(DateUtil.dateToYYYYMMDDHHMMSS(new Date()) + " end processing file [" + file + "] : count " + count);

    }
    @Override
    public void run() {
        try {
            process();
        }
        catch (Exception e) {
            System.out.println("got error in file: " + file);
            e.printStackTrace();
        }
    }
}
