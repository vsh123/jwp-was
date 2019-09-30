package http.response;

import http.common.Cookie;
import http.common.HeaderFields;
import http.common.HttpStatus;
import http.common.HttpVersion;
import http.request.HttpRequest;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import utils.FileIoUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static http.common.HttpStatus.NOT_FOUND;

public class HttpResponse {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponse.class);
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String LOCATION = "Location";
    public static final String CHARSET_UTF_8 = "charset=utf-8";

    private HttpRequest request;
    private HttpStatus status;
    private final HeaderFields headerFields;
    private byte[] body;

    public HttpResponse(HttpRequest request) {
        this.request = request;
        headerFields = new HeaderFields(new ArrayList<>());
    }

    public void forward(String path) {
        try {
            status = HttpStatus.OK;
            body = FileIoUtils.loadFileFromClasspath(path);
            String type = new Tika().detect(path);

            headerFields.addHeader(CONTENT_TYPE, type + ";" + CHARSET_UTF_8);
            headerFields.addHeader(CONTENT_LENGTH, String.valueOf(body.length));
        } catch (IOException | URISyntaxException e) {
            logger.error(e.getMessage());
            forwardErrorPage(NOT_FOUND);
        }
    }

    public void forwardErrorPage(HttpStatus status) {
        this.status = status;
        try {
            body = FileIoUtils.loadFileFromClasspath("/error.html");

            headerFields.addHeader(CONTENT_TYPE, "text/html;" + CHARSET_UTF_8);
            headerFields.addHeader(CONTENT_LENGTH, String.valueOf(body.length));
        } catch (IOException | URISyntaxException e) {
            logger.error(e.getMessage());
        }
    }

    public void forward(byte[] body) {
        status = HttpStatus.OK;
        this.body = body;
        String type = new Tika().detect(body);

        headerFields.addHeader(CONTENT_TYPE, type + ";" + CHARSET_UTF_8);
        headerFields.addHeader(CONTENT_LENGTH, String.valueOf(body.length));
    }

    public void sendRedirect(String location) {
        status = HttpStatus.FOUND;
        headerFields.addHeader(LOCATION, location);
    }

    public String convert() {
        setSessionIdCookie();
        if (StringUtils.isEmpty(body)) {
            return convertHeader();
        }
        return convertHeader() + new String(body);
    }

    private String convertHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(HttpVersion.HTTP_1_1.getVersion()).append(" ")
                .append(status.getStatusCode()).append(" ")
                .append(status.getStatusName()).append("\r\n");
        sb.append(headerFields.convert());
        sb.append("\r\n");

        logger.debug("\n--response Header--\n{}", sb.toString());
        return sb.toString();
    }

    private void setSessionIdCookie() {
        if (request.isCreatedSession()) {
            addCookie("sessionId", request.getSession().getId());
        }
    }

    public void addHeader(String fieldName, String field) {
        headerFields.addHeader(fieldName, field);
    }

    public void addCookie(String name, String value) {
        headerFields.addCookie(name, value);
    }

    public void addCookieOption(String name, String option, String value) {
        headerFields.addCookieOption(name, option, value);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getHeader(String fieldName) {
        return headerFields.getHeader(fieldName);
    }

    public Cookie getCookie(String cookieName) {
        return headerFields.getCookie(cookieName);
    }

    public byte[] getBody() {
        return body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpResponse response = (HttpResponse) o;
        return status == response.status &&
                Objects.equals(headerFields, response.headerFields) &&
                Arrays.equals(body, response.body);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(status, headerFields);
        result = 31 * result + Arrays.hashCode(body);
        return result;
    }

    @Override
    public String toString() {
        return convert();
    }

}
