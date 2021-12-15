package com.songwj.openvideo.cache;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.file.Md5FileNameGenerator;

import java.io.File;

/**
 * 使用AndroidVideoCache进行缓存，只要将url经过HttpProxyCacheServer转化就可以了，AndroidVideoCache会处理缓存
 * String url = "XXXXXXXXXXX";
 * HttpProxyCacheServer proxy = HttpProxyCacheUtil.getAudioProxy()
 * url = proxy.getProxyUrl(url);
 */
public class HttpProxyCacheManager {
    private volatile HttpProxyCacheServer proxy;
//    Environment.getExternalStorageDirectory() + "/OpenVideo/cache/"
    private static String cacheDirectory = ""; // 缓存路径
    private static long maxCacheSize = 4 * 1024 * 1024 * 1024L; // 缓存大小：默认4G
    private static int maxCacheFilesCount = 200; // 缓存文件个数：默认200个
    private static boolean isSetExternalStorageCacheDirectory = false; // 是否成功指定缓存路径设置

    private HttpProxyCacheManager() {
        cacheDirectory = Environment.getExternalStorageDirectory() + "/OpenVideo/cache/";
    }

    private static class SingletonHolder {
        private static HttpProxyCacheManager instance = new HttpProxyCacheManager();
    }

    public static HttpProxyCacheManager getInstance() {
        return SingletonHolder.instance;
    }

    private void setProxyConfig(String cacheDirectory,
                           long maxCacheSize,
                           int maxCacheFilesCount) {
        this.cacheDirectory = cacheDirectory;
        this.maxCacheSize = maxCacheSize;
        this.maxCacheFilesCount = maxCacheFilesCount;
    }

    public String getCacheDirectory(Context context) {
        if (proxy != null) {
            if (isSetExternalStorageCacheDirectory) {
                return new File(cacheDirectory).getAbsolutePath();
            } else {
                return StorageUtils.getIndividualCacheDirectory(context).getAbsolutePath();
            }
        } else {
            getProxy(context);
            return getCacheDirectory(context);
        }
    }

    public HttpProxyCacheServer getProxy(Context context) {
        if (proxy == null) {
            synchronized (HttpProxyCacheManager.class) {
                if (proxy == null) {
                    boolean isCacheDirectory = true;
                    File file = null;
                    if (!TextUtils.isEmpty(cacheDirectory)) {
//                        Environment.getExternalStorageDirectory() + "/FirstGrid/cache/"
                        file = new File(cacheDirectory);
                        if (file != null) {
                            if (!file.exists()) {
                                file.mkdirs();
                            }
                            file = new File(cacheDirectory);
                            if (!file.exists()) {
                                isCacheDirectory = false;
                            }
                        } else {
                            isCacheDirectory = false;
                        }

                    } else {
                        isCacheDirectory = false;
                    }
                    if (isCacheDirectory && file != null) {
                        isSetExternalStorageCacheDirectory = true;
                        proxy = new HttpProxyCacheServer.Builder(context)
                                .cacheDirectory(file)
                                .maxCacheSize(maxCacheSize) // 缓存大小
                                .maxCacheFilesCount(maxCacheFilesCount) // 缓存文件个数
//                                .fileNameGenerator(new CacheFileNameGenerator())
                                .build();
                    } else {
                        isSetExternalStorageCacheDirectory = false;
                        proxy = new HttpProxyCacheServer.Builder(context)
                                .maxCacheSize(maxCacheSize) // 缓存大小
                                .maxCacheFilesCount(maxCacheFilesCount) // 缓存文件个数
//                                .fileNameGenerator(new CacheFileNameGenerator())
                                .build();
                    }
                }
            }
        }
        return proxy;
    }

    public void clearCurrentCache(Context context, String url) {
        if (proxy != null) {
            String cacheUrl = proxy.getProxyUrl(url);
            String currentCacheDirectory = getCacheDirectory(context);
            if (cacheUrl.contains("127.0.0.1")) {
                //是否为缓存了未完成的文件
                Md5FileNameGenerator md5FileNameGenerator = new Md5FileNameGenerator();
                String name = md5FileNameGenerator.generate(url);
                String path = currentCacheDirectory + File.separator + name + ".download";
                CommonUtil.deleteFile(path);
            } else {
                CommonUtil.deleteFile(cacheUrl.replace("file://", ""));
            }
        }
    }

    public void clearAllCache(Context context) {
        if (proxy != null) {
            String currentCacheDirectory = getCacheDirectory(context);
            CommonUtil.deleteFile(currentCacheDirectory);
        }
    }

    public void clearAllCache(Context context, String type) {
        if (proxy != null) {
            String currentCacheDirectory = getCacheDirectory(context);
            deleteFiles(new File(currentCacheDirectory), type);
        }
    }

    public void deleteFiles(File root, String type) {
        File files[] = root.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.isDirectory() && f.exists() && f.getName().endsWith(type)) { // 判断是否存在
                    try {
                        f.delete();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static class Builder {
        private Context context;
        private String cacheDirectory = ""; // 缓存路径
        private long maxCacheSize = 2 * 1024 * 1024 * 1024L; // 缓存大小：默认200M
        private int maxCacheFilesCount = 100; // 缓存文件个数：默认100个

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setCacheDirectory(String cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
            return this;
        }

        public Builder setMaxCacheSize(long maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        public Builder setMaxCacheFilesCount(int maxCacheFilesCount) {
            this.maxCacheFilesCount = maxCacheFilesCount;
            return this;
        }

        public HttpProxyCacheManager build() {
            getInstance().setProxyConfig(cacheDirectory, maxCacheSize, maxCacheFilesCount);
            return getInstance();
        }
    }
}
