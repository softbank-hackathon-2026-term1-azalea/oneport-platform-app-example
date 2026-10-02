package com.oneport.notes.system;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class ReleaseHeaderFilter extends OncePerRequestFilter {

	static final String HEADER = "X-Launchpad-Release";

	private final String releaseId;

	ReleaseHeaderFilter(@Value("${LAUNCHPAD_RELEASE_ID:}") String releaseId) {
		this.releaseId = releaseId;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (StringUtils.hasText(releaseId)) {
			response.setHeader(HEADER, releaseId);
		}
		chain.doFilter(request, response);
	}

}
