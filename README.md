# java_functional_clones

Goal of this project is to learn how to find chunks of java source code which can be functionally refactored and become clones after this refactoring. 
Then we're going to use existing clones find'n'replace algorithms to refactor obtained clones automatically.

IntelliJ IDEA has been chosen as development environment and provider of base refactoring mechanisms.

STEPS:

1. At first, we are using builtin IDEA for-loops to stream API refactorings to easily fild places which can be replaced by predicates, e.g. .filter(/*here comes piece of code which surely can be replaced by Predicate variable*/)
