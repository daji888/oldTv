package com.github.tvbox.osc.util;

import androidx.annotation.NonNull;

import static okhttp3.ConnectionSpec.CLEARTEXT;
import static okhttp3.ConnectionSpec.COMPATIBLE_TLS;
import static okhttp3.ConnectionSpec.MODERN_TLS;
import static okhttp3.ConnectionSpec.RESTRICTED_TLS;
import com.github.catvod.net.SSLCompat;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.api.ApiConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.orhanobut.hawk.Hawk;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.Cache;
import okhttp3.ConnectionSpec;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.dnsoverhttps.DnsOverHttps;
import okhttp3.internal.Util;
import xyz.doikki.videoplayer.exo.ExoMediaSourceHelper;

public class OkGoHelper {
    public static final long DEFAULT_MILLISECONDS = 10000;      //默认的超时时间

    //https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/200
    public static HashMap<Integer, String > httpPhaseMap  = new HashMap<Integer, String>(){{
        put(200,"OK");
        put(301,"Moved Permanently");
        put(302,"Found");
        put(400,"Bad Request");
        put(401,"Unauthorized");
        put(403,"Forbidden");
        put(404,"Not Found");
        put(429,"Too Many Requests");
        put(500,"Internal Server Error");
        put(502,"Bad Gateway");
        put(503,"Service Unavailable");
        put(504,"Gateway Timeout");
    }};

    // 内置doh json
    private static final String dnsConfigJson = "["
            + "{\"name\": \"腾讯\", \"url\": \"https://doh.pub/dns-query\"},"
            + "{\"name\": \"阿里\", \"url\": \"https://dns.alidns.com/dns-query\"},"
            + "{\"name\": \"360\", \"url\": \"https://doh.360.cn/dns-query\"},"
            + "{\"name\": \"Google\", \"url\": \"https://dns.google/dns-query\"},"
            + "{\"name\": \"AdGuard\", \"url\": \"https://dns.adguard.com/dns-query\"},"
            + "{\"name\": \"Quad9\", \"url\": \"https://dns.quad9.net/dns-query\"}"
            + "]";

    static OkHttpClient ItvClient = null;

    static void initExoOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkExoPlayer");

        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
        }
        builder.addInterceptor(loggingInterceptor);
        builder.connectionSpecs(getConnectionSpec());
        builder.retryOnConnectionFailure(true);
        builder.followRedirects(true);
        builder.followSslRedirects(true);


        try {
            setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }
     //   builder.dns(dnsOverHttps);
        builder.dns(new CustomDns(dnsOverHttps));
        ItvClient = builder.build();

        ExoMediaSourceHelper.getInstance(App.getInstance()).setOkClient(ItvClient);
    }

    public static DnsOverHttps dnsOverHttps = null;

    public static ArrayList<String> dnsHttpsList = new ArrayList<>();

    public static List<ConnectionSpec> getConnectionSpec() {
        return Util.immutableListOf(RESTRICTED_TLS, MODERN_TLS, COMPATIBLE_TLS, CLEARTEXT);
    }

    public static boolean is_doh = false;
    
    public static Map<String,String> myHosts = null;

    public static String getDohUrl(int type) {
   /*     switch (type) {
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
                // return "https://1.1.1.1/dns-query";   // takagen99 - removed Cloudflare
                return "https://dns.google/dns-query";
            }
            case 5: {
                return "https://dns.adguard.com/dns-query";
            }
            case 6: {
                return "https://dns.quad9.net/dns-query";
            }  */
        String json = Hawk.get(HawkConfig.DOH_JSON,"");
        if (json.isEmpty()) json = dnsConfigJson;
        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
        if (type >= 1 && type < dnsHttpsList.size()) {
            JsonObject dnsConfig = jsonArray.get(type - 1).getAsJsonObject();
            return dnsConfig.get("url").getAsString();  // 获取对应的 URL        
        }
        return "";
    }

    public static void setDnsList() {
        dnsHttpsList.clear();
        String json = Hawk.get(HawkConfig.DOH_JSON,"");
        if (json.isEmpty()) json = dnsConfigJson;
        JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
        dnsHttpsList.add("运营商");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject dnsConfig = jsonArray.get(i).getAsJsonObject();
            String name = dnsConfig.has("name") ? dnsConfig.get("name").getAsString() : "Unknown Name";
            dnsHttpsList.add(name);
        }
        if (Hawk.get(HawkConfig.DOH_URL, 0) + 1 > dnsHttpsList.size()) Hawk.put(HawkConfig.DOH_URL, 0);

    }

    private static List<InetAddress> DohIps(JsonArray ips) {
        List<InetAddress> inetAddresses = new ArrayList<>();
        if (ips != null) {
            for (int j = 0; j < ips.size(); j++) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(ips.get(j).getAsString());
                    inetAddresses.add(inetAddress);  // 添加到 List 中
                } catch (Exception e) {
                    e.printStackTrace();  // 处理无效的 IP 字符串
                }
            }
        }
        return inetAddresses;
    }

    static void initDnsOverHttps() {
        Integer dohSelector = Hawk.get(HawkConfig.DOH_URL, 0);
        JsonArray ips = null;
        try {
            dnsHttpsList.add("运营商");
            String json = Hawk.get(HawkConfig.DOH_JSON,"");
            if (json.isEmpty()) json = dnsConfigJson;
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            if (dohSelector + 1 > jsonArray.size()) Hawk.put(HawkConfig.DOH_URL, 0);
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject dnsConfig = jsonArray.get(i).getAsJsonObject();
                String name = dnsConfig.has("name") ? dnsConfig.get("name").getAsString() : "Unknown Name";
                dnsHttpsList.add(name);
                if (dohSelector == (i+1)) ips = dnsConfig.has("ips") ? dnsConfig.getAsJsonArray("ips") : null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkExoPlayer");
        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
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
        dnsOverHttps = new DnsOverHttps.Builder().client(dohClient).url(dohUrl.isEmpty() ? null : HttpUrl.get(dohUrl)).bootstrapDnsHosts(ips != null ? DohIps(ips) : null).build();
    }

    // 自定义 DNS
    static class CustomDns implements Dns {
        private  ConcurrentHashMap<String, List<InetAddress>> map;
        private final String excludeIps = "2409:8087:6c02:14:100::14,2409:8087:6c02:14:100::18,39.134.108.253,39.134.108.245";
        private final DnsOverHttps mDnsOverHttps;
 
        // 接收外部注入的 DoH 实例
        public CustomDns(DnsOverHttps dnsOverHttps) {
            this.mDnsOverHttps = dnsOverHttps;
        }
        @NonNull
        @Override
        public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
            if (myHosts == null) {
                myHosts = ApiConfig.get().getMyHost(); //确保只获取一次减少消耗
            }
            if (!myHosts.isEmpty() && myHosts.containsKey(hostname)) {
                hostname = myHosts.get(hostname);
            }
            assert hostname != null;
            if (isValidIpAddress(hostname)) {
                return Collections.singletonList(InetAddress.getByName(hostname));
            }
            else {
                return  mDnsOverHttps.lookup(hostname);
            }
        }

        public synchronized void mapHosts(Map<String,String> hosts) throws UnknownHostException {
            map = new ConcurrentHashMap<>();
            for (Map.Entry<String, String> entry : hosts.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (isValidIpAddress(value)) {
                    map.put(key,Collections.singletonList(InetAddress.getByName(value)));
                } else {
                    map.put(key,getAllByName(value));
                }
            }
        }

        private List<InetAddress> getAllByName(String host) {
            try {
                // 获取所有与主机名关联的 IP 地址
                InetAddress[] allAddresses = InetAddress.getAllByName(host);
                if (excludeIps.isEmpty()) return Arrays.asList(allAddresses);
                // 创建一个列表用于存储有效的 IP 地址
                List<InetAddress> validAddresses = new ArrayList<>();
                Set<String> excludeIpsSet = new HashSet<>();
                for (String ip : excludeIps.split(",")) {
                    excludeIpsSet.add(ip.trim());  // 添加到集合，去除多余的空格
                }
                for (InetAddress address : allAddresses) {
                    if (!excludeIpsSet.contains(address.getHostAddress())) {
                        validAddresses.add(address);
                    }
                }
                return validAddresses;
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        //简单判断减少开销
        private boolean isValidIpAddress(String str) {
            if (str.indexOf('.') > 0) return isValidIPv4(str);
            return str.indexOf(':') > 0;
        }

        private boolean isValidIPv4(String str) {
            String[] parts = str.split("\\.");
            if (parts.length != 4) return false;
            for (String part : parts) {
                try {
                    Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
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
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");

        if (Hawk.get(HawkConfig.DEBUG_OPEN, false)) {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
            loggingInterceptor.setColorLevel(Level.INFO);
        } else {
            loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.NONE);
            loggingInterceptor.setColorLevel(Level.OFF);
        }

        //builder.retryOnConnectionFailure(false);
        builder.connectionSpecs(getConnectionSpec());
        builder = builder.addInterceptor(loggingInterceptor)
                .readTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .connectTimeout(DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS)
                .dns(dnsOverHttps);
        try {
            builder = setOkHttpSsl(builder);
        } catch (Throwable th) {
            th.printStackTrace();
        }

        HttpHeaders.setUserAgent(Util.userAgent);
        OkHttpClient okHttpClient = builder.build();
        OkGo.getInstance().setOkHttpClient(okHttpClient);

        defaultClient = okHttpClient;
        builder.followRedirects(false);
        builder.followSslRedirects(false);
        noRedirectClient = builder.build();

        initExoOkHttpClient();        
    }

    private static synchronized OkHttpClient.Builder setOkHttpSsl(OkHttpClient.Builder builder) {
        try {
            final SSLSocketFactory sslSocketFactory = new SSLCompat();
            return builder
                   .sslSocketFactory(sslSocketFactory, SSLCompat.TM)
                   .hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class Tls12SocketFactory extends SSLSocketFactory {

        private static final String[] TLS_SUPPORT_VERSION = {"TLSv1.1", "TLSv1.2"};

        final SSLSocketFactory delegate;

        private Tls12SocketFactory(SSLSocketFactory base) {
            this.delegate = base;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return patch(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return patch(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort));
        }

        private Socket patch(Socket s) {
            //代理SSLSocketFactory在创建一个Socket连接的时候，会设置Socket的可用的TLS版本。
            if (s instanceof SSLSocket) {
                ((SSLSocket) s).setEnabledProtocols(TLS_SUPPORT_VERSION);
            }
            return s;
        }
    }
}
