package com.yantriks.urbandatacomparator.sterlingapis;

import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantriks.urbandatacomparator.util.UrbanConstants;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
public class SterlingAPIDocumentCreator {

    public Document createInDocForGetInvReservation(String reservationID) {
        Document inDoc = SCXmlUtil.createDocument(UrbanConstants.ELE_INV_RESERVATION);
        inDoc.getDocumentElement().setAttribute(UrbanConstants.A_RESERVATION_ID, reservationID);
        return inDoc;
    }

    public Document createInDocForGetOrderList(String enterpriseCode, String orderNo) {
        Document inDoc = SCXmlUtil.createDocument(UrbanConstants.ELE_ORDER);
        inDoc.getDocumentElement().setAttribute(UrbanConstants.A_ORDER_NO, orderNo);
        inDoc.getDocumentElement().setAttribute(UrbanConstants.A_DOCUMENT_TYPE, UrbanConstants.V_DOCTYPE_0001);
        inDoc.getDocumentElement().setAttribute(UrbanConstants.A_ENTERPRISE_CODE, enterpriseCode);
        return inDoc;
    }

    public Document createInDocForGetShipNodeList(String shipNode) {
        Document inDoc = SCXmlUtil.createDocument(UrbanConstants.ELE_SHIPNODE);
        inDoc.getDocumentElement().setAttribute(UrbanConstants.A_SHIP_NODE, shipNode);
        return inDoc;
    }
}
