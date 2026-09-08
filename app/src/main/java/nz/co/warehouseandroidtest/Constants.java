package nz.co.warehouseandroidtest;

public class Constants {
    public static final String HTTP_URL_ENDPOINT = "https://twg.azure-api.net/";
    public static final String PREF_USER_ID = "userId";
    public static final int BRANCH_ID = 208;
    // Injected at build time from local.properties (twg.subscriptionKey) or the
    // TWG_SUBSCRIPTION_KEY environment variable. See app/build.gradle.
    public static final String SUBSCRIPTION_KEY = BuildConfig.SUBSCRIPTION_KEY;
    public static final String MACHINE_ID = "1234567890";
}
