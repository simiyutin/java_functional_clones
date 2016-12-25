package com.company;

import java.util.*;

public class Main2 {


    public static void main(String[] args) {
        List<Set<String>> words = new ArrayList<>();
        words.add(new HashSet<>(Arrays.asList("helloworld", "endswithhello", "bestplaceforhelloismiddle")));

        int starts = foo(words);
        int ends = bar(words);
        int contains = baz(words);
    }

    public static int foo(List<Set<String>> nested) {
        int count = 0;
        for (Set<String> element : nested) {
            if (! element.isEmpty()) {
                for (String str : element) {
                    if (str.startsWith("Hello")) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public static int bar(List<Set<String>> nested) {
        int count = 0;
        for (Set<String> element : nested) {
            if (element != null) {
                for (String str : element) {
                    if (str.endsWith("Hello")) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public static int baz(List<Set<String>> nested) {
        int count = 0;
        for (Set<String> element : nested) {
            if (element.size() < 10) {
                for (String str : element) {
                    if (str.contains("Hello")) {
                        count++;
                    }
                }
            }
        }

        return count;
    }
}
