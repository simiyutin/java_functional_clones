package com.company;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        ArrayList<Invoice> allInvoices= new ArrayList<>();

        allInvoices.add(new Invoice());

        List<Invoice> test2 = findInvoicesFromMicrosoft(allInvoices);
        List<Invoice> test3 = findInvoicesFromOracle(allInvoices);
    }

    public static List<Invoice> findInvoicesFromOracle(List<Invoice> invoices) {
        List<Invoice> result = new ArrayList<>();
        for(Invoice invoice: invoices) {
            if(invoice.getCustomer() == Customer.ORACLE) {
                result.add(invoice);
            }
        }
        return result;
    }

    public static  List<Invoice> findInvoicesFromMicrosoft(List<Invoice> invoices) {
        List<Invoice> result = new ArrayList<>();
        for(Invoice invoice: invoices) {
            if(invoice.getCustomer() == Customer.MICROSOFT) {
                result.add(invoice);
            }
        }
        return result;
    }
}