package io.pockethive.hivewatch.service.provisioning;

import io.pockethive.hivewatch.service.api.EnvironmentProvisioningPlanDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanValidationResultDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentProvisioningController {
    private final ProvisioningPlanValidationService validationService;

    public EnvironmentProvisioningController(ProvisioningPlanValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping("/api/v1/admin/environment-provisioning/plans/validate")
    public ProvisioningPlanValidationResultDto validate(@RequestBody EnvironmentProvisioningPlanDto plan) {
        return validationService.validate(plan);
    }
}
