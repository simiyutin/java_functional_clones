package com.company;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
	// write your code here
    }

    public List<Invoice> findInvoicesFromOracle(List<Invoice> invoices) {
        List<Invoice> result = invoices.stream().filter(invoice -> invoice.getCustomer() == Customer.ORACLE).collect(Collectors.toList());
        return result;
    }

    public List<Invoice> findInvoicesFromMicrosoft(List<Invoice> invoices) {
        List<Invoice> result = invoices.stream().filter(invoice -> invoice.getCustomer() == Customer.MICROSOFT).collect(Collectors.toList());
        return result;
    }

}

    /*
    *     public List<Invoice> findInvoicesFromOracle(List<Invoice> invoices) {
        List<Invoice> result = new ArrayList<>();
        for(Invoice invoice: invoices) {
            if(invoice.getCustomer() == Customer.ORACLE) {
                result.add(invoice);
            }
        }
        return result;
    }

    public List<Invoice> findInvoicesFromMicrosoft(List<Invoice> invoices) {
        List<Invoice> result = new ArrayList<>();
        for(Invoice invoice: invoices) {
            if(invoice.getCustomer() == Customer.MICROSOFT) {
                result.add(invoice);
            }
        }
        return result;
    }*/
