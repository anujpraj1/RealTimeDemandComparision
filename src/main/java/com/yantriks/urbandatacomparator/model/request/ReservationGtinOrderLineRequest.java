/*
 * Copyright (c) 2017 Yantriks LLC. All Rights Reserved. No warranty, explicit or implicit, is provided.
 * NOTICE: All information contained herein is, and remains the property of Yantriks LLC and its suppliers or licensors,
 * if any. The intellectual and technical concepts contained herein are proprietary to Yantriks LLC and may be covered
 * by U.S. and Foreign Patents, patents in process, and are protected by trade secret or copyright law. Dissemination of
 * this information or reproduction of this material is strictly forbidden unless prior written permission is obtained
 * from Yantriks LLC.
 */

package com.yantriks.urbandatacomparator.model.request;

import lombok.*;

import java.util.List;

@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"productId"})
@AllArgsConstructor
@NoArgsConstructor
public class ReservationGtinOrderLineRequest {

    private String productId;

    private List<ReservationProductLocationRequest> locationReservationDetails;
}