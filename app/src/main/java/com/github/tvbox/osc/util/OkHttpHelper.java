package com.github.tvbox.osc.util;

import static okhttp3.ConnectionSpec.CLEARTEXT;
import static okhttp3.ConnectionSpec.COMPATIBLE_TLS;
import static okhttp3.ConnectionSpec.MODERN_TLS;
import static okhttp3.ConnectionSpec.RESTRICTED_TLS;

import com.github.catvod.net.OkHttp;
import com.github.catvod.net.SSLCompat;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.bean.ProxyRule;
import com.github.tvbox.osc.util.net.OkProxySelector;
import com.github.tvbox.osc.util.net.ProxyAuthenticator;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.List;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.Cache;
import okhttp3.ConnectionSpec;
import okhttp3.dnsoverhttps.DnsOverHttps;
import okhttp3.HttpUrl;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import xyz.doikki.videoplayer.exo.ExoMediaSourceHelper;

public class OkHttpHelper {
    public static final long DEFAULT_MILLISECONDS = 10000;  //默认的超时时间
    static OkHttpClient ItvClient = null;
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
    
    static void initExoOkHttpClient() {
        OkHttpClient base = getDefaultClient();
        OkHttpClient.Builder builder = base != null ? base.newBuilder() : new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        builder.addInterceptor(loggingInterceptor);
        builder.connectionSpecs(getConnectionSpec());
        builder.retryOnConnectionFailure(true);
        builder.followRedirects(true);
        builder.followSslRedirects(true);
        builder.proxySelector(proxySelector());
        builder.proxyAuthenticator(proxyAuthenticator());

        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        builder.dns(dnsOverHttps);
        ItvClient = builder.build();

        ExoMediaSourceHelper.getInstance(App.getInstance()).setOkClient(ItvClient);
    }

    public static DnsOverHttps dnsOverHttps = null;
    public static ArrayList<String> dnsHttpsList = new ArrayList<>();

    public static List<ConnectionSpec> getConnectionSpec() {
        return Arrays.asList(RESTRICTED_TLS, MODERN_TLS, COMPATIBLE_TLS, CLEARTEXT);
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
                return "https://dns.adguard.com/dns-query";
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

    static void initDnsOverHttps() {
        if (!dnsHttpsList.contains("运营商")) dnsHttpsList.add("运营商");
        if (!dnsHttpsList.contains("腾讯")) dnsHttpsList.add("腾讯");
        if (!dnsHttpsList.contains("阿里")) dnsHttpsList.add("阿里");
        if (!dnsHttpsList.contains("360")) dnsHttpsList.add("360");
        if (!dnsHttpsList.contains("Google")) dnsHttpsList.add("Google");
        if (!dnsHttpsList.contains("Cloudflare")) dnsHttpsList.add("Cloudflare");
        if (!dnsHttpsList.contains("AdGuard")) dnsHttpsList.add("AdGuard");
        if (!dnsHttpsList.contains("DNSWatch")) dnsHttpsList.add("DNSWatch");
        if (!dnsHttpsList.contains("Quad9")) dnsHttpsList.add("Quad9");
        
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.proxySelector(proxySelector());
        builder.proxyAuthenticator(proxyAuthenticator());
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }
        builder.addInterceptor(loggingInterceptor);
        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }
        builder.connectionSpecs(getConnectionSpec());
        builder.cache(new Cache(new File(App.getInstance().getCacheDir().getAbsolutePath(), "dohcache"), 10 * 1024 * 1024));
        OkHttpClient dohClient = builder.build();
        String dohUrl = getDohUrl(Hawk.get(HawkConfig.DOH_URL, 0));
        dnsOverHttps = new DnsOverHttps.Builder().client(dohClient).url(dohUrl.isEmpty() ? null : HttpUrl.get(dohUrl)).bootstrapDnsHosts(getHosts()).build();
    }

    static OkHttpClient defaultClient = null;
    static OkHttpClient noRedirectClient = null;

    public static OkHttpClient getDefaultClient() {
        return defaultClient;
    }

    public static OkHttpClient getNoRedirectClient() {
        return noRedirectClient;
    }

    public static void init() {
        initDnsOverHttps();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        //builder.retryOnConnectionFailure(false);
        builder.addInterceptor(loggingInterceptor);
        builder.connectionSpecs(getConnectionSpec());
        builder.readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.dns(dnsOverHttps);
        builder.proxySelector(proxySelector());
        builder.proxyAuthenticator(proxyAuthenticator());
        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        OkHttpClient okHttpClient = builder.build();
        defaultClient = okHttpClient;
        
        builder.followRedirects(false);
        builder.followSslRedirects(false);
        noRedirectClient = builder.build();

        initExoOkHttpClient();        
    }

    public static synchronized void reloadDns() {
        initDnsOverHttps();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        builder.addInterceptor(loggingInterceptor);
        builder.connectionSpecs(getConnectionSpec());
        builder.readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        builder.dns(dnsOverHttps);
        builder.proxySelector(proxySelector());
        builder.proxyAuthenticator(proxyAuthenticator());
        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        OkHttpClient okHttpClient = builder.build();
        defaultClient = okHttpClient;
        
        builder.followRedirects(false);
        builder.followSslRedirects(false);
        noRedirectClient = builder.build();

        initExoOkHttpClient();
        OkHttp.resetClient();
    }

    private static synchronized OkHttpClient.Builder setOkHttpSsl(OkHttpClient.Builder builder) {
        try {
            final SSLSocketFactory sslSocketFactory = new SSLCompat();
            return builder
                   .sslSocketFactory(sslSocketFactory, SSLCompat.TM)
                   .hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
