package com.yantriks.urbandatacomparator.model;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.sterlingcommerce.tools.datavalidator.XmlUtils;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfc.dom.YFCDocument;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;

public class KTLocalAPIClient {
	
	public static void main(String[] args) throws FactoryConfigurationError, Exception {
		YFCDocument indoc = YFCDocument.createDocument("Order");
		indoc.getDocumentElement().setAttribute("OrderHeaderkey", "20160307100503642933037");
		remoteApiInvoke(indoc.getDocument(), "getOrderList");
		//localApiInvoke("getOrderList");
	}
	
	
	public static void remoteApiInvoke(Document doc , String apiName) throws FactoryConfigurationError, Exception {
		//YFSEnvironment env = getRemoteEnvironment("http://stlrck-vdcn013:18081/","deepakmi",
		//		"Yantriks1");
		YFSEnvironment env = getRemoteEnvironment("sushqssteriap01.urbanout.com:8080","admin",
				"password");
		/*File fXmlFile1 = new File("C:\\Kuldeep\\omsWs\\petco\\src\\createOrder.xml");
		DocumentBuilderFactory dbFactory1 = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder1 = dbFactory1.newDocumentBuilder();
		Document inDoc = (Document) dBuilder1.parse(fXmlFile1);*/
//		Document doc = XmlUtils.createDocument("Order");
		System.out.println("***** Input Xml Details *****" + XmlUtils.getString(doc));
		YIFApi api = YIFClientFactory.getInstance().getApi();
		Document doc2 = api.invoke(env, apiName, doc);
		System.out.println("***** output Xml Details *****" + XmlUtils.getString(doc2));
		
	}
	
	public static Document localApiInvoke(Document inDoc,String ApiName) throws FactoryConfigurationError, Exception {
		YFSEnvironment env = getLocalEnvironmentEnvironment();
//		Document doc = XmlUtils.createDocument("Order");
		// <Order OrderHeaderKey='20170901105246246323591'/>
//		doc.getDocumentElement().setAttribute("OrderHeaderKey", "201804121505492037104268");
		System.out.println("***** Input Xml Details *****" + XmlUtils.getString(inDoc));
		YIFApi api = YIFClientFactory.getInstance().getApi();
		Document doc2 = api.invoke(env, ApiName, inDoc);
		System.out.println("***** output Xml Details *****" + XmlUtils.getString(doc2));
		return doc2;

	}
	public static void localApiInvoke(String ApiName) throws FactoryConfigurationError, Exception {
		YFSEnvironment env = getLocalEnvironmentEnvironment();
		Document doc = XmlUtils.createDocument("Order");
		 //<Order OrderHeaderKey='20170901105246246323591'/>
		doc.getDocumentElement().setAttribute("OrderHeaderKey", "2019112113113918067");
		System.out.println("***** Input Xml Details *****" + XmlUtils.getString(doc));
		YIFApi api = YIFClientFactory.getInstance().getApi();
		Document doc2 = api.invoke(env, ApiName, doc);
		System.out.println("***** output Xml Details *****" + XmlUtils.getString(doc2));
	}
	
	public static YFSEnvironment getLocalEnvironmentEnvironment()
			throws ParserConfigurationException, YFSException, RemoteException, YIFClientCreationException {
		Document envParams = XmlUtils.createDocument("YFSEnvironment");
		Element elem = envParams.getDocumentElement();
		elem.setAttribute("userId", "admin");
		elem.setAttribute("progId", "SterlingHttpTester");
		elem.setAttribute("password", "password");

		Map<String, String> overrideProperties = new HashMap<String, String>();
		overrideProperties.put("yif.apifactory.protocol", "HTTP");
		
		//http://localhost:8181/smcfs/interop/InteropHttpServlet
		overrideProperties.put("yif.httpapi.url",
				"http://localhost:7001/smcfs/interop/InteropHttpServlet?&YFSEnvironment.userId=admin&YFSEnvironment.password=password");
		YFSEnvironment environment = YIFClientFactory.getInstance().getApi("HTTP", overrideProperties)
				.createEnvironment(envParams);

		environment.setRollbackOnly(true);
		return environment;
	
	}
	
	public static YFSEnvironment getRemoteEnvironment(String hostAndPort, String userId, String password)
			throws ParserConfigurationException, YFSException, RemoteException, YIFClientCreationException {
		try {
			Document envParams = XmlUtils.createDocument("YFSEnvironment");
			Element elem = envParams.getDocumentElement();
			elem.setAttribute("userId", userId);
			elem.setAttribute("progId", "SterlingHttpTester");
			elem.setAttribute("password", password);

			Map<String, String> overrideProperties = new HashMap<String, String>();
			overrideProperties.put("yif.apifactory.protocol", "HTTP");
			overrideProperties.put("yif.httpapi.url",
					"http://" + hostAndPort + "/smcfs/interop/InteropHttpServlet?&YFSEnvironment.userId=" + userId
							+ "&YFSEnvironment.password=" + password);
			YFSEnvironment environment = YIFClientFactory.getInstance().getApi("HTTP", overrideProperties)
					.createEnvironment(envParams);
			System.out.println("environment:::" + environment.getUserId());
			System.out.println("environment:::" + environment.getAdapterName());
			environment.setRollbackOnly(true);
			return environment;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
