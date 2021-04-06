# OAuth-Identity Development Roadmap

## Version 1.0

* Include a generic OAuth driver
	* Documentation _How to create my own provider?_
* Support single-sign on (at least [Azure Active Directory](https://github.com/xwikisas/integration-azure-oauth/))
    * Compatibility with objects created by the [OpenIDConnect](extensions.xwiki.org/xwiki/bin/view/Extension/OpenID+Connect/)
	  so that using either integration with OIDC or AzureAD offers a similar single-sign-on experience 
* Only activate authenticator if module is active	

## Version 1.1

* Migrate to use ResourceHandlere instead of JavaScript modifying the login pag
* Support Office365 application (an extension of IdentityOAuth)
* Support GoogleApps (an extension of IdentityOAuth)

## Version 2.0

* Wallet of tokens made available through an API broader than the IdentityOAuth providers.
* Migrate to use [OpenIDConnect](extensions.xwiki.org/xwiki/bin/view/Extension/OpenID+Connect/) 
   instead of or complementary to the ScribeJava library. Include group synchronization as a feature.
* Support a two-factor-authentication as well
* Approach solutions to arbitrate between the various authenticators (thus far, ActiveDirectory, the default one, IdentityOAuth)
