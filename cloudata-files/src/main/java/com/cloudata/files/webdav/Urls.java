package com.cloudata.files.webdav;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class Urls {

    public static String trimLastSlash(String path) {
        int len = path.length();
        if (len == 0) {
            return path;
        }
        char lastChar = path.charAt(len - 1);
        if (lastChar != '/') {
            return path;
        }

        return path.substring(0, len - 1);
    }

    public static String getParentPath(String path) {
        path = trimLastSlash(path);

        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return null;
        }
        return path.substring(0, lastSlashIndex + 1);
    }

    public static String getLastPathComponent(String path, boolean decode) {
        path = trimLastSlash(path);

        int lastSlashIndex = path.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            return null;
        }
        String component = path.substring(lastSlashIndex + 1);
        if (decode) {
            component = decodeComponent(component);
        }
        return component;
    }

    public static String decodeComponent(String component) {
        try {
            return URLDecoder.decode(component, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    public static List<String> pathToTokens(String path) {
        List<String> pathTokens;

        if (path.equals("") || path.equals("/")) {
            pathTokens = Collections.emptyList();
        } else {
            if (path.contains("//")) {
                path = path.replace("//", "/");
            }

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            pathTokens = Lists.newArrayList();
            for (String token : Splitter.on('/').split(path)) {
                pathTokens.add(Urls.decodeComponent(token));
            }
        }
        return pathTokens;
    }

}
