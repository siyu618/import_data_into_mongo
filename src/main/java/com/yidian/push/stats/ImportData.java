package com.yidian.push.stats;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by yidianadmin on 15-2-1.
 */

public class ImportData {
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage : ImportData <config_file> <processing_file>  <file> [<file1> ...]");
        }
        String configFile = args[0];
        String processingDay = args[1];
        List<String> files = new LinkedList<String>();
        for (int i = 2; i < args.length; i ++) {
            files.add(args[i]);
        }

        Config.setCONFIG_FILE(configFile);
        Config config = Config.getInstance();
        ExecutorService pool = Executors.newCachedThreadPool();
        System.out.println(DateUtil.dateToYYYYMMDDHHMMSS(new Date()) + ": start");
        for (String file : files) {
            pool.execute(new ImportFile(file, processingDay, config));
        }
        pool.shutdown();
        System.out.println(DateUtil.dateToYYYYMMDDHHMMSS(new Date()) + ": done");

    }
}
