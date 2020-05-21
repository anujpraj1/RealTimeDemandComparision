package com.yantriks.urbandatacomparator.sterlingapis;

import com.ibm.sterling.afc.xapiclient.japi.XApi;
import com.ibm.sterling.afc.xapiclient.japi.XApiClientFactory;
import com.ibm.sterling.afc.xapiclient.japi.XApiEnvironment;
import com.ibm.sterling.afc.xapiclient.util.XApiXmlUtil;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.yfc.core.YFCObject;
import com.yantra.yfc.dom.YFCDocument;
import com.yantriks.urbandatacomparator.model.UrbanURI;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import com.yantriks.yih.adapter.util.YantriksCommonUtil;
import com.yantriks.yih.adapter.util.YantriksConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SterlingGetOrganizationListCall {

	@Autowired
	UrbanURI urbanURI;

	@Value("${urban.sterling.apitester.userid}")
	private String userid;

	@Value("${urban.sterling.apitester.password}")
	private String password;

	public Document executeGetOrganizationListCall(String locationId) throws Exception
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
		root = input.createElement(UrbanConstants.ELE_ORGANIZATION);
		//root = inEle;
		input.appendChild(root);
		root.setAttribute(UrbanConstants.A_ORGANIZATION_CODE, locationId);
		env.setApiTemplate(UrbanConstants.API_GET_ORGANIZATION_LIST, SCXmlUtil.createFromString(UrbanConstants.TEMPLATE_GET_ORGANIZATION_LIST));
		System.out.println("Input for getOrganizationList "+XApiXmlUtil.getString(input));
		Document doc = api.invoke(env, UrbanConstants.API_GET_ORGANIZATION_LIST, input);
		System.out.println("Output for getOrganizationList "+XApiXmlUtil.getString(doc));

		return doc;
	}
}
