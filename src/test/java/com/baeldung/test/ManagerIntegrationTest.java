package com.baeldung.test;

import com.baeldung.Application;
import com.baeldung.persistence.dao.UserRepository;
import com.baeldung.persistence.model.User;
import com.baeldung.spring.TestDbConfig;
import com.baeldung.spring.TestIntegrationConfig;
import io.restassured.RestAssured;
import io.restassured.authentication.FormAuthConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Application.class, TestDbConfig.class, TestIntegrationConfig.class }, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ManagerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${local.server.port}")
    int port;

    private FormAuthConfig formConfig;
    private String MANAGEMENT_URL;

    //

    @Before
    public void init() {
        // manager
        User user = userRepository.findByEmail("manager@test.com");
        if (user == null) {
            user = new User();
            user.setFirstName("Manager");
            user.setLastName("Manger");
            user.setPassword(passwordEncoder.encode("test"));
            user.setEmail("manager@test.com");
            user.setEnabled(true);
            userRepository.save(user);
        } else {
            user.setPassword(passwordEncoder.encode("test"));
            userRepository.save(user);
        }

        // other user
        user = userRepository.findByEmail("test@test.com");
        if (user == null) {
            user = new User();
            user.setFirstName("Test");
            user.setLastName("Test");
            user.setPassword(passwordEncoder.encode("test"));
            user.setEmail("test@test.com");
            user.setEnabled(true);
            userRepository.save(user);
        } else {
            user.setPassword(passwordEncoder.encode("test"));
            userRepository.save(user);
        }

        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        MANAGEMENT_URL = "/management";
        formConfig = new FormAuthConfig("/login", "username", "password");
    }

    @Test
    public void givenManagerUser_whenGettingLoggedAndGoManagementSite_thenResponseIsManagementLandingPage() {
        final RequestSpecification request = RestAssured.given().auth().form("manager@test.com", "test", formConfig);

        final Map<String, String> params = new HashMap<>();
        params.put("password", "test");

        final Response response = request.with().params(params).get(MANAGEMENT_URL);

        assertEquals(200, response.statusCode());
        assertTrue(response.getBody().asString().contains("Management Site"));
    }

    @Test
    public void givenOtherUser_whenGettingLoggedAndGoManagementSite_thenResponseForbidden() {
        final RequestSpecification request = RestAssured.given().auth().form("test@test.com", "test", formConfig);

        final Map<String, String> params = new HashMap<>();
        params.put("password", "test");

        final Response response = request.with().params(params).get(MANAGEMENT_URL);

        assertEquals(403, response.statusCode());
    }
}
