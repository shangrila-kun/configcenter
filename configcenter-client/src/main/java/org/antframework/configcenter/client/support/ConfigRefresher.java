/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2017-09-13 10:24 创建
 */
package org.antframework.configcenter.client.support;

import org.antframework.configcenter.client.core.ConfigurableConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class ConfigRefresher {
    private static final Logger logger = LoggerFactory.getLogger(ConfigRefresher.class);
    private static final Object REFRESH_ELEMENT = new Object();
    private static final Object STOP_ELEMENT = new Object();

    private ConfigurableConfigProperties configProperties;
    private ServerQuerier serverQuerier;
    private CacheFileHandler cacheFileHandler;
    private BlockingQueue queue;
    private RefreshThread refreshThread;

    public ConfigRefresher(ConfigurableConfigProperties configProperties, ServerQuerier serverQuerier, CacheFileHandler cacheFileHandler) {
        this.configProperties = configProperties;
        this.serverQuerier = serverQuerier;
        this.cacheFileHandler = cacheFileHandler;
        this.queue = new LinkedBlockingQueue();
        this.refreshThread = new RefreshThread();
        this.refreshThread.start();
    }

    public void refresh() throws InterruptedException {
        if (!queue.contains(REFRESH_ELEMENT)) {
            queue.put(REFRESH_ELEMENT);
        }
    }

    public void close() {
        try {
            queue.put(STOP_ELEMENT);
        } catch (InterruptedException e) {
            logger.error("关闭刷新线程失败");
        }
    }

    private class RefreshThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Object element = queue.take();
                    if (element == STOP_ELEMENT) {
                        break;
                    }
                    Map<String, String> properties = serverQuerier.queryProperties();
                    configProperties.replaceProperties(properties);
                    cacheFileHandler.writeProperties(properties);
                } catch (Throwable e) {
                    logger.error("刷新配置出错：", e);
                }
            }
        }
    }
}