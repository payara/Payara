package fish.payara.test.containers.tst.security.jar.jaspic;

import static javax.security.auth.message.AuthStatus.SEND_SUCCESS;
import static javax.security.auth.message.AuthStatus.SUCCESS;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A test SAM that always authenticates a hard-coded user "test" with role "architect" for every request.
 *
 * @author Arjan Tijms
 */
public class CustomSAM implements ServerAuthModule {
    public static final String RESPONSE_CUSTOMSAM_INVOKED = "CustomSAM.validateRequest invoked!\n";

    private static final Logger LOG = Logger.getLogger(CustomSAM.class.getName());

    private final Class<?>[] supportedMessageTypes = new Class[] {HttpServletRequest.class, HttpServletResponse.class};
    private CallbackHandler handler;

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler,
        Map options) throws AuthException {
        this.handler = handler;
        LOG.finest(() -> String.format("initialize(requestPolicy=%s, responsePolicy=%s, handler=%s, options=%s)",
            requestPolicy, responsePolicy, handler, options));
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
        throws AuthException {

        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        try {
            response.getWriter().write(RESPONSE_CUSTOMSAM_INVOKED);

            boolean isMandatory = Boolean
                .valueOf((String) messageInfo.getMap().get("javax.security.auth.message.MessagePolicy.isMandatory"));

            response.getWriter().write("isMandatory: " + isMandatory + "\n");

            handler.handle(new Callback[] {
                new CallerPrincipalCallback(clientSubject, "test"),
                new GroupPrincipalCallback(clientSubject, new String[] { "architect" }) });
        } catch (IOException | UnsupportedCallbackException e) {
            throw (AuthException) new AuthException().initCause(e);
        }
        return SUCCESS;
    }

    @Override
    public Class<?>[] getSupportedMessageTypes() {
        return supportedMessageTypes;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {

        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        try {
            response.getWriter().write("secureResponse invoked\n");
        } catch (IOException e) {
            throw (AuthException) new AuthException().initCause(e);
        }

        return SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();
        try {
            response.getWriter().write("cleanSubject invoked\n");
        } catch (IOException e) {
            throw (AuthException) new AuthException().initCause(e);
        }
    }
}