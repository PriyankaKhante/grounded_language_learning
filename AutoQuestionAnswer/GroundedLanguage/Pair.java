package GroundedLanguage;
/*
 *  A generic pair class, with specific var/method labels for clarity.
 *  Ex. Pair<String, String> myPair = Pair.createPair("drop_audio", "material")
 */

public class Pair{
    private String context; //first member of pair
    private String attribute; //second member of pair

    public Pair(String context, String attribute) {
        this.context = context;
        this.attribute = attribute;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getContext() {
        return context;
    }

    public String getAttribute() {
        return attribute;
    }
}