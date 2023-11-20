/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.RegisterDTO;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.queue.util.TbCoreComponent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class RegisterController extends BaseController {
    public static final String URL_BASE = "http://requestiot.com/api/";
    
    @ApiOperation(value = "Register a user (registerUser)",
            notes = "Create or update the User. When creating user, platform generates User Id as " + "uuid" +
                    "The newly created User Id will be present in the response. " +
                    "Specify existing User Id to update the device. " +
                    "Referencing non-existing User Id will cause 'Not Found' error." +
                    "\n\nDevice email is unique for entire platform setup." +
                    "Remove 'id', 'tenantId' and optionally 'customerId' from the request body example (below) to create new User entity." +
                    "\n\nAvailable for users with 'SYS_ADMIN', 'TENANT_ADMIN' or 'CUSTOMER_USER' authority.")
    @PreAuthorize("permitAll()")
    @RequestMapping(value = "/register/criar", method = RequestMethod.POST)
    @ResponseBody
    public User registerUser(
            @ApiParam(value = "A JSON value representing the User and customer", required = true)
            @RequestBody RegisterDTO user,
            @ApiParam(value = "Send activation email (or use activation link)", defaultValue = "true")
            @RequestParam(required = false, defaultValue = "true") boolean sendActivationMail, HttpServletRequest request
            ) throws ThingsboardException {
                Gson gson = new GsonBuilder().create();

                RestTemplate restTemplate = new RestTemplate();
                Map<String, String> loginRequest = new HashMap<>();

                //TODO: criar uma classe para credenciais de Tenant admin, com encryptação
                loginRequest.put("username", "sat@satsolucoes.com.br");
                loginRequest.put("password", "dwm2016");

                ResponseEntity<JsonNode> responseLogin = restTemplate.postForEntity(URL_BASE+"auth/login", loginRequest, JsonNode.class);

                String token = responseLogin.getBody().get("token").toString();

                List<MediaType> listagemMidia = new ArrayList<>();
                listagemMidia.add(MediaType.APPLICATION_JSON);

                restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON); 
                headers.setAccept(listagemMidia);
                headers.add("X-Authorization","Bearer "+token.replace("\"", ""));
                HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(user.getCustomerId()).toString(), headers);
                
                ResponseEntity<Customer> responseCreatedCustomer = restTemplate.postForEntity(URL_BASE+"customer", entity, Customer.class);
                
                User userToCreate = new User();

                userToCreate.setTenantId(user.getTenantId() );
                userToCreate.setCustomerId(new CustomerId(UUID.fromString(responseCreatedCustomer.getBody().getId().toString())));
                userToCreate.setEmail(user.getEmail());
                userToCreate.setAuthority(user.getAuthority());
                userToCreate.setFirstName(user.getFirstName());
                userToCreate.setLastName(user.getLastName());

                //Jogada para adicionar entity type no tenant e customer
                ObjectMapper oMapper = new ObjectMapper();
                Map<String, Object> map = oMapper.convertValue(userToCreate, Map.class);

                String jsonString = "";
                
                try {
                    jsonString = oMapper.writeValueAsString(map);
                    
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }

                entity = new HttpEntity<String>(jsonString, headers);
                ResponseEntity<User> responseCreatedUser = restTemplate.postForEntity(URL_BASE+"user?sendActivationMail=true", entity, User.class);

        return responseCreatedUser.getBody();
    }

}
