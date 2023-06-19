package com.dyinvoice.backend.controller;


import com.dyinvoice.backend.exception.ResourceNotFoundException;
import com.dyinvoice.backend.exception.ValidationException;
import com.dyinvoice.backend.model.entity.AppUser;
import com.dyinvoice.backend.model.entity.EntitiesRoleName;
import com.dyinvoice.backend.model.form.AppUserForm;
import com.dyinvoice.backend.model.form.LoginForm;
import com.dyinvoice.backend.model.form.RegisterForm;
import com.dyinvoice.backend.model.response.JWTLoginResponse;
import com.dyinvoice.backend.model.view.AppUserView;
import com.dyinvoice.backend.service.AppUserService;
import com.dyinvoice.backend.service.implementation.AppUserServiceImpl;
import io.swagger.annotations.*;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;


@Api(value="AppUserController", description="Rest API for App User operations.")
@RestController
@CrossOrigin
@AllArgsConstructor
@RequestMapping(value = "/v1/user")
public class AppUserController {

    private AppUserService appUserService;
    private final AppUserServiceImpl appUserServiceImpl;


    @ApiOperation(value = "Get App user profile by ID.", response = AppUserView.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Validation Exception"),
            @ApiResponse(code = 404, message = "Resource Not Found Exception"),
            @ApiResponse(code = 500, message = "Internal Exception")

    })

    @GetMapping(value="/{appUserId}")
    public AppUserView getUserInfo(
            @ApiParam(value = "AppUser ID.", name = "appUserId", required = true)
            @PathVariable("appUserId") final String appUserId,
            Authentication authentication) throws ValidationException, ResourceNotFoundException {

        AppUserForm form = new AppUserForm();

        boolean useId = false;

        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(EntitiesRoleName.ROLE_PREFIX + EntitiesRoleName.ROLE_ADMIN)
                        || a.getAuthority().equals(EntitiesRoleName.ROLE_PREFIX + EntitiesRoleName.ROLE_ADMIN) )) {
            useId = true;
        }

        if(useId){
            try {
                form.setId(Long.parseLong(appUserId));
            } catch(NumberFormatException e) {
                form.setEmail(appUserId); // If not a long, assume it's an email
            }
        } else {
            try {
                form.setId(Long.parseLong(authentication.getName()));
            } catch(NumberFormatException e) {
                form.setEmail(authentication.getName()); // If not a long, assume it's an email
            }
        }

        return appUserService.getAppUserInfo(form);
    }

    @ApiOperation(value = "Register User.", response = AppUserView.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Validation Exception"),
            @ApiResponse(code = 404, message = "Resource Not Found Exception"),
            @ApiResponse(code = 500, message = "Internal Exception")

    })
    @PostMapping(value = "/register")
    public ResponseEntity<AppUser> registerUser(@Valid @RequestBody RegisterForm form) throws ValidationException, ResourceNotFoundException {
        AppUser appUser = appUserService.registerUser(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(appUser);
    }



    @ApiOperation(value = "Login User.", response = AppUserView.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Validation Exception"),
            @ApiResponse(code = 404, message = "Resource Not Found Exception"),
            @ApiResponse(code = 500, message = "Internal Exception")

    })
    @PostMapping(value = "/login")
    public ResponseEntity<JWTLoginResponse> loginUser(@RequestBody LoginForm form) throws ValidationException, ResourceNotFoundException {
        String token = appUserService.loginUser(form);

        JWTLoginResponse response = new JWTLoginResponse();
        response.setAccessToken(token);

        return ResponseEntity.ok(response);
    }

}