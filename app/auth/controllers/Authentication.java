package auth.controllers;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import auth.utils.SAMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;
import auth.AuthFactory;
import auth.Configuration;
import auth.IAuthModule;
import auth.WebSession;

/**
 * Handles user authentication.
 * It requires configuration of the authentication module via util.Config.getAuthnHandler();
 */
public class Authentication extends Controller {

    private static Logger logger = LoggerFactory.getLogger(Authentication.class);

    /**
     * Login using JAAS.
     * Invoked by security authentication so the context must be passed as argument.
     *
     * @param ctx - HTTP context
     * @return
     */
    public static Result login(Context ctx) {
        IAuthModule auth = AuthFactory.getAuthenticator(Configuration.getInstance().authnHandler);
        if (auth == null) {
            return badRequest("Failed loading Authentication module!");
        }
        try {
            CallbackHandler cbh = auth.getCallbackHandler(ctx);
            LoginContext lc = null;
            if (cbh == null) {
                // must set a default callback handler in configuration
                lc = new LoginContext(auth.getModuleName());
            } else {
                lc = new LoginContext(auth.getModuleName(), cbh);
            }
            lc.login();
            return auth.onAuthSucceeded(lc.getSubject());
        } catch (LoginException e) {
            e.printStackTrace();
            return auth.onAuthFailed(e);
        } catch (SecurityException e) {
            return auth.onAuthFailed(e);
        }
    }

    public static Result logout() {
        WebSession.removeSession(session("uuid"));
        session().clear();
        if (Configuration.getInstance().ssoLogout) {
            IAuthModule auth = AuthFactory.getAuthenticator(Configuration.getInstance().authnHandler);
            if (auth != null) {
                if (auth.equals(Configuration.HANDLER_SAML2)) {
                    // TODO: get configuration and invoke logout
                } else if (auth.equals(Configuration.HANDLER_OPENAM)) {
                    // TODO: get configuration and invoke logout
                }
            }
        }
        //Authenticate.logout() // TODO
        return Controller.redirect(Configuration.getInstance().urlLogout);
    }

    /**
     * Used for Federated Authentication (SAML2). Process the AuthnResponse from IdP.
     *
     * @return
     */
    public static Result samlAuthnResponse() {
        logger.debug("samlAuthnResponse()");
        return login(ctx());
    }

    public static Result samlMetadata() {
        if(!Configuration.HANDLER_SAML2.equals(Configuration.getInstance().authnHandler)) {
            return notFound();
        }

        logger.debug("samlMetadata()");
        SAMLUtils samlUtils = SAMLUtils.getInstance();

        AppConfigurationEntry[] federatedAuths = javax.security.auth.login.Configuration.getConfiguration().getAppConfigurationEntry("FederatedAuth");
        if(federatedAuths.length == 0) {
            return internalServerError();
        }
        AppConfigurationEntry federatedAuth = federatedAuths[0];
        samlUtils.setAssertionConsumerServiceUrl(federatedAuth.getOptions().get(SAMLUtils.ATTR_CONSUMER_URL).toString());
        samlUtils.setSamlIssuerUrl(federatedAuth.getOptions().get(SAMLUtils.ATTR_ISSUER).toString());

        String metadata = samlUtils.buildSAMLMetadata();
        if(metadata == null)
            return internalServerError();

        return ok(metadata).as("application/xml");
    }

}