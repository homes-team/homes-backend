package com.homes.backend.domain.realtor.controller;

import com.homes.backend.domain.realtor.dto.request.RealtorSignupReqDto;
import com.homes.backend.domain.realtor.dto.response.RealtorSignupResDto;
import com.homes.backend.domain.realtor.service.RealtorService;
import com.homes.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class RealtorController implements RealtorControllerDocs {

    private final RealtorService realtorService;

    @Override
    @PostMapping("/realtors")
    public ApiResponse<RealtorSignupResDto> signUpRealtor(@RequestBody @Valid RealtorSignupReqDto request) {
        RealtorSignupResDto response = realtorService.signUp(request);
        return ApiResponse.onSuccess(response);
    }
}
