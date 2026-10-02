package com.oneport.notes.system;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	static final String HEADER = "X-Request-ID";

	static final String MDC_KEY = "request_id";

	private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

	private static final Set<String> QUIET_PATHS = Set.of("/health", "/ready");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(HEADER));
		response.setHeader(HEADER, requestId);
		MDC.put(MDC_KEY, requestId);
		long started = System.nanoTime();
		try {
			chain.doFilter(request, response);
		}
		finally {
			if (!QUIET_PATHS.contains(request.getRequestURI())) {
				log.atInfo()
					.addKeyValue("method", request.getMethod())
					.addKeyValue("path", request.getRequestURI())
					.addKeyValue("status", response.getStatus())
					.addKeyValue("duration_ms", (System.nanoTime() - started) / 1_000_000.0)
					.log("request");
			}
			MDC.remove(MDC_KEY);
		}
	}

	private static String resolveRequestId(String header) {
		if (header != null) {
			try {
				return UUID.fromString(header).toString();
			}
			catch (IllegalArgumentException ignored) {

			}
		}
		return UUID.randomUUID().toString();
	}

}
