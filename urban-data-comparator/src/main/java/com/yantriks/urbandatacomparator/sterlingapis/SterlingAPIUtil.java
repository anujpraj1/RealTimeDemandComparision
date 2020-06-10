package com.yantriks.urbandatacomparator.sterlingapis;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.sterlingcommerce.tools.datavalidator.XmlUtils;
import com.yantra.interop.japi.YIFApi;
import com.yantra.interop.japi.YIFClientCreationException;
import com.yantra.interop.japi.YIFClientFactory;
import com.yantra.yfs.japi.YFSEnvironment;
import com.yantra.yfs.japi.YFSException;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.ParserConfigurationException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SterlingAPIUtil {

    @Value("${urban.sterling.apitester.userid}")
    private String userid;

    @Value("${urban.sterling.apitester.password}")
    private String password;

    @Value("${urban.sterling.protocol}")
    private String protocol;

    @Value("${urban.sterling.url}")
    private String url;

    public Document invokeSterlingAPI(Document inDoc, String apiName) throws YIFClientCreationException, RemoteException, ParserConfigurationException {
        log.info("API Name :: "+apiName);
        YFSEnvironment env = getRemoteEnvironment(protocol, url, userid, password);
        log.debug("invokeSterlingAPI : InputDoc : " + SCXmlUtil.getString(inDoc));
        YIFApi api = YIFClientFactory.getInstance().getApi();
        Document outputDoc = api.invoke(env, apiName, inDoc);
        log.debug("invokeSterlingAPI : OutputDoc : " + XmlUtils.getString(outputDoc));
        return outputDoc;
    }

    public Document invokeSterlingAPI(Document inDoc, Document template, String apiName) throws YIFClientCreationException, RemoteException, ParserConfigurationException {
        log.info("API Name :: "+apiName);
        YFSEnvironment env = getRemoteEnvironment(protocol, url, userid, password);
        log.debug("invokeSterlingAPI : InputDoc : " + SCXmlUtil.getString(inDoc));
        YIFApi api = YIFClientFactory.getInstance().getApi();
        env.setApiTemplate(apiName, template);
        Document outputDoc = api.invoke(env, apiName, inDoc);
        log.debug("invokeSterlingAPI : OutputDoc : " + SCXmlUtil.getString(outputDoc));
        return outputDoc;
    }

    public static YFSEnvironment getRemoteEnvironment(String protocol, String url, String userID, String password)
            throws ParserConfigurationException, YFSException, RemoteException, YIFClientCreationException {
        try {
            Document envParams = XmlUtils.createDocument(UrbanConstants.CONST_YFS_ENV);
            Element elem = envParams.getDocumentElement();
            elem.setAttribute(UrbanConstants.CONST_USER_ID, userID);
            elem.setAttribute(UrbanConstants.CONST_PROG_ID, UrbanConstants.CONST_STERLING_API_TESTER);
            elem.setAttribute(UrbanConstants.CONST_PASSWORD, password);

            Map<String, String> overrideProperties = new HashMap<>();
            overrideProperties.put(UrbanConstants.CONST_YIF_APIFACTORY_PROTOCOL, UrbanConstants.CONST_CAPS_HTTP);
            overrideProperties.put(UrbanConstants.CONST_YIF_HTTP_API_URL,
                    protocol + "://" + url + "/smcfs/interop/InteropHttpServlet?&YFSEnvironment.userId=" + userID
                            + "&YFSEnvironment.password=" + password);
            YFSEnvironment environment = YIFClientFactory.getInstance().getApi(UrbanConstants.CONST_CAPS_HTTP, overrideProperties)
                    .createEnvironment(envParams);
            environment.setRollbackOnly(true);
            return environment;
        } catch (Exception e) {
            log.error("Exception Caught while creating environment");
            log.error(e.getMessage());
        }
        return null;
    }
}
