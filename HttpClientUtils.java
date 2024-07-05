

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.xiaomi.keycenter.org.apache.http.HttpStatus;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;


@Slf4j
public class HttpClientUtils {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final int DEFAULT_LENGTH = 1000;
    private static final RequestBuilder.Interceptor DEFAULT_LOG_INTERCEPTOR = new RequestBuilder.Interceptor() {
        @Override
        public void beforeHttp(String url, HttpMethod method, String body, Map<String, String> headers) {
            log.info("try to {} the url:{},body is:{},headers: {} ", method, url, StringUtils.left(body, DEFAULT_LENGTH), headers);
        }

        @Override
        public void afterHttp(HttpRequest request, HttpResponse<String> response, Throwable throwable) {
            if (throwable != null) {
                log.error("request url:{} error, exception:", request.uri().getPath(), throwable);
                return;
            }

            if (HttpStatus.SC_MOVED_TEMPORARILY == response.statusCode()) {

                log.info("httpCode: {} from the url:{}, redirect to :{} ",
                        response.statusCode(), request.uri().getPath(), response.headers().firstValue("location").orElse(""));
            } else {
                log.info("httpCode: {} from the url:{},body is:{} ",
                        response.statusCode(), request.uri().getPath(), StringUtils.left(response.body(), DEFAULT_LENGTH));

            }
        }
    };


    private static final List<RequestBuilder.Interceptor> INTERCEPTOR_LIST = Lists.newArrayList(DEFAULT_LOG_INTERCEPTOR);

    public static RequestBuilder post(String url) {
        RequestBuilder builder = new RequestBuilder();
        builder.url = url;
        builder.httpMethod = HttpMethod.POST;
        return builder;
    }

    public static RequestBuilder get(String url) {
        RequestBuilder builder = new RequestBuilder();
        builder.url = url;
        builder.httpMethod = HttpMethod.GET;
        return builder;
    }

    public static class RequestBuilder {

        private final Map<String, String> headers = Maps.newHashMap();
        private final Map<String, Object> requestParameters = Maps.newHashMap();
        private final Map<String, Object> formData = Maps.newHashMap();
        private HttpMethod httpMethod;
        private String body = "";
        private String url;
        private Duration timeout = Duration.ofSeconds(10);

        public static void main(String[] args) {
            System.out.printf(HttpClientUtils.post("http://localhost:28002/api/test")
                    .execute()
                    .body());
        }

        public RequestBuilder requestBody(String body) {
            if (this.httpMethod == HttpMethod.POST) {
                this.body = body;
                //default content type
                header("Content-Type", "application/json");
            }
            return this;
        }

        public RequestBuilder formData(Map<String, Object> formData) {
            if (this.httpMethod == HttpMethod.POST) {
                this.formData.putAll(formData);
                //default content type
                header("Content-Type", "application/x-www-form-urlencoded");
            }
            return this;
        }

        public RequestBuilder formMapData(Map<String, Object> map) {
            if (this.httpMethod == HttpMethod.POST) {
                formData.putAll(map);
                //default content type
                header("Content-Type", "application/x-www-form-urlencoded");
            }
            return this;
        }

        public RequestBuilder formData(String key, Object value) {
            if (this.httpMethod == HttpMethod.POST) {
                formData.put(key, value);
                //default content type
                header("Content-Type", "application/x-www-form-urlencoded");
            }
            return this;
        }

        public RequestBuilder header(String headerKey, String value) {
            this.headers.put(headerKey, value);
            return this;
        }

        public RequestBuilder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public RequestBuilder requestParameters(Map<String, Object> requestParameters) {
            this.requestParameters.putAll(requestParameters);
            return this;
        }

        public RequestBuilder requestParameter(String key, Object value) {
            this.requestParameters.put(key, value);
            return this;
        }

        public RequestBuilder timeout(int timeout) {
            this.timeout = Duration.ofSeconds(timeout);
            return this;
        }

        @SneakyThrows
        public HttpResponse<String> execute() {
            Builder requestBuilder;
            if (httpMethod == HttpMethod.POST) {
                if (Strings.isNullOrEmpty(body) && !formData.isEmpty()) {
                    body = format(formData, false);
                }
                requestBuilder = HttpRequest.newBuilder().POST(body == null ? BodyPublishers.noBody() : BodyPublishers.ofString(body));
            } else {
                requestBuilder = HttpRequest.newBuilder().GET();
            }
            headers.forEach(requestBuilder::header);
            if (timeout != null) {
                requestBuilder.timeout(timeout);
            }
            if (!requestParameters.isEmpty()) {
                StringBuilder parameters = new StringBuilder(url).append("?").append(format(requestParameters, true));
                url = parameters.toString();
            }

            HttpRequest request = null;
            HttpResponse<String> response = null;
            Throwable throwable = null;

            INTERCEPTOR_LIST.forEach(interceptor -> interceptor.beforeHttp(url, httpMethod, body, headers));
            try {
                requestBuilder.uri(new URI(url));
                request = requestBuilder.build();
                response = CLIENT.send(request, BodyHandlers.ofString());
            } catch (Exception ex) {
                throwable = ex;
            } finally {
                HttpRequest finalRequest = request;
                HttpResponse<String> finalResponse = response;
                Throwable finalThrowable = throwable;
                INTERCEPTOR_LIST.forEach(interceptor -> interceptor.afterHttp(finalRequest, finalResponse, finalThrowable));
            }

            if (throwable != null) {
                throw throwable;
            }

            return response;
        }

        private String format(Map<String, Object> data, boolean isEncoded) {
            StringBuilder reduce;
            if (isEncoded) {
                reduce = data.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .reduce(new StringBuilder(),
                                (s, e) -> s.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append("=")
                                        .append(URLEncoder.encode(e.getValue().toString(), StandardCharsets.UTF_8)).append("&"),
                                StringBuilder::append);
            } else {
                reduce = data.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .reduce(new StringBuilder(),
                                (s, e) -> s.append(e.getKey()).append("=")
                                        .append(e.getValue().toString()).append("&"),
                                StringBuilder::append);
            }
            return reduce.substring(0, reduce.length() - 1);
        }

        public interface Interceptor {

            void beforeHttp(String url, HttpMethod method, String body, Map<String, String> headers);

            void afterHttp(HttpRequest request, HttpResponse<String> response, Throwable throwable);
        }
    }
}
