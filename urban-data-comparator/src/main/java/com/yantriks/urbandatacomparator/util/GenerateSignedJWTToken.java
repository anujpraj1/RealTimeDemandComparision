package com.yantriks.urbandatacomparator.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.sterlingcommerce.baseutil.SCXmlUtil;
import com.yantra.interop.japi.YIFCustomApi;
import com.yantra.yfc.log.YFCLogCategory;
import com.yantra.yfs.core.YFSSystem;
import com.yantra.yfs.japi.YFSEnvironment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;
import java.util.concurrent.TimeUnit;


public class GenerateSignedJWTToken implements YIFCustomApi {

  private Properties props;
  private static YFCLogCategory log = YFCLogCategory.instance(GenerateSignedJWTToken.class);

  @Override
  public void setProperties(Properties props) throws Exception {
    // TODO Auto-generated method stub
    this.props = props;
  }

  /**
   * This method fetches
   * 
   * @param env
   * @param inDoc
   * @return Sting JWTToken
   */

  public Document getJWTToken(YFSEnvironment env, Document inDoc) {


    log.debug("Executing GenerateSignedJWTToken.getJWTToken()-Start");
    log.debug("Input to getJWTToken() -\n" + SCXmlUtil.getString(inDoc));

    Element jwtTokenEle=inDoc.getDocumentElement(); 
    log.debug("JWT Token Element " + SCXmlUtil.getString(jwtTokenEle));

    String strJWTToken = getJWTTokenStr() ;
    
    jwtTokenEle.setAttribute("Token", strJWTToken);
    log.debug("JWT Token Element with Token" + SCXmlUtil.getString(jwtTokenEle));
    
    return inDoc;

  }

  
  /**
   * This method generates JWT Token
   * 
   * @return Sting JWTToken
   */
  public static String getJWTTokenStr() {

    log.debug("Executing GenerateSignedJWTToken.getJWTToken()-Start");

    // Getting SecretKey from customer_overrides
    //String strSecretKey = YFSSystem.getProperty("yantriks.jwt.token.secretKey");
    // DEV String strSecretKey = "da53d169065e21d726190c529d2c28f6a3b41ded45b5b382c4c23d139faebe95";
    String strSecretKey = "c3VwZXJzZWNyZXRzdHJpbmdmb3JzdGFnaW5nc3Rlcmxpbmc";
    // Getting Key ID from customer_overrides
    //String strKeyID = YFSSystem.getProperty("yantriks.jwt.token.kid");

    String strKeyID = "STERLING-1";
    log.debug(">>>>>>>>>KeyID:" + strKeyID);
    
    // Getting expiryLength from customer_overrides
    //String strExpiryLenght = YFSSystem.getProperty("yantriks.jwt.token.expiryLength");
    String strExpiryLenght = "3600";
    log.debug(">>>>>>>>>>expiryLength:" + strExpiryLenght);

    // Code to generate signed token

    Date now = new Date();

    Date expTime = new Date(
        System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(Long.parseLong(strExpiryLenght)));

    // Build the JWT payload
    Map<String, Object> headerClaims = new HashMap<String, Object>();

    headerClaims.put("kid", strKeyID);

    JWTCreator.Builder token = JWT.create().withHeader(headerClaims).withIssuedAt(now)
        // Expires after 'expiraryLength' seconds
        .withExpiresAt(expTime).withIssuer(strKeyID).withSubject("OMS");
    
 // need to decode the token
    byte[] decodedKeyBytes = Base64.getDecoder().decode(strSecretKey);

    // Sign the JWT
    Algorithm algorithm = Algorithm.HMAC256(decodedKeyBytes);
    
    String strJWTToken = token.sign(algorithm);

    log.debug("JWT Token Generated" + strJWTToken);
    return strJWTToken;

  }


}