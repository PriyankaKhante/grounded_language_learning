/*
 *  A generic triple class, with specific var/method labels for clarity.
 *  Though generic, has a specific usage in the the context of question answering
 *  ex usage: Triple<String, ArrayList<Integer>, Integer> myTiple = Triple.createTriple("label", new ArrayList<Integer>(), 0)
 */

public class Triple<F, S, T> {
    private F label; //first member of pair
    private S objects; //second member of pair
    private T question_num; //third member of pair

    public Triple(F label, S objects, T question_num) {
        this.label = label;
        this.objects = objects;
        this.question_num = question_num;
    }

    public void setLabel(F label) {
        this.label = label;
    }

    public void setObjects(S objects) {
        this.objects = objects;
    }
    public void setQuestionNum(S object_nums) {
        this.question_num = question_num;
    }

    public F getLabel() {
        return label;
    }

    public S getObjects() {
        return object_nums;
    }

    public T getQuestionNum() {
        return question_num;
    }
}