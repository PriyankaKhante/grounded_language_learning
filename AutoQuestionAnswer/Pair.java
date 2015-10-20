/*
 *  A generic pair class, with specific var/method labels for clarity.
 *  Ex. Pair<String, String> myPair = Pair.createPair("drop_audio", "material")
 */

public class Pair<F, S> {
    private F context; //first member of pair
    private S attribute; //second member of pair

    public Pair(F context, S attribute) {
        this.context = context;
        this.attribute = attribute;
    }

    public void setContext(F context) {
        this.context = context;
    }

    public void setAttribute(S attribute) {
        this.attribute = attribute;
    }

    public F getContext() {
        return context;
    }

    public S getAttribute() {
        return attribute;
    }
}