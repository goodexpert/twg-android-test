package nz.co.warehouseandroidtest.data;

import java.util.List;

public class User {
    public String customerId;
    // Always empty for guest sessions, so the element type is unconfirmed.
    public List<String> preferredBranchIds;
    public boolean eReceiptsPreferred;
    public boolean isTeamMember;
    public boolean isStaff;
    public boolean masterEmailOptIn;
    public String expiresDatetime;
    public int expiryMinutes;
    public boolean guest;
    public String platformDemandWare;
    public String environment;
    public boolean developmentPlatform;
    public double apiVersion;
    public double requestedApiVersion;
}
