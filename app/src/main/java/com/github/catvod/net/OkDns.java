package com.github.catvod.net;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.util.StringUtils;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import okhttp3.Cache;
import okhttp3.Dns;
import okhttp3.dnsoverhttps.DnsOverHttps;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class OkDns implements Dns {

    private final ConcurrentHashMap<String, String> hosts = new ConcurrentHashMap<>();
    private OkHttpClient bootstrapClient = new OkHttpClient();
    private volatile DnsOverHttps doh;

    public void setDoh(HttpUrl url) {
        bootstrapClient = new OkHttpClient.Builder()
            .cache(new Cache(new File(App.getInstance().getCacheDir().getAbsolutePath(), "okhttpcache"), 10 * 1024 * 1024))
            .build();
        this.doh = url == null ? null : new DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url(url)
            .bootstrapDnsHosts(OkHttp.getHosts())
            .build();
    }

    public void addAll(List<String> hosts) {
        this.hosts.putAll(hosts.stream().filter(Objects::nonNull).map(host -> host.split("=", 2)).filter(splits -> splits.length == 2).collect(Collectors.toMap(s -> s[0].trim(), s -> s[1].trim(), (oldHost, newHost) -> newHost)));
    }

    public void clear() {
        hosts.clear();
    }

    private String get(String hostname) {
        String target = hosts.get(hostname);
        if (target != null) return target;
        for (Map.Entry<String, String> entry : hosts.entrySet()) if (StringUtils.containOrMatch(hostname, entry.getKey())) return entry.getValue();
        return hostname;
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        Dns dns = doh != null ? doh : Dns.SYSTEM;
        return dns.lookup(get(hostname));
    }
}
