require(['jquery'], function (jQuery) {

  window.iocObj = this;
  var prefix = '#IdentityOAuth\\.IdentityOAuthConfigClass_0_';

  //deactivating
  function deactive(elts)
  {
    elts.each(function () {
      jQuery(jQuery(this).closest('dl')).find('label').css('color', 'darkgrey');
      if(!jQuery(this).hasClass("mandatory"))
        jQuery(this).prop('disabled', true);
    });
  }

  // reactivating
  function reactive(elements)
  {
    jQuery(elements).each(function () {
      jQuery(jQuery(this).closest('dl')).find('label').css('color', 'black');
      if(!jQuery(this).hasClass("mandatory"))
        jQuery(this).prop('disabled', false);
    });
  }

  // updaters
  function updateAllInputs()
  {
    if (this.checked) {
      reactive(allInputs);
    } else {
      deactive(allInputs);
    }
  }

  function updateCookieFields()
  {
    if (this.checked) {
      reactive(cookieInputs);
    } else {
      deactive(cookieInputs);
    }
  }

  var allInputs, cookieInputs;
  var hintTextOn = "${escapetool.javascript($services.localization.render('IdentityOAuth.IdentityOAuthConfigClass_domain.hintTextOn'))}",
      hintTextOff = "${escapetool.javascript($services.localization.render('IdentityOAuth.IdentityOAuthConfigClass_domain.hintTextOff'))}",
      hintTextInvaliddomain = "${escapetool.javascript($services.localization.render('IdentityOAuth.IdentityOAuthConfigClass_domain.hintTextInvaliddomain'))}";

  function updateScopes()
  {
    var scopes = "";
    jQuery(("input[name^='scope_']")).each(function () {
      if (this.checked) {
        scopes = scopes + " " + this.name.substring('scope_'.length);
      }
    });
    jQuery("input[name$='_scope']").val(scopes);
  }

  function readScopes()
  {
    var scope = jQuery("input[name$='_scope']").val();
    jQuery(("input[name^='scope_']")).each(function () {
      if (jQuery.attr(this, "disabled") !== "disabled") {
        var n = this.name.substring('scope_'.length);
        if (scope.indexOf(n) > -1) {
          this.checked = true;
        } else {
          this.checked = false;
        }
      }
    });
  }

  var redirectUrlInput;
  // note: see macro calcRedirectUrlsJS() which calculates browser's and server's return URLs
  // defining serverReturnUrl, browserReturnUrl at the window
  // in some proxy cases the browser and server calculated URLs are not the same
  // because consistency for the return URL is primordial for OAuth, a possibility to adjust this URL
  // is provided.


  function resetRedirectUrl() {
    redirectUrlInput.value = browserReturnUrl;
    return false;
  }
  function resetRedirectUrlBrowser() {
    redirectUrlInput.value = window.browserReturnUrl;
    return false;
  }
  function resetRedirectUrlServer() {
    redirectUrlInput.value = window.serverReturnUrl;
    return false;
  }
  window.resetRedirectUrl = resetRedirectUrl;
  window.resetRedirectUrlBrowser = resetRedirectUrlBrowser;
  window.resetRedirectUrlServer = resetRedirectUrlServer;


  function redirectUrlChanged()
  {
    var serv = window.serverReturnUrl, brows = window.browserReturnUrl, val = redirectUrlInput.value;
    if( val === '') {
      console.log("RedirectUrlChanged: empty value");
      jQuery("#redirectUrlAsExpected")[0].style.display = "none";
      jQuery("#redirectUrlAsServers")[0].style.display =   "none";
      jQuery("#redirectUrlAsBrowsers")[0].style.display =  "none";
      jQuery("#redirectUrlDifferentOfAll")[0].style.display =  "none";
      jQuery("#redirectMakeItLikePredicted")[0].style.display = "none";
      jQuery("#redirectMakeItLikePredictedBrowser")[0].style.display = "none";
      jQuery("#redirectMakeItLikePredictedServer")[0].style.display =   "none";
    } else if( serv === brows ) {
      console.log("RedirectUrlChanged: accordance");
      jQuery("#redirectUrlAsExpected")[0].style.display = brows === val ? "inline" : "none";
      jQuery("#redirectUrlAsServers")[0].style.display =   "none";
      jQuery("#redirectUrlAsBrowsers")[0].style.display =  "none";
      jQuery("#redirectUrlDifferentOfAll")[0].style.display =  brows !== val ? "inline" : "none";
      jQuery("#redirectMakeItLikePredicted")[0].style.display = brows !== val ? "inline" : "none";
      jQuery("#redirectMakeItLikePredictedBrowser")[0].style.display = "none";
      jQuery("#redirectMakeItLikePredictedServer")[0].style.display =   "none";
    } else {
      console.log("RedirectUrlChanged: disonance");
      jQuery("#redirectUrlAsExpected")[0].style.display =  "none";
      jQuery("#redirectUrlAsServers")[0].style.display =  serv === val ? "inline" : "none";
      jQuery("#redirectUrlAsBrowsers")[0].style.display =  brows === val ? "inline" : "none";
      jQuery("#redirectUrlDifferentOfAll")[0].style.display = serv !== val && brows !== val ? "inline" : "none";
      jQuery("#redirectMakeItLikePredicted")[0].style.display =  "none";
      jQuery("#redirectMakeItLikePredictedBrowser")[0].style.display =  bbrows !== val ? "inline" : "none";
      jQuery("#redirectMakeItLikePredictedServer")[0].style.display =  serv !== val ? "inline" : "none";
    }
  }

  function validateFields() {
    var checkbox = jQuery("input[name$='_active']")[0];
    if(jQuery(checkbox).prop("checked")) {
      var passed = true;
      passed = passed && jQuery(jQuery("input[name$='_clientid']")[0]).val() !== "";
      passed = passed && jQuery(jQuery("input[name$='_secret'  ]")[0]).val() !== "";
      passed = passed && jQuery(jQuery("input[name$='_tenantid']")[0]).val() !== "";
      return passed;
    } else { // deactivated: can save
      return true;
    }
  }

  function validateAndUpdate() {
    var passed = validateFields();
    if(passed) {
      jQuery("#warningIncomplete").hide();
      jQuery("input[name='formactionsac']").prop("disabled", false);
    } else {
      jQuery("#warningIncomplete").show();
      jQuery("input[name='formactionsac']").prop("disabled", true);
    }
  }

  (function () {
    // register listeners
    cookieInputs = jQuery(prefix + 'skipLoginPage, ' + prefix + 'authWithCookies, ' + prefix + 'cookiesTTL');
    jQuery(prefix + 'useCookies').each(updateCookieFields).change(updateCookieFields);

    var keepThem = "name$='_active'],[name='formactionsac'],[name='form_token']," +
        "[name='xcontinue'],[name='xredirect'";

    allInputs = jQuery(
        '.xform input:not([' + keepThem + '])');
    jQuery('input[name$="_active"][type="checkbox"]').each(updateAllInputs).change(updateAllInputs);

    jQuery(("input[name^='scope_']")).each(function () {
      jQuery(this).change(function () {
        updateScopes();
      });
    });
    readScopes();

    jQuery("form.xform input").each(function() {jQuery(this).change(validateAndUpdate)});

    redirectUrlInput = jQuery(prefix + 'redirectUrl')[0];
    redirectUrlInput.placeholder = window.placeHolderForRedirectUrl;

    console.log("redirectUrlInput: " + jQuery(prefix + 'redirectUrl'));
    jQuery(prefix + 'redirectUrl').change(redirectUrlChanged);
    jQuery(prefix + 'redirectUrl').keyup(redirectUrlChanged);
    jQuery(prefix + 'redirectUrl').blur(redirectUrlChanged);
    redirectUrlChanged();
    validateAndUpdate();


    jQuery('#IdentityOAuth_IdentityOAuth\.IdentityOAuthConfig').submit(cancelCancelEdit);

  }).defer();
});
