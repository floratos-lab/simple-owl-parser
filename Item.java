public class Item {
    private String vaccineID;
    private String label;
    private String vaccineProperName;
    private String tradeName;

    public String getVaccineID() {
        return vaccineID;
    }

    public void setVaccineID(String vaccineID) {
        this.vaccineID = vaccineID;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getVaccineProperName() {
        return vaccineProperName;
    }

    public void setVaccineProperName(String vaccineProperName) {
        this.vaccineProperName = vaccineProperName;
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    @Override
    public String toString() {
        String tmp = "[vaccineID=" + vaccineID + ", label=" + label;
        if (vaccineProperName != null)
            tmp += ", vaccine proper name=" + vaccineProperName;
        if (tradeName != null)
            tmp += ", trade name=" + tradeName;
        return tmp + "]";
    }
}
