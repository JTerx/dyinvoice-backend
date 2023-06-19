package com.dyinvoice.backend.config;


import com.dyinvoice.backend.model.entity.EntitiesRoleName;
import com.dyinvoice.backend.model.entity.Role;
import com.dyinvoice.backend.repository.RoleRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;


@AllArgsConstructor
@Configuration
public class LoadDataBase {

    private RoleRepository roleRepository;

    private static final Logger log = LoggerFactory.getLogger(LoadDataBase.class);

    @Bean
    CommandLineRunner initDataBase(){
        return args -> {
            log.info("Init database check ");
            log.info("Create Roles ");

            Optional<Role> admin = roleRepository.findByName(EntitiesRoleName.ROLE_ADMIN);
            if(admin.isEmpty()){
                Role adminRole = new Role();
                adminRole.setName(EntitiesRoleName.ROLE_ADMIN);
                adminRole.setShortName("admin");
                roleRepository.save(adminRole);
            }

            Optional<Role> user = roleRepository.findByName(EntitiesRoleName.ROLE_USER);
            if(user.isEmpty()){
                Role userRole = new Role();
                userRole.setName(EntitiesRoleName.ROLE_USER);
                userRole.setShortName("user");
                roleRepository.save(userRole);
            }

            Optional<Role> superAdmin = roleRepository.findByName(EntitiesRoleName.ROLE_SUPER_ADMIN);
            if(superAdmin.isEmpty()){
                Role superAdminRole = new Role();
                superAdminRole.setName(EntitiesRoleName.ROLE_SUPER_ADMIN);
                superAdminRole.setShortName("superadmin");
                roleRepository.save(superAdminRole);
            }
            log.info("End");


        };
    }
}