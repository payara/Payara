package fish.payara.appserver.fang.service.adapter;

/**
 *
 * @author Andrew Pielage
 */
public enum PayaraFangAdapterState {
    NOT_LOADED("state.notLoaded", "Payara Fang is registered in the config but not loaded yet"),
    LOADING("state.loading", "Payara Fang is loading"),
    LOADED("state.loaded", "Payara Fang is loaded"),
    UNINITIALISED("state.uninitialised", "Payara Fang has not been initialised yet"),
    REGISTERING("state.registering", "Payara Fang is being registered as a system application"),
    NOT_REGISTERED("state.notRegistered", "Payara Fang is not registered in the config"),
    RECONFIGURING("state.reconfiguring", "Payara Fang system-application entry is being reconfigured"),
    WELCOME_TO("status.welcometo", "Welcome to ");
    
    private final String desc;
    private final String i18nKey;
    
    private PayaraFangAdapterState(String i18nKey, String desc) {
	this.i18nKey = i18nKey;
        this.desc = desc;
    }

    /**
     *	This is the key that should be used to retrieve the localised message from a properties file.
     */
    public String getI18NKey() {
	return i18nKey;
    }
    
    @Override
    public String toString() {
        return (desc);
    }
}
