package com.farmland.intel.config.interceptor;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.farmland.intel.common.Constants;
import com.farmland.intel.entity.User;
import com.farmland.intel.exception.ServiceException;
import com.farmland.intel.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;

public class JwtInterceptor implements HandlerInterceptor {
    private static final List<String> PUBLIC_PATH_PATTERNS = Arrays.asList(
            "/user/login",
            "/user/register",
            "/api/chat/**",
            "/api/agent/**",
            "/fruit-detect/**",
            "/crop-analysis/**",
            "/swagger**/**",
            "/webjars/**",
            "/v3/**",
            "/doc.html",
            "/actuator/**",
            "/health",
            "/error"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private IUserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestMethod = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(requestMethod)) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        if (handler instanceof HandlerMethod) {
            AuthAccess annotation = ((HandlerMethod) handler).getMethodAnnotation(AuthAccess.class);
            if (annotation != null) {
                return true;
            }
        }

        String requestUri = request.getRequestURI();
        if (isPublicPath(requestUri) || isPublicFileDownloadPath(requestMethod, requestUri)) {
            return true;
        }

        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("token");
        }

        if (StrUtil.isBlank(token)) {
            throw new ServiceException(Constants.CODE_401, "无 token，请重新登录");
        }

        String userId;
        try {
            userId = JWT.decode(token).getAudience().get(0);
        } catch (JWTDecodeException ex) {
            throw new ServiceException(Constants.CODE_401, "token 验证失败，请重新登录");
        }

        User user = userService.getById(userId);
        if (user == null) {
            throw new ServiceException(Constants.CODE_401, "用户不存在，请重新登录");
        }

        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getPassword())).build();
        try {
            jwtVerifier.verify(token);
        } catch (JWTVerificationException ex) {
            throw new ServiceException(Constants.CODE_401, "token 验证失败，请重新登录");
        }
        return true;
    }

    private boolean isPublicPath(String requestUri) {
        for (String pattern : PUBLIC_PATH_PATTERNS) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPublicFileDownloadPath(String requestMethod, String requestUri) {
        if (!"GET".equalsIgnoreCase(requestMethod) || !requestUri.startsWith("/file/")) {
            return false;
        }
        String path = StrUtil.removePrefix(requestUri, "/file/");
        return StrUtil.isNotBlank(path) && !path.contains("/") && !"page".equals(path);
    }
}
