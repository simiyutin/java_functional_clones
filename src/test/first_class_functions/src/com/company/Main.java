package com.company;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args, Predicate<Invoice> invoicePredicate) {
	ArrayList<Invoice> allInvoices= new ArrayList<>();

        allInvoices.add(new Invoice());

        List<Invoice> test2 = findInvoicesFromMicrosoft(allInvoices, invoice -> invoice.getCustomer() == Customer.MICROSOFT);
        List<Invoice> test3 = findInvoicesFromOracle(allInvoices, invoice -> invoice.getCustomer() == Customer.ORACLE);
    }

    public static List<Invoice> findInvoicesFromOracle(List<Invoice> invoices, Predicate<Invoice> azaza) {
        List<Invoice> result = invoices.stream().filter(azaza).collect(Collectors.toList());
        return result;
    }

    public static  List<Invoice> findInvoicesFromMicrosoft(List<Invoice> invoices, Predicate<Invoice> azaza) {
        List<Invoice> result = invoices.stream().filter(azaza).collect(Collectors.toList());
        return result;
    }
}

    /*
    public static void main(String[] args, Predicate<Invoice> invoicePredicate) {
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
    */
