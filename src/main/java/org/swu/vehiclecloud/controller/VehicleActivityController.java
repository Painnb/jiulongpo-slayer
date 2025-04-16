package org.swu.vehiclecloud.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.swu.vehiclecloud.service.VehicleActivityService;

import java.util.Map;

@RestController
@RequestMapping("/api/activecontroller")
public class VehicleActivityController {

    private final VehicleActivityService vehicleActivityService;

    public VehicleActivityController(VehicleActivityService vehicleActivityService) {
        this.vehicleActivityService = vehicleActivityService;
    }

    @GetMapping("/public/vehicle-activity")
    public Map<String, Object> getAllVehicleActivities() {
        return vehicleActivityService.getVehicleActivityData();
    }
}
