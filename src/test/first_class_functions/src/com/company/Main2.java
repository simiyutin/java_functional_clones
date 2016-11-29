package com.company;

import java.util.*;

/**
 * Created by boris on 23.11.16.
 */
public class Main2 {


    public static void main(String[] args) {
        List<Set<String>> words = new ArrayList<>();
        words.add(new HashSet<>(Arrays.asList("helloworld", "endswithhello", "bestplaceforhelloismiddle")));

        int starts = startsWithHello(words);
        int ends = endsWithHello(words);
        int contains = containsHello(words);
    }

    public static int startsWithHello(List<Set<String>> nested) {
        int count = 0;
        for (Set<String> element : nested) {
            if (element != null) {
                for (String str : element) {
                    if (str.startsWith("Hello")) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public static int endsWithHello(List<Set<String>> nested) {
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

    public static int containsHello(List<Set<String>> nested) {
        int count = 0;
        for (Set<String> element : nested) {
            if (element != null) {
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
