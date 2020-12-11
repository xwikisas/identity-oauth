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
  var p = window.location.href.indexOf('/admin');
  var targetRedirectUrl = (p == -1 ?
      window.location.href :
      window.location.href.substring(0, p)) + "/login/XWiki/XWikiLogin"

  function resetRedirectUrl()
  {
    redirectUrlInput.value = targetRedirectUrl;
  }

  function redirectUrlChanged()
  {
    var matches = (redirectUrlInput.value === targetRedirectUrl);
    jQuery("#redirectUrlAsExpected")[0].style.display = matches ? "inline" : "none";
    jQuery("#redirectUrlNotAsExpected")[0].style.display = matches ? "none" : "inline";
  }

  function validateFields() {
    var checkbox = jQuery("input[name$='_active']")[0];
    if(jQuery(checkbox).prop("checked")) {
      var passed = true;
      passed = passed && jQuery(jQuery("input[name$='_clientid']")[0]).val() !== "";
      passed = passed && jQuery(jQuery("input[name$='_secret'  ]")[0]).val() !== "";
      passed = passed && jQuery(jQuery("input[name$='_tenantid']")[0]).val() !== "";
      passed = passed && jQuery(jQuery("input[name$='_redirectUrl']")[0]).val() !== "";
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

    jQuery("form.xform input").each(()=>{jQuery(this).change(validateAndUpdate)});

    redirectUrlInput = jQuery(prefix + 'redirectUrl')[0];
    if (redirectUrlInput.value === "") {
      resetRedirectUrl();
    }

    jQuery(prefix + 'redirectUrl').change(redirectUrlChanged);
    jQuery(prefix + 'redirectUrl').keyup(redirectUrlChanged);
    redirectUrlChanged();
    validateAndUpdate();


    jQuery('#IdentityOAuth_IdentityOAuth\.IdentityOAuthConfig').submit(cancelCancelEdit);

  }).defer();
});
