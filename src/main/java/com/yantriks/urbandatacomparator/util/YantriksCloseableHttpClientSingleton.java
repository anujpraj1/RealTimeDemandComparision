package com.yantriks.urbandatacomparator.util;

import java.net.InetSocketAddress;
import java.net.Proxy;

import org.apache.http.HttpHost;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;

public class YantriksCloseableHttpClientSingleton {

  private static CloseableHttpClient closeableHttpClient= null;

  public static synchronized CloseableHttpClient createCloseableHttpClient() {
    if (closeableHttpClient == null) {
    	
//    	 Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("nyproxy.urbanout.com", 8080));
         HttpHost httpHost = new HttpHost("nyproxy.urbanout.com",8080,"http");
        
         DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(httpHost);
         
         closeableHttpClient =  HttpClients.custom()
              .setMaxConnTotal(100)
              .setMaxConnPerRoute(20)
              .setRoutePlanner(routePlanner) //setting proxy
              .build();
         
//         System.out.println(" proxy "+);
    }
    return closeableHttpClient;
  }
}
