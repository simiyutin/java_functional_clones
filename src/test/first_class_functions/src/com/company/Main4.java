package com.company;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Main4 {

    static void bar() {
        foo(Container::callme);
    }

    static void foo(Function<Container, String> mapper) {
        List<String> messageIds;
        List<Container> msgIdRaw = new ArrayList<>();

        messageIds = msgIdRaw.stream().map(mapper).collect(Collectors.toList());
    }



}
