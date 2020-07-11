package com.yantriks.urbandatacomparator.util;


import com.google.common.collect.ImmutableList;
import com.yantriks.yih.adapter.util.YantriksConstants;

public class UrbanConstants {

    public static final String CONST_YFS_ENV = "YFSEnvironment";
    public static final String CONST_PROG_ID = "progId";
    public static final String CONST_STERLING_API_TESTER = "SterlingHttpTester";
    public static final String CONST_USER_ID = "userId";
    public static final String CONST_CAMELCASE_LOGIN = "Login";
    public static final String CONST__LOGIN = "login";
    public static final String CONST_LOGIN_ID = "LoginID";
    public static final String CONST_CAMELCASE_PASSWORD = "Password";
    public static final String CONST_PASSWORD = "password";
    public static final String CONST_CAPS_HTTP = "HTTP";
    public static final String CONST_YIF_HTTP_API_URL = "yif.httpapi.url";
    public static final String CONST_YIF_APIFACTORY_PROTOCOL = "yif.apifactory.protocol";
    public static final String CONST_API_TESTER_URL = "/smcfs/interop/InteropHttpServlet";
    public static final String A_ORDER_NO = "OrderNo";
    public static final String A_DOCUMENT_TYPE = "DocumentType";
    public static final String A_ENTERPRISE_CODE = "EnterpriseCode";
    public static final String API_GET_ORDER_LIST = "getOrderList";
    public static final String API_GET_INV_RESERVATION_LIST = "getInventoryReservationList";
    public static final String API_GET_ORGANIZATION_LIST = "getOrganizationList";
    public static final String ELE_ORDER = "Order";
    public static final String A_TOTAL_NUM_OF_RECORDS = "TotalNumberOfRecords";

    public static final String TEMPLATE_GET_ORDER_LIST = "<OrderList TotalNumberOfRecords=\"\">\n" +
            "\t<Order OrderHeaderKey=\"\" OrderNo=\"\" EnterpriseCode=\"\">\n" +
            "\t<Extn ExtnReservationID=\"\" />\n" +
            "\t\t<OrderLines>\n" +
            "\t\t\t<OrderLine FulfillmentType=\"\" PrimeLineNo=\"\" LineType=\"\" DeliveryMethod=\"\"  OrderedQty=\"\" ShipNode=\"\" ReqShipDate=\"\">\n" +
            "\t\t\t\t<Item ItemID=\"\" UnitOfMeasure=\"\"/>\n" +
            "\t\t\t\t<PersonInfoShipTo ZipCode=\"\"/>\n" +
            "\t\t\t\t<Schedules>\n" +
            "\t\t\t\t\t<Schedule OrderLineScheduleKey=\"\" ShipNode=\"\" Quantity=\"\" Modifyts=\"\" ExpectedShipmentDate=\"\">\n" +
            "\t\t\t\t\t</Schedule>\n" +
            "\t\t\t\t</Schedules>\n" +
            "\t\t\t\t<OrderStatuses>\n" +
            "\t\t\t\t\t<OrderStatus OrderHeaderKey=\"\" OrderLineKey=\"\" OrderLineScheduleKey=\"\" OrderReleaseKey=\"\" OrderReleaseStatusKey=\"\" PipelineKey=\"\" ReceivingNode=\"\" ShipNode=\"\" Status=\"\" StatusDate=\"\" StatusDescription=\"\" StatusQty=\"\" StatusReason=\"\" TotalQuantity=\"\">\n" +
            "\t\t\t\t\t\t<Details ExpectedDeliveryDate=\"\" ExpectedShipmentDate=\"\" ProcureFromNode=\"\"  OverrideItemID=\"\" OverrideProductClass=\"\" OverrideUnitOfMeasure=\"\" ReceivingNode=\"\" ShipByDate=\"\" ShipNode=\"\" TagNumber=\"\">\n" +
            "\t\t\t\t\t\t</Details>\n" +
            "\t\t\t\t\t\t<OrderStatusTranQuantity StatusQty=\"\" TotalQuantity=\"\" TransactionalUOM=\"\"/>\n" +
            "\t\t\t\t\t\t<ShipNode Description=\"\" IdentifiedByParentAs=\"\" OwnerKey=\"\"/>\n" +
            "\t\t\t\t\t</OrderStatus>\n" +
            "\t\t\t\t</OrderStatuses>\n" +
            "\t\t\t</OrderLine>\n" +
            "\t\t</OrderLines>\n" +
            "\t\t<PersonInfoShipTo ZipCode=\"\"/>\n" +
            "\t</Order>\n" +
            "</OrderList>";

    public static final String av = "<OrderList><Order OrderNo='' ><Extn ExtnReservationID='' /><OrderLines><OrderLine "
            + "OrderLineKey='' PrimeLineNo='' /></OrderLines></Order></OrderList>";

    public static final String TEMPLATE_GET_ORGANIZATION_LIST = "<OrganizationList>\n" +
            "<Organization BusinessCalendarKey='' CatalogOrganizationCode='' InventoryOrganizationCode='' IsEnterprise='' IsNode=''\n" +
            "IsSeller='' LocaleCode='' OrganizationCode='' OrganizationName='' ParentOrganizationCode='' PrimaryEnterpriseKey='' CapacityOrganizationCode=''\n" +
            "InventoryKeptExternally=' ' CreateProgId='' ModifyProgId='' >\n" +
            "    <Extn ExtnATF='' ExtnSterPU='' ExtnATS=''  ExtnISPUDirections=''\n" +
            "          ExtnISPUStorePickUpLocation='' ExtnStoreSlug=''/>\n" +
            "<CorporatePersonInfo Latitude='' Longitude='' ZipCode='' Country='' AddressLine1='' AddressLine2=''\n" +
            "                     City='' State=''/>\n" +
            "<Node InventoryTracked='' Inventorytype='' Latitude='' Localecode='' Longitude='' NodeType='' ReceiptProcessingTime='' ReceiptProcessingTimeForForwarding='' ShipNode='' CanShipToOtherAddresses=''>\n" +
            "    <ShipNodePersonInfo AddressLine1='' AddressLine2='' City='' Country=''\n" +
            "                        State='' ZipCode='' />\n" +
            "    <ShippingCalendar CalendarDescription='' CalendarId='' CalendarKey=''/>\n" +
            "    <Extn ExtnNodeClass=''/>\n" +
            "</Node>\n" +
            "</Organization>\n" +
            "</OrganizationList>";

    public static final String TEMPLATE_GET_SHIPNODE_LIST = "<ShipNodeList>\n" +
            "<ShipNode NodeType=\"\">\n" +
            "<Extn ExtnNodeClass=\"\"/>\n" +
            "</ShipNode>\n" +
            "</ShipNodeList>";


    public static final String V_DOCTYPE_0001 = "0001";
    public static final String V_PRODUCT_YAS = "yas";
    public static final String V_PRODUCT_ILT = "ilt";
    public static final String V_PRODUCT_YCS = "ycs";
    public static final String HTTP_METHOD_POST = "POST";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_GET = "GET";
    public static final String YANTRIKS_GET_RESERVE_URL = "/availability-services/reservations/v3.0";
    public static final String YANTRIKS_LINE_RESERVE_URL = "/availability-services/reservations/v3.0/lines";
    public static final String YANTRIKS_RESERVE_URL = "/availability-services/reservations/v3.0";
    public static final String V_FAILURE = "FAILURE";
    public static final String V_EXC_FAILURE = "EXCEPTION_FAILURE";
    public static final String V_ORGID_URBN = "URBN";

    public static final String ELE_INV_RESERVATION = "InventoryReservation";
    public static final String ELE_ORGANIZATION = "Organization";
    public static final String A_ORGANIZATION_CODE = "OrganizationCode";
    public static final String ELE_SHIPNODE = "ShipNode";
    public static final String A_RESERVATION_ID = "ReservationID";
    public static final String A_DELIVERY_METHOD = "DeliveryMethod";
    public static final String A_DEMAND_TYPE = "DemandType";
    public static final String A_QUANTITY = "Quantity";
    public static final String A_SHIP_DATE = "ShipDate";
    public static final String A_SHIP_NODE = "ShipNode";
    public static final String A_EXPIRATION_DATE = "ExpirationDate";

    public static final String V_SECONDS = "SECONDS";
    public static final String V_MINUTES = "MINUTES";
    public static final String V_RT_URBN_USER = "RTURBNUSER";
    public static final String V_TZ_UTC = "UTC";
    public static final String FT_SHIP = "Ship";
    public static final String FT_ISPU = "ISPU";
    public static final String FT_STS = "STS";
    public static final String E_ITEM = "Item";
    public static final String A_ITEM_ID = "ItemID";
    public static final String A_UOM = "UnitOfMeasure";
    public static final String DT_RESERVED = "RESERVED";
    public static final String DT_OPEN = "OPEN";
    public static final String DT_SCHEDULED = "SCHEDULED";
    public static final String DT_ALLOCATED = "ALLOCATED";
    public static final String DT_BACKORDERED = "BACKORDERED";

    public static final String NODE_TYPE = "NodeType";
    public static final String NODE = "Node";
    public static final String E_NODE_CLASS = "NodeClass";
    public static final String ELE_EXTN = "Extn";
    public static final String V_DS = "DS";
    public static final String V_AN = "AN";
    public static final String V_PRIMARY_VENDOR = "PrimaryVendor";
    public static final String V_MIRAKL_SHIP_NODE = "Mirakl Ship Node";
    public static final String V_GC = "GC";
    public static final String V_CALL_CENTRE = "CallCenter";
    public static final String LT_MSN = "MSN";
    public static final String LT_BACK_OFFICE = "BackOffice";
    public static final String LT_CC = "CC";
    public static final String LT_DC = "DC";
    public static final String LT_VENDOR = "Vendor";
    public static final String LT_AGG_NODES = "AggNodes";
    public static final String V_STORE = "Store";
    public static final String VENDOR_LIST= "DS,Primary Vendor,PrimaryVendor";
    public static final String VAL_DC = "DC";

    public static final String CAN_RESERVE_AFTER = "canReserveAfter";
    public static final String CONSIDER_CAPACITY = "considerCapacity";
    public static final String CONSIDER_GTIN = "considerGtin";
    public static final String IGNORE_AVAILABILITY_CHECK = "ignoreAvailabilityCheck";
    public static final String SC_GLOBAL = "GLOBAL";
    public static final String TT_ECOMMERCE = "ecommerce";
    public static final String TT_RESERVE = "oms-reserve";
    public static final String TT_SCHEDULE = "oms-schedule";
    public static final String TT_RELEASE = "oms-release";
    public static final String TT_TRANSFER = "oms-transfer";

    public static final String E_ORDER_LINES = "OrderLines";
    public static final String E_ORDER_LINE = "OrderLine";
    public static final String E_ORDER_STATUSES = "OrderStatuses";
    public static final String E_ORDER_STATUS = "OrderStatus";
    public static final String A_ORDER_LINE_SCHEDULE_KEY = "OrderLineScheduleKey";
    public static final String A_STATUS = "Status";
    public static final String A_SCHEDULES = "Schedules";
    public static final String A_SCHEDULE = "Schedule";
    public static final String A_EXP_SHIP_DATE = "ExpectedShipmentDate";
    public static final String A_PRIME_LINE_NO = "PrimeLineNo";
    public static final String EXTN_RESERVATION_ID = "ExtnReservationID";

    // Statuses to be utilized for Demands
    public static final ImmutableList<String> IM_LIST_SHIPPED_STATUSES = ImmutableList.of("3700");
    public static final ImmutableList<String> IM_LIST_ALLOCATED_STATUSES = ImmutableList.of("3350", "3200");
    public static final ImmutableList<String> IM_LIST_BACKORDER_STATUSES = ImmutableList.of("1300", "1300.100", "1400");
    public static final ImmutableList<String> IM_LIST_OPEN_STATUSES = ImmutableList.of("1100");
    public static final ImmutableList<String> IM_LIST_SCHEDULED_STATUSES = ImmutableList.of("1500");

    public static final String API_GET_SHIP_NODE_LIST = "getShipNodeList";

    public static final String RS_MATCH = "MATCH";
    public static final String RS_MISMATCH = "MISMATCH";
    public static final String RS_MISSING = "MISSING";
    public static final String ERR_YANT_SERVER_DOWN = "YANTRIKS_SERVER_DOWN";
    public static final String ERR_GET_RESERVATION_FAILED = "YANT_GET_RESERVATION_FAILED";
    public static final String ERR_GET_ORDER_LIST_FAILED = "STER_GET_ORDER_FAILED";
    public static final String ERR_NO_ORDER_FOUND = "STER_NO_ORDER_FOUND";
    public static final String ERR_GET_INV_RESERVATION_FAILED = "STER_GET_INV_RESERVATION_FAILED";
    public static final String ERR_DATA_INCORRECT = "DATA_INCORRECT";
    public static final int RC_201 = 201;
    public static final int RC_200 = 200;
    public static final int RC_500 = 500;
    public static final String MSG_SUCCESS = "SUCCESS";
    public static final String MSG_NO_UPDATE_REQUIRED = "MATCH_NO_UPDATE";
    public static final String A_MAX_ORDER_STATUS = "MaxOrderStatus";

    //
    public static final String JSON_ATTR_MESSAGE ="message";
    public static final String JSON_ATTR_ERROR ="error";
    public static final ImmutableList IM_LIST_ENTITY_NOT_EXISTS = ImmutableList.of("ENTITY_DOES_NOT_EXIST", "Entity does not exist");
    public static final String V_ENTITY_NOT_EXISTS = "ENTITY_NOT_EXISTS";
    public static final String V_NO_CONTENT_FOUND = "NO_CONTENT_FOUND";
    public static final ImmutableList<String> IM_LIST_GET_RESERVATION_FAILURES = ImmutableList.of("FAILURE", "NON_RETRY_EXCEPTION");






}
