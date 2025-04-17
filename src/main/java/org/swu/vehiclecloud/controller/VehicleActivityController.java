package org.swu.vehiclecloud.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.swu.vehiclecloud.service.VehicleActivityService;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/activecontroller")
public class VehicleActivityController {

    private final VehicleActivityService vehicleActivityService;

    public VehicleActivityController(VehicleActivityService vehicleActivityService) {
        this.vehicleActivityService = vehicleActivityService;
    }

    @GetMapping("/public/vehicle-activity")
    public Map<String, Object> getAllVehicleActivities(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return vehicleActivityService.getVehicleActivityData(startTime, endTime);
    }
}
