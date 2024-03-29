package com.dyinvoice.backend.dao.implementation;

import com.dyinvoice.backend.config.EmailSender;
import com.dyinvoice.backend.dao.AppUserDAO;
import com.dyinvoice.backend.exception.ExceptionType;
import com.dyinvoice.backend.exception.InvoiceApiException;
import com.dyinvoice.backend.exception.ResourceNotFoundException;
import com.dyinvoice.backend.model.entity.*;
import com.dyinvoice.backend.model.view.AppUserView;
import com.dyinvoice.backend.repository.AppUserRepository;
import com.dyinvoice.backend.repository.EntrepriseRepository;
import com.dyinvoice.backend.repository.InvitationRepository;
import com.dyinvoice.backend.repository.RoleRepository;
import com.dyinvoice.backend.security.JwtTokenProvider;
import com.dyinvoice.backend.utils.EntityToViewConverter;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


@AllArgsConstructor
@Component
public class AppUserDAOImpl implements AppUserDAO {

    AppUserRepository appUserRepository;
    AuthenticationManager authenticationManager;
    RoleRepository roleRepository;
    InvitationRepository invitationRepository;
    PasswordEncoder passwordEncoder;
    JwtTokenProvider jwtTokenProvider;
    EmailSender emailSender;
    EntrepriseRepository enterpriseRepository;

    private static final Logger logger = LoggerFactory.getLogger(AppUserDAOImpl.class);

    @Override
    public boolean isUserExist(AppUser appUser) {
        Optional<AppUser> appUserEntity = appUserRepository.findById(appUser.getId());

        return appUserEntity.isPresent();
    }

    @Override
    public AppUserView getUserInfo(String token) throws ResourceNotFoundException {

        String email = jwtTokenProvider.getEmail(token);
        logger.debug(email);
        AppUser appUser = appUserRepository.findByEmail(email);
        if (appUser == null) {
            throw new ResourceNotFoundException("User not found");
        }
        return EntityToViewConverter.convertEntityToAppUserView(appUser);
    }

    @Override
    public AppUser getAppUserByEmail(String email) {
        return appUserRepository.findByEmail(email);
    }

    @Override
    public AppUser getAppUserById(Long id) {
        return appUserRepository.findById(id).orElse(null);
    }

    @Override
    public AppUserView getAppUserInfoById(AppUser appUser) throws ResourceNotFoundException {
        return EntityToViewConverter.convertEntityToAppUserView(getAppUser(appUser));
    }

    @Override
    public AppUser getAppUser(AppUser appUser) throws ResourceNotFoundException {

        AppUser appUserEntity = null;

        if(appUser.getId()  != null) {
            appUserEntity = appUserRepository.findById(appUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(ExceptionType.ERROR_MSG_USER_PROFILE_NOT_FOUND));
        }else if(appUser.getEmail()!= null) {
            appUserEntity = appUserRepository.findByEmail(appUser.getEmail());

            if(appUserEntity == null){
                throw  new ResourceNotFoundException(ExceptionType.ERROR_MSG_USER_PROFILE_NOT_FOUND);
            }
        }
        return appUserEntity;
    }

    @Override
    public String login(AppUser appUser) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                appUser.getEmail(), appUser.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return jwtTokenProvider.generateToken(authentication);
    }

    @Override
    public String register(AppUser appUser) {

        // Check if the user already exists
        AppUser existingUser = appUserRepository.findByEmail(appUser.getEmail());
        if (existingUser != null) {
            throw new InvoiceApiException("User with this email already exists");
        }

        existingUser = appUserRepository.findByPhoneNumber(appUser.getPhoneNumber());
        if (existingUser != null) {
            throw new InvoiceApiException( "User with this phone number already exists");
        }

        // Check if a company with this SIRET already exists
/*        Optional<Entreprise> existingCompany = Optional.ofNullable(enterpriseRepository.findBySiret(appUser.getEntreprise().getSiret()));
        if (existingCompany.isPresent()) {
            throw new InvoiceApiException( "Company with this SIRET already exists");
        }*/

        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));

        // Assign roles to the user
        Set<Role> roles = new HashSet<>();
        Optional<Role> userRoleOptional = roleRepository.findByName(EntitiesRoleName.ROLE_ADMIN);
        if (userRoleOptional.isEmpty()) {
            throw new InvoiceApiException( "Admin role not found");
        }
        roles.add(userRoleOptional.get());

        appUser.setRoles(roles);

        // Create the company
        Entreprise company = new Entreprise();
        company.setName(appUser.getEntreprise().getName());
        company.setSiret(appUser.getEntreprise().getSiret());
        company.setAppUser(appUser);
        appUser.setEntreprise(company);

        appUserRepository.save(appUser);

        return "User Created successfully";
    }

    public Long getLoggedInUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email;
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.User) {
            email = ((org.springframework.security.core.userdetails.User) principal).getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        } else {
            return null;
        }

        AppUser appUser = appUserRepository.findByEmail(email);
        return appUser != null ? appUser.getId() : null;
    }


    public Long getLoggedInUserEntrepriseId() {
        Long userId = getLoggedInUserId();
        if (userId == null) {
            return null;
        }

        AppUser appUser = appUserRepository.findById(userId).orElse(null);
        if (appUser == null || appUser.getEntreprise() == null) {
            return null;
        }

        return appUser.getEntreprise().getId();
    }

    public String getLoggedInUserPhoneNumber() {
        Long userId = getLoggedInUserId();
        if (userId == null) {
            return null;
        }

        AppUser appUser = appUserRepository.findById(userId).orElse(null);
        return appUser != null ? appUser.getPhoneNumber() : null;
    }

    public String getLoggedInUserEmail(){
        Long userId = getLoggedInUserId();
        if(userId == null) {
            return null;
        }

        AppUser appUser = appUserRepository.findById(userId).orElse(null);
        return appUser != null ? appUser.getEmail() : null;
    }

    @Override
    public AppUser updateAppUser(AppUser updatedAppUser) throws ResourceNotFoundException {
        // Trouver l'utilisateur existant
        AppUser existingAppUser = appUserRepository.findById(updatedAppUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("AppUser not found with id : " + updatedAppUser.getId()));

        // Mettre à jour les informations de l'utilisateur
        existingAppUser.setFirstName(updatedAppUser.getFirstName());
        existingAppUser.setLastName(updatedAppUser.getLastName());
        existingAppUser.setEmail(updatedAppUser.getEmail());
        existingAppUser.setPhoneNumber(updatedAppUser.getPhoneNumber());
        // Supposons que le mot de passe est déjà encodé
        existingAppUser.setPassword(updatedAppUser.getPassword());

        // Trouver l'entreprise existante
        Entreprise existingCompany = enterpriseRepository.findById(updatedAppUser.getEntreprise().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with id : " + updatedAppUser.getEntreprise().getId()));

        // Mettre à jour les informations de l'entreprise
        existingCompany.setName(updatedAppUser.getEntreprise().getName());
        existingCompany.setSiret(updatedAppUser.getEntreprise().getSiret());
        existingCompany.setAddress(updatedAppUser.getEntreprise().getAddress());
        existingCompany.setCapitalSocial(updatedAppUser.getEntreprise().getCapitalSocial());
        existingCompany.setFormeJuridique(updatedAppUser.getEntreprise().getFormeJuridique());

        // Sauvegarder les modifications pour l'entreprise
        enterpriseRepository.save(existingCompany);

        // Sauvegarder les modifications pour l'utilisateur
        return appUserRepository.save(existingAppUser);
    }




}
