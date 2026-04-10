package io.pockethive.hivewatch.service.provisioning;

import io.pockethive.hivewatch.service.api.EnvironmentProvisioningPlanDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanApplyRequestDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanApplyResultDto;
import io.pockethive.hivewatch.service.api.ProvisioningPlanValidationResultDto;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnvironmentProvisioningController {
    private final ProvisioningPlanValidationService validationService;
    private final ProvisioningPlanApplyService applyService;

    public EnvironmentProvisioningController(
            ProvisioningPlanValidationService validationService,
            ProvisioningPlanApplyService applyService
    ) {
        this.validationService = validationService;
        this.applyService = applyService;
    }

    @PostMapping("/api/v1/admin/environment-provisioning/plans/validate")
    public ProvisioningPlanValidationResultDto validate(@RequestBody EnvironmentProvisioningPlanDto plan) {
        return validationService.validate(plan);
    }

    @PostMapping("/api/v1/admin/environment-provisioning/plans/apply")
    public ProvisioningPlanApplyResultDto apply(@RequestBody ProvisioningPlanApplyRequestDto request) {
        return applyService.apply(request);
    }
}
