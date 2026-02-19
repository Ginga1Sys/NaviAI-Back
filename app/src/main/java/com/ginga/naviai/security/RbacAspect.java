package com.ginga.naviai.security;

import com.ginga.naviai.security.annotation.RequirePermissions;
import com.ginga.naviai.security.annotation.RequireRoles;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class RbacAspect {

    @Before("@annotation(com.ginga.naviai.security.annotation.RequireRoles) || @within(com.ginga.naviai.security.annotation.RequireRoles)")
    public void checkRoles(JoinPoint jp) {
        Method method = ((MethodSignature) jp.getSignature()).getMethod();
        RequireRoles ann = method.getAnnotation(RequireRoles.class);
        if (ann == null) {
            ann = jp.getTarget().getClass().getAnnotation(RequireRoles.class);
        }
        if (ann == null) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Insufficient permissions");

        Set<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        boolean ok = Arrays.stream(ann.value()).anyMatch(r -> authorities.contains(r) || authorities.contains("ROLE_" + r));
        if (!ok) throw new AccessDeniedException("Insufficient roles");
    }

    @Before("@annotation(com.ginga.naviai.security.annotation.RequirePermissions) || @within(com.ginga.naviai.security.annotation.RequirePermissions)")
    public void checkPermissions(JoinPoint jp) {
        Method method = ((MethodSignature) jp.getSignature()).getMethod();
        RequirePermissions ann = method.getAnnotation(RequirePermissions.class);
        if (ann == null) {
            ann = jp.getTarget().getClass().getAnnotation(RequirePermissions.class);
        }
        if (ann == null) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) throw new AccessDeniedException("Insufficient permissions");

        Set<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        boolean ok = Arrays.stream(ann.value()).allMatch(p -> authorities.contains(p));
        if (!ok) throw new AccessDeniedException("Insufficient permissions");
    }
}
