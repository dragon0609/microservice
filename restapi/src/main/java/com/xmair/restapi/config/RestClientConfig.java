package com.xmair.restapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.ribbon.ClientOptions;
import io.netty.handler.ssl.SslContextBuilder;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.internal.connection.RealConnection;
import okhttp3.internal.http2.Settings;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.okhttp.RetryableOkHttpLoadBalancingClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.AsyncClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import scala.util.Try;

import javax.net.ssl.SSLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * resttemplate使用okhttp连接池
 * */
@Configuration
public class RestClientConfig {

    /**
     * 注入okhttp客户端工具类，全局唯一，共享连接池，线程安全
     */
    @Bean
    public OkHttpClient okHttpClient() {
        //注意：只有明确知道服务端支持H2C协议的时候才能使用。添加H2C支持，
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
       // .protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE));


        Dispatcher dispatcher=new Dispatcher();
        //设置连接池大小
        dispatcher.setMaxRequests(1000);
        dispatcher.setMaxRequestsPerHost(200);
       ConnectionPool pool = new ConnectionPool(40, 10, TimeUnit.MINUTES);



        builder.connectTimeout(550, TimeUnit.MILLISECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .connectionPool(pool)

                .dispatcher(dispatcher)

                .addNetworkInterceptor(new OkHttpInterceptor())
                .retryOnConnectionFailure(false);
        return builder.build();
    }

    @Primary
    @Bean
    public ClientHttpRequestFactory OkHttp3Factory() {

        return new OkHttp3ClientHttpRequestFactory(okHttpClient());
    }


    private AsyncClientHttpRequestFactory AsyncClientHttpRequestFactory() {

        return new OkHttp3ClientHttpRequestFactory(okHttpClient());
    }

    @LoadBalanced
    @Bean
    public AsyncRestTemplate asyncRestTemplate(){
        AsyncRestTemplate restTemplate=new AsyncRestTemplate(AsyncClientHttpRequestFactory());
        return  restTemplate;

    }
    @Autowired
    private ObjectMapper objectMapper;

    @Primary
    @LoadBalanced
    @Bean
    public RestTemplate restTemplateLB() {

        RestTemplate restTemplate= new RestTemplate(OkHttp3Factory());
       // RestTemplate restTemplate= new RestTemplate(nettyFactory());
        SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(myDateFormat);

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        MappingJackson2HttpMessageConverter jsonMessageConverter = new MappingJackson2HttpMessageConverter();
        jsonMessageConverter.setObjectMapper(objectMapper);
        messageConverters.add(jsonMessageConverter);
        restTemplate.setMessageConverters(messageConverters);
        return  restTemplate;
    }

    @Bean(name = "signleTemplate")
    public RestTemplate restTemplate() {
        RestTemplate restTemplate= new RestTemplate(OkHttp3Factory());
        SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(myDateFormat);

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        messageConverters.add(new FormHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        MappingJackson2HttpMessageConverter jsonMessageConverter = new MappingJackson2HttpMessageConverter();
        jsonMessageConverter.setObjectMapper(objectMapper);
        messageConverters.add(jsonMessageConverter);
        restTemplate.setMessageConverters(messageConverters);

        return restTemplate;
    }


}
