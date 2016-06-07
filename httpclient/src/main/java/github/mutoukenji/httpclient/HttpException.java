package github.mutoukenji.httpclient;

/**
 * HTTP异常 Http Exception
 * Created by mutoukenji on 2016/6/7.
 */
public class HttpException extends Exception {
    public HttpException() {
    }

    public HttpException(String detailMessage) {
        super(detailMessage);
    }

    public HttpException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public HttpException(Throwable throwable) {
        super(throwable);
    }
}
