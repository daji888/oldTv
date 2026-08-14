package com.github.tvbox.osc.util;

import android.annotation.SuppressLint;

import com.github.catvod.net.OkHttp;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.ProxyRule;
import com.github.tvbox.osc.util.net.OkProxySelector;
import com.github.tvbox.osc.util.net.ProxyAuthenticator;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.dnsoverhttps.DnsOverHttps;
import okhttp3.HttpUrl;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import xyz.doikki.videoplayer.exo.ExoMediaSourceHelper;

public class OkHttpHelper {
    private static final long DEFAULT_MILLISECONDS = 30000;  //默认的超时时间
    static OkHttpClient ItvClient = null;
    public static DnsOverHttps dnsOverHttps = null;
    public static ArrayList<String> dnsHttpsList = new ArrayList<>();
    static OkHttpClient defaultClient = null;
    static OkHttpClient noRedirectClient = null;
    private static OkHttpClient dohClient; // 缓存DOH专用的OkHttpClient
    private static List<String> ips;
    private static OkProxySelector proxySelector = null;
    private static ProxyAuthenticator proxyAuthenticator = null;

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
        OkHttp.reset();
    }

    // 统一封装通用的OkHttpClient.Builder配置逻辑
    private static OkHttpClient.Builder createBaseBuilder() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // 统一配置日志拦截器，根据DEBUG开关控制日志级别
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        builder.addInterceptor(loggingInterceptor);
        builder.addInterceptor(OkHttp.requestInterceptor());
        builder.addInterceptor(OkHttp.authInterceptor());
        builder.addNetworkInterceptor(OkHttp.responseInterceptor());
        // 统一配置HTTPS忽略证书校验的逻辑
        builder.hostnameVerifier((hostname, session) -> true);
        builder.sslSocketFactory(getSSLContext().getSocketFactory(), trustAllCertificates());
        // 统一配置代理相关规则
        builder.proxySelector(proxySelector());
        builder.proxyAuthenticator(proxyAuthenticator());
        return builder;
    }
    
    static void initExoOkHttpClient() {
        OkHttpClient.Builder builder = getDefaultClient() != null 
                ? getDefaultClient().newBuilder() 
                : createBaseBuilder();
        // 补充ExoPlayer专属的额外配置
        builder.retryOnConnectionFailure(true);
        builder.followRedirects(true);
        builder.followSslRedirects(true);
        if (dnsOverHttps != null) {
            builder.dns(dnsOverHttps);
        }
        ItvClient = builder.build();
        ExoMediaSourceHelper.getInstance(App.getInstance()).setOkClient(ItvClient);
    }

    private static List<String> getIps() {
        return ips == null ? Collections.emptyList() : ips;
    }

    private static List<InetAddress> getHosts() {
        try {
            List<InetAddress> list = new ArrayList<>();
            for (String ip : getIps()) list.add(InetAddress.getByName(ip));
            return list.isEmpty() ? null : list;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getDohUrl(int type) {
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

    private static List<String> getBootstrapIps(int type) {
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

    public static synchronized void setDoh(HttpUrl dohUrl) {
        dnsOverHttps = buildDnsOverHttps(dohUrl);
    }

    static void initDnsOverHttps() {
        // 初始化公共DNS列表
        String[] dnsNames = {"运营商", "腾讯", "阿里", "360", "Google", "Cloudflare", "AdGuard", "DNSWatch", "Quad9"};
        for (String dnsName : dnsNames) {
            if (!dnsHttpsList.contains(dnsName)) {
                dnsHttpsList.add(dnsName);
            }
        }

        int dohType = Hawk.get(HawkConfig.DOH_URL, 0);
        ips = getBootstrapIps(dohType);
        
        String dohUrl = getDohUrl(Hawk.get(HawkConfig.DOH_URL, 0));
        dnsOverHttps = buildDnsOverHttps(dohUrl.isEmpty() ? null : HttpUrl.get(dohUrl));
    }

    private static DnsOverHttps buildDnsOverHttps(HttpUrl dohUrl) {
        if (dohUrl == null) {
            return null;
        }
        
        // 懒加载DOH客户端，避免重复创建
        if (dohClient == null) {
            OkHttpClient.Builder builder = createBaseBuilder();
            builder.cache(new Cache(
                new File(App.getInstance().getCacheDir().getAbsolutePath(), "dohcache"), 
                10 * 1024 * 1024
            ));
            dohClient = builder.build();
        }
        
        return new DnsOverHttps.Builder()
                .client(dohClient)
                .url(dohUrl)
                .bootstrapDnsHosts(getHosts())
                .build();
    }

    public static OkHttpClient getDefaultClient() {
        return defaultClient;
    }

    public static OkHttpClient getNoRedirectClient() {
        return noRedirectClient;
    }

    public static void init() {
        initDnsOverHttps();
        // 复用通用Builder，补充基础超时配置
        OkHttpClient.Builder builder = createBaseBuilder();
        builder.readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        if (dnsOverHttps != null) {
            builder.dns(dnsOverHttps);
        }
        defaultClient = builder.build();
        
        // 基于已有Builder快速生成禁止重定向的客户端，避免重复创建配置
        builder.followRedirects(false);
        builder.followSslRedirects(false);
        noRedirectClient = builder.build();

        initExoOkHttpClient();        
    }

    public static SSLContext getSSLContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{trustAllCertificates()}, new SecureRandom());
            return context;
        } catch (Throwable e) {
            return null;
        }
    }

    @SuppressLint({"TrustAllX509TrustManager", "CustomX509TrustManager"})
    public static X509TrustManager trustAllCertificates() {
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
