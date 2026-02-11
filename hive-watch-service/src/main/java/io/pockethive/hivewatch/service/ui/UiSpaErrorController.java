package io.pockethive.hivewatch.service.ui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class UiSpaErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, HttpServletResponse response) {
        Object status = request.getAttribute("jakarta.servlet.error.status_code");
        Object uri = request.getAttribute("jakarta.servlet.error.request_uri");
        if (!(status instanceof Integer statusCode) || !(uri instanceof String requestUri)) {
            return "error";
        }

        if (statusCode != HttpStatus.NOT_FOUND.value()) {
            return "error";
        }

        if (requestUri.startsWith("/api/")) {
            return "error";
        }

        if (requestUri.startsWith("/actuator/")) {
            return "error";
        }

        if (requestUri.contains(".")) {
            return "error";
        }

        response.setStatus(HttpStatus.OK.value());
        return "forward:/index.html";
    }
}
