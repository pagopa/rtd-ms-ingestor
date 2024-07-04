package it.gov.pagopa.rtd.ms.rtdmsingestor.utils;

import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

public class ApacheUtils {

    private static final String EMPTY_BODY = "EMPTY_BODY";

    public static String readEntityResponse(HttpEntity entity) {
        try {
            if (entity != null) {
                return EntityUtils.toString(entity);
            }
            return EMPTY_BODY;
        } catch (Exception e) {
            return EMPTY_BODY;
        }
    }
}
