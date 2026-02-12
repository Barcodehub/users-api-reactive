package com.example.resilient_api.infrastructure.entrypoints;

import com.example.resilient_api.infrastructure.entrypoints.handler.AuthHandler;
import com.example.resilient_api.infrastructure.entrypoints.handler.UserHandlerImpl;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {

    @Bean
    @RouterOperations({
        @RouterOperation(path = "/auth/login", method = RequestMethod.POST, beanClass = AuthHandler.class, beanMethod = "login"),
        @RouterOperation(path = "/users", method = RequestMethod.POST, beanClass = UserHandlerImpl.class, beanMethod = "createUser"),
        @RouterOperation(path = "/users/{id}", method = RequestMethod.GET, beanClass = UserHandlerImpl.class, beanMethod = "getUserById"),
        @RouterOperation(path = "/users/check-exists", method = RequestMethod.POST, beanClass = UserHandlerImpl.class, beanMethod = "checkUsersExist"),
        @RouterOperation(path = "/users/by-ids", method = RequestMethod.POST, beanClass = UserHandlerImpl.class, beanMethod = "getUsersByIds")
    })
    public RouterFunction<ServerResponse> routerFunction(UserHandlerImpl userHandler, AuthHandler authHandler) {
        return route(POST("/auth/login"), authHandler::login)
            .andRoute(POST("/users"), userHandler::createUser)
            .andRoute(GET("/users/{id}"), userHandler::getUserById)
            .andRoute(POST("/users/check-exists"), userHandler::checkUsersExist)
            .andRoute(POST("/users/by-ids"), userHandler::getUsersByIds);
    }

}
