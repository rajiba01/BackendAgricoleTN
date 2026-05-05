package tn.economic.system.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // CORS: when running behind Nginx/Cloudflare, the Origin will be the public host.
        // We reflect the Origin (instead of '*') to stay compatible with credentials.
        // Note: some non-browser requests may not send Origin at all.
        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Vary", "Origin");
            resp.setHeader("Access-Control-Allow-Credentials", "true");
        }

        // Important pour le preflight
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization");

        // Only cache preflight responses when Origin is present (browser context).
        // Without Origin, caching can behave unexpectedly and isn't useful.
        if (origin != null && !origin.isBlank()) {
            resp.setHeader("Access-Control-Max-Age", "3600");
        }

        // Si c'est un preflight, on répond 200 directement
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}