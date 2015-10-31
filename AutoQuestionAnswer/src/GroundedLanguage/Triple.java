package GroundedLanguage;
import java.util.ArrayList;
/*
 *  A generic triple class, with specific var/method labels for clarity.
 *  Though generic, has a specific usage in the the context of question answering
 *  ex usage: Triple<String, ArrayList<Integer>, Integer> myTiple = Triple.createTriple("label", new ArrayList<Integer>(), 0)
 */

public class Triple{
    private String label; //first member of pair
    private ArrayList<String> objects; //second member of pair
    private int question_num; //third member of pair

    public Triple(String label, ArrayList<String> objects, int question_num) {
        this.label = label;
        this.objects = objects;
        this.question_num = question_num;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setObjects(ArrayList<String> objects) {
        this.objects = objects;
    }
    public void setQuestionNum(int object_nums) {
        this.question_num = question_num;
    }

    public String getLabel() {
        return label;
    }

    public ArrayList<String> getObjects() {
        return objects;
    }

    public int getQuestionNum() {
        return question_num;
    }
}