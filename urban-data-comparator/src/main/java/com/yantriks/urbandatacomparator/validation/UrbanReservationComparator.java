package com.yantriks.urbandatacomparator.validation;

import com.yantriks.urbandatacomparator.util.UrbanConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UrbanReservationComparator {

    public String getDemandTypeForCurrentStatus(String status) {
        log.debug("Yantriks Util : Checking appropriate demand type for status");
        if (UrbanConstants.IM_LIST_OPEN_STATUSES.contains(status)) {
            return UrbanConstants.DT_OPEN;
        } else if (UrbanConstants.IM_LIST_SCHEDULED_STATUSES.contains(status)) {
            return UrbanConstants.DT_SCHEDULED;
        } else if (UrbanConstants.IM_LIST_ALLOCATED_STATUSES.contains(status)) {
            return UrbanConstants.DT_ALLOCATED;
        } else if (UrbanConstants.IM_LIST_BACKORDER_STATUSES.contains(status)) {
            return UrbanConstants.DT_BACKORDERED;
        } else {
            return null;
        }
    }

}
