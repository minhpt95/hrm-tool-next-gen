package com.minhpt.hrmtoolnextgen.controller;

import com.minhpt.hrmtoolnextgen.constant.ApiConstant;
import com.minhpt.hrmtoolnextgen.service.device.DeviceCommandService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@AllArgsConstructor
@Log4j2
@RequestMapping({ApiConstant.DEVICE_BASE, ApiConstant.DEVICE_V1_BASE})
@Tag(name = "Device", description = "Device management APIs")
public class DeviceController {
    private final DeviceCommandService deviceCommandService;

    @PostMapping("/")
    public void createDevice() {
        log.info("Create device");
    }
}
