package com.songwj.openvideo.cache;

import android.net.Uri;

import com.danikula.videocache.file.FileNameGenerator;

import java.util.List;

public class CacheFileNameGenerator implements FileNameGenerator {

    private static final String TAG = "CacheFileNameGenerator";

    /**
     * @param url
     * @return
     */
    @Override
    public String generate(String url) {
        Uri uri = Uri.parse(url);
        List<String> pathSegList = uri.getPathSegments();
        String path = null;
        if (pathSegList != null && pathSegList.size() > 0) {
            path = pathSegList.get(pathSegList.size() - 1);
        } else {
            path = url;
        }
        return path;
    }
}
