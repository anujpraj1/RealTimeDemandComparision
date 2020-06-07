package com.yantriks.urbandatacomparator.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yantriks.urbandatacomparator.sterlingapis.SterlingGetOrderListCall;

public class Test {


    public static void main(String args[]) throws Exception {
        /*String in = "{\n" +
                "    \"a\": \"anuj\"," +
                "    \"b\": \"bnj\"" +
                "}";
        ObjectMapper ob = new ObjectMapper();
        TestPojo tp = ob.readValue(in, TestPojo.class);
        System.out.println("PPPPPPPPP"+tp.getA());*/
        int n = 16;
        int count = 0;
        for (int i = 1; i<=n; i++) {
            if (i%3 == 0 || i%5 == 0) {
                count++;
            }
        }
        System.out.println("Count :: "+count);
        SterlingGetOrderListCall sb = new SterlingGetOrderListCall();
        sb.executeGetOLListApi("AP00576416", "DEFAULT");
    }
}
