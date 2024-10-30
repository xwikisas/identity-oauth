# OAuth Identity XWiki App (Pro)

* Project Lead: N/A
* Communication: [Mailing List](http://dev.xwiki.org/xwiki/bin/view/Community/MailingLists>), [Chat](https://dev.xwiki.org/xwiki/bin/view/Community/Chat)
* [Development Practices](http://dev.xwiki.org)
* Minimal XWiki version supported: XWiki 14.10
* License: LGPL 2.1+
* Translations: (done in the source within the `Translation*.properties` files of [the IdentityOAuth directory](ui/src/main/resources/IdentityOAuth))
* Sonar Dashboard: N/A
* Continuous Integration Status: [![Build Status](http://ci.xwikisas.com/view/All/job/xwikisas/job/identity-oauth/job/master/badge/icon)](http://ci.xwikisas.com/view/All/job/xwikisas/job/identity-oauth/job/master/)

- - -

# How to create my own provider for Identity OAuth

* Decide on a short and sweet name
* Follow the steps on creating an
  [XWiki Extension](https://www.xwiki.org/xwiki/bin/view/Documentation/DevGuide/Tutorials/CreatingExtensions/)
  (you will need both a Xar extension depending on a Jar extension specific to the requiremeents below)
* this will be stored in an XWiki space where you include in WebHome all necessary resources and should be stored as a maven project of pacakging xar
* create an API project with a Java class implementing com.xwiki.identityoauth.IdentityOAuthProvider
  Make it a [component](https://www.xwiki.org/xwiki/bin/view/Documentation/DevGuide/Tutorials/WritingComponents/)
  and assign it the annotation `@Named("name")` (with `name` replaced by your chosen short and seet name)
* create a provider page in the space and add the XWiki object of XClass IdentityOAuth.OAuthProvider
    * give it the (short and sweet) name
    * give it a `loginCode`: the property's content will be rendered and embedded in the login page with the
      following strings replaced:
        * `-URL-`: The URL of the login page of this wiki (configured later)
        * `-PROVIDER-`: The short and sweet name
    * indicate the configuration page reference so it can be reloaded
    * indicate the order hint to decide on the order of the login buttons (XWiki's login is always there and order 0).
* create a configuration page in the space and add the XWiki object of XClass IdentityOAuthConfigClass
* you should be ready to run already by logging out and trying to go to the identity provider and coming back

* (optional) create a configuration sheet by creating a wiki page to be rendered (a sheet) and adding an
  XWiki.ConfigurableClass XWiki object. In the `codeToExecute` property, include
  `{{include reference="Space.ConfigSheet" /}}` replacing appropriately to include the sheet. This will allow
  administrators to configure the service (e.g. including API client and secrets).
    * Let this sheet start with `{{include reference="IdentityOAuth.IdentityOAuthConfigMacros" /}}` (outside of
      a `{{velocity}}` macro) and invoke the macro
      `initConfigObjects("Space.ConfigDoc", "Space.ExtraConfigClass","translationPrefix_")` where `ExtraConfig`
      is an XWiki object to store the provider specific information.
    
