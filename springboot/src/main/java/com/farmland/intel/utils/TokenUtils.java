package com.farmland.intel.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.farmland.intel.entity.User;
import com.farmland.intel.service.IUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@Component
public class TokenUtils {

    private static IUserService staticUserService;
    private static String staticJwtSecret;
    private static int staticExpireHours;

    @Resource
    private IUserService userService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expire-hours:12}")
    private Integer expireHours;

    @PostConstruct
    public void init() {
        staticUserService = userService;
        staticJwtSecret = jwtSecret;
        staticExpireHours = expireHours == null ? 12 : expireHours;
    }

    public static String genToken(String userId) {
        return JWT.create()
                .withAudience(userId)
                .withIssuedAt(new Date())
                .withExpiresAt(DateUtil.offsetHour(new Date(), staticExpireHours))
                .sign(Algorithm.HMAC256(staticJwtSecret));
    }

    public static JWTVerifier getVerifier() {
        return JWT.require(Algorithm.HMAC256(staticJwtSecret)).build();
    }

    public static User getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String token = request.getHeader("token");
            if (StrUtil.isNotBlank(token)) {
                String userId = JWT.decode(token).getAudience().get(0);
                return staticUserService.getById(Integer.valueOf(userId));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
