public class Item {
    private String vaccineID;
    private String label;
    private String productName;
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
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
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
        if(productName!=null) tmp += ", product name=" + productName;
        if(tradeName!=null) tmp += ", trade name=" + tradeName;
        return  tmp+"]";
    }
}
