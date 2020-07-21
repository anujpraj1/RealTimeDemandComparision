package com.yantriks.urbandatacomparator.model.responses;

import com.yantra.httpclient.common.KeyValuePair;
import com.yantra.httpclient.response.Response;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.http.HttpResponse;

import java.util.List;

@Data
@AllArgsConstructor
public class HttpResponseImpl implements Response {

    int status;

    List<? extends KeyValuePair<String, String>> headers;

    String body;

    String message;


}
