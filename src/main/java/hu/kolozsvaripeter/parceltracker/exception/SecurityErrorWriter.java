package hu.kolozsvaripeter.parceltracker.exception;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.json.JsonMapper;

public final class SecurityErrorWriter {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private SecurityErrorWriter() {}

    public static void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        JSON.writeValue(response.getOutputStream(), ErrorResponse.of(code, message));
    }
}
