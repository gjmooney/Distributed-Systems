package taskone;

import java.util.List;
import java.util.ArrayList;

class StringList {
    
    List<String> strings = new ArrayList<String>();

    synchronized public void add(String str) {
        int pos = strings.indexOf(str);
        if (pos < 0) {
            strings.add(str);
        }
    }

    synchronized public boolean contains(String str) {
        return strings.indexOf(str) >= 0;
    }

    synchronized public String pop() {
        if (strings.isEmpty()) {
            return "null";
        } else {
            return strings.get(strings.size() - 1);
        }
    }

    synchronized public String switchString(String dataString) {
        String[] indices = dataString.split(" ");
        int first = Integer.parseInt(indices[0]);
        int second = Integer.parseInt(indices[1]);

        if (first < 0 || first > size() - 1
        || second < 0 || second > size() - 1) {
            return "null";
        } else {
            String temp = strings.get(first);
            strings.set(first, strings.get(second));
            strings.set(second, temp);

            return toString();
        }

    }

    synchronized public int size() {
        return strings.size();
    }

    synchronized public String toString() {
        return strings.toString();
    }
}