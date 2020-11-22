import java.util.LinkedList;
import java.lang.StringBuilder;

public class CircularQueue<E> extends LinkedList<E> {
    private int limit;

    public CircularQueue(int limit) {
        this.limit = limit;
    }

    @Override
    public boolean add(E o) {
        super.add(o);
        while (size() > limit) { super.remove(); }
        return true;
    }

    public String getString() {
        StringBuilder st = new StringBuilder();
        while(!super.isEmpty()) {
            st.append(super.remove() + " ");
        }
        return st.toString();
    }
}