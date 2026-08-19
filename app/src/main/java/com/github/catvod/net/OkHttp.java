package com.github.catvod.net;

import android.annotation.SuppressLint;

import androidx.collection.ArrayMap;

import com.github.catvod.net.interceptor.AuthInterceptor;
import com.github.catvod.net.interceptor.RequestInterceptor;
import com.github.catvod.net.interceptor.ResponseInterceptor;
import com.github.tvbox.osc.bean.ProxyRule;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import java.net.InetAddress;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;


import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OkHttp {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static OkDns dns;
    private static OkHttpClient client;
    private static OkHttpClient player;
    private static AuthInterceptor authInterceptor;
    private static RequestInterceptor requestInterceptor;
    private static ResponseInterceptor responseInterceptor;
    private static OkProxySelector proxySelector = null;
    private static ProxyAuthenticator proxyAuthenticator = null;
    private static List<String> ips;
    public static ArrayList<String> dnsHttpsList = new ArrayList<>();

    public static synchronized OkDns dns() {
        if (dns == null) dns = new OkDns();
        return dns;
    }

    private static synchronized AuthInterceptor authInterceptor() {
        if (authInterceptor != null) return authInterceptor;
        return authInterceptor = new AuthInterceptor();
    }

    private static synchronized RequestInterceptor requestInterceptor() {
        if (requestInterceptor != null) return requestInterceptor;
        return requestInterceptor = new RequestInterceptor();
    }

    private static synchronized ResponseInterceptor responseInterceptor() {
        if (responseInterceptor != null) return responseInterceptor;
        return responseInterceptor = new ResponseInterceptor();
    }

    public static synchronized OkProxySelector proxySelector() {
        if (proxySelector == null) proxySelector = new OkProxySelector();
        return proxySelector;
    }

    public static synchronized ProxyAuthenticator proxyAuthenticator() {
        if (proxyAuthenticator == null) proxyAuthenticator = new ProxyAuthenticator(proxySelector());
        return proxyAuthenticator;
    }

    public static synchronized void setProxyList(List<ProxyRule> proxyRules) {
        proxySelector().clear();
        if (proxyRules != null && !proxyRules.isEmpty()) proxySelector().addAll(proxyRules);
        client = null;
        dns = null;
    }

    public static synchronized OkHttpClient client() {
        if (client != null) return client;
        return client = getBuilder().build();
    }

    public static synchronized OkHttpClient player() {
        if (player != null) return player;
        return player = getBuilder().build();
    }

    public static OkHttpClient client(long timeout) {
        return client().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).build();
    }

    public static OkHttpClient noRedirect() {
        return noRedirect(TIMEOUT);
    }

    public static OkHttpClient noRedirect(long timeout) {
        return client().newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS).writeTimeout(timeout, TimeUnit.MILLISECONDS).followRedirects(false).followSslRedirects(false).build();
    }

    public static OkHttpClient client(boolean redirect, long timeout) {
        return redirect ? client(timeout) : noRedirect(timeout);
    }

    public static String string(String url) {
        if (url == null || !url.startsWith("http")) return "";
        try (Response res = newCall(url).execute()) {
            return res.body() != null ? res.body().string() : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String string(String url, long timeout) {
        if (url == null || !url.startsWith("http")) return "";
        try (Response res = newCall(client(timeout), url).execute()) {
            return res.body() != null ? res.body().string() : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String string(String url, Map<String, String> headers) {
        if (url == null || !url.startsWith("http")) return "";
        try (Response res = newCall(url, headers).execute()) {
            return res.body() != null ? res.body().string() : "";
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static Call newCall(String url) {
        return client().newCall(new Request.Builder().url(url).build());
    }

    public static Call newCall(String url, String tag) {
        return client().newCall(new Request.Builder().url(url).tag(tag).build());
    }

    public static Call newCall(OkHttpClient client, String url) {
        return client.newCall(new Request.Builder().url(url).build());
    }

    public static Call newCall(OkHttpClient client, String url, String tag) {
        return client.newCall(new Request.Builder().url(url).tag(tag).build());
    }

    public static Call newCall(String url, Map<String, String> headers) {
        return client().newCall(new Request.Builder().url(url).headers(headers(headers)).build());
    }

    public static Call newCall(String url, Map<String, String> headers, ArrayMap<String, String> params) {
        return client().newCall(new Request.Builder().url(buildUrl(url, params)).headers(headers(headers)).build());
    }

    public static Call newCall(String url, Map<String, String> headers, RequestBody body) {
        return client().newCall(new Request.Builder().url(url).headers(headers(headers)).post(body).build());
    }

    public static Call newCall(String url, RequestBody body, String tag) {
        return client().newCall(new Request.Builder().url(url).post(body).tag(tag).build());
    }

    public static Call newCall(OkHttpClient client, String url, RequestBody body) {
        return client.newCall(new Request.Builder().url(url).post(body).build());
    }

    public static void cancel(String tag) {
        cancel(client(), tag);
    }

    public static void cancel(OkHttpClient client, String tag) {
        if (client == null || tag == null) return;
        for (Call call : client.dispatcher().queuedCalls()) if (tag.equals(call.request().tag())) call.cancel();
        for (Call call : client.dispatcher().runningCalls()) if (tag.equals(call.request().tag())) call.cancel();
    }

    public static void cancelAll() {
        cancelAll(client());
    }

    public static void cancelAll(OkHttpClient client) {
        if (client != null) client.dispatcher().cancelAll();
    }

    public static FormBody toBody(ArrayMap<String, String> params) {
        FormBody.Builder body = new FormBody.Builder();
        if (params != null) for (Map.Entry<String, String> entry : params.entrySet()) body.add(entry.getKey(), entry.getValue());
        return body.build();
    }

    private static Headers headers(Map<String, String> headers) {
        return headers == null ? new Headers.Builder().build() : Headers.of(headers);
    }

    private static HttpUrl buildUrl(String url, ArrayMap<String, String> params) {
        HttpUrl.Builder builder = HttpUrl.parse(url).newBuilder();
        if (params != null) for (Map.Entry<String, String> entry : params.entrySet()) builder.addQueryParameter(entry.getKey(), entry.getValue());
        return builder.build();
    }

    private static OkHttpClient.Builder getBuilder() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .addInterceptor(requestInterceptor())
                    .addInterceptor(authInterceptor())
                    .addNetworkInterceptor(responseInterceptor())
                    .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                    .dns(dns())
                    .hostnameVerifier((hostname, session) -> true)
                    .sslSocketFactory(getSSLContext().getSocketFactory(), trustAllCertificates())
                    .proxySelector(proxySelector())
                    .proxyAuthenticator(proxyAuthenticator())
                    .addInterceptor(loggingInterceptor);
        return builder;
    }

    public static void init() {
        initDnsOverHttps();
        client();
        player();
    }

    private static void initDnsOverHttps() {
        for (String dnsName : DNS_PROVIDERS) {
            if (!dnsHttpsList.contains(dnsName)) {
                dnsHttpsList.add(dnsName);
            }
        }

        int dohType = Hawk.get(HawkConfig.DOH_URL, 0);
        ips = getBootstrapIps(dohType);
        
        String dohUrl = getDohUrl(Hawk.get(HawkConfig.DOH_URL, 0));
        dns().setDoh(dohUrl.isEmpty() ? null : HttpUrl.get(dohUrl));
    }

    private static List<String> getIps() {
        return ips == null ? Collections.emptyList() : ips;
    }

    static List<InetAddress> getHosts() {
        try {
            List<InetAddress> list = new ArrayList<>();
            for (String ip : getIps()) list.add(InetAddress.getByName(ip));
            return list.isEmpty() ? null : list;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final String[] DNS_PROVIDERS = {
        "运营商", "腾讯", "阿里", "360", "Google", "Cloudflare", "AdGuard", "DNSWatch", "Quad9"
    };

    public static final String getDohUrl(int type) {
        switch (type) {
            case 1: {
                return "https://doh.pub/dns-query";
            }
            case 2: {
                return "https://dns.alidns.com/dns-query";
            }
            case 3: {
                return "https://doh.360.cn/dns-query";
            }
            case 4: {
                return "https://dns.google/dns-query";
            }
            case 5: {
                return "https://cloudflare-dns.com/dns-query";
            }
            case 6: {
                return "https://unfiltered.adguard-dns.com/dns-query";
            }
            case 7: {
                return "https://resolver2.dns.watch/dns-query";
            }
            case 8: {
                return "https://dns.quad9.net/dns-query";
            }    
        }
        return "";
    }

    private static final List<String> getBootstrapIps(int type) {
        List<String> ipList = new ArrayList<>();
        switch (type) {
        //    case 1: // 腾讯 DNSPod
        //        ipList.add("2402:4e00::");
        //        ipList.add("119.29.29.29");
        //        break;
            case 2: // 阿里云 DNS
                ipList.add("2400:3200::1");
                ipList.add("2400:3200:baba::1");
                ipList.add("223.5.5.5");
                ipList.add("223.6.6.6");
                break;
            case 3: // 360 DNS
                ipList.add("101.226.4.6");
                ipList.add("218.30.118.6");
                break;
            case 4: // Google DNS
                ipList.add("2001:4860:4860::8888");
                ipList.add("2001:4860:4860::8844");
                ipList.add("8.8.8.8");
                ipList.add("8.8.4.4");
                break;
            case 5: // Cloudflare DNS
                ipList.add("2606:4700:4700::1111");
                ipList.add("2606:4700:4700::1001");
                ipList.add("1.1.1.1");
                ipList.add("1.0.0.1");
                break;
            case 6: // AdGuard DNS
                ipList.add("2a10:50c0::1:ff");
                ipList.add("2a10:50c0::2:ff");
                ipList.add("94.140.14.140");
                ipList.add("94.140.14.141");
                break;
            case 7: // DNSWatch DNS
                ipList.add("2001:1608:10:25::1c04:b12f");
                ipList.add("2001:1608:10:25::9249:d69b");
                ipList.add("84.200.69.80");
                ipList.add("84.200.70.40");
                break;
            case 8: // Quad9 DNS
                ipList.add("2620:fe::fe");
                ipList.add("2620:fe::9");
                ipList.add("9.9.9.9");
                ipList.add("149.112.112.112");
                break;
        }
        return ipList;
    }

    private static SSLContext getSSLContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{trustAllCertificates()}, new SecureRandom());
            return context;
        } catch (Throwable e) {
            return null;
        }
    }

    @SuppressLint({"TrustAllX509TrustManager", "CustomX509TrustManager"})
    private static X509TrustManager trustAllCertificates() {
        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
    }
    
}
