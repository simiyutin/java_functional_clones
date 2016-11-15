package com.company;

import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {

        ArrayList<String> test1 = new ArrayList<>();
        test1.add("asas");
        test1.add("asas");
        test1.add("asas");
        test1.add("asasas");


        for (int i = 0; i < test1.size(); i++) {
            if (test1.get(i).equals("asasas")) {
                System.out.println(test1.get(i));
            }
        }

        for (String element: test1) {
            if (element.equals("asasas")) {
                System.out.println(element);
            }
        }

        //////////////// MUST FAIL //////////////////////

        for (int i = 0; i < test1.size(); i++) {
            if (test1.get(i).equals("asasas")) {
                return;
            }
        }

        for (int i = 0; i < test1.size(); i++) {
            if (test1.get(i).equals("asasas")) {
                test1 = new ArrayList<>();
            }
        }

        for (int i = 0; i < test1.size(); i--) {
            if (test1.get(i).equals("asasas")) {
                System.out.println(test1.get(i));
            }
        }


    }
}
