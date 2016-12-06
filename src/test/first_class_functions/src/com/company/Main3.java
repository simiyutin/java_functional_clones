package com.company;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

class Destination {
    void addAnnotation(String key, Annotation annotation){};
    void doImportantBusiness(List<Annotation> annotations) {}
    static void doImportantBusinessWithEntry(Map.Entry<String, List<Annotation>> e) {}
    static void doAnotherImportantBusinessWithEntry(Map.Entry<String, List<Annotation>> e) {}
}

public class Main3 {

    Map<String, List<Annotation>> getAnnotations() {
        return null;
    }

    void caller() {
        addNotEmptyAnnotations();
        printDatabaseAnnotations();
        doOneBusinessWithEachAnnotations();
        doAnotherBusinessWithAnnotations();
    }



    void addNotEmptyAnnotations() {

        Destination destination = new Destination();

        for (Map.Entry<String, List<Annotation>> e : getAnnotations().entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                for (Annotation annotation: e.getValue()) {
                    destination.addAnnotation(e.getKey(), annotation);
                }
            }
        }

        for (Map.Entry<String, List<Annotation>> e : getAnnotations().entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                Destination.doImportantBusinessWithEntry(e);
            }
        }
    }

    void doOneBusinessWithEachAnnotations() {

        for (Map.Entry<String, List<Annotation>> e : getAnnotations().entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                Destination.doImportantBusinessWithEntry(e);
            }
        }
    }

    void doAnotherBusinessWithAnnotations() {

        for (Map.Entry<String, List<Annotation>> e : getAnnotations().entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) {
                Destination.doAnotherImportantBusinessWithEntry(e);
            }
        }
    }


    void printDatabaseAnnotations() {

        for (Map.Entry<String, List<Annotation>> e : getAnnotations().entrySet()) {
            if (e.getKey().contains("database")) {
                for (Annotation annotation: e.getValue()) {
                    System.out.println("annotation is used for mapping database fields!");
                }
            }
        }
    }

}
