package io.pockethive.hivewatch.service.expectedsets;

import io.pockethive.hivewatch.service.api.ExpectedSetTemplateDto;
import io.pockethive.hivewatch.service.api.ExpectedSetTemplateKind;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
public class ExpectedSetTemplateController {
    private final ExpectedSetTemplateQueryService queryService;

    public ExpectedSetTemplateController(ExpectedSetTemplateQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/api/v1/expected-set-templates")
    public List<ExpectedSetTemplateDto> list(@RequestParam("kind") String kind) {
        if (kind == null || kind.trim().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "kind is required");
        }
        ExpectedSetTemplateKind k;
        try {
            k = ExpectedSetTemplateKind.valueOf(kind.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid kind: " + kind.trim());
        }
        return queryService.list(k);
    }
}

