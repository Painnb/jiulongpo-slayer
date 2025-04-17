package org.swu.vehiclecloud.service;

import java.time.LocalDateTime;
import java.util.Map;

public interface VehicleActivityService {
    Map<String, Object> getVehicleActivityData(LocalDateTime startTime, LocalDateTime endTime);
}

