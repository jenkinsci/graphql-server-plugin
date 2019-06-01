package io.jenkins.plugins.io.jenkins.plugins.graphql.filters;

import hudson.Extension;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Extension
@SuppressWarnings("unused")
public class GraphQLCrumbExclusion extends hudson.security.csrf.CrumbExclusion {

    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        String pathInfo = req.getPathInfo();
        if (pathInfo != null && pathInfo.startsWith("/graphql")) {
            chain.doFilter(req, resp);
            return true;
        }
        return false;
    }
}
