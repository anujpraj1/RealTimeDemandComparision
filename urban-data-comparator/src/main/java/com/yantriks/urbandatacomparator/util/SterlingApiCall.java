package com.yantriks.urbandatacomparator.util;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.model.UrbanURI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ibm.sterling.afc.xapiclient.japi.XApi;
import com.ibm.sterling.afc.xapiclient.japi.XApiClientFactory;
import com.ibm.sterling.afc.xapiclient.japi.XApiEnvironment;
import com.ibm.sterling.afc.xapiclient.util.XApiXmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SterlingApiCall {

	@Autowired
	UrbanURI urbanURI;

	@Value("${urban.sterling.apitester.userid}")
	private String userid;

	@Value("${urban.sterling.apitester.password}")
	private String password;

	public void test() throws Exception {
		System.out.println("Inside Test");
		log.info("UDDD"+urbanURI.getSterlingURL());
		Document doc = SCXmlUtil.createDocument("Locale");
		Element eleInput = doc.getDocumentElement();
		try {
			executeApi("getLocaleList", eleInput);
		} catch (Exception e) {
			System.out.println("Here");
			e.printStackTrace();
		}
	}

	public Document executeApi(String apiName, Element inEle) throws Exception
	{
		System.out.println("Inside");
		Map<String, String> map = new HashMap<String, String>();
		System.out.println("Inside Here");
		System.out.println("Inside Here now");
		map.put(UrbanConstants.CONST_YIF_HTTP_API_URL, urbanURI.getSterlingURL());
		XApi api = XApiClientFactory.getInstance().getApi(UrbanConstants.CONST_CAPS_HTTP, map);
		

		Document input = XApiXmlUtil.createDocument();
		Element envElem = input.createElement(UrbanConstants.CONST_YFS_ENV);
		envElem.setAttribute(UrbanConstants.CONST_PROG_ID, UrbanConstants.CONST_STERLING_API_TESTER);
		envElem.setAttribute(UrbanConstants.CONST_USER_ID, userid);
		input.appendChild(envElem);
		XApiEnvironment env = api.createEnvironment(input);
		

		input = XApiXmlUtil.createDocument();
		Element root = input.createElement(UrbanConstants.CONST_CAMELCASE_LOGIN);
		root.setAttribute(UrbanConstants.CONST_LOGIN_ID, userid);
		root.setAttribute(UrbanConstants.CONST_CAMELCASE_PASSWORD, password);
		input.appendChild(root);
		api.invoke(env, UrbanConstants.CONST__LOGIN, input);


		input = XApiXmlUtil.createDocument();
		root = input.createElement("Locale");
		root = inEle;
		input.appendChild(root);
		Document doc = api.invoke(env, apiName, input);
		System.out.println("HEHEHHEEEHHEE"+XApiXmlUtil.getString(input));

		return input;
	}
}
