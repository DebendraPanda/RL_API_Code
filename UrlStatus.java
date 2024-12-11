package test;

public class UrlStatus {
    private boolean isValid;
    private String classification;

    public UrlStatus(boolean isValid, String classification) {
        this.isValid = isValid;
        this.classification = classification;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getClassification() {
        return classification;
    }
}
