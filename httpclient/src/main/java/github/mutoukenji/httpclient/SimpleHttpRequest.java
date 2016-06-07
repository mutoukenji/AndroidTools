package github.mutoukenji.httpclient;

import android.os.Environment;
import android.support.annotation.NonNull;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 简易HTTP请求工具 Simple Http request sending tool
 * Created by mutoukenji on 2016/6/7.
 */
public class SimpleHttpRequest {
    /**
     * 下载进度接口 Download Progress interface
     */
    public interface DownloadProgress {
        /**
         * 下载进度变化通知 The notification of download progress changed.
         * <p>如果 {@code total} 小于 0 ，则表示服务器未返回文件总长度<br>
         * If {@code total} is less than 0, it is said that the server did not return the file size</p>
         *
         * @param downloaded 已下载大小( byte ) Downloaded bytes
         * @param total      文件总大小( byte ) File size in bytes
         */
        void onDownloadProgressChanged(int downloaded, int total);
    }

    /**
     * 发起POST请求 Send a POST request.
     *
     * @param requestUrl 请求地址 The request url
     * @return 服务器返回内容 The message returned from server
     * @throws IOException   网络通讯异常 Network exception
     * @throws HttpException 服务器返回错误 Error returned from server
     */
    @NonNull
    public static String post(String requestUrl) throws IOException, HttpException {
        return post(requestUrl, new HashMap<String, Object>());
    }

    /**
     * 发起GET请求 Send a GET request.
     *
     * @param requestUrl 请求地址 The request url
     * @return 服务器返回内容 The message returned from server
     * @throws IOException   网络通讯异常 Network exception
     * @throws HttpException 服务器返回错误 Error returned from server
     */
    @NonNull
    public static String get(String requestUrl) throws IOException, HttpException {
        return get(requestUrl, null);
    }

    /**
     * 下载文件 Download file
     *
     * @param requestUrl 请求的文件地址 The request file address
     * @param savePath   文件保存路径 The path for saving downloaded file
     * @return 下载完的文件对象 The file object of downloaded file
     * @throws IOException   网络通讯异常或文件写入异常 Network exception or File writing exception
     * @throws HttpException 服务器返回错误 Error returned from server
     */
    public static File download(String requestUrl, String savePath) throws IOException, HttpException {
        return download(requestUrl, savePath, 8192, null);
    }

    /**
     * 下载文件 Download file
     *
     * @param requestUrl 请求的文件地址 The request file address
     * @param savePath   文件保存路径 The path for saving downloaded file
     * @param listener   进度变化通知监听 The download progress listener
     * @return 下载完的文件对象 The file object of downloaded file
     * @throws IOException   网络通讯异常或文件写入异常 Network exception or File writing exception
     * @throws HttpException 服务器返回错误 Error returned from server
     */
    public static File download(String requestUrl, String savePath, DownloadProgress listener) throws IOException, HttpException {
        return download(requestUrl, savePath, 8192, listener);
    }

    /**
     * 下载文件 Download file
     *
     * @param requestUrl 请求的文件地址 The request file address
     * @return 下载完的文件对象 The file object of downloaded file
     * @throws IOException   网络通讯异常或文件写入异常 Network exception or File writing exception
     * @throws HttpException 服务器返回错误 Error returned from server
     */
    public static File download(String requestUrl) throws IOException, HttpException {
        return download(requestUrl, Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).getPath(), 8192, null);
    }

    /**
     * 下载文件 Download file
     *
     * @param requestUrl       请求的文件地址 The request file address
     * @param savePath         文件保存路径 The path for saving downloaded file
     * @param progressStepSize 每隔多少大小发出一次进度变化通知( byte ) The step size of notification, in bytes
     * @param listener         进度变化通知监听 The download progress listener
     * @return 下载完的文件对象 The file object of downloaded file
     * @throws IOException   网络通讯异常或文件写入异常 Network exception or File writing exception
     * @throws HttpException 服务器返回错误 Error returned from server
     */
    public static File download(String requestUrl, String savePath, int progressStepSize, DownloadProgress listener) throws IOException, HttpException {
        URL url = new URL(requestUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // 允许Input，不使用Cache
        con.setDoInput(true);
        con.setUseCaches(true);
        // 设置以GET方式进行传送
        con.setRequestMethod("GET");
        // 设置RequestProperty
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Charset", "UTF-8");
        int status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            int total = con.getContentLength();
            String raw = con.getHeaderField("Content-Disposition");
            String fileName;
            if (raw != null && raw.contains("=")) {
                fileName = raw.split("=")[1];
            } else {
                fileName = url.getFile();
            }
            if (fileName == null || fileName.length() <= 0) {
                fileName = UUID.randomUUID().toString();
            }
            File file = new File(savePath + File.separatorChar + fileName);
            FileOutputStream fos = new FileOutputStream(file);
            // 获取响应流
            InputStream is = con.getInputStream();
            int length;
            int downloaded = 0;
            byte[] buffer = new byte[progressStepSize];
            while ((length = is.read(buffer)) > 0) {
                downloaded += length;
                fos.write(buffer, 0, length);
                if (listener != null) {
                    listener.onDownloadProgressChanged(downloaded, total);
                }
            }
            fos.flush();
            fos.close();
            is.close();
            return file;
        } else {
            throw new HttpException(con.getResponseMessage());
        }
    }

    /**
     * 发起GET请求 Send a GET request.
     *
     * @param requestUrl 请求地址 The request url
     * @param parameters 参数对 Request data in pair
     * @return 服务器返回内容 The message returned from server
     * @throws IOException   网络通讯异常 Network exception
     * @throws HttpException 服务器返回错误 Error returned from server
     */
    @NonNull
    public static String get(String requestUrl, Map<String, Object> parameters) throws IOException, HttpException {
        if (parameters != null && parameters.size() > 0) {
            if (requestUrl.contains("?")) {
                if (!requestUrl.endsWith("&") && !requestUrl.endsWith("?")) {
                    requestUrl += "&";
                }
            } else {
                requestUrl += "?";
            }
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                requestUrl += entry.getKey() + "=" + URLEncoder.encode(entry.getValue().toString(), "UTF-8");
                requestUrl += "&";
            }
            requestUrl = requestUrl.substring(0, requestUrl.length() - 1);
        }

        StringBuilder sb = new StringBuilder();
        URL url = new URL(requestUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // 允许Input，不使用Cache
        con.setDoInput(true);
        con.setUseCaches(false);
        // 设置以GET方式进行传送
        con.setRequestMethod("GET");
        // 设置RequestProperty
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Charset", "UTF-8");
        int status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            // 获取响应流
            InputStream is = con.getInputStream();
            int ch;
            while ((ch = is.read()) != -1) {
                sb.append((char) ch);
            }
            return sb.toString();
        } else {
            throw new HttpException(con.getResponseMessage());
        }
    }

    /**
     * 发起POST请求 Send a POST request.
     * 如果参数对中的对象是 {@link File} 类型，则按照文件方式提交，否则则会对象的调用 {@code toString} 方法，转换为String类型提交<br>
     * If value in {@code parameters} is an instance of {@link File} class, it will be sent as a file, otherwise, the {@code toString} method of the
     * object will be called to convert value into {@link String} type to be submit
     *
     * @param requestUrl 请求地址 The request url
     * @param parameters 参数对 Request data in pair
     * @return 服务器返回内容 The message returned from server
     * @throws IOException   网络通讯异常 Network exception
     * @throws HttpException 服务器返回错误 Error returned from server
     */
    @NonNull
    public static String post(String requestUrl, Map<String, Object> parameters) throws IOException, HttpException {

        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        StringBuilder sb = new StringBuilder();
        URL url = new URL(requestUrl);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        // 允许Input、Output，不使用Cache
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        // 设置以POST方式进行传送
        con.setRequestMethod("POST");
        // 设置RequestProperty
        con.setRequestProperty("Connection", "Keep-Alive");
        con.setRequestProperty("Charset", "UTF-8");
        con.setRequestProperty("Content-Type",
                "multipart/form-data;boundary=" + boundary);
        // 构造DataOutputStream流
        DataOutputStream ds = new DataOutputStream(con.getOutputStream());
        if (parameters != null && parameters.size() > 0) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                ds.writeBytes(twoHyphens + boundary + end);
                if (File.class.isInstance(entry.getValue())) {
                    File file = (File) entry.getValue();
                    ds.writeBytes("Content-Disposition: form-data; " + String.format(Locale.getDefault(), "name=\"%s\";filename=\"%s\"", entry.getKey(), file.getName()) + end);
                    ds.writeBytes("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName()) + end);
                    ds.writeBytes("Content-Transfer-Encoding: binary" + end);
                    ds.writeBytes(end);
                    // 构造要上传文件的FileInputStream流
                    FileInputStream fis = new FileInputStream(file);
                    // 设置每次写入1024bytes
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    int length;
                    // 从文件读取数据至缓冲区
                    while ((length = fis.read(buffer)) != -1) {
                        // 将资料写入DataOutputStream中
                        ds.write(buffer, 0, length);
                    }
                    ds.writeBytes(end);
                    // 关闭流
                    fis.close();
                    ds.flush();
                } else {
                    ds.writeBytes(String.format(Locale.getDefault(), "Content-Disposition: form-data; name=\"%s\"", entry.getKey()) + end);
                    ds.writeBytes("Content-Type: text/plain; charset=UTF-8" + end);
                    ds.writeBytes(end);
                    ds.writeBytes(entry.getValue().toString());
                    ds.writeBytes(end);
                    ds.flush();
                }
            }
        }
        ds.writeBytes(end);
        ds.writeBytes(twoHyphens + boundary + end);
        // 关闭DataOutputStream
        ds.close();

        int status = con.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            // 获取响应流
            InputStream is = con.getInputStream();
            int ch;
            while ((ch = is.read()) != -1) {
                sb.append((char) ch);
            }
            return sb.toString();
        } else {
            throw new HttpException(con.getResponseMessage());
        }
    }
}
