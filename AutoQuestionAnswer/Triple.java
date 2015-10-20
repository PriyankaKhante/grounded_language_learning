/*
 *  A generic triple class, with specific var/method labels for clarity.
 *  Though generic, has a specific usage in the the context of question answering
 *  ex usage: Triple<String, ArrayList<Integer>, Integer> myTiple = Triple.createTriple("label", new ArrayList<Integer>(), 0)
 */

public class Triple<F, S, T> {
    private F label; //first member of pair
    private S object_nums; //second member of pair
    private T question_num; //third member of pair

    public Triple(F label, S object_nums) {
        this.label = label;
        this.object_nums = object_nums;
    }

    public void setLabel(F label) {
        this.label = label;
    }

    public void setObjectNums(S object_nums) {
        this.object_nums = object_nums;
    }
    public void setQuestionNum(S object_nums) {
        this.question_num = question_num;
    }

    public F getLabel() {
        return label;
    }

    public S getObjectNums() {
        return object_nums;
    }

    public T getQuestionNum() {
        return question_num;
    }
}