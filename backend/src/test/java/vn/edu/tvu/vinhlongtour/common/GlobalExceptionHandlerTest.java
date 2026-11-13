package vn.edu.tvu.vinhlongtour.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra mọi loại lỗi đều trả về cùng dạng {@link ApiError}: lỗi nghiệp vụ, dữ liệu sai,
 * JSON hỏng, sai phương thức HTTP và lỗi không mong đợi (không lộ chi tiết bên trong).
 */
class GlobalExceptionHandlerTest {

    record SampleInput(@NotBlank String name, @Email String email) {}

    @RestController
    static class SampleController {
        @GetMapping("/business-error")
        void businessError() {
            throw new ApiException(HttpStatus.CONFLICT, "SEAT_NOT_ENOUGH", "Đợt khởi hành chỉ còn 2 chỗ");
        }

        @GetMapping("/crash")
        void crash() {
            throw new IllegalStateException("bí mật nội bộ");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody SampleInput input) {
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new SampleController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void businessErrorKeepsStatusCodeAndMessage() throws Exception {
        mvc.perform(get("/business-error"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SEAT_NOT_ENOUGH"))
                .andExpect(jsonPath("$.message").value("Đợt khởi hành chỉ còn 2 chỗ"));
    }

    @Test
    void invalidFieldsAreListedByName() throws Exception {
        mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"email\":\"sai\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content("{hỏng"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void wrongHttpMethodIsMethodNotAllowed() throws Exception {
        mvc.perform(delete("/business-error"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void unexpectedErrorHidesInternalDetails() throws Exception {
        mvc.perform(get("/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Lỗi hệ thống, vui lòng thử lại sau"));
    }
}
