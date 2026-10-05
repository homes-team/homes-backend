package com.homes.backend.domain.property.price.controller;

import com.homes.backend.domain.property.price.service.PropertyPricePredictionService;
import com.homes.backend.global.exception.CustomException;
import com.homes.backend.global.exception.GlobalErrorCode;
import com.homes.backend.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PropertyPricePredictionControllerTest {
    @Test
    void passesRemoteAddressToServiceAndReturns429ForRejectedRequest() throws Exception {
        PropertyPricePredictionService service = mock(PropertyPricePredictionService.class);
        when(service.predict(21L, "192.0.2.1"))
                .thenThrow(new CustomException(GlobalErrorCode.TOO_MANY_REQUESTS));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PropertyPricePredictionController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(get("/properties/21/price-prediction")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "192.0.2.2")
                        .with(request -> {
                            request.setRemoteAddr("192.0.2.1");
                            return request;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("COMMON429"));
        verify(service).predict(21L, "192.0.2.1");
    }
}
