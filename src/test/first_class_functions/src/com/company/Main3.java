package com.company;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

class Destination {
    void addAnnotation(String key, Annotation annotation){};
    void doImportantBusiness() {}
}

public class Main3 {

    Map<String, List<Annotation>> getAnnotations() {
        return null;
    }



    void test() {

        Destination destination = new Destination();

        for (Map.Entry<String, List<Annotation>> e : getAnnotations().entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                for (Annotation annotation: e.getValue()) {
                    destination.addAnnotation(e.getKey(), annotation);
                }
            }
        }
    }

    void test2() {

        Destination destination = new Destination();

        for (Map.Entry<String, List<Annotation>> e : getAnnotations().entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                destination.doImportantBusiness();
            }
        }
    }

    void test3() {

        Destination destination = new Destination();

        for (Map.Entry<String, List<Annotation>> e : getAnnotations().entrySet()) {
            if (e.getKey().contains("database")) {
                for (Annotation annotation: e.getValue()) {
                    System.out.println("annotation is used for mapping database fields!");
                }
            }
        }
    }

}
