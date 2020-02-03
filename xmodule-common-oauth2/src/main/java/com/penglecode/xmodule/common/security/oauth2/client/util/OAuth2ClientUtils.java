package com.penglecode.xmodule.common.security.oauth2.client.util;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.ResolvableType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import com.penglecode.xmodule.common.security.oauth2.client.support.OAuth2PrincipalNameAuthentication;
import com.penglecode.xmodule.common.security.oauth2.consts.OAuth2ApplicationConstants;
import com.penglecode.xmodule.common.util.CollectionUtils;
import com.penglecode.xmodule.common.util.SpringUtils;
import com.penglecode.xmodule.common.util.SpringWebMvcUtils;

/**
 * 有关OAuth2客户端的工具类
 * 
 * @author 	pengpeng
 * @date 	2020年1月27日 上午11:41:21
 */
@SuppressWarnings("unchecked")
public class OAuth2ClientUtils {

	/**
	 * 获取所有OAuth2客户端注册
	 * @return
	 */
	public static Set<ClientRegistration> getAllClientRegistrations() {
		Set<ClientRegistration> clientRegistrations = new LinkedHashSet<ClientRegistration>();
		ClientRegistrationRepository clientRegistrationRepository = SpringUtils.getBean(ClientRegistrationRepository.class);
		ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository).as(Iterable.class);
		if (type != ResolvableType.NONE && ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
			Iterable<ClientRegistration> registrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
			if(registrations != null) {
				registrations.forEach(clientRegistrations::add);
			}
		}
		return clientRegistrations;
	}
	
	/**
	 * 获取指定AuthorizationGrantType的OAuth2客户端注册(多个)
	 * @param authorizationGrantType
	 * @return
	 */
	public static Set<ClientRegistration> getClientRegistrationByType(AuthorizationGrantType authorizationGrantType) {
		Set<ClientRegistration> allClientRegistrations = getAllClientRegistrations();
		return allClientRegistrations.stream().filter(r -> r.getAuthorizationGrantType().equals(authorizationGrantType)).collect(Collectors.toSet());
	}
	
	/**
	 * 根据clientRegistrationId查找ClientRegistration
	 * @param clientRegistrationId
	 * @return
	 */
	public static ClientRegistration getClientRegistrationById(String clientRegistrationId) {
		ClientRegistrationRepository clientRegistrationRepository = SpringUtils.getBean(ClientRegistrationRepository.class);
		return clientRegistrationRepository.findByRegistrationId(clientRegistrationId);
	}
	
	/**
	 * 获取已授权客户端
	 * 
	 * 注意该方法仅仅获取被缓存的OAuth2AuthorizedClient,而不管其是否有效或已过期
	 * @param clientRegistrationId
	 * @param principal
	 * @param request
	 * @return
	 */
	public static OAuth2AuthorizedClient getOAuth2AuthorizedClient(String clientRegistrationId, Authentication principal, HttpServletRequest request) {
		if(request == null) {
			request = SpringWebMvcUtils.getCurrentHttpServletRequest();
		}
		OAuth2AuthorizedClientRepository authorizedClientRepository = SpringUtils.getBean(OAuth2AuthorizedClientRepository.class);
		//此处需要注意principal.name值必须与username的值一致
		return authorizedClientRepository.loadAuthorizedClient(clientRegistrationId, principal, request);
	}
	
	/**
	 * 准备OAuth2登录(Password模式)的Authentication
	 * @param clientRegistration
	 * @param principal
	 * @param principalName
	 * @return
	 */
	public static Authentication preparePasswordOAuth2Authentication(ClientRegistration clientRegistration, Authentication principal, String principalName) {
		if(AuthorizationGrantType.PASSWORD.equals(clientRegistration.getAuthorizationGrantType())) {
			if(principal == null) {
				//使用具名的Authentication而不是默认的AnonymousAuthenticationToken，使得获得到的新的OAuth2AuthorizedClient存储在同一位置(@see #AuthenticatedPrincipalOAuth2AuthorizedClientRepository.loadAuthorizedClient)
				Set<String> scopes = (Set<String>) CollectionUtils.defaultIfEmpty(clientRegistration.getScopes(), OAuth2ApplicationConstants.DEFAULT_OAUTH2_CLIENT_SCOPES.get(clientRegistration.getAuthorizationGrantType()));
				return new OAuth2PrincipalNameAuthentication(principalName, scopes.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
			} else if (principal instanceof AnonymousAuthenticationToken) {
				AnonymousAuthenticationToken anonAuthentication = (AnonymousAuthenticationToken) principal;
				//使用具名的Authentication而不是默认的AnonymousAuthenticationToken，使得获得到的新的OAuth2AuthorizedClient存储在同一位置(@see #AuthenticatedPrincipalOAuth2AuthorizedClientRepository.loadAuthorizedClient)
				return new OAuth2PrincipalNameAuthentication(principalName, anonAuthentication.getAuthorities());
			}
		}
		return principal;
	}
	
	/**
	 *  准备OAuth2登录(Client_credentials模式)的Authentication
	 * @param clientRegistration
	 * @param principal
	 * @param principalName
	 * @return
	 */
	public static Authentication prepareClientCredentialsOAuth2Authentication(ClientRegistration clientRegistration, Authentication principal, String principalName) {
		if(AuthorizationGrantType.CLIENT_CREDENTIALS.equals(clientRegistration.getAuthorizationGrantType())) {
			if(principal == null) {
				//使用具名的Authentication而不是默认的AnonymousAuthenticationToken，使得获得到的新的OAuth2AuthorizedClient存储在同一位置(@see #AuthenticatedPrincipalOAuth2AuthorizedClientRepository.loadAuthorizedClient)
				Set<String> scopes = (Set<String>) CollectionUtils.defaultIfEmpty(clientRegistration.getScopes(), OAuth2ApplicationConstants.DEFAULT_OAUTH2_CLIENT_SCOPES.get(clientRegistration.getAuthorizationGrantType()));
				return new OAuth2PrincipalNameAuthentication(principalName, scopes.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet()));
			} else if (principal instanceof AnonymousAuthenticationToken) {
				AnonymousAuthenticationToken anonAuthentication = (AnonymousAuthenticationToken) principal;
				//使用具名的Authentication而不是默认的AnonymousAuthenticationToken，使得获得到的新的OAuth2AuthorizedClient存储在同一位置(@see #AuthenticatedPrincipalOAuth2AuthorizedClientRepository.loadAuthorizedClient)
				return new OAuth2PrincipalNameAuthentication(principalName, anonAuthentication.getAuthorities());
			}
		}
		return principal;
	}
	
	/**
	 * 构造欺骗OAuth2AuthorizedClient
	 * 通过修改OAuth2AuthorizedClient中所持令牌的失效时间达到强制令牌立即失效的途径，来欺骗#RefreshTokenOAuth2AuthorizedClientProvider.hasTokenExpired(...)的校验
	 * 使#RefreshTokenOAuth2AuthorizedClientProvider立即刷新令牌
	 * @param oauth2AuthorizedClient
	 * @return
	 */
	public static OAuth2AuthorizedClient createTrickOAuth2AuthorizedClient(OAuth2AuthorizedClient realAuthorizedClient) {
		OAuth2AccessToken realAccessToken = realAuthorizedClient.getAccessToken();
		Instant expiresAt = OAuth2ApplicationConstants.DEFAULT_OAUTH2_CLIENT_CONFIG.getClock().instant(); //立即使其失效
		//构造一个欺骗令牌，使其能通过#RefreshTokenOAuth2AuthorizedClientProvider.hasTokenExpired(...)的校验认为该欺骗令牌是失效的，从而达到强制刷新令牌
		OAuth2AccessToken trickAccessToken = new OAuth2AccessToken(realAccessToken.getTokenType(), realAccessToken.getTokenValue(), realAccessToken.getIssuedAt(), expiresAt, realAccessToken.getScopes());
		//构造一个欺骗OAuth2AuthorizedClient，从而达到强制刷新令牌
		OAuth2AuthorizedClient trickAuthorizedClient = new OAuth2AuthorizedClient(realAuthorizedClient.getClientRegistration(), realAuthorizedClient.getPrincipalName(), trickAccessToken, realAuthorizedClient.getRefreshToken());
		return trickAuthorizedClient;
	}
	
}